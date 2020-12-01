
package gust.backend.model

import com.google.protobuf.Message
import javax.annotation.Nonnull
import javax.annotation.concurrent.Immutable
import javax.annotation.concurrent.ThreadSafe


/**
 * Specifies a [ModelCodec] which uses [CollapsedMessage] instances and native Java types to model business
 * data. This codec uses the partner [ObjectModelSerializer] and [ObjectModelDeserializer] to go between
 * these alternate/intermediate representations and [Message] instances.
 */
@Immutable
@ThreadSafe
class ObjectModelCodec<Model: Message> private constructor(@Nonnull instance: Model) :
  ModelCodec<Model, Map<String, *>, Map<String, *>> {
  /**
   * @return Builder for the model handled by this codec.
   */
  /** Model builder instance to use for spawning models.  */
  val builder: Message.Builder = instance.newBuilderForType()

  /** Serializer to use.  */
  private val serializer: ObjectModelSerializer<Model> = ObjectModelSerializer.defaultInstance()

  /** De-serializer to use.  */
  private val deserializer: ObjectModelDeserializer<Model> = ObjectModelDeserializer.defaultInstance(instance)

  // -- Components -- //
  /**
   * Acquire an instance of the [ModelSerializer] attached to this adapter.
   *
   * @return Serializer instance.
   */
  @Nonnull
  override fun serializer(): ModelSerializer<Model, Map<String, *>> {
    return serializer
  }

  /**
   * Acquire an instance of the [ModelDeserializer] attached to this adapter.
   *
   * @return Deserializer instance.
   */
  @Nonnull
  override fun deserializer(): ModelDeserializer<Map<String, *>, Model> {
    return deserializer
  }

  // -- Getters -- //

  companion object {
    /**
     * Create or resolve an [ObjectModelCodec] instance for the provided model type. Object model codecs are
     * immutable and share no state, so they may be shared between threads for a given type.
     *
     * @param <M> Model type for which we are acquiring an object codec.
     * @param messageInstance Message instance (empty) to use for type information.
     * @return Object model codec for the provided data model.
     */
    @Nonnull
    @Suppress("unused")
    fun <M : Message> forModel(messageInstance: M): ObjectModelCodec<M> {
      return forModel(messageInstance.newBuilderForType())
    }

    /**
     * Create or resolve an [ObjectModelCodec] instance for the provided model builder. Object model codecs are
     * immutable and share no state, so they may be shared between threads for a given type.
     *
     * @param <M> Model type for which we are acquiring an object codec.
     * @param messageBuilder Message builder (empty) to use for type information.
     * @return Object model codec for the provided data model.
     */
    @Nonnull
    @Suppress("unused")
    fun <M : Message> forModel(messageBuilder: Message.Builder): ObjectModelCodec<M> {
      @Suppress("UNCHECKED_CAST")
      return ObjectModelCodec(messageBuilder.defaultInstanceForType as M)
    }
  }
}
