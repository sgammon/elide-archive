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
 * Specifies the requisite interface for a data codec implementation. These objects are responsible for performing model
 * serialization and deserialization, within different circumstances. For example, models are used with databases via
 * adapters that serialize each model into a corresponding database object, or series of database calls.
 *
 * <p>Adapters are <i>bi-directional</i>, i.e., they must support transitioning both <i>to</i> and <i>from</i> message
 * representations, based on circumstance. Services are the only case where this is generally not necessary, because
 * gRPC handles serialization automatically.</p>
 *
 * @see ModelSerializer Surface definition for a model serializer.
 * @see ModelDeserializer Surface definition for a model de-serializer.
 * @param <Model> Model type which this codec is responsible for serializing and de-serializing.
 * @param <Intermediate> Intermediate record type which this codec converts model instances into.
 */
@SuppressWarnings("unused")
public interface ModelCodec<Model extends Message, Intermediate> {
  // -- Components -- //
  /**
   * Acquire an instance of the {@link ModelSerializer} attached to this adapter. The instance is not guaranteed to be
   * created fresh for this invocation.
   *
   * @see #deserializer() For the inverse of this method.
   * @see #deserialize(Object) To call into de-serialization directly.
   * @return Serializer instance.
   */
  @Nonnull ModelSerializer<Model, Intermediate> serializer();

  /**
   * Acquire an instance of the {@link ModelDeserializer} attached to this adapter. The instance is not guaranteed to be
   * created fresh for this invocation.
   *
   * @see #serializer() For the inverse of this method.
   * @see #serialize(Message) To call into serialization directly.
   * @return Deserializer instance.
   */
  @Nonnull ModelDeserializer<Intermediate, Model> deserializer();

  // -- Proxies -- //
  /**
   * Sugar shortcut to serialize a model through the current codec's installed {@link ModelSerializer}.
   *
   * <p>This method just proxies to that object (which can be acquired via {@link #serializer()}). If any error occurs
   * while serializing, {@link ModelDeflateException} is thrown.</p>
   *
   * @param instance Input model to serialize.
   * @return Serialized output data or object.
   * @throws ModelDeflateException If some error occurs while serializing the model.
   * @throws IOException If some IO error occurs.
   */
  default @Nonnull Intermediate serialize(Model instance) throws ModelDeflateException, IOException {
    return serializer().deflate(instance);
  }

  /**
   * Sugar shortcut to de-serialize a model through the current codec's installed {@link ModelDeserializer}.
   *
   * <p>This method just proxies to that object (which can be acquired via {@link #deserializer()}). If any error occurs
   * while de-serializing, {@link ModelInflateException} is thrown.</p>
   *
   * @param input Input data to de-serialize into a model instance.
   * @return Model instance, deserialized from the input data.
   * @throws ModelInflateException If some error occurs while de-serializing the model.
   * @throws IOException If some IO error occurs.
   */
  default @Nonnull Model deserialize(Intermediate input) throws ModelInflateException, IOException {
    return deserializer().inflate(input);
  }
}
