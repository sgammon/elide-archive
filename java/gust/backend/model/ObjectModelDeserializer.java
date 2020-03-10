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
