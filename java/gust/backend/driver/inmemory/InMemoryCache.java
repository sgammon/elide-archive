package gust.backend.driver.inmemory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.Message;
import gust.backend.model.*;
import gust.backend.runtime.ReactiveFuture;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.Optional;


/**
 * Defines a {@link CacheDriver} backed by a Guava in-memory cache, which statically holds onto cached full model
 * instances, potentially on behalf of some other persistence driver (via use with a {@link ModelAdapter}).
 *
 * <p>Cache options may be adjusted based on the operation being memoized, using the {@link CacheOptions} interface,
 * which is supported by various other higher-order options interfaces (i.e. {@link FetchOptions}).</p>
 *
 * @param <K> Type of key used with the cache and model.
 * @param <M> Type of model supported by this cache facade.
 */
@ThreadSafe
@SuppressWarnings("UnstableApiUsage")
public final class InMemoryCache<K extends Message, M extends Message> implements CacheDriver<K, M> {
  /** Static in-memory instance cache. */
  private final static @Nonnull InMemoryCaching CACHE = new InMemoryCaching();

  /** Responsible for managing static cache access. */
  private final static class InMemoryCaching {
    /** Internal backing cache storage. */
    private final @Nonnull Cache<String, Message> inMemoryCache;

    /** Initialize in-memory caching from scratch. */
    private InMemoryCaching() {
      inMemoryCache = CacheBuilder.newBuilder()
        .concurrencyLevel(2)
        .maximumSize(50)
        .expireAfterWrite(Duration.ofHours(1))
        .weakKeys()
        .recordStats()
        .build();
    }

    /**
     * Acquire the in-memory static cache, which holds onto model instances without requiring serialization. In-memory
     * caching may be used transparently with any backing {@link PersistenceDriver}.
     *
     * @return In-memory static model cache.
     */
    @Nonnull Cache<String, Message> acquire() {
      return inMemoryCache;
    }
  }

  /**
   * Acquire an instance of the in-memory caching driver, generalized to support the provided key type {@code K} and
   * model instance type {@code M}.
   *
   * @param <K> Generic type for the key associated with model type {@code M}.
   * @param <M> Generic model type managed by this cache.
   * @return Instance of the acquired cache engine.
   */
  static @Nonnull <K extends Message, M extends Message> InMemoryCache<K, M> acquire() {
    return new InMemoryCache<>();
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture put(@Nonnull Message key,
                                     @Nonnull Message model,
                                     @Nonnull ListeningScheduledExecutorService executor) {
    return ReactiveFuture.wrap(executor.submit(() -> {
      String id = (
        ModelMetadata.<String>id(key).orElseThrow(() -> new IllegalArgumentException("Cannot inject with empty key.")));
      CACHE.acquire().put(id, model);
    }), executor);
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Optional<M>> fetch(@Nonnull K key,
                                                    @Nonnull FetchOptions options,
                                                    @Nonnull ListeningScheduledExecutorService executor) {
    return ReactiveFuture.wrap(options.executorService().orElse(executor).submit(() -> {
      String id = (
        ModelMetadata.<String>id(key).orElseThrow(() -> new IllegalArgumentException("Cannot fetch empty key.")));
      Message cached = (CACHE.acquire().getIfPresent(id));

      //noinspection unchecked
      return cached == null ? Optional.empty() : Optional.of((M)cached);
    }), executor);
  }
}
