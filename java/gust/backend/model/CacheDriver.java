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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.Message;
import gust.backend.runtime.ReactiveFuture;

import javax.annotation.Nonnull;
import java.util.*;


/**
 * Describes the surface of a <i>cache driver</i>, which is a partner object to a {@link PersistenceDriver} specifically
 * tailored to deal with caching engines. Cache drivers may be used with any {@link ModelAdapter} implementation for
 * transparent read-through caching support.
 *
 * <p>Caches implemented in this manner are expected to adhere to options defined on {@link CacheOptions}, particularly
 * with regard to eviction and timeouts. Specific implementations may extend that interface to define custom options,
 * which may be provided to the implementation at runtime either via stubbed options parameters or app config.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public interface CacheDriver<Key extends Message, Model extends Message> {
  /**
   * Flush the entire cache managed by this driver. This should drop all keys related to model instance caching that are
   * currently held by the cache.
   *
   * @param executor Executor to use for any async operations.
   * @return Future, which simply completes when the flush is done.
   */
  @Nonnull ReactiveFuture flush(@Nonnull ListeningScheduledExecutorService executor);

  /**
   * Write a record ({@code model}) at {@code key} into the cache, overwriting any model currently stored at the same
   * key, if applicable. The resulting future completes with no value, when the cache write has finished, to let the
   * framework know the cache is done following up.
   *
   * @param key Key for the record we should inject into the cache.
   * @param model Record data to inject into the cache.
   * @param executor Executor to use for any async operations.
   * @return Future, which simply completes when the write is done.
   */
  @Nonnull ReactiveFuture put(@Nonnull Key key,
                              @Nonnull Model model,
                              @Nonnull ListeningScheduledExecutorService executor);

  /**
   * Force-evict any cached record at the provided {@code key} in the cache managed by this driver. This operation is
   * expected to succeed in all cases and perform its work in an idempotent manner.
   *
   * @param key Key for the record to force-evict from the cache.
   * @param executor Executor to use for any async operations.
   * @return Future, which resolves to the evicted key when the operation completes.
   */
  @Nonnull ReactiveFuture<Key> evict(@Nonnull Key key, @Nonnull ListeningScheduledExecutorService executor);

  /**
   * Force-evict the set of cached records specified by {@code keys}, in the cache managed by this driver. This
   * operation is expected to succeed in all cases and perform its work in an idempotent manner, similar to the single-
   * key version of this method (see: {@link #evict(Message, ListeningScheduledExecutorService)}).
   *
   * @param keys Set of keys to evict from the cache.
   * @param executor Executor to sue for any async operations.
   * @return Future, which simply completes when the bulk-evict operation is done.
   */
  default @Nonnull ReactiveFuture evict(@Nonnull Iterable<Key> keys,
                                        @Nonnull ListeningScheduledExecutorService executor) {
    List<ListenableFuture<?>> evictions = new ArrayList<>();
    keys.forEach((key) -> evictions.add(evict(key, executor)));
    return ReactiveFuture.wrap(Futures.allAsList(evictions));
  }

  /**
   * Attempt to resolve a known model, addressed by {@code key}, from the cache powered/backed by this driver, according
   * to {@code options} and making use of {@code executor}.
   *
   * <p>If no value is available in the cache, {@link Optional#empty()} must be returned, which triggers a call to the
   * driver to resolve the record. If the record can be fetched originally, it will later be added to the cache by a
   * separate call to {@link #put(Message, Message, ListeningScheduledExecutorService)}.</p>
   *
   * @param key Key for the record which we should look for in the cache.
   * @param options Options to apply to the fetch routine taking place.
   * @param executor Executor to use for async tasks. Provided by the driver or adapter.
   * @return Future value, which resolves either to {@link Optional#empty()} or a wrapped result value.
   */
  @Nonnull ReactiveFuture<Optional<Model>> fetch(@Nonnull Key key,
                                                 @Nonnull FetchOptions options,
                                                 @Nonnull ListeningScheduledExecutorService executor);
}
