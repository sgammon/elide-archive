package gust.backend.model

import tools.elide.core.Datamodel
import tools.elide.core.FieldType as CoreFieldType
import com.google.cloud.firestore.DocumentReference
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import gust.backend.model.ModelDeserializer.DeserializationError
import gust.backend.runtime.Logging
import java.nio.charset.StandardCharsets
import java.util.*
import javax.annotation.Nonnull


/**
 * Specifies a deserializer which is capable of converting generic Java [Map] objects (expected to have
 * [String] keys) into arbitrary [Message] types.
 *
 * @param <Model> Model record type which this serializer is responsible for converting.
 */
class ObjectModelDeserializer<Model: Message> private constructor(
  /** Default model instance to spawn builders from. */
  private val defaultInstance: Model): ModelDeserializer<Map<String, *>, Model> {
  companion object {
    /** Private logging pipe. */
    private val logging = Logging.logger(ObjectModelDeserializer::class.java)

    /**
     * Return an object model deserializer tailored to the parameterized model specified with `M`, with the specified
     * deserialization settings.
     *
     * @param <M> Model type to acquire an object model serializer for.
     * @return Deserializer, customized to the specified type.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun <M: Message> withSettings(instance: M): ObjectModelDeserializer<M> {
      return ObjectModelDeserializer(instance)
    }

    /**
     * Return an object model de-serializer tailored to the parameterized model specified with `M`, with default
     * selections for deserialization settings.
     *
     * @param <M> Model type to acquire an object model deserializer for.
     * @return Deserializer, customized to the specified type.
     */
    fun <M: Message> defaultInstance(instance: M): ObjectModelDeserializer<M> {
      return withSettings(instance)
    }
  }

  /**
   * Utility function to convert Google Cloud's specific timestamp type into a temporal instant from OCP. This function
   * must live in this module, and not in common with the `InstantFactory`, to avoid adding a dependency on common for
   * Google's Cloud commons for Java.
   *
   * @param ts
   * @returns
   */
  private fun instantFromCloudTimestamp(ts: com.google.cloud.Timestamp): Timestamp {
    return Timestamp.newBuilder()
      .setSeconds(ts.seconds)
      .setNanos(ts.nanos)
      .build()
  }

  /**
   * Set a field value on a given builder, depending on the field's declared type (provided by Protobuf via field
   * descriptors). "Simple" values are defined as items that are un-repeated and composed solely of native scalar types
   * defined in the Protobuf `proto3` spec.
   *
   * @param type
   * @param field
   * @param builder
   * @param dataValue
   * @throws DeserializationError
   */
  @Throws(DeserializationError::class)
  private fun <B: Message.Builder> setSimpleField(type: Type,
                                                  field: Descriptors.FieldDescriptor,
                                                  builder: B,
                                                  dataValue: Any) {
    // this is only for un-repeated simple values
    if (field.isRepeated)
      throw DeserializationError("Cannot set repeated fields as simple values.")

    // only operate on fields with a value
    when (type) {
      Type.BOOL,
      Type.INT32, Type.INT64,
      Type.SFIXED32, Type.SFIXED64,
      Type.SINT32, Type.SINT64,
      Type.FIXED64, Type.FIXED32,
      Type.STRING ->
        // serialize a simple native type
        builder.setField(field, dataValue)

      Type.DOUBLE, Type.FLOAT -> {
        // serialize a precision number type
        when (dataValue) {
          is Int -> builder.setField(field, dataValue.toFloat())
          is Long -> builder.setField(field, dataValue.toFloat())
          is String -> builder.setField(field, dataValue.toFloat())
          is Double -> builder.setField(field, dataValue.toFloat())
          else -> {
            logging.warn("Unable to serialize number precise numeric field: '${field.name}'.")
            builder.setField(field, dataValue)
          }
        }
      }

      Type.UINT32, Type.UINT64 ->
        // serialize with care taken for longs
        when (dataValue) {
          is Long -> builder.setField(field, dataValue.toInt())
          is Double -> builder.setField(field, dataValue.toInt())
          is Float -> builder.setField(field, dataValue.toInt())
          else -> builder.setField(field, dataValue)
        }

      Type.BYTES -> {
        // decode from base64 if it's not already raw
        if (dataValue is String) {
          // it's probably base64 encoded
          val bytes: ByteArray = Base64.getDecoder().decode(dataValue.toByteArray(StandardCharsets.UTF_8))
          val bytestring: ByteString = ByteString.copyFrom(bytes)
          builder.setField(field, bytestring)
        } else {
          builder.setField(field, dataValue)
        }
      }

      Type.ENUM -> {
        val enumType = field.enumType ?:
        throw DeserializationError("Unable to resolve enum without attached type, for field '${field.name}' " +
          "on entity '${builder.descriptorForType.name}'.")

        // resolve enum type for this field
        // decode from either string or numeric reference
        val enumValue = when (dataValue) {
          // resolve by name?
          is String -> enumType.findValueByName(dataValue)

          // resolve by ID number?
          is Int, is Long, is Double -> enumType.findValueByNumber(dataValue as Int)

          else ->
            // unable to resolve enum type
            throw DeserializationError("Unable to resolve enum type from raw value for field '${field.name}' " +
              "on entity '${builder.descriptorForType.name}'.")
        } ?: throw DeserializationError("Unable to resolve enum value for field '${field.name}' " +
          "on entity '${builder.descriptorForType.name}'.")

        // we should have an enum value now
        builder.setField(field, enumValue)
      }

      Type.GROUP, Type.MESSAGE ->
        throw DeserializationError("Cannot set sub-message types as simple values.")
    }
  }

  /**
   * Repeated enums are a special case, because they may be specially-encoded as either a list of strings (or numbers),
   * or a map of strings to boolean or integer values.
   *
   * @param descriptor
   * @param field
   * @param builder
   * @param dataList
   * @throws DeserializationError
   */
  private fun <B: Message.Builder> setRepeatedEnum(descriptor: Descriptors.Descriptor,
                                                   field: Descriptors.FieldDescriptor,
                                                   builder: B,
                                                   dataList: Any) {
    val enumType = field.enumType ?: throw DeserializationError("Unable to deserialize repeated enum with missing " +
      "enum type, at field '${field.name}' on entity '${descriptor.name}'.")
    val enumValues: ArrayList<Descriptors.EnumValueDescriptor>

    when (dataList) {
      // a list of enum values can be strings or numbers
      is List<*> -> {
        enumValues = ArrayList(dataList.size)

        var pos = 0
        for (rawEnumValue in dataList) {
          pos += 1

          when (rawEnumValue) {
            // handle as the enum name if it's a string
            is String -> enumValues.add(enumType.findValueByName(rawEnumValue.toUpperCase()))

            // handle as a numeric ID of the enum value
            is Int, is Double, is Long -> enumValues.add(enumType.findValueByNumber(rawEnumValue as Int))

            // reject unrecognized types
            else ->
              throw DeserializationError("Unable to decode repeated enum value in position '$pos' on field " +
                "'${field.name}' on entity '${descriptor.name}'.")
          }
        }
      }

      // a list of map values should be strings mapped to boolean or integer values
      is Map<*, *> -> {
        enumValues = ArrayList(dataList.size)
        for (rawEnumKey in dataList.keys) {
          if (rawEnumKey !is String)
            throw DeserializationError("Repeated enum values expressed as a map must have string keys, at field " +
              "'${field.name}' on entity '${descriptor.name}'.")

          // string value should be the enum type
          enumValues.add(enumType.findValueByName(rawEnumKey.toUpperCase()))
        }
      }

      // unrecognized types should fail
      else -> throw DeserializationError("Failed to identify type of repeated enum data at field '${field.name}' " +
        "on entity '${descriptor.name}'.")
    }

    // fill it in on the builder
    builder.setField(field, enumValues)
  }

  /**
   * Fill in a repeated field on a message. This involves iterating over the list of values, preparing a list of our own
   * filtered and decoded values, and then setting it via the builder.
   *
   * @param type
   * @param field
   * @param builder
   * @param dataList
   * @throws DeserializationError
   */
  private fun <B: Message.Builder> setRepeatedField(type: Type,
                                                    field: Descriptors.FieldDescriptor,
                                                    builder: B,
                                                    dataList: List<*>) {
    val targetValues = ArrayList<Any>(dataList.size)

    if (type == Type.ENUM)
      // should never get here
      throw DeserializationError("Repeated enums cannot be decoded as regular fields.")

    // we have a list of raw values
    for (item in dataList) {
      // skip nulls
      item ?: continue

      // decode a simple type
      targetValues.add(item)
    }
    builder.setField(field, targetValues)
  }

  /**
   * Load a raw set of mapped data, from underlying storage, into a message builder so that it may be constructed into
   * a concrete Protobuf representation.
   *
   * @param builder
   * @param data
   * @returns
   * @throws DeserializationError
   */
  @Throws(DeserializationError::class)
  @Suppress("UNCHECKED_CAST", "DuplicatedCode")
  fun <B: Message.Builder> build(builder: B, data: Map<String, *>): B {
    // setup a new builder
    if (data.isEmpty()) return builder  // it's empty, return a default proto

    // otherwise parse the fields
    val descriptor = builder.descriptorForType
    fields@for (field in descriptor.fields) {
      // every field must have a type
      val type = field.type ?: throw DeserializationError("Cannot inflate a field without a type.")
      val fieldProto = field.toProto()
      val fieldOptions = if (fieldProto.hasOptions() && fieldProto.options.hasExtension(Datamodel.opts)) {
        fieldProto.options.getExtension(Datamodel.opts)
      } else {
        null
      }
      val persistenceOptions = if (fieldProto.hasOptions() && fieldProto.options.hasExtension(Datamodel.field)) {
        fieldProto.options.getExtension(Datamodel.field)
      } else {
        null
      }

      // skip ephemeral fields
      if (fieldOptions?.ephemeral == true) continue@fields

      if (!field.isRepeated) {
        if (data.containsKey(field.name)) {
          // extract value, make sure it's not null
          val dataValue = data[field.name] ?: continue@fields
          when (type) {
            Type.GROUP, Type.MESSAGE -> {
              // it's a singular sub-message field. recurse.
              val subBuilder = builder.newBuilderForField(field)
                ?: throw DeserializationError("Unable to resolve message type for property '${field.name}' " +
                  "on entity '${descriptor.name}'")
              if (dataValue is Map<*, *>) {
                // prepare the sub-builder, then attach to the top-level field
                this.build(subBuilder, dataValue as Map<String, Any>)
                builder.setField(field, subBuilder.build())
              } else {
                // special case: consider timestamps
                if (field.messageType.fullName == "google.protobuf.Timestamp") {
                  when (dataValue.javaClass.name) {
                    // it's a Google Cloud well-known-value (`Timestamp`), for which we have a converter
                    "com.google.cloud.Timestamp" ->
                      builder.setField(field, instantFromCloudTimestamp(
                        dataValue as com.google.cloud.Timestamp))


                    // it's a Protobuf well-known-value (`Timestamp`), for which we need no conversion
                    "google.protobuf.Timestamp" ->
                      builder.setField(field, dataValue as Timestamp)


                    // if it's numeric, it should be a millisecond-resolution Unix epoch timestamp
                    else -> builder.setField(field, when (dataValue) {
                      is Int, is Double, is Long -> Timestamp.newBuilder()
                        .setSeconds(dataValue as Long)

                      // all other types should fail
                      else ->
                        throw DeserializationError("Failed to decode timestamp/instant type. Could not determine " +
                          "native type at field '${field.name}' on entity '${descriptor.name}'.")
                    }.build())
                  }
                } else {
                  // see if it is annotated as a parent, or a reference, which would explain this state
                  if (persistenceOptions != null && (
                      persistenceOptions.type == CoreFieldType.REFERENCE ||
                        persistenceOptions.type == CoreFieldType.PARENT)) {
                    // it should be a reference type
                    if (dataValue.javaClass.name != DocumentReference::class.java.name)
                      throw DeserializationError("Found non-reference value for reference property.")
                    val ref = dataValue as DocumentReference
                    if (ref.parent.parent == null) {
                      // has no parent, so it's easy. set up a new instance of the key.
                      val keyInstance = builder.newBuilderForField(field) ?:
                      throw DeserializationError("Unable to resolve builder for key reference instance.")

                      // find the field to inflate the ID into
                      var idField: Descriptors.FieldDescriptor? = null
                      keyFields@for (keyField in keyInstance.descriptorForType.fields) {
                        if (keyField.options.hasExtension(Datamodel.field)) {
                          val persistenceOptsForSubfield = keyField.options.getExtension(Datamodel.field)
                          if (persistenceOptsForSubfield.type == CoreFieldType.ID) {
                            // found it
                            idField = keyField
                            break@keyFields
                          }
                        }
                      }
                      if (idField == null)
                        throw DeserializationError("Could not resolve key structure ID field for reference inflate.")

                      // fill the builder at that field with the trimmed value
                      keyInstance.setField(idField, ref.id)
                      builder.setField(field, keyInstance.build())

                    } else {
                      TODO("recursive inflation of references not yet supported")
                    }
                  } else {
                    // dunno why it's not an object
                    throw DeserializationError("Found non-map value where sub-message value was expected, " +
                      "in field '${field.name}' on entity '${descriptor.name}'.")
                  }
                }
              }
            }

            else -> setSimpleField(type, field, builder, dataValue)
          }
        } else if (fieldOptions?.concrete == true && (type == Type.MESSAGE || type == Type.GROUP)) {
          // if it's a concrete record, examine the field name, against the containing one-of name. if the containing
          // one-of name (concrete synthesized name) and the property name match here, it's supposed to be a concrete
          // type, flattened into the map we're currently de-serializing.
          val concreteType = data[ObjectModelSerializer.concreteTypeProperty] as? String
          if (concreteType != null && concreteType.toLowerCase().trim() == field.jsonName.toLowerCase().trim()) {
            // we found the concrete type expressed by this generic entity. now we need to decode it as if it's the
            // underlying concrete type specified.
            val subBuilder = builder.newBuilderForField(field)
            subBuilder ?: throw DeserializationError("Unable to resolve message type for concrete property " +
              "'${field.name}' on entity '${descriptor.name}'")
            this.build(subBuilder, data)
            builder.setField(field, subBuilder.build())
          }
        } else if (fieldOptions?.required == true) {
          throw DeserializationError("Unable to resolve required field '${field.name}' on message " +
            "'${field.containingType.fullName}'.")
        }
      } else {
        // field is repeated: try to grab a list of values, decode for each one
        val dataList = data[field.name] ?: continue@fields
        if (type == Type.ENUM) {
          // handle special case: repeated enums
          setRepeatedEnum(descriptor, field, builder, dataList)
        } else {
          // only operate on lists with values
          if (dataList is List<*> && dataList.isNotEmpty()) {
            when (type) {
              Type.GROUP, Type.MESSAGE -> {
                val submessageList: ArrayList<Message> = ArrayList(dataList.size)

                // make a new list of decoded messages
                var pos = 0
                for (subObj in dataList) {
                  pos += 1

                  if (subObj is Map<*, *>) {
                    // reset field for next round
                    val subBuilder = builder.newBuilderForField(field) ?:
                    throw DeserializationError("Unable to resolve builder for field '${field.name}' on " +
                      "entity '${descriptor.name}'.")
                    this.build(subBuilder, subObj as Map<String, Any>)
                    submessageList.add(subBuilder.build())

                  } else {
                    throw DeserializationError("Cannot identify type for message in repeated field " +
                      "'${field.name}' at position '$pos' on entity " +
                      "'${descriptor.name}'.")
                  }
                }

                if (submessageList.isNotEmpty())
                  builder.setField(field, submessageList)
              }
              else ->
                // set as regular repeated field
                setRepeatedField(type, field, builder, dataList)
            }
          }
        }
      }
    }
    return builder
  }

  /** @inheritDoc */
  @Nonnull
  @Suppress("UNCHECKED_CAST")
  @Throws(ModelInflateException::class)
  override fun inflate(@Nonnull input: Map<String, *>): Model {
    val builder = defaultInstance.newBuilderForType()
    build(builder, input)
    return builder.build() as Model
  }
}
