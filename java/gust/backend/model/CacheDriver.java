package gust.backend.model;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.Message;
import gust.backend.runtime.ReactiveFuture;

import javax.annotation.Nonnull;
import java.util.Optional;


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
   * Write a record ({@code model}) at {@code key} into the cache, overwriting any model currently stored at the same
   * key, if applicable. The resulting future completes with no value, when the cache write has finished, to let the
   * framework know the cache is done following up.
   *
   * @param key Key for the record we should inject into the cache.
   * @param model Record data to inject into the cache.
   * @param executor Executor to use for any async operations.
   * @return Future, which simply completes when the write is done.
   */
  @Nonnull ReactiveFuture injectRecord(@Nonnull Key key,
                                       @Nonnull Model model,
                                       @Nonnull ListeningScheduledExecutorService executor);

  /**
   * Attempt to resolve a known model, addressed by {@code key}, from the cache powered/backed by this driver, according
   * to {@code options} and making use of {@code executor}.
   *
   * <p>If no value is available in the cache, {@link Optional#empty()} must be returned, which triggers a call to the
   * driver to resolve the record. If the record can be fetched originally, it will later be added to the cache by a
   * separate call to {@link #injectRecord(Message, Message, ListeningScheduledExecutorService)}.</p>
   *
   * @param key Key for the record which we should look for in the cache.
   * @param options Options to apply to the fetch routine taking place.
   * @param executor Executor to use for async tasks. Provided by the driver or adapter.
   * @return Future value, which resolves either to {@link Optional#empty()} or a wrapped result value.
   */
  @Nonnull ReactiveFuture<Optional<Model>> fetchCached(@Nonnull Key key,
                                                       @Nonnull FetchOptions options,
                                                       @Nonnull ListeningScheduledExecutorService executor);
}
