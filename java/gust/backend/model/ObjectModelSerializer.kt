/*
 * Copyright © 2020, The Gust Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
package gust.backend.model

import tools.elide.core.Datamodel
import tools.elide.core.CollectionMode
import tools.elide.core.DatapointOptions
import tools.elide.core.FieldType as CoreFieldType
import tools.elide.core.FieldPersistenceOptions
import com.google.firestore.v1.ArrayValue
import com.google.firestore.v1.MapValue
import com.google.firestore.v1.Value
import com.google.protobuf.*
import com.google.protobuf.Descriptors.*
import com.google.protobuf.Descriptors.FieldDescriptor.Type as FieldType
import gust.backend.model.ModelSerializer.EnumSerializeMode
import gust.backend.model.ModelSerializer.InstantSerializeMode
import gust.backend.runtime.Logging
import gust.util.InstantFactory
import java.util.*
import javax.annotation.Nonnull
import javax.annotation.concurrent.Immutable
import javax.annotation.concurrent.ThreadSafe


/**
 * Specifies a serializer which is capable of converting [Message] instances into generic Java [SortedMap] objects with
 * regular [String] keys. If there are nested records on the model instance, they will be serialized into recursive
 * [SortedMap] instances.
 *
 * @param <Model> Model record type which this serializer is responsible for converting.
 */
@Immutable
@ThreadSafe
class ObjectModelSerializer<Model : Message>
  /**
   * Construct a model serializer from scratch.
   *
   * @param includeDefaults Whether to include default field values.
   * @param includeNulls Whether to serialize nulls or simply omit them.
   * @param emptyListsAsNulls Whether to serialize empty lists as nulls, or as empty lists.
   * @param enumMode Enum serialization mode.
   * @param instantMode Temporal instant serialization mode.
   */
  private constructor(
    /** Whether to include default values.  */
    @field:Nonnull @param:Nonnull private val includeDefaults: Boolean,
    /** Whether to serialize nulls, or simply omit them.  */
    @field:Nonnull @param:Nonnull private val includeNulls: Boolean,
    /** Whether to encode empty lists as nulls, or empty lists.  */
    @field:Nonnull @param:Nonnull private val emptyListsAsNulls: Boolean,
    /** Typed enumeration serialization mode.  */
    @field:Nonnull @param:Nonnull private val enumMode: EnumSerializeMode,
    /** Temporal instant serialization mode.  */
    @field:Nonnull @param:Nonnull private val instantMode: InstantSerializeMode)
  : ModelSerializer<Model, SortedMap<String, *>> {
  /** @return Empty serialized object. */
  private fun serializedObject(): SerializedModel = SerializedModel.factory()

  /**
   * Generate a full referential database path, given the concrete type and document ID to reference. This variant
   * allows specification of each detail individually, including the project ID and database ID.
   *
   * @param type Type of model we are generating a reference for.
   * @param id ID for the model we are generating a reference for.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun referenceValue(type: String, id: String): String = "$type/$id"

  /**
   * Extract, as a [List], items from a repeated field value.
   *
   * @param proto Message to extract from.
   * @param base Optional message to compare with.
   * @param field Descriptor for the field.
   * @return Pair: Whether a value was found, and the value (or default).
   */
  private fun extractRepeatedValue(proto: Message, base: Message?, field: FieldDescriptor): Pair<Boolean, List<Any>> {
    val valueCount = proto.getRepeatedFieldCount(field)
    val previousValueCount = base?.getRepeatedFieldCount(field) ?: 0

    return if (valueCount < 1 && previousValueCount == valueCount) {
      // nothing to set: it's an empty repeated field
      true to emptyList()
    } else {
      val targetList = ArrayList<Any>(valueCount)
      for (valueIndex in 0 until valueCount) {
        // extract the items, build, and return
        val repeatedValue: Any = proto.getRepeatedField(field, valueIndex)
        targetList.add(repeatedValue)
      }
      false to targetList
    }
  }

  /**
   * Extract a field's set value from a given Proto record, making sure to indicate via the return value whether that
   * value was/is a default value or not.
   *
   * @param proto Proto message to extract from.
   * @param base Base message to compare.
   * @param field Proto field to extract.
   * @return Pair: Whether it was found, and the resulting value (or default).
   */
  @Suppress("UNCHECKED_CAST")
  private fun <T> extractValue(proto: Message, base: Message?, field: FieldDescriptor): Pair<Boolean, T?> {
    return when {
      // is it a repeated field?
      field.isRepeated -> return extractRepeatedValue(proto, base, field) as Pair<Boolean, T?>

      // it's a simple field
      else -> {
        try {
          val value: T? = proto.getField(field) as T
          val baseValue: T? = base?.getField(field) as? T

          when {
            // if it's null, it's null
            value == null -> (baseValue == null) to null

            // if it has the field value, it's not a default
            proto.hasField(field) -> false to value

            // otherwise, any returned value is a default
            else -> (baseValue == null || baseValue == value) to value
          }
        } catch (exc: ClassCastException) {
          // cast error
          logging.error("Experienced casting error in ModelSerializer: '${exc.message}'. Skipping field.")
          true to null
        }
      }
    }
  }

  /**
   * Wrap an arbitrary Kotlin value in a Firestore proto `Value` for serialization. Although this adapter is not
   * inherently coupled to Firestore, at least functionally, these containers are used because they are type-safe and
   * present a full superset versus the standard Protocol Buffer value set.
   *
   * @param value Builder for the value wrap.
   * @param type Type of value we are wrapping.
   * @param data Data value to wrap.
   * @return Value builder, pre-filled with the data.
   */
  private fun <T> wrapValue(value: Value.Builder, type: FieldType, data: T): Value.Builder {
    return when (type) {
      FieldType.INT32, FieldType.UINT32, FieldType.SINT32, FieldType.INT64,
      FieldType.UINT64, FieldType.SINT64, FieldType.FIXED32, FieldType.FIXED64,
      FieldType.SFIXED32, FieldType.SFIXED64 -> {
        val decoded = data as? Long
        if (decoded == null) {
          null
        } else {
          value.setIntegerValue(decoded)
        }
      }
      FieldType.FLOAT, FieldType.DOUBLE -> {
        val decoded = data as? Double
        if (decoded == null) {
          null
        } else {
          value.setDoubleValue(decoded)
        }
      }
      FieldType.ENUM -> {
        when (enumMode) {
          EnumSerializeMode.NAME -> {
            val decoded = (data as? EnumValueDescriptor)?.name
            if (decoded == null) {
              null
            } else {
              value.setStringValue(decoded)
            }
          }
          EnumSerializeMode.NUMERIC -> {
            val decoded = (data as? EnumValueDescriptor)?.number?.toLong()
            if (decoded == null) {
              null
            } else {
              value.setIntegerValue(decoded)
            }
          }
        }
      }
      FieldType.BOOL -> {
        val decoded = data as? Boolean
        if (decoded == null) {
          null
        } else {
          value.setBooleanValue(decoded)
        }
      }
      FieldType.STRING -> {
        val decoded = data as? String
        if (decoded == null) {
          null
        } else {
          value.setStringValue(decoded)
        }
      }
      FieldType.BYTES -> {
        // encode as Base64
        val decoded = data as? ByteArray
        if (decoded == null) {
          null
        } else {
          val encoder = bytesEncoder(value::setStringValue)
          encoder(decoded)
        }
      }
      else -> {
        // log a warning
        logging.warn("Unresolvable or null type for proto field: '$type'. Skipping.")
        value
      }
    } ?: throw ModelSerializer.SerializationError("Failed to serialize integer value as long (for type: '$type').")
  }

  /**
   * Serialize a list of values into an [ArrayValue] container.
   *
   * @param field Field we are serializing values for.
   * @param targetList List we are pulling values from.
   */
  private fun serializeList(field: FieldDescriptor, targetList: List<*>): ArrayValue? {
    return if (targetList.isEmpty()) {
      if (emptyListsAsNulls)
        null
      else
        ArrayValue.getDefaultInstance()
    } else {
      val value = ArrayValue.newBuilder()
      for (innerValue in targetList) {
        // encode a value raw
        val innerWrapped = Value.newBuilder()
        val wrapped = this.wrapValue(innerWrapped, field.type, innerValue)
        value.addValues(wrapped)
      }
      value.build()
    }
  }

  /**
   * Extract a value from the given concrete message object, at the specified field. If there is a value, and it is
   * eligible to be included (i.e. default values are being included, and it is a default value, or it is not a default
   * value), then set it using the builder method also given.
   *
   * @param proto Message record to extract from.
   * @param base Base record to compare to (optional).
   * @param field Field descriptor we are pulling a value for.
   * @param builder Field value builder.
   * @param setter Function that sets on the builder.
   * @return Pair: Whether the field was found, and the resulting value builder.
   */
  private fun <T> extractAndSetValue(proto: Message,
                                     base: Message?,
                                     field: FieldDescriptor,
                                     builder: Value.Builder,
                                     setter: (T) -> Value.Builder): Pair<Boolean, Value.Builder> {
    val (isDefault, value) = extractValue<T>(proto, base, field)
    return if (isDefault && includeDefaults || !isDefault) {
      // it's eligible, we can continue
      if (value == null) {
        true to builder.setNullValue(NullValue.NULL_VALUE)
      } else {
        if (field.isRepeated) {
          val listVal = value as? List<*>
          if (listVal != null) {
            // we have some kind of list value
            val serializedList = serializeList(field, listVal)
            if (serializedList == null) {
              // underlying list serializer is indicating it's withheld list value
              true to builder.setNullValue(NullValue.NULL_VALUE)
            } else {
              false to builder.setArrayValue(serializedList)
            }
          } else {
            // unidentified value
            logging.error("Found supposedly repeated field '${field.name}' with no list value.")
            true to builder.setNullValue(NullValue.NULL_VALUE)
          }
        } else {
          // we have a value of some sort, of type `T`
          if (field.type == FieldType.BYTES) {
            val valueAsBytestring = value as? ByteString
            if (valueAsBytestring != null) {
              val rawBytes = value.toByteArray()

              try {
                @Suppress("UNCHECKED_CAST")
                val casted = rawBytes as? T
                if (casted != null) {
                  false to setter(casted)
                } else {
                  logging.warn("Failed to encode raw bytestring in Firestore. Failing.")

                  // skip it by setting a null value
                  true to builder.setNullValue(NullValue.NULL_VALUE)
                }
              } catch (cce: ClassCastException) {
                logging.warn("Failed to cast raw bytestring in Firestore. Failing.")

                // skip it by setting a null value
                true to builder.setNullValue(NullValue.NULL_VALUE)
              }
            } else {
              false to setter(value)
            }
          } else {
            false to setter(value)
          }
        }
      }
    } else {
      // skip it by setting a null value
      true to builder.setNullValue(NullValue.NULL_VALUE)
    }
  }

  /**
   * Extract an enum field value, encoding it as directed by enum encoding settings listed above. Enums are either
   * serialized as their numeric ID, or their string name.
   *
   * @param proto Protocol message to extract from.
   * @param base Optional protocol message to compare to.
   * @param field Field we are extracting an enum for.
   * @param builder Value wrapper builder.
   * @param persistenceOpts Field persistence options, extracted as annotations from the field in question.
   * @return Pair: Whether a value was found, and a value-builder for the value, or the default, as applicable.
   */
  private fun extractEnum(proto: Message,
                          base: Message?,
                          field: FieldDescriptor,
                          builder: Value.Builder,
                          persistenceOpts: FieldPersistenceOptions?): Pair<Boolean, Value.Builder> {
    return if (field.isRepeated) {
      // extract list of enums
      val (isDefault, extractedValue) = extractValue<List<EnumValueDescriptor>>(
        proto, base, field)

      if (extractedValue == null || (isDefault && !includeDefaults)) {
        true to builder.setNullValue(NullValue.NULL_VALUE)
      } else if (isDefault || extractedValue.isEmpty()) {
        false to builder.setArrayValue(ArrayValue.getDefaultInstance())
      } else {
        // extract enums, they are eligible to be included by definition at this point
        val enumsList = ArrayValue.newBuilder()

        for (enumValue in extractedValue) {
          val valueWrap = Value.newBuilder()
          when (enumMode) {
            EnumSerializeMode.NAME -> valueWrap.stringValue = enumValue.name
            EnumSerializeMode.NUMERIC -> valueWrap.integerValue = enumValue.number.toLong()
          }
          enumsList.addValues(valueWrap)
        }
        false to builder.setArrayValue(enumsList)
      }
    } else {
      // extract the enum value
      val (isDefault, extractedValue) = extractValue<EnumValueDescriptor>(
        proto, base, field)

      if (extractedValue == null || (isDefault && !includeDefaults && persistenceOpts?.explicit != true)) {
        true to builder.setNullValue(NullValue.NULL_VALUE)
      } else if (isDefault && includeDefaults || !isDefault || persistenceOpts?.explicit == true) {
        // depending on enum serialization mode, serialize
        false to when (enumMode) {
          EnumSerializeMode.NAME -> builder.setStringValue(extractedValue.name)
          EnumSerializeMode.NUMERIC -> builder.setIntegerValue(extractedValue.number.toLong())
        }
      } else {
        // skip it by setting a null value
        true to builder.setNullValue(NullValue.NULL_VALUE)
      }
    }
  }

  /**
   * Serialize a temporal instant (i.e. a timestamp). This handles a special case where a timestamp record is being
   * expressed, and we need to consider timestamp serialization settings before proceeding.
   *
   * @param proto Message to extract the timestamp from.
   * @param base Optional message for a comparison base.
   * @param field Field we are extracting the instant from.
   * @param builder Value builder we should wrap the instant in.
   * @return Pair: Whether the instant value was found, and a value builder which wraps it (or a default value).
   */
  private fun serializeInstant(proto: Message,
                               base: Message?,
                               field: FieldDescriptor,
                               builder: Value.Builder): Pair<Boolean, Value.Builder> {
    // extract the temporal instant record
    val (isDefault, extractedValue) = extractValue<Timestamp>(proto, base, field)

    return if (extractedValue == null || (isDefault && !includeDefaults)) {
      // if it's not there, or it's a default instance and we shouldn't include those, just return as null
      true to builder.setNullValue(NullValue.NULL_VALUE)
    } else {
      // it's eligible to be included. serialize based on instant serialization settings.
      when (instantMode) {
        InstantSerializeMode.TIMESTAMP -> {
          if (extractedValue.seconds > 0) {
            // we got lucky: no need to convert
            false to builder.setTimestampValue(extractedValue)

          } else {
            val jti: java.time.Instant? = InstantFactory.instant(extractedValue)
            if (jti == null) {
              // unable to handle it
              logging.warn("Unable to convert protobuf Timestamp to Java structure for serialization.")
              true to builder.setNullValue(NullValue.NULL_VALUE)
            } else {
              val millis = jti.toEpochMilli()
              false to builder.setTimestampValue(Timestamp.newBuilder()
                .setSeconds(millis / 1000)
                .setNanos(((millis % 1000) * 1000000).toInt()))
            }
          }
        }
        InstantSerializeMode.ISO8601 -> {
          val jti: java.time.Instant? = InstantFactory.instant(extractedValue)
          if (jti == null) {
            true to builder.setNullValue(NullValue.NULL_VALUE)
          } else {
            false to builder.setStringValue(jti.toString())
          }
        }
      }
    }
  }

  /**
   * Calculate the collection path for a given type. The collection path is declared in the data model, or else defaults
   * to a value calculated from the model name.
   *
   * @param descriptor Descriptor we would like a collection path for.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  fun collectionPath(descriptor: Descriptor): String {
    val protoObj = descriptor.toProto()
    if (protoObj.hasOptions() && protoObj.options.hasExtension(Datamodel.db)) {
      // process settings
      val dbSettings = protoObj.options.getExtension(Datamodel.db)
      return dbSettings.path
    }
    throw IllegalStateException("Failed to calculate collection path for type: '${descriptor.name}'.")
  }

  /**
   * Find the parent field, and potentially parent ID value, for a given message. The `PARENT` annotation should be
   * affixed to an object field, which itself makes reference to an `ID`-annotated property. Find this ID property, and
   * extract its value. Return a triple of the value, the object it was mounted on, and the field descriptor describing
   * the field that contained the ID.
   *
   * @param proto Proto message to scan on.
   * @param fields List of fields to scan through.
   * @return Pair of the collection path segment name for the parent, and the ID value, or `null` for both if a parent
   *         could not be resolved.
   */
  private fun scanForParent(proto: Message,
                            fields: List<FieldDescriptor>):
    Pair<Pair<String?, String?>, Pair<Message?, FieldDescriptor?>> {
    parentScan@ for (field in fields) {
      if (field.type == FieldType.MESSAGE) {
        val fieldProto = field.toProto()
        if (fieldProto.options.hasExtension(Datamodel.field)) {
          // potentially annotated with `PARENT`
          val fieldInfo = fieldProto.options.getExtension(Datamodel.field)
          if (fieldInfo != null && fieldInfo.type == CoreFieldType.PARENT) {
            // we found the parent
            parentIdScan@ for (subfield in field.messageType.fields) {
              if (subfield.type == FieldType.STRING) {
                // potentially a parent ID
                val subfieldProto = subfield.toProto()
                if (subfieldProto.options.hasExtension(Datamodel.field)) {
                  val subfieldInfo = subfieldProto.options.getExtension(
                    Datamodel.field)
                  if (subfieldInfo != null && subfieldInfo.type == CoreFieldType.ID) {
                    // found it
                    val idContainer = (proto.getField(field) as? Message)
                    val idValue = idContainer?.getField(subfield) as? String ?:
                      throw ModelSerializer.SerializationError("Cannot serialize sub-write with missing parent ID.")
                    val collectionPath = this.collectionPath(field.messageType)
                    if (collectionPath.isBlank() || collectionPath.isEmpty() ||
                      idValue.isBlank() || idValue.isEmpty())
                        throw ModelSerializer.SerializationError("Cannot serialize sub-write with empty parent ID.")
                    return (collectionPath to idValue) to (idContainer to subfield)
                  }
                }
              }
            }
            break@parentScan
          }
        }
      }
    }
    return null to null to (null to null)
  }

  /**
   * Find the ID field, value, and entity for a given message. If the ID field is nested inside a key object, find it
   * anyway. Return a triple of the found ID, message it was mounted on, and field descriptor, if it could be found,
   * otherwise null in each case or where things could not be found.
   *
   * @param proto Proto message to scan on.
   * @param fields Fields to look in for the annotation.
   * @return Pair of the value in the field, to a pair of the matching message and descriptor, as applicable.
   */
  private fun scanForIdProperty(proto: Message,
                                fields: List<FieldDescriptor>):
    Pair<String?, Pair<Message?, FieldDescriptor?>> {
    var idField: FieldDescriptor? = null
    var idValue: String? = null
    var idEntity: Message? = proto

    idScan@ for (field in fields) {
      when (field.type) {
        FieldType.STRING -> {
          val fieldProto = field.toProto()
          if (fieldProto.options.hasExtension(Datamodel.field)) {
            // potentially an ID field
            val fieldInfo = fieldProto.options.getExtension(Datamodel.field)
            if (fieldInfo != null && fieldInfo.type == CoreFieldType.ID) {
              idField = field
              idEntity = proto

              val (_, extractedIdValue) = extractValue<String>(idEntity, null, idField)
              idValue = extractedIdValue
              break@idScan
            }
          }
        }
        FieldType.GROUP, FieldType.MESSAGE -> {
          val fieldProto = field.toProto()

          // special case: is this a key or ID field?
          if (fieldProto.options.hasExtension(Datamodel.field)) {
            val fieldInfo = fieldProto.options.getExtension(Datamodel.field)
            when (fieldInfo.type) {
              CoreFieldType.KEY -> {
                // a sub-property on this message type is this item's ID field
                val keyType = field.messageType

                for (subfield in keyType.fields) {
                  val subfieldProto = subfield.toProto()
                  if (subfieldProto != null
                    && subfieldProto.hasOptions()
                    && subfieldProto.options.hasExtension(Datamodel.field)) {
                    // it has some kind of field annotation
                    val subfieldInfo = subfieldProto.options.getExtension(
                      Datamodel.field)
                    if (subfieldInfo != null && subfieldInfo.type == CoreFieldType.ID) {
                      // we have an ID property
                      idField = subfield
                      val (_, extractedIdEntity) = extractValue<Message>(proto, null, field)
                      if (extractedIdEntity != null) {
                        idEntity = extractedIdEntity

                        val (_, extractedIdValue) = extractValue<String>(idEntity, null, idField)
                        idValue = extractedIdValue
                      }
                      break@idScan
                    }
                  }
                }
              }
              else -> continue@idScan  // other fields are not interesting in this case
            }
          }
        }
        else -> continue@idScan  // other fields are not interesting in this case
      }
    }
    return idValue to (idEntity to idField)
  }

  /**
   * Calculate the full path prefix for a parent-ed write that is potentially more than 1 level nested. This crawls for
   * `PARENT` annotations on key structures in the parent message, and recursively builds a path which can be used to
   * prefix a deeply-nested write operation.
   *
   * @param parent Parent object we are generating a prefix at.
   * @param parentDescriptor Descriptor for the parent object.
   * @return Generated path to use as a prefix to a deeply-nested write.
   */
  private fun recursiveParentPathPrefix(parent: Message,
                                        parentDescriptor: Descriptor,
                                        immediateParentPath: String): String {
    // scan for parent property
    val (values, coordinates) = this.scanForParent(
      parent, parentDescriptor.fields)
    val (prefix, idValue) = values
    val (container, idField) = coordinates

    if (idValue == null || idField == null || container == null || prefix == null)
      return immediateParentPath  // it is 1-level deep

    // generate a path for the parent, and return that
    return recursiveParentPathPrefix(
      container, container.descriptorForType, "$prefix/$idValue/$immediateParentPath")
  }

  /**
   * Calculate the path to specify for a given reference property, considering the collection path and ID extracted from
   * whatever record is being referenced.
   *
   * @param container Message containing the ID reference.
   * @param descriptor Descriptor for the container message.
   * @param idValue Value of the ID, resolved via scanning.
   * @param postfix String to prepend from previous recursive invocations.
   * @return String path to use for the reference.
   */
  private fun referenceForSubmessage(container: Message,
                                     descriptor: Descriptor,
                                     idValue: String,
                                     postfix: String? = null,
                                     effectiveId: String? = null): Pair<String, String> {
    val path = this.collectionPath(descriptor)
    val (values, coordinates) = this.scanForParent(
      container, descriptor.fields)
    val (parentPrefix, parentId) = values
    val (subContainer, subField) = coordinates

    return if (parentPrefix == null || parentId == null || subContainer == null || subField == null) {
      // it has no additional parent
      (if (postfix != null) {
        "$path/$idValue/$postfix"
      } else {
        path
      }) to (effectiveId ?: idValue)

    } else {
      // it has a parent, recurse
      this.referenceForSubmessage(
        subContainer,
        subContainer.descriptorForType,
        parentId,
        postfix = path,
        effectiveId = effectiveId ?: idValue)
    }
  }

  /**
   * Generate an operation's symbolic write path. The "write path" refers to the combination of the
   * table/collection/dataset (as applicable), and the unique record ID, for a given object. How this symbolic path is
   * precisely used depends on the storage engine in use.
   *
   * @param descriptor Descriptor matching the model for which we are generating a default write path.
   * @param nested Whether the entity being written is a nested entity.
   * @param field Field which we are generating the write for, as applicable. Required for `nested` fields.
   * @param mode Collection mode to apply to the write, as applicable.
   * @return Generated default write path, or `null` if a write path is not applicable.
   */
  private fun generateDefaultWritePath(descriptor: Descriptor,
                                       nested: Boolean,
                                       field: FieldDescriptor?,
                                       mode: CollectionMode): String? {
    // nested entities don't have a write path
    return if (nested && mode == CollectionMode.NESTED) {
      // consider the field name
      if (field == null) throw ModelSerializer.SerializationError("Cannot serialize nested field without descriptor.")
      field.name
    } else {
      if (!descriptor.name.endsWith("s")) {
        "${descriptor.name.toLowerCase()}s"
      } else {
        descriptor.name.toLowerCase()
      }
    }
  }

  /**
   * Given an entity, and either a nested or root context, resolve a write operation to persist it.
   *
   * @param descriptor Descriptor for the model we are resolving a write for.
   * @param descriptorProto Pre-fetched proto version of the descriptor.
   * @param data Serialized data object which we are building.
   * @param field Descriptor for the field we are resolving a write for, as applicable.
   * @param id Resolved ID value for the record being written.
   * @param nested Whether the record is a nested entity.
   * @param parentWrite Parent write that governs the scope for this one, if applicable.
   * @param disposition Disposition (execution strategy) for the resulting write.
   * @return Write operation characterized by the provided data.
   */
  private fun resolveWrite(descriptor: Descriptor,
                           descriptorProto: DescriptorProtos.DescriptorProto,
                           data: SerializedModel,
                           field: FieldDescriptor?,
                           id: String,
                           nested: Boolean,
                           parentWrite: CollapsedMessage.Operation?,
                           disposition: ModelSerializer.WriteDisposition): CollapsedMessage.Write {
    val defaultMode = if (nested) {
      CollectionMode.NESTED
    } else {
      CollectionMode.GROUP
    }
    val fieldProto = field?.toProto()

    // resolve storage mode for this object, and storage path (pluralized + lower-cased message name)
    val (storageMode: CollectionMode, storagePath: String?) = if (
      descriptorProto.hasOptions() && descriptorProto.options.hasExtension(Datamodel.db)) {
      val extType = descriptorProto.options.getExtension(Datamodel.db)

      if (extType != null) {
        if (extType.path.isEmpty()) {
          extType.mode to generateDefaultWritePath(descriptor, nested, field, defaultMode)
        } else {
          extType.mode to extType.path
        }
      } else {
        (if (nested) {
          CollectionMode.NESTED
        } else {
          CollectionMode.GROUP
        }) to generateDefaultWritePath(descriptor, nested, field, defaultMode)
      }
    } else if (fieldProto != null && fieldProto.options.hasExtension(Datamodel.collection)) {
      // the field has an extension on it
      val extCollection = fieldProto.options.getExtension(Datamodel.collection)
      extCollection.mode to if (extCollection.path.isEmpty()) {
        // generate a default path
        generateDefaultWritePath(descriptor, nested, field, extCollection.mode)
      } else {
        extCollection.path
      }
    } else {
      defaultMode to generateDefaultWritePath(descriptor, nested, field, defaultMode)
    }

    // append parent prefix if we have one
    val parentPrefix = if (parentWrite != null) {
      "${parentWrite.path}/"
    } else {
      ""
    }

    return CollapsedMessage.Write(
      "$parentPrefix$storagePath/$id",
      disposition,
      storageMode,
      if (parentWrite != null) {
        Optional.of(parentWrite)
      } else {
        Optional.empty()
      },
      Optional.empty(),
      data
    )
  }

  /**
   * Extract a sub-message field, where a potential value means we may need to recurse and decode it, too. In cases
   * where the sub-message is a default value, only recurse if we are directed by configuration to include default
   * values while serializing.
   *
   * @param proto
   * @param base
   * @param field
   * @param fieldProto
   * @param fieldOptions
   * @param builder
   * @param dataMap
   * @param skipCollections
   * @return
   */
  private fun extractSubmessage(proto: Message,
                                base: Message?,
                                field: FieldDescriptor,
                                fieldProto: DescriptorProtos.FieldDescriptorProto,
                                fieldOptions: DatapointOptions?,
                                builder: Value.Builder,
                                dataMap: SerializedModel,
                                skipCollections: Boolean): Pair<Boolean, Value.Builder?> {
    // if the sub-message is a temporal instance, and not repeated, it's a special case. set it directly.
    if (!field.isRepeated && field.messageType.fullName == "google.protobuf.Timestamp")
      return serializeInstant(proto, base, field, builder)

    // check for other options that might affect serialization
    val fieldPersistenceOpts: FieldPersistenceOptions? = if (
      !field.isRepeated && fieldProto.options.hasExtension(Datamodel.field)) {
      fieldProto.options.getExtension(Datamodel.field)
    } else {
      null
    }

    if ((fieldPersistenceOpts?.type == CoreFieldType.REFERENCE ||
         fieldPersistenceOpts?.type == CoreFieldType.PARENT) &&
      proto.hasField(field)) {
      // parent and reference types should both be references, not sub-objects
      val subProto = proto.getField(field) as? Message ?:
        throw ModelSerializer.SerializationError("Unable to extract reference sub-message.")
      val (idValue, coordinates) = this.scanForIdProperty(
        subProto, field.messageType.fields)
      val (idContainer, idField) = coordinates

      return if (idValue == null || idContainer == null || idField == null) {
        // skip this, it has no value.
        true to builder.setNullValue(NullValue.NULL_VALUE)
      } else {
        // calculate the path for this ID field, and make it into a reference.
        val (prefix, id) = referenceForSubmessage(
          idContainer, idContainer.descriptorForType, idValue)
        false to builder.setReferenceValue(referenceValue(prefix, id))
      }
    }

    // if we're told to skip collections, scan for non-NESTED annotations first
    if (skipCollections) {
      // first things first: scan both the proto and the field for collection settings. if any collection setting other
      // than `NESTED` is found, skip this field, because we've been told to do that.
      val fieldTypeProto = field.messageType.toProto()
      val mode: CollectionMode = if (
        fieldProto.hasOptions() && fieldProto.options.hasExtension(Datamodel.collection)) {
        // field annotation overrides, because it has stronger context
        fieldProto.options.getExtension(Datamodel.collection).mode
      } else if (fieldTypeProto.hasOptions() && fieldTypeProto.options.hasExtension(Datamodel.db)) {
        // if there's no field annotation, check the model
        fieldTypeProto.options.getExtension(Datamodel.db).mode
      } else {
        CollectionMode.NESTED  // default mode
      }
      if (mode != CollectionMode.NESTED)
        // skip this object, it's not a nested write. we do that by adding a null placeholder.
        return true to builder.setNullValue(NullValue.NULL_VALUE)
    }

    // handle as a regular sub-message... which may be repeated
    if (field.isRepeated) {
      if (fieldOptions?.concrete == true)
        throw ModelSerializer.SerializationError("Cannot annotate repeated nested field with `concrete`.")

      // extract repeated messages
      val (isDefault, extractedValue) = extractValue<List<Message>>(proto, base, field)

      return when {
        // either no list at all, or an empty one, or some default value?
        extractedValue == null -> true to builder.setNullValue(NullValue.NULL_VALUE)
        isDefault || extractedValue.isEmpty() -> {
          if (emptyListsAsNulls) {
            true to builder.setNullValue(NullValue.NULL_VALUE)
          } else {
            true to builder.setArrayValue(ArrayValue.getDefaultInstance())
          }
        }

        // otherwise, we have a list with values.
        else -> {
          val objectList = ArrayValue.newBuilder()

          // we have a list with values
          for (message in extractedValue) {
            // it's eligible to be included
            val subStruct = MapValue.newBuilder()
            val subMap: SerializedModel = this.serialize(message, base = base)
            subStruct.putAllFields(subMap)

            // add it to the outer list value
            objectList.addValues(Value.newBuilder().setMapValue(subStruct))
          }

          false to builder.setArrayValue(objectList)
        }
      }
    } else {
      // just one, extract the sub-message
      val (isDefault, extractedValue) = extractValue<Message>(proto, base, field)
      return if (extractedValue == null || (isDefault && !includeDefaults)) {
        // if it's not there, or it's a default instance and we shouldn't include those, just return as null
        true to builder.setNullValue(NullValue.NULL_VALUE)
      } else {
        val subMap: SerializedModel = this.serialize(extractedValue)
        if (fieldOptions?.concrete == true) {
          // the field is "concrete," for an outer generic. this means we are tasked with applying each serialized field
          // value to the upper data map, for our parent object, rather than building a struct, which is nested under a
          // regular property (like a JSON object).
          val errorKeys = TreeSet<String>()
          subMap.entries.forEach { entry ->
            if (dataMap.containsKey(entry.key)) {
              if (entry.key == "key" && entry.value == dataMap[entry.key]) {
                // they are keys and they are identical, so it's fine.
                return@forEach
              } else {
                // we don't allow concrete models to trample properties on the base objects they inject values into. so,
                // file away this key, and prep to error hard.
                errorKeys.add(entry.key)
              }
            } else {
              // there is no collision, so, file the key away.
              dataMap[entry.key] = entry.value
            }
          }
          if (errorKeys.isNotEmpty()) {
            val formattedKeys = errorKeys.joinToString(", ") { "`$it`" }
            throw ModelSerializer.SerializationError(
              "Cannot handle property collisions for concrete model: $formattedKeys on "
                + "`${proto.descriptorForType.name}`, at field `${field.name}` on `${field.containingType.name}`.")
          }

          // add synthesized type property, then we're good
          dataMap[concreteTypeProperty] = Value.newBuilder().setStringValue(field.jsonName).build()
          false to null

        } else {
          // it's a normal struct that is eligible to be included in the parent's payload. generate it, and return it,
          // so  the parent can choose whether to include it.
          false to builder.setMapValue(MapValue.newBuilder().putAllFields(subMap))
        }
      }
    }
  }

  /**
   * Build a map where each value is a protocol buffer `Value`, and there is an entry for each enabled field in the
   * given code-generated message object.
   *
   * @param proto
   * @param base
   * @param skipCollections
   * @param collection
   * @param concrete
   * @param id
   * @return
   */
  fun serialize(proto: Message,
                base: Message? = null,
                skipCollections: Boolean = false,
                collection: Boolean = false,
                concrete: String? = null,
                id: Pair<String, Pair<Message?, FieldDescriptor?>>? = null): SerializedModel {
    val descriptor = proto.descriptorForType
    val fields = descriptor.fields
    val dataMap = serializedObject()

    // for each field, serialize raw and put it into the map
    fields@ for (field in fields) {
      val value = Value.newBuilder()

      // resolve field options, if any
      val fieldProto = field.toProto()
      val fieldOptions = if (fieldProto.hasOptions() && fieldProto.options.hasExtension(
          Datamodel.opts)) {
        fieldProto.options.getExtension(Datamodel.opts)
      } else {
        null
      }

      // persistence options for the field
      val persistenceOptions = if (
        fieldProto.hasOptions() && fieldProto.options.hasExtension(Datamodel.field)) {
        fieldProto.options.getExtension(Datamodel.field)
      } else {
        null
      }

      if (fieldOptions?.ephemeral == true)
        continue@fields

      val (wasNull, sentinel) = when (field.type) {
        FieldType.INT32, FieldType.UINT32, FieldType.SINT32,
        FieldType.INT64, FieldType.UINT64, FieldType.SINT64,
        FieldType.FIXED32, FieldType.FIXED64, FieldType.SFIXED32, FieldType.SFIXED64, FieldType.FLOAT,
        FieldType.DOUBLE -> extractAndSetValue(proto, base, field, value, value::setDoubleValue)
        FieldType.BOOL -> extractAndSetValue(proto, base, field, value, value::setBooleanValue)
        FieldType.STRING -> extractAndSetValue(proto, base, field, value, value::setStringValue)
        FieldType.BYTES -> extractAndSetValue(proto, base, field, value, bytesEncoder(value::setStringValue))
        FieldType.ENUM -> extractEnum(proto, base, field, value, persistenceOptions)
        FieldType.GROUP, FieldType.MESSAGE -> extractSubmessage(
          proto, base, field, fieldProto, fieldOptions, value, dataMap, skipCollections)
        else -> {
          // log a warning
          logging.warn("Unresolvable or null type for proto field: '${field.type}'. Skipping.")
          continue@fields
        }
      }

      // if field value is not null, it is always included. if it is null, it is only included if `includeNulls` is set
      // to `true`. in either case, if `sentinel` is null, some sub-routine handled it for us (likely via the `concrete`
      // annotation on a nested non-repeated sub-message), so we can safely skip it ourselves.
      if (sentinel != null && (!wasNull || (wasNull && (includeNulls || persistenceOptions?.explicit == true))))
      // if we get here, we were able to extract a value and set it on the value builder
        dataMap[field.name] = value.build()

      // enforce required-ness of fields. if we reach this line, there was no value or the value was skipped, so we make
      // sure the value was indeed null (a decision that is delegated to the type-specific sub-routine, considering that
      // proto will substitute empty models where sub-message nesting is accessed).
      else if (sentinel != null && wasNull && !collection && fieldOptions?.required == true
        && (field.type != FieldType.ENUM))
        throw ModelSerializer.SerializationError(
          "Required field was missing a value: `${field.name}` on record `${descriptor.name}`.")
    }
    return dataMap
  }

  /**
   * Recursive boundary for message collapsing operations. "Collapsing" a message refers to converting it into a
   * serialized set of operations, each of which represents an interaction with underlying storage, and collectively
   * which, define the underlying set of writes that constitute the materialized entity being written.
   *
   * Because this method mutates the values passed to it (for instance, [writes]), it has no return value, but is
   * instead dispatched for side-effects.
   *
   * @param proto Protocol message to collapse.
   * @param base Base message, for comparison (optional).
   * @param writes Set of writes - specify to pre-load, but typically unused (or, used for recursion internally).
   * @param nested Whether we are currently in a re-cursed state.
   * @param parent Parent operation which governs this one.
   * @param parentField Parent field which holds this one.
   * @param disposition Disposition for the resulting set of writes.
   * @param collection Internal flag.
   * @param concrete Internal flag.
   */
  private fun collapseMessage(proto: Message,
                              base: Message?,
                              writes: ArrayList<CollapsedMessage.Operation>,
                              nested: Boolean,
                              parent: CollapsedMessage.Operation?,
                              parentField: FieldDescriptor?,
                              disposition: ModelSerializer.WriteDisposition,
                              collection: Boolean = false,
                              concrete: String? = null) {
    // prepare to collapse
    val descriptor = proto.descriptorForType
    val descriptorProto = descriptor.toProto()
    val fields = descriptor.fields
    val subwrites = ArrayList<CollapsedMessage.Operation>()
    val (idValue, idProperty) = this.scanForIdProperty(proto, fields)

    if (base != null && base.descriptorForType.name != descriptor.name)
      throw ModelSerializer.SerializationError("Unable to serialize with merged object of foreign kind.")

    if (idValue == null)
      // additionally, fill in the property before we serialize
      throw ModelSerializer.SerializationError("Unable to resolve ID proto or field to fill autogenerated ID slot.")

    // serialize the entity, and any simple nested entities
    val dataMap = this.serialize(proto,
      base = base,
      skipCollections = true,
      collection = collection,
      concrete = concrete,
      id = idValue to idProperty)

    val writeOp = resolveWrite(
      descriptor, descriptorProto, dataMap, parentField, idValue, nested, parent, disposition)

    // if this passes, all fields are simple
    if (dataMap.size != descriptor.fields.size) {
      // for each field, serialize raw and put it into the map
      fields@ for (field in fields) {
        if (dataMap.containsKey(field.name))
        // we already have this field: probably because it's nested
          continue@fields

        // skip it if it's marked as concrete, or ephemeral
        val fieldProto = field.toProto()
        val fieldOptions = if (
          fieldProto.hasOptions() && fieldProto.options.hasExtension(Datamodel.opts)) {
          fieldProto.options.getExtension(Datamodel.opts)
        } else {
          null
        }
        if (fieldOptions?.concrete == true)
        // concrete fields can be skipped
          continue@fields

        val groupOptions = if (
          fieldProto.hasOptions() && fieldProto.options.hasExtension(Datamodel.collection)) {
          fieldProto.options.getExtension(Datamodel.collection)
        } else {
          null
        }

        val value = Value.newBuilder()
        val (wasNull, _) = when (field.type) {
          FieldType.GROUP, FieldType.MESSAGE -> {
            if (field.isRepeated) {
              // firstly, extract the list of messages...
              val (isDefault, extractedValue) = extractValue<List<Message>>(proto, base, field)
              if (extractedValue == null) {
                // specify as null
                true to value.setNullValue(NullValue.NULL_VALUE)
              } else if (extractedValue.isEmpty()) {
                // consider treating empty lists as nulls
                if (emptyListsAsNulls) {
                  true to value.setNullValue(NullValue.NULL_VALUE)
                } else {
                  // if empty lists should not be null, they end up as empty lists...
                  false to value.setArrayValue(ArrayValue.getDefaultInstance())
                }
              } else if (!isDefault || includeDefaults) {
                // it's either not a default, or it's a default and should be included. for each included model,
                // recurse to perform the same collapse routine.
                for (subMessage in extractedValue) {
                  this.collapseMessage(
                    subMessage, null, subwrites, true, writeOp, field, disposition,
                    collection = true,
                    concrete = groupOptions?.concrete)
                }
              }
              continue@fields
            } else {
              // resolve a write for the message type
              val (isDefault, extractedValue) = extractValue<Message>(proto, base, field)

              if (extractedValue == null) {
                // specify as null
                true to value.setNullValue(NullValue.NULL_VALUE)
              } else if (!isDefault || includeDefaults) {
                if (ModelMetadata.matchCollectionAnnotation(
                    field,
                    CollectionMode.COLLECTION)) {
                  // it's eligible to be included
                  this.collapseMessage(
                    extractedValue, base, subwrites, true, writeOp, field, disposition)
                  continue@fields
                } else {
                  val serialized = this.serialize(
                    extractedValue, null, false, collection, concrete, null)
                  val mapBuilder = MapValue.newBuilder()

                  serialized.entries.forEach { entry ->
                    mapBuilder.putFields(entry.key, entry.value)
                  }

                  false to value.setMapValue(mapBuilder)
                }
              } else {
                // sub-message is ineligible, because it is a default or null, and defaults or nulls are not eligible
                continue@fields
              }
            }
          }
          else ->
            // all other fields are simple types
            continue@fields
        }

        // if field value is not null, it is always included. if it is null, it is only included if `includeNulls`
        // is set to `true`.
        if (!wasNull || includeNulls)
        // if we get here, we were able to extract a value and set it on the value builder
          dataMap[field.name] = value.build()
      }
    }

    // resolve the root write operation
    writes.add(writeOp)
    writes.addAll(subwrites)
  }

  /**
   * Collapse a message instance according to its configured data model settings. This may include nested serialization
   * for sub-collections, and prep for group-based collections.
   *
   * @param proto
   * @param base
   * @param parent
   * @param disposition
   * @return
   */
  fun collapse(proto: Message,
               base: Message? = null,
               parent: Message? = null,
               disposition: ModelSerializer.WriteDisposition = defaultDisposition): CollapsedMessage {
    val subwrites: ArrayList<CollapsedMessage.Operation> = ArrayList()

    // if we have a parent, calculate the parent's path and ID
    val parentWrite = if (parent != null) {
      val parentDescriptor = parent.descriptorForType
      val (immediateParentId, _) = this.scanForIdProperty(
        parent, parentDescriptor.fields)
      if (immediateParentId == null)
        throw ModelSerializer.SerializationError("Cannot serialize with parent entity with undefined ID.")

      // calculate collection prefix, and add ID
      val immediateParentPath = this.collectionPath(parentDescriptor)
      val parentPath = this.recursiveParentPathPrefix(
        parent, parentDescriptor, "$immediateParentPath/$immediateParentId")
      CollapsedMessage.Parent(parentPath, Optional.of(parent))
    } else {
      null
    }
    if (parentWrite != null)
      subwrites.add(parentWrite)

    this.collapseMessage(proto, base, subwrites,
      false, parentWrite, null, disposition = disposition)

    return CollapsedMessage.of(subwrites)
  }

  /**
   * Serialize a model instance from the provided object type to a generic Java [Map], throwing exceptions
   * verbosely if we are unable to correctly, and properly export the record.
   *
   *
   * Records serialized in this manner are immutable, and the maps produced by this interface are sorted during
   * property insertion.
   *
   * @param input Input record object to serialize.
   * @return Serialized record data, of the specified output type.
   * @throws ModelDeflateException If the model fails to export or serialize for any reason.
   */
  @Nonnull
  @Throws(ModelDeflateException::class)
  override fun deflate(@Nonnull input: Model): SortedMap<String, *> {
    return serialize(input).data
  }

  companion object {
    /** Private logging pipe. */
    private val logging = Logging.logger(ObjectModelSerializer::class.java)

    /** Default write disposition setting. */
    private val defaultDisposition = ModelSerializer.WriteDisposition.BLIND

    /**
     * Name of a special property within Firebase, that denotes the concrete type for a given generic type. This property is
     * only specified in cases where the engine has detected and synthesized a concrete type, according to annotations in
     * the protos specified via the schema.
     */
    const val concreteTypeProperty = "concreteType"

    /**
     * Return an object model serializer tailored to the parameterized model specified with `M`, with the specified
     * serializer settings.
     *
     * @param includeDefaults Whether to include default field values.
     * @param includeNulls Whether to serialize nulls or simply omit them.
     * @param emptyListsAsNulls Whether to serialize empty lists as nulls, or as empty lists.
     * @param enumMode Enum serialization mode.
     * @param instantMode Temporal instant serialization mode.
     * @param <M> Model type to acquire an object model serializer for.
     * @return Serializer, customized to the specified type.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun <M: Message> withSettings(
      @Nonnull includeDefaults: Boolean,
      @Nonnull includeNulls: Boolean,
      @Nonnull emptyListsAsNulls: Boolean,
      @Nonnull enumMode: EnumSerializeMode,
      @Nonnull instantMode: InstantSerializeMode): ObjectModelSerializer<M> {
      return ObjectModelSerializer(
        includeDefaults,
        includeNulls,
        emptyListsAsNulls,
        enumMode,
        instantMode
      )
    }

    /**
     * Return an object model serializer tailored to the parameterized model specified with `M`, with default selections
     * for serializer settings.
     *
     * @param <M> Model type to acquire an object model serializer for.
     * @return Serializer, customized to the specified type.
     */
    fun <M: Message> defaultInstance(): ObjectModelSerializer<M> {
      return withSettings(
        includeDefaults = false,
        includeNulls = false,
        emptyListsAsNulls = true,
        enumMode = EnumSerializeMode.NAME,
        instantMode = InstantSerializeMode.TIMESTAMP
      )
    }

    /**
     * Special case: return a function that can Base64-encode raw bytes. Raw bytes fields are assumed to contain binary
     * data that is not safe to encode blindly with UTF-8.
     *
     * @param setter Value builder callback to dispatch once bytes are ready.
     */
    private fun bytesEncoder(setter: (String) -> Value.Builder): (ByteArray) -> Value.Builder = { bytes ->
      setter(Base64.getEncoder().encodeToString(bytes))
    }
  }
}
