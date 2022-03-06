/*
 * Copyright © 2022, The Elide Framework Authors. All rights reserved.
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
package elide.model;

import com.google.firestore.v1.Value;
import com.google.protobuf.Message;
import javax.annotation.Nonnull;
import java.util.*;


/** Describes a model which has been serialized into a backing map of keys and properties. */
public final class SerializedModel implements Map<String, Value> {
  /** Raw serialized property data for the backing message. */
  private final @Nonnull SortedMap<String, Value> data;

  /** Original model (message) that we serialized into `data`. */
  private final @Nonnull Optional<Message> message;

  /**
   * Create a serialized model object from scratch.
   *
   * @param data Serialized key-value pairs for the model.
   * @param message Original model message instance.
   */
  SerializedModel(@Nonnull SortedMap<String, Value> data,
                  @Nonnull Optional<Message> message) {
    this.data = data;
    this.message = message;
  }

  /**
   * Create an empty serialized model, for use as a container.
   *
   * @return Empty serialized model.
   */
  public static @Nonnull SerializedModel factory() {
    return factory(new TreeMap<>());
  }

  /**
   * Create a serialized model, pre-filled with the provided backing data.
   *
   * @param data Data to pre-fill the serialized model with.
   * @return Serialized model, pre-filled with the specified data.
   */
  public static @Nonnull SerializedModel factory(@Nonnull SortedMap<String, Value> data) {
    return new SerializedModel(data, Optional.empty());
  }

  /**
   * Create a serialized model, pre-filled with the provided backing data.
   *
   * @param data Data to pre-fill the serialized model with.
   * @param proto Message instance to wrap, for which `data` is provided.
   * @return Serialized model, pre-filled with the specified data.
   */
  public static @Nonnull SerializedModel wrap(@Nonnull SortedMap<String, Value> data,
                                              @Nonnull Message proto) {
    return new SerializedModel(data, Optional.of(proto));
  }

  // -- Getters -- //

  /** @return Underlying data for this serialized model instance. */
  @Nonnull public SortedMap<String, Value> getData() {
    return data;
  }

  /** @return Message instance which spawned this serialized model. */
  @Nonnull
  public Optional<Message> getMessage() {
    return message;
  }

  // -- Interface: Map -- //

  /** @inheritDoc */
  @Override public int size() {
    return data.size();
  }

  /** @inheritDoc */
  @Override public boolean isEmpty() {
    return data.isEmpty();
  }

  /** @inheritDoc */
  @Override public boolean containsKey(Object key) {
    return data.containsKey(key);
  }

  /** @inheritDoc */
  @Override public Value get(Object key) {
    return data.get(key);
  }

  /** @inheritDoc */
  @Override public Value put(String key, Value value) {
    return data.put(key, value);
  }

  /** @inheritDoc */
  @Override public Value remove(Object key) {
    return data.remove(key);
  }

  /** @inheritDoc */
  @Override public boolean containsValue(Object value) {
    return data.containsValue(value);
  }

  /** @inheritDoc */
  @Override public void putAll(Map<? extends String, ? extends Value> map) {
    data.putAll(map);
  }

  /** @inheritDoc */
  @Override public void clear() {
    data.clear();
  }

  /** @inheritDoc */
  @Override public Set<String> keySet() {
    return data.keySet();
  }

  /** @inheritDoc */
  @Override public Collection<Value> values() {
    return data.values();
  }

  /** @inheritDoc */
  @Override public Set<Entry<String, Value>> entrySet() {
    return data.entrySet();
  }
}
