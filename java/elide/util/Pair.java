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
package elide.util;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;


/**
 * Simple container for a pair of values, one assigned to the name "key," and one assigned to the name "value."
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
@Immutable
@SuppressWarnings("FieldCanBeLocal")
public final class Pair<K, V> {
  /** Key for this pair. */
  private final K key;

  /** Value for this pair. */
  private final V value;

  /**
   * Construct a pair.
   *
   * @param key Key to assign.
   * @param value Value to assign.
   */
  private Pair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  /**
   * Spawn a new K-V pair.
   *
   * @param key   Key to use for the pair.
   * @param value Value to use for the pair.
   * @param <K>   Generic key type.
   * @param <V>   Generic value type.
   * @return Pair instance wrapping the provided key and value.
   */
  public static <K, V> Pair<K, V> of(@Nonnull K key, @Nonnull V value) {
    return new Pair<>(key, value);
  }

  /** @return Key. */
  public @Nonnull K getKey() {
    return key;
  }

  /** @return Value. */
  public @Nonnull V getValue() {
    return value;
  }
}
