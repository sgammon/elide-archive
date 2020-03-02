package gust.backend.model;

import com.google.protobuf.Message;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;


/**
 * Specifies a {@link ModelCodec} which uses {@link CollapsedMessage} instances and native Java types to model business
 * data. This codec uses the partner {@link ObjectModelSerializer} and {@link ObjectModelDeserializer} to go between
 * these alternate/intermediate representations and {@link Message} instances.
 */
@Immutable
@ThreadSafe
public final class ObjectModelCodec<Model extends Message> implements ModelCodec<Model, Map<String, ?>> {
  /** Model builder instance to use for spawning models. */
  private final Message.Builder builder;

  /**
   * Private constructor. Creates a new object model codec from scratch.
   *
   * @param builder Builder for the message which is handled by this codec instance.
   */
  private ObjectModelCodec(@Nonnull Message.Builder builder) {
    this.builder = builder;
  }

  /**
   * Create or resolve an {@link ObjectModelCodec} instance for the provided model type. Object model codecs are
   * immutable and share no state, so they may be shared between threads for a given type.
   *
   * @param <M> Model type for which we are acquiring an object codec.
   * @param messageInstance Message instance (empty) to use for type information.
   * @return Object model codec for the provided data model.
   */
  public static @Nonnull <M extends Message> ObjectModelCodec<M> forModel(M messageInstance) {
    return forModel(messageInstance.newBuilderForType());
  }

  /**
   * Create or resolve an {@link ObjectModelCodec} instance for the provided model builder. Object model codecs are
   * immutable and share no state, so they may be shared between threads for a given type.
   *
   * @param <M> Model type for which we are acquiring an object codec.
   * @param messageBuilder Message builder (empty) to use for type information.
   * @return Object model codec for the provided data model.
   */
  public static @Nonnull <M extends Message> ObjectModelCodec<M> forModel(Message.Builder messageBuilder) {
    return new ObjectModelCodec<>(messageBuilder);
  }

  // -- Components -- //
  /**
   * Acquire an instance of the {@link ModelSerializer} attached to this adapter.
   *
   * @return Serializer instance.
   */
  @Override
  public @Nonnull ModelSerializer<Model, Map<String, ?>> serializer() {
    return null;
  }

  /**
   * Acquire an instance of the {@link ModelDeserializer} attached to this adapter.
   *
   * @return Deserializer instance.
   */
  @Override
  public @Nonnull ModelDeserializer<Map<String, ?>, Model> deserializer() {
    return null;
  }
}
