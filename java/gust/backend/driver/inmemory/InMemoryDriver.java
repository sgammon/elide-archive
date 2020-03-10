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

import static java.lang.String.format;
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
    final var id = id(key).orElseThrow(() -> new IllegalArgumentException("Cannot fetch model with empty key."));

    if (logging.isDebugEnabled())
      logging.debug(format("Retrieving model at ID '%s' from in-memory storage.", id));

    return ReactiveFuture.wrap(this.executorService.submit(() -> {
      if (logging.isTraceEnabled())
        logging.trace(format("Began async task to retrieve model at ID '%s' from in-memory storage.", id));

      EncodedModel data = InMemoryStorage.acquire().get(id);
      if (data != null) {
        if (logging.isTraceEnabled())
          logging.trace(format("Model found at ID '%s'. Sending to deserializer...", id));

        var deserialized = this.codec.deserialize(data);
        if (logging.isDebugEnabled())
          logging.debug(format("Found and deserialized model at ID '%s'. Record follows:\n%s", id, deserialized));
        if (logging.isInfoEnabled())
          logging.info(format("Retrieved record at ID '%s' from in-memory storage.", id));

        // we found encoded data at the provided key. inflate it with the codec.
        return Optional.of(spliceKey(applyMask(deserialized, options), Optional.of(key)));
      } else {
        if (logging.isWarnEnabled())
          logging.warn(format("Model not found at ID '%s'.", id));

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

    // resolve target key, and then write mode
    final @Nonnull Key targetKey = key != null ? key : generateKey(model);
    //noinspection OptionalGetWithoutIsPresent
    final @Nonnull Object targetId = id(targetKey).get();

    if (logging.isDebugEnabled())
      logging.debug(format("Persisting model at ID '%s' using in-memory storage.", targetId));

    return ReactiveFuture.wrap(this.executorService.submit(() -> {
      WriteOptions.WriteDisposition writeMode = (
        key == null ? WriteOptions.WriteDisposition.MUST_NOT_EXIST : options.writeMode()
          .orElse(WriteOptions.WriteDisposition.BLIND));

      if (logging.isTraceEnabled())
        logging.trace(format(
          "Began async task to write model at ID '%s' to in-memory storage. Write disposition: '%s'.",
          targetId,
          writeMode.name()));

      // enforce write mode
      boolean conflictFailure = false;
      switch (writeMode) {
        case MUST_NOT_EXIST: conflictFailure = InMemoryStorage.acquire().containsKey(targetId); break;
        case MUST_EXIST: conflictFailure = !InMemoryStorage.acquire().containsKey(targetId); break;
        case BLIND: break;
      }
      if (conflictFailure) {
        logging.error(format("Encountered conflict failure: key collision at ID '%s'.", targetId));
        throw new ModelWriteConflict(targetId, model, writeMode);
      }

      // if we make it this far, we're ready to write. serialize and put.
      InMemoryStorage
        .acquire()
        .put(targetId, codec.serialize(model));

      if (logging.isTraceEnabled())
        logging.trace(format(
          "No conflict failure encountered, model was written at ID '%s'.",
          targetId));

      var rval = ModelMetadata.<Model, Key>spliceKey(model, Optional.of(targetKey));
      if (logging.isInfoEnabled())
        logging.info(format(
          "Wrote record to in-memory storage at ID '%s'.",
          targetId));
      if (logging.isDebugEnabled())
        logging.debug(format(
          "Returning written model at ID '%s' after write to in-memory storage. Record follows:\n%s",
          targetId,
          rval));

      return rval;

    }), options.executorService().orElse(this.executorService));
  }

  // -- API: Delete -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Key> delete(@Nonnull Key key, @Nonnull DeleteOptions options) {
    Objects.requireNonNull(key, "Cannot delete `null` key.");
    Objects.requireNonNull(options, "Cannot delete model without `options`.");

    final @Nonnull Object targetId = id(key)
      .orElseThrow(() -> new IllegalStateException("Cannot delete record with empty key/ID."));

    if (logging.isDebugEnabled())
      logging.debug(format("Deleting model at ID '%s' from in-memory storage.", targetId));

    return ReactiveFuture.wrap(this.executorService.submit(() -> {
      if (logging.isTraceEnabled())
        logging.trace(format("Began async task to delete model at ID '%s' from in-memory storage.", targetId));

      InMemoryStorage
        .acquire()
        .remove(targetId);

      if (logging.isInfoEnabled())
        logging.info(format("Model at ID '%s' deleted from in-memory storage.", targetId));

      return key;
    }));
  }
}
