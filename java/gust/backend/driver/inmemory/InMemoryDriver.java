package gust.backend.driver.inmemory;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import gust.backend.model.*;
import gust.backend.runtime.Logging;
import gust.backend.runtime.ReactiveFuture;
import org.slf4j.Logger;
import tools.elide.core.DatapointType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static gust.backend.model.ModelMetadata.*;


/**
 * Proxies calls to a static concurrent map, held by a private singleton. This nicely supplies local entity storage for
 * simple testing and mocking purposes. Please do not use this in production. The in-memory data engine does not support
 * queries, persistence, or nearly anything except get/put/delete.
 *
 * @param <Model> Model/message type which we are storing with this driver.
 */
@SuppressWarnings("UnstableApiUsage")
public final class InMemoryDriver<Key extends Message, Model extends Message>
  implements PersistenceDriver<Key, Model, EncodedModel> {
  /** Private logging pipe. */
  private static final Logger logging = Logging.logger(InMemoryStorage.class);

  /** Codec to use for model serialization/de-serialization. */
  private final @Nonnull ModelCodec<Model, EncodedModel> codec;

  /** Executor service to use for storage calls. */
  private final @Nonnull ListeningScheduledExecutorService executorService;

  /** Holds private "data storage" via in-memory concurrent map. */
  @Immutable
  private final static class InMemoryStorage {
    /** Storage singleton instance. */
    private final static InMemoryStorage INSTANCE;

    /** Backing storage map. */
    private final @Nonnull ConcurrentMap<Object, EncodedModel> storageMap;

    static {
      INSTANCE = new InMemoryStorage();
    }

    /** Private constructor. Acquire via {@link #acquire()}. */
    private InMemoryStorage() {
      storageMap = new ConcurrentSkipListMap<>();
    }

    /** @return In-memory storage window singleton. */
    @CanIgnoreReturnValue
    private static @Nonnull ConcurrentMap<Object, EncodedModel> acquire() {
      return INSTANCE.storageMap;
    }
  }

  /**
   * Construct a new in-memory driver from scratch. This constructor is private to force use of static factory methods
   * also defined on this class.
   *
   * @param codec Codec to use when serializing and de-serializing models with this driver.
   * @param executorService Executor service to run against.
   */
  private InMemoryDriver(@Nonnull ModelCodec<Model, EncodedModel> codec,
                         @Nonnull ListeningScheduledExecutorService executorService) {
    this.codec = codec;
    this.executorService = executorService;
  }

  /**
   * Acquire an in-memory driver instance for the provided model type and builder. Although the driver object itself is
   * created for the purpose, it accesses a static concurrent map backing all in-memory driver instances to facilitate
   * storage.
   *
   * <p>It is generally recommended to acquire an instance of this driver through the adapter instead. This can be
   * accomplished via {@link InMemoryAdapter#acquire(Message, Message, Optional, ListeningScheduledExecutorService)},
   * followed by {@link InMemoryAdapter#engine()}.</p>
   *
   * @see InMemoryAdapter#acquire(Message, Message, ListeningScheduledExecutorService) to acquire a full adapter.
   * @param <K> Key type to specify for the attached model type.
   * @param <M> Model/message type for which we should return an in-memory storage driver.
   * @param codec Codec to use when serializing and de-serializing models with this driver.
   * @param executorService Executor service to use for storage calls.
   * @return In-memory driver instance created for the specified message type.
   */
  static @Nonnull <K extends Message, M extends Message> InMemoryDriver<K, M> acquire(
    @Nonnull ModelCodec<M, EncodedModel> codec,
    @Nonnull ListeningScheduledExecutorService executorService) {
    return new InMemoryDriver<>(codec, executorService);
  }

  // -- Getters -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ModelCodec<Model, EncodedModel> codec() {
    return this.codec;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull ListeningScheduledExecutorService executorService() {
    return this.executorService;
  }

  // -- API: Fetch -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Optional<Model>> retrieve(final @Nonnull Key key,
                                                           final @Nonnull FetchOptions options) {
    Objects.requireNonNull(key, "Cannot fetch model with `null` for key.");
    Objects.requireNonNull(options, "Cannot fetch model without `options`.");
    enforceRole(key, DatapointType.OBJECT_KEY);
    var id = id(key).orElseThrow(() -> new IllegalArgumentException("Cannot fetch model with empty key."));

    return ReactiveFuture.wrap(this.executorService.submit(() -> {
      EncodedModel data = InMemoryStorage.acquire().get(id);
      if (data != null) {
        // we found encoded data at the provided key. inflate it with the codec.
        return Optional.of(applyMask(this.codec.deserialize(data), options));
      } else {
        // the model was not found.
        return Optional.empty();
      }
    }), options.executorService().orElse(this.executorService));
  }

  // -- API: Persist -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Model> persist(final @Nullable Key key,
                                                final @Nonnull Model model,
                                                final @Nonnull WriteOptions options) {
    Objects.requireNonNull(model, "Cannot persist `null` model.");
    Objects.requireNonNull(options, "Cannot persist model without `options`.");

    return ReactiveFuture.wrap(this.executorService.submit(() -> {
      // resolve target key, and then write mode
      @Nonnull Message targetKey = key != null ? key : generateKey(model);
      //noinspection OptionalGetWithoutIsPresent
      @Nonnull Object targetId = id(targetKey).get();

      WriteOptions.WriteDisposition writeMode = (
        key == null ? WriteOptions.WriteDisposition.MUST_NOT_EXIST : options.writeMode());

      // enforce write mode
      boolean conflictFailure = false;
      switch (writeMode) {
        case MUST_NOT_EXIST: conflictFailure = InMemoryStorage.acquire().containsKey(targetId); break;
        case MUST_EXIST: conflictFailure = !InMemoryStorage.acquire().containsKey(targetId); break;
        case BLIND: break;
      }
      if (conflictFailure) {
        logging.error(String.format(
          "Encountered conflict failure: key collision at ID '%s'.", targetId));
        throw new ModelWriteConflict(targetId, model, writeMode);
      }

      // if we make it this far, we're ready to write. serialize and put.
      InMemoryStorage
        .acquire()
        .put(targetId, codec.serialize(model));

      return spliceKey(model, Optional.of(targetKey));
    }), options.executorService().orElse(this.executorService));
  }
}
