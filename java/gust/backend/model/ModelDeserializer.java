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
 * Describes the interface for a de-serializer, which is responsible for transitioning (adapting) objects from a given
 * type (or tree of types) to {@link Message} instances, corresponding with the data record type managed by this object.
 *
 * <p>Deserializers are part of a wider set of objects, including the inverse of this object, which is called a
 * {@link ModelSerializer}. Generally these objects come in matching pairs, but sometimes they are mixed and matched as
 * needed. Pairs of serializers/de-serializers are grouped by a {@link ModelCodec}.</p>
 *
 * @see ModelSerializer The inverse of this object, which is responsible for adapting {@link Message} instances to some
 *      other type of object or data.
 * @param <Input> Input data type, which this de-serializer will convert into a message instance.
 * @param <Model> Message object type, which is the output of this de-serializer.
 */
public interface ModelDeserializer<Input, Model extends Message> {
  /**
   * De-serialize a model instance from the provided input type, throwing exceptions verbosely if we are unable to
   * correctly, verifiably, and properly load the record.
   *
   * @param input Input data or object from which to load the model instance.
   * @return De-serialized and inflated model instance. Always a {@link Message}.
   * @throws ModelInflateException If the model fails to load for any reason.
   * @throws IOException If some IO error occurs.
   */
  @Nonnull Model inflate(@Nonnull Input input) throws ModelInflateException, IOException;

  /** Describes errors that occur during model deserialization or inflation activities. */
  final class DeserializationError extends RuntimeException {
    /**
     * Create a generic de-serializer error from the provided message.
     *
     * @param message Error message.
     */
    DeserializationError(@Nonnull String message) {
      super(message);
    }
  }
}
