package gust.backend.model;


import com.google.protobuf.Message;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;


/**
 * Specifies a serializer which is capable of converting {@link Message} instances into generic Java {@link Map} objects
 * with regular {@link String} keys. If there are nested records on the model instance, they will be serialized into
 * recursive {@link Map} instances.
 *
 * @param <Model> Model record type which this serializer is responsible for converting.
 */
@Immutable
@ThreadSafe
public final class ObjectModelSerializer<Model extends Message> implements ModelSerializer<Model, Map<String, ?>> {
  /**
   * Serialize a model instance from the provided object type to a generic Java {@link Map}, throwing exceptions
   * verbosely if we are unable to correctly, verifiably, and properly export the record.
   *
   * <p>Records serialized in this manner are immutable.</p>
   *
   * @param input Input record object to serialize.
   * @return Serialized record data, of the specified output type.
   * @throws ModelDeflateException If the model fails to export or serialize for any reason.
   */
  @Nonnull
  @Override
  public Map<String, ?> deflate(@Nonnull Model input) throws ModelDeflateException {
    return null;
  }
}
