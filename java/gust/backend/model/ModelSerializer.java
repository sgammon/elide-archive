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
