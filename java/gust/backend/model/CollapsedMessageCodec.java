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
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;


/**
 *
 * @param <Model>
 */
@Factory
@Immutable
@ThreadSafe
public final class CollapsedMessageCodec<Model extends Message> implements ModelCodec<Model, CollapsedMessage> {
  /** Builder proto-type used to spawn new instances. */
  private final Message.Builder builder;

  /** Singleton access to serializer. */
  private final CollapsedMessageSerializer serializer = new CollapsedMessageSerializer();

  /** Singleton access to de-serializer. */
  private final CollapsedMessageDeserializer deserializer = new CollapsedMessageDeserializer();

  private CollapsedMessageCodec(@Nonnull Message.Builder builder) { this.builder = builder; }

  /**
   * Create a collapsed message codec which adapts the provided builder to {@link CollapsedMessage} and back. These
   * "collapsed" messages follow the framework-defined protocol for serializing hierarchical data.
   *
   * @param <M> Model type for which we will constructor or otherwise resolve a collapsed message codec.
   * @return Collapsed message codec bound to the provided message type.
   */
  @Context
  public static @Nonnull <M extends Message> CollapsedMessageCodec<M> forModel(Message.Builder builder) {
    return new CollapsedMessageCodec<>(builder);
  }

  /**
   * Acquire an instance of the {@link ModelSerializer} attached to this adapter. The instance is not guaranteed to be
   * created fresh for this invocation.
   *
   * @return Serializer instance.
   * @see #deserializer() For the inverse of this method.
   * @see #deserialize(Object) To call into de-serialization directly.
   */
  @Override
  public @Nonnull ModelSerializer<Model, CollapsedMessage> serializer() {
    return serializer;
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
  public @Nonnull ModelDeserializer<CollapsedMessage, Model> deserializer() {
    return deserializer;
  }

  /** Serializes model instances into {@link CollapsedMessage} objects. */
  private final class CollapsedMessageSerializer implements ModelSerializer<Model, CollapsedMessage> {
    /**
     * Serialize a model instance from the provided object type to the specified output type, throwing exceptions
     * verbosely if we are unable to correctly, verifiably, and properly export the record.
     *
     * @param input Input record object to serialize.
     * @return Serialized record data, of the specified output type.
     * @throws ModelDeflateException If the model fails to export or serialize for any reason.
     */
    @Override
    public @Nonnull CollapsedMessage deflate(@Nonnull Message input) throws ModelDeflateException {
      return null;
    }
  }

  /** De-serializes model instances from {@link CollapsedMessage} objects. */
  private final class CollapsedMessageDeserializer implements ModelDeserializer<CollapsedMessage, Model> {
    /**
     * De-serialize a model instance from the provided input type, throwing exceptions verbosely if we are unable to
     * correctly, verifiably, and properly load the record.
     *
     * @param model Input data or object from which to load the model instance.
     * @return De-serialized and inflated model instance. Always a {@link Message}.
     * @throws ModelInflateException If the model fails to load for any reason.
     */
    @Override
    public @Nonnull Model inflate(@Nonnull CollapsedMessage model) throws ModelInflateException {
      return null;
    }
  }

  /** @return Builder for the model handled by this codec. */
  public Message.Builder getBuilder() {
    return builder;
  }
}
