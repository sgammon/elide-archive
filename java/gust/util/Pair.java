package gust.util;

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
