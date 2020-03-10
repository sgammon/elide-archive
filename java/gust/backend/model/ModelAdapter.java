package gust.backend.model;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.Message;
import gust.backend.runtime.ReactiveFuture;
import tools.elide.core.DatapointType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static gust.backend.model.ModelMetadata.*;


/**
 * Specifies an adapter for data models. "Adapters" are responsible for handling data storage and recall, and generic
 * model serialization and deserialization activities. Adapters are composed of a handful of components, which together
 * define the functionality that composes the adapter writ-large.
 *
 * <p>Major components of functionality are described below:
 * <ul>
 *   <li><b>Codec:</b> The {@link ModelCodec} is responsible for serialization and deserialization. In some cases,
 *   codecs can be mixed with other objects to customize how data is stored. For example, the Redis cache layer supports
 *   using ProtoJSON, Protobuf binary, or JVM serialization, optionally with compression. On the other hand, the
 *   Firestore adapter specifies its own codecs which serialize into Firestore models.</li>
 *   <li><b>Driver:</b> The {@link PersistenceDriver} is responsible for persisting serialized/collapsed models into
 *   underlying storage, deleting data recalling data via key fetches, and querying indexes to produce result-sets.</li>
 * </ul></p>
 *
 * @see PersistenceDriver Interface which defines basic driver functionality.
 * @see CacheDriver Cache-specific persistence driver support, included in this object.
 * @see DatabaseAdapter Extends this interface with richer data engine features.
 * @param <Key> Key type, instances of which uniquely address instances of {@code Model}.
 * @param <Model> Model type which this adapter is responsible for adapting.
 * @param <Intermediate> Intermediate record format used by the implementation when de-serializing model instances.
 */
@SuppressWarnings("UnstableApiUsage")
public interface ModelAdapter<Key extends Message, Model extends Message, Intermediate>
  extends PersistenceDriver<Key, Model, Intermediate> {
  // -- Interface: Drivers -- //
  /**
   * Return the cache driver in use for this particular model adapter. If a cache driver is present, and active/enabled
   * according to database driver settings, it will be used on read-paths (such as fetching objects by ID).
   *
   * @return Cache driver currently in use by this model adapter.
   */
  @Nonnull Optional<CacheDriver<Key, Model>> cache();

  /**
   * Return the lower-level {@link PersistenceDriver} powering this adapter. The driver is responsible for communicating
   * with the actual backing storage service, either via local stubs/emulators or a production API.
   *
   * @return Persistence driver instance currently in use by this model adapter.
   */
  @Nonnull PersistenceDriver<Key, Model, Intermediate> engine();

  // -- Interface: Execution -- //
  /** {@inheritDoc} */
  @Override
  default @Nonnull ListeningScheduledExecutorService executorService() {
    return engine().executorService();
  }

  // -- Interface: Key Generation -- //
  /** {@inheritDoc} */
  @Override
  default @Nonnull Key generateKey(@Nonnull Message instance) {
    return engine().generateKey(instance);
  }

  // -- Interface: Fetch -- //
  /** {@inheritDoc} */
  @Override
  default @Nonnull ReactiveFuture<Optional<Model>> retrieve(@Nonnull Key key, @Nonnull FetchOptions options) {
    enforceRole(key, DatapointType.OBJECT_KEY);
    final ListeningScheduledExecutorService exec = options.executorService().orElseGet(this::executorService);
    if (Internals.logging.isTraceEnabled())
      Internals.logging.trace(format("Retrieving record '%s' from storage (executor: '%s')...", id(key), exec));

    final Optional<CacheDriver<Key, Model>> cache = this.cache();
    if (options.enableCache() && cache.isPresent()) {
      if (Internals.logging.isDebugEnabled())
        Internals.logging.debug(
          format("Caching enabled with object of type '%s'.", cache.get().getClass().getSimpleName()));

      // cache result future
      final ReactiveFuture<Optional<Model>> cacheFetchFuture = Objects.requireNonNull(
        cache.get().fetch(key, options, exec), "Cache cannot return `null` for `retrieve`.");

      // wrap in a future, with a non-propagating cancelling timeout, which handles any nulls from the cache.
      final ListenableFuture<Optional<Model>> cacheFuture = (Futures.nonCancellationPropagating(
        Futures.transform(cacheFetchFuture, new Function<>() {
          @Override
          public @Nonnull Optional<Model> apply(@Nullable Optional<Model> cacheResult) {
            if (Internals.logging.isDebugEnabled()) {
              //noinspection OptionalAssignedToNull
              Internals.logging.debug(
                format("Received response from cache (value present: '%s').",
                  cacheResult == null ? "null" : cacheResult.isPresent()));
            }
            if (cacheResult != null && cacheResult.isPresent()) {
              return cacheResult;
            }
            return Optional.empty();  // not found
          }
        }, exec)));

      // wrap the cache future in a timeout function, which enforces the configured (or default) cache timeout
      final ListenableFuture<Optional<Model>> limitedCacheFuture = Futures.withTimeout(
        cacheFuture,
        options.cacheTimeout().orElse(PersistenceDriver.DEFAULT_CACHE_TIMEOUT),
        options.cacheTimeoutUnit(),
        exec);

      // finally, respond to a cache miss by deferring to the driver directly. this must be separate from `cacheFuture`
      // to allow separate cancellation of the cache future and the future which backstops it.
      return ReactiveFuture.wrap(Futures.transformAsync(limitedCacheFuture, new AsyncFunction<>() {
        @Override
        public @Nonnull ListenableFuture<Optional<Model>> apply(@Nullable Optional<Model> cacheResult) {
          if (Internals.logging.isTraceEnabled()) {
            //noinspection OptionalAssignedToNull
            Internals.logging.debug(
              format("Returning response from cache (value present: '%s')",
                cacheResult == null ? "null" : cacheResult.isPresent()));
          }

          if (cacheResult != null && cacheResult.isPresent()) {
            return Futures.immediateFuture(cacheResult);
          } else {
            var record = engine().retrieve(key, options);
            record.addListener(() -> {
              if (Internals.logging.isDebugEnabled()) {
                Internals.logging.debug("Response was NOT cached. Storing in cache...");
              }

              Internals.swallowExceptions(() -> {
                Optional<Model> fetchResult = record.get();
                if (fetchResult.isPresent()) {
                  cache.get().put(
                    key,
                    fetchResult.get(),
                    options.executorService().orElseGet(ModelAdapter.this::executorService));
                }
              });
            }, options.executorService().orElseGet(ModelAdapter.this::executorService));
            return record;
          }
        }
      }, exec), exec);
    } else {
      if (Internals.logging.isDebugEnabled()) {
        Internals.logging.debug("Caching is disabled. Deferring to driver.");
      }
      return engine().retrieve(key, options);
    }
  }

  // -- Interface: Persist -- //
  /** {@inheritDoc} */
  @Override
  default @Nonnull ReactiveFuture<Model> persist(@Nullable Key key,
                                                 @Nonnull Model model,
                                                 @Nonnull WriteOptions options) {
    return engine().persist(key, model, options);
  }

  // -- Interface: Delete -- //
  /** {@inheritDoc} */
  @Override
  default @Nonnull ReactiveFuture<Key> delete(@Nonnull Key key,
                                              @Nonnull DeleteOptions options) {
    ReactiveFuture<Key> op = engine().delete(key, options);
    if (options.enableCache()) {
      // if caching is enabled and a cache driver is present, make sure to evict any cached record behind this key.
      Optional<CacheDriver<Key, Model>> cacheDriver = this.cache();
      if (cacheDriver.isPresent()) {
        ListeningScheduledExecutorService exec = options.executorService().orElseGet(this::executorService);
        ReactiveFuture<Key> storageDelete = engine().delete(key, options);
        ReactiveFuture<Key> cacheEvict = cacheDriver.get().evict(key, exec);
        return ReactiveFuture.wrap(Futures.whenAllComplete(storageDelete, cacheEvict).call(() -> key, exec));
      }
    }
    return op;
  }
}
