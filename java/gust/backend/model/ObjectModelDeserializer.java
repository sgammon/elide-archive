package gust.backend.model;

import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import java.util.Map;


/**
 * Specifies a deserializer which is capable of converting generic Java {@link Map} objects (expected to have
 * {@link String} keys) into arbitrary {@link Message} types.
 *
 * @param <Model> Model record type which this serializer is responsible for converting.
 */
public final class ObjectModelDeserializer<Model extends Message> implements ModelDeserializer<Map<String, ?>, Model> {
  /**
   * De-serialize a model instance from a generic Java map type, throwing exceptions verbosely if we are unable to
   * correctly, verifiably, and properly load the record.
   *
   * @param input Input data or object from which to load the model instance.
   * @return De-serialized and inflated model instance. Always a {@link Message}.
   * @throws ModelInflateException If the model fails to load for any reason.
   */
  @Nonnull
  @Override
  public Model inflate(@Nonnull Map<String, ?> input) throws ModelInflateException {
    return null;
  }
}
