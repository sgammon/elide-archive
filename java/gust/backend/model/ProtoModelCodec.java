package gust.backend.model;

import com.google.protobuf.Message;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import gust.backend.runtime.Logging;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;


/**
 * Defines a {@link ModelCodec} which uses Protobuf serialization to export and import protos to and from from raw
 * byte-strings. These formats are built into Protobuf and are considered extremely reliable, even across languages.
 *
 * <p>Two formats of Protobuf serialization are supported:
 * <ul>
 *   <li><b>Binary:</b> Most efficient format. Best for production use. Completely illegible to humans.</li>
 *   <li><b>ProtoJSON:</b> Protocol Buffers-defined JSON translation protocol.</li>
 * </ul></p>
 *
 * @see ModelCodec Generic model codec interface.
 */
@Immutable
@ThreadSafe
public final class ProtoModelCodec<Model extends Message> implements ModelCodec<Model, EncodedModel> {
  /** Default wire format mode. */
  private static final EncodingMode DEFAULT_FORMAT = EncodingMode.BINARY;

  /** Log pipe to use. */
  private static final Logger logging = Logging.logger(ProtoModelCodec.class);

  /** Protobuf wire format to use. */
  private final EncodingMode wireMode;

  /** Builder from which to spawn models. */
  private final Model instance;

  /** JSON printer utility, initialized when operating with `wireMode=JSON`. */
  private final @Nullable JsonFormat.Printer jsonPrinter;

  /** JSON parser utility, initialized when operating with `wireMode=JSON`. */
  private final @Nullable JsonFormat.Parser jsonParser;

  /** Serializer object. */
  private final @Nonnull ModelSerializer<Model, EncodedModel> serializer;

  /** De-serializer object. */
  private final @Nonnull ModelDeserializer<EncodedModel, Model> deserializer;

  /**
   * Private constructor. Use static factory methods.
   *
   * @see #forModel(Message) To spawn a proto-codec for a given model.
   * @param instance Model instance (empty) to use for type information.
   * @param mode Mode to apply to this codec instance.
   * @param registry Optional type registry of other types to use with {@link JsonFormat}.
   */
  private ProtoModelCodec(@Nonnull Model instance, @Nonnull EncodingMode mode, @Nullable TypeRegistry registry) {
    this.wireMode = mode;
    this.instance = instance;
    this.serializer = new ProtoMessageSerializer();
    this.deserializer = new ProtoMessageDeserializer();

    if (logging.isTraceEnabled())
      logging.trace(String.format("Initializing `ProtoModelCodec` with format %s.", mode.name()));

    if (mode == EncodingMode.JSON) {
      TypeRegistry resolvedRegisry = registry != null ? registry : TypeRegistry.newBuilder()
        .add(instance.getDescriptorForType())
        .build();

      this.jsonParser = JsonFormat.parser()
        .usingTypeRegistry(resolvedRegisry);

      this.jsonPrinter = JsonFormat.printer()
        .usingTypeRegistry(resolvedRegisry)
        .sortingMapKeys()
        .omittingInsignificantWhitespace();

    } else {
      this.jsonParser = null;
      this.jsonPrinter = null;
    }
  }

  // -- Factories -- //

  /**
   * Acquire a Protobuf model codec for the provided model instance. The codec will operate in the default
   * {@link EncodingMode} unless specified otherwise via the other method variants on this object.
   *
   * @param <M> Model instance type.
   * @param instance Model instance to return a codec for.
   * @return Model codec which serializes and de-serializes to/from Protobuf wire formats.
   */
  @SuppressWarnings({"WeakerAccess", "unused"})
  public @Nonnull static <M extends Message> ProtoModelCodec<M> forModel(@Nonnull M instance) {
    return forModel(instance, DEFAULT_FORMAT);
  }

  /**
   * Acquire a Protobuf model codec for the provided model instance. The codec will operate in the default
   * {@link EncodingMode} unless specified otherwise via the other method variants on this object.
   *
   * @param <M> Model instance type.
   * @param instance Model instance to return a codec for.
   * @param mode Wire format mode to operate in (one of {@code JSON} or {@code BINARY}).
   * @return Model codec which serializes and de-serializes to/from Protobuf wire formats.
   */
  public @Nonnull static <M extends Message> ProtoModelCodec<M> forModel(@Nonnull M instance,
                                                                         @Nonnull EncodingMode mode) {
    return forModel(instance, mode, Optional.empty());
  }

  /**
   * Acquire a Protobuf model codec for the provided model instance. The codec will operate in the default
   * {@link EncodingMode} unless specified otherwise via the other method variants on this object.
   *
   * @param <M> Model instance type.
   * @param instance Model instance to return a codec for.
   * @param mode Wire format mode to operate in (one of {@code JSON} or {@code BINARY}).
   * @return Model codec which serializes and de-serializes to/from Protobuf wire formats.
   */
  @SuppressWarnings("WeakerAccess")
  public @Nonnull static <M extends Message> ProtoModelCodec<M> forModel(@Nonnull M instance,
                                                                         @Nonnull EncodingMode mode,
                                                                         @Nonnull Optional<TypeRegistry> registry) {
    return new ProtoModelCodec<>(
      instance,
      mode,
      registry.orElse(null));
  }

  /** Serializes model instances into raw bytes, according to Protobuf wire protocol semantics. */
  private final class ProtoMessageSerializer implements ModelSerializer<Model, EncodedModel> {
    /**
     * Serialize a model instance from the provided object type to the specified output type, throwing exceptions
     * verbosely if we are unable to correctly, verifiably, and properly export the record.
     *
     * @param input Input record object to serialize.
     * @return Serialized record data, of the specified output type.
     * @throws ModelDeflateException If the model fails to export or serialize for any reason.
     */
    @Override
    public @Nonnull EncodedModel deflate(@Nonnull Message input) throws ModelDeflateException, IOException {
      if (logging.isDebugEnabled())
        logging.debug(String.format(
          "Deflating record of type '%s' with format %s.",
          input.getDescriptorForType().getFullName(),
          wireMode.name()));

      if (wireMode == EncodingMode.BINARY) {
        return EncodedModel.wrap(
          input.getDescriptorForType().getFullName(),
          wireMode,
          input.toByteArray());
      } else {
        return EncodedModel.wrap(
          input.getDescriptorForType().getFullName(),
          wireMode,
          Objects.requireNonNull(jsonPrinter).print(input).getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  /** De-serializes model instances from raw bytes, according to Protobuf wire protocol semantics. */
  private final class ProtoMessageDeserializer implements ModelDeserializer<EncodedModel, Model> {
    /**
     * De-serialize a model instance from the provided input type, throwing exceptions verbosely if we are unable to
     * correctly, verifiably, and properly load the record.
     *
     * @param data Input data or object from which to load the model instance.
     * @return De-serialized and inflated model instance. Always a {@link Message}.
     * @throws ModelInflateException If the model fails to load for any reason.
     */
    @Override
    public @Nonnull Model inflate(@Nonnull EncodedModel data) throws ModelInflateException, IOException {
      if (logging.isDebugEnabled())
        logging.debug(String.format(
          "Inflating record of type '%s' with format %s.",
          data.getType(),
          wireMode.name()));

      if (wireMode == EncodingMode.BINARY) {
        //noinspection unchecked
        return (Model)instance.newBuilderForType().mergeFrom(data.getRawBytes().toByteArray()).build();
      } else {
        Message.Builder builder = instance.newBuilderForType();
        Objects.requireNonNull(jsonParser).merge(
          data.getRawBytes().toStringUtf8(),
          builder);

        //noinspection unchecked
        return (Model)builder.build();  // need to install proto JSON
      }
    }
  }

  // -- API: Codec -- //
  /**
   * Acquire an instance of the {@link ModelSerializer} attached to this adapter. The instance is not guaranteed to be
   * created fresh for this invocation.
   *
   * @return Serializer instance.
   * @see #deserializer() For the inverse of this method.
   * @see #deserialize(Object) To call into de-serialization directly.
   */
  @Override
  public @Nonnull ModelSerializer<Model, EncodedModel> serializer() {
    return this.serializer;
  }

  /**
   * Acquire an instance of the {@link ModelDeserializer} attached to this adapter. The instance is not guaranteed to be
   * created fresh for this invocation.
   *
   * @return Deserializer instance.
   * @see #serializer() For the inverse of this method.
   * @see #serialize(Message) To call into serialization directly.
   */
  @Override
  public @Nonnull ModelDeserializer<EncodedModel, Model> deserializer() {
    return this.deserializer;
  }
}
