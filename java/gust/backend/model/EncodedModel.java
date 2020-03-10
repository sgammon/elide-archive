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

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;


/**
 * Container class for an encoded Protocol Buffer model. Holds raw encoded model data, in any of Protobuf's built-in
 * well-defined serialization formats (for instance {@code BINARY} or {@code JSON}).
 *
 * <p>Raw model data is encoded before being held by this record. In addition to holding the raw data, it also keeps
 * the fully-qualified path to the model that the data came from, and the serialization format the data lives in. After
 * being wrapped in this class, a batch of model data is additionally compliant with {@link Serializable}.</p>
 */
@Immutable
@ThreadSafe
@SuppressWarnings({"unused", "WeakerAccess"})
public final class EncodedModel implements Serializable, Cloneable {
  private static final long serialVersionUID = 1L;

  /** Raw bytes of the enclosed model. */
  private @Nonnull byte[] rawBytes;

  /** Type of model held by this entity. */
  private @Nonnull String type;

  /** Operating mode for the underlying data. Always {@code BINARY} unless manually constructed. */
  private @Nonnull EncodingMode dataMode;

  /**
   * Initialize a new encoded model directly from a {@link Message}.
   *
   * @param rawBytes Raw bytes to hold.
   * @param mode Operating data mode (usually {@code BINARY}).
   * @param type Fully-qualified model type name.
   */
  private EncodedModel(@Nonnull byte[] rawBytes, @Nonnull EncodingMode mode, @Nonnull String type) {
    this.type = type;
    this.dataMode = mode;
    this.rawBytes = rawBytes;
  }

  /**
   * Write the attached encoded Protocol Buffer data to the specified object stream, such that this object is fully
   * packed for Java serialization purposes.
   *
   * @param out Output stream to write this encoded model object to.
   * @throws IOException If an IO error of some kind occurs.
   */
  private void writeObject(@Nonnull ObjectOutputStream out) throws IOException {
    out.writeObject(type);
    out.writeObject(dataMode);
    out.write(rawBytes.length);
    out.write(rawBytes);
  }

  /**
   * Re-inflate an encoded model from a Java serialization context. Read the stream to install local properties, such
   * that the object is re-constituted.
   *
   * @param in Input stream to read object data from.
   * @throws IOException If an IO error of some kind occurs.
   * @throws ClassNotFoundException If the specified Protobuf model cannot be found or resolved.
   */
  private void readObject(@Nonnull ObjectInputStream in) throws IOException,ClassNotFoundException {
    this.type = Objects.requireNonNull((String)in.readObject(),
      "Cannot deserialize EncodedModel with empty type.");
    this.dataMode = Objects.requireNonNull((EncodingMode)in.readObject(),
      "Cannot deserialize EncodedModel with empty data mode.");

    // read length-prefixed raw bytes
    int datasize = in.read();
    byte[] data = new byte[datasize];
    int read = in.read(data);
    assert datasize == read;
    this.rawBytes = data;
  }

  /**
   * Return an encoded representation of the provided message. This method is rather heavy-weight: it fully encodes the
   * provided Protobuf message into the Protocol Buffers binary format.
   *
   * <p>Note that this method must access the message's descriptor. If a descriptor is already on-hand, use the other
   * variant of this method.</p>
   *
   * @see #from(Message, Descriptors.Descriptor) If a descriptor is already on-hand for the provided message.
   * @param message Message to encode as binary.
   * @return Java-serializable wrapper including the encoded Protobuf model.
   */
  public static EncodedModel from(@Nonnull Message message) {
    return from(message, null);
  }

  /**
   * Return an encoded representation of the provided message. This method is rather heavy-weight: it fully encodes the
   * provided Protobuf message into the Protocol Buffers binary format.
   *
   * <p>If a descriptor is <i>not</i> already on-hand, use the other variant of this method, which accesses the
   * descriptor directly from the message.</p>
   *
   * @see #from(Message, Descriptors.Descriptor) If no descriptor is on-hand.
   * @param message Message to encode as binary.
   * @return Java-serializable wrapper including the encoded Protobuf model.
   */
  public static EncodedModel from(@Nonnull Message message, @Nullable Descriptors.Descriptor descriptor) {
    return new EncodedModel(
      message.toByteArray(),
      EncodingMode.BINARY,
      (descriptor != null ? descriptor : message.getDescriptorForType()).getFullName());
  }

  /**
   * Wrap a blob of opaque data, asserting that it is actually an encoded model record. Using this factory method, any
   * of the Protobuf serialization formats may be used safely with this class.
   *
   * <p>All details must be provided manually to this method variant. It is incumbent on the developer that they line
   * up properly. For safer options, see the other factory methods on this class.</p>
   *
   * @see #from(Message) To encode a model instance.
   * @see #from(Message, Descriptors.Descriptor) To encode a model instance with a descriptor already in-hand.
   * @param type Fully-qualified type name, for the encoded instance we are storing.
   * @param data Raw data for the encoded model to be wrapped.
   * @return Encoded model instance.
   */
  public static EncodedModel wrap(@Nonnull String type, @Nonnull EncodingMode mode, @Nonnull byte[] data) {
    return new EncodedModel(data, mode, type);
  }

  // -- Equals/Hash -- //

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EncodedModel that = (EncodedModel) o;
    return com.google.common.base.Objects.equal(type, that.type) &&
      dataMode == that.dataMode &&
      com.google.common.base.Objects.equal(
        Arrays.hashCode(rawBytes),
        Arrays.hashCode(that.rawBytes));
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return com.google.common.base.Objects
      .hashCode(type, dataMode, Arrays.hashCode(rawBytes));
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "EncodedModel{" +
      "dataMode='" + dataMode + '\'' +
      ", type=" + type +
      '}';
  }

  // -- Getters -- //

  /** @return Raw bytes held by this encoded model. */
  public @Nonnull ByteString getRawBytes() {
    return ByteString.copyFrom(this.rawBytes);
  }

  /** @return Fully-qualified path to the type of model backing this encoded instance. */
  public @Nonnull String getType() {
    return type;
  }

  /** @return Operating mode for the underlying model instance data. */
  public @Nonnull EncodingMode getDataMode() {
    return dataMode;
  }

  // -- Inflate -- //

  /**
   * Re-inflate the encoded model data held by this object, into an instance of {@code Model}, via the provided
   * {@code builder}.
   *
   * <p><b>Note:</b> before the model is returned from this method, it will be casted to match the generic type the user
   * is looking for. It is incumbent on the invoking developer to make sure the generic access that occurs won't produce
   * a {@link ClassCastException}. {@link #getType()} can be interrogated to resolve types before inflation.</p>
   *
   * @param model Empty model instance from which to resolve a parser.
   * @param <Model> Generic model type inflated and returned by this method.
   * @return Instance of the model, inflated from the encoded data.
   * @throws InvalidProtocolBufferException If the held data is incorrectly formatted.
   */
  public @Nonnull <Model extends Message> Model inflate(@Nonnull Message model) throws InvalidProtocolBufferException {
    if (dataMode == EncodingMode.JSON) {
      Message.Builder builder = model.newBuilderForType();
      JsonFormat.parser().merge(
        new String(this.rawBytes, StandardCharsets.UTF_8),
        builder);

      //noinspection unchecked
      return (Model)builder.build();
    } else {
      //noinspection unchecked
      return (Model)model.getParserForType().parseFrom(this.rawBytes);
    }
  }
}
