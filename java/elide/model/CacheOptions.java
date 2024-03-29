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

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Specifies operational options related to caching. These options are usable orthogonally to the main
 * {@link OperationOptions} tree. See below for a description of each configurable property.
 *
 * <p><b>Cache configuration (<b>defaults</b> in parens):
 * <ul>
 *   <li>{@link #enableCache()} ({@code true}): Whether to allow caching at all.</li>
 *   <li>{@link #cacheTimeout()} ({@code 1}): Amount of time to give the cache before falling back to storage.</li>
 *   <li>{@link #cacheTimeoutUnit()} ({@code SECONDS}): Time unit to correspond with {@code cacheTimeoutValue}.</li>
 *   <li>{@link #cacheDefaultTTL()}: Default amount of time to let things stick around in the cache.</li>
 *   <li>{@link #cacheDefaultTTLUnit()}: Time unit to correspond with {@code cacheDefaultTTL}.</li>
 *   <li>{@link #cacheEvictionMode()}: Eviction mode to operate in.
 * </ul></p>
 */
public interface CacheOptions extends OperationOptions {
  /** Describes operating modes with regard to cache eviction. */
  enum EvictionMode {
    /** Flag to enable TTL enforcement. */
    TTL("Time-to-Live"),

    /** Least-Frequently-Used mode for cache eviction. */
    LFU("Least-Frequently Used"),

    /** Least-Recently-Used mode for cache eviction. */
    LRU("Least-Recently Used");

    /** Pretty label for this mode. */
    private final @Nonnull String label;

    EvictionMode(@Nonnull String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return String.format("EvictionMode(%s - %s)", this.name(), this.label);
    }

    /** @return Human-readable label for this eviction mode. */
    public @Nonnull String getLabel() {
      return label;
    }
  }

  /** @return Whether the cache should be enabled, if installed. Defaults to `true`. */
  default @Nonnull Boolean enableCache() {
    return true;
  }

  /** @return Value to apply to the cache timeout. If left unspecified, the global default is used. */
  default @Nonnull Optional<Long> cacheTimeout() {
    return Optional.of(2L);
  }

  /** @return Unit to apply to the cache timeout. If left unspecified, the global default is used. */
  default @Nonnull TimeUnit cacheTimeoutUnit() {
    return TimeUnit.SECONDS;
  }

  /** @return Default amount of time to let things remain in the cache. */
  default @Nonnull Optional<Long> cacheDefaultTTL() {
    return Optional.of(1L);
  }

  /** @return Unit to apply to the default cache lifetime (TTL) value. */
  default @Nonnull TimeUnit cacheDefaultTTLUnit() {
    return TimeUnit.HOURS;
  }

  /** @return Specifier describing the cache eviction mode to apply, if any. */
  default @Nonnull Optional<EvictionMode> cacheEvictionMode() {
    return Optional.of(EvictionMode.TTL);
  }
}
