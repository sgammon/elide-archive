package gust.backend.model;


import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import java.io.IOException;


/**
 * Describes the surface interface of an object responsible for <i>serializing</i> business data objects (hereinafter,
 * "models"). In other words, converting {@link Message} instances into some generic type <pre>Output</pre>.
 *
 * @param <Model> Data model which a given serializer implementation is responsible for adapting.
 * @param <Output> Output type which the serializer will provide when invoked with a matching model instance.
 */
public interface ModelSerializer<Model extends Message, Output> {
  /**
   * Serialize a model instance from the provided object type to the specified output type, throwing exceptions
   * verbosely if we are unable to correctly, verifiably, and properly export the record.
   *
   * @param input Input record object to serialize.
   * @return Serialized record data, of the specified output type.
   * @throws ModelDeflateException If the model fails to export or serialize for any reason.
   * @throws IOException If an IO error of some kind occurs.
   */
  @Nonnull Output deflate(@Nonnull Model input) throws ModelDeflateException, IOException;
}
