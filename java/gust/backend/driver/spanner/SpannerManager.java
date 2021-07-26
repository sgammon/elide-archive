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
package gust.backend.driver.spanner;

import com.google.cloud.spanner.DatabaseId;
import com.google.protobuf.Message;
import gust.backend.model.DatabaseManager;
import gust.backend.runtime.Logging;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.context.scope.Refreshable;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Main adapter manager interface for interaction between Gust/Elide apps and Google Cloud Spanner, enabling seamless
 * persistence for generated {@link Message}-driven models.
 *
 * <p>This {@link DatabaseManager} implementation is backed by a customized {@link SpannerDriver} and
 * {@link SpannerAdapter} which manage remote database interactions and cache/transaction state, respectively. While
 * these objects may be acquired directly, `SpannerManager` has the added benefit of a generic singleton pattern which
 * saves re-cycling of the adapter and driver objects.</p>
 *
 * @see SpannerDriver `SpannerDriver`, the main driver for interacting with Cloud Spanner
 * @see SpannerAdapter `SpannerAdapter`, which manages cache/transaction state
 */
@Immutable @ThreadSafe @Refreshable
@SuppressWarnings("rawtypes")
public final class SpannerManager
        implements DatabaseManager<SpannerAdapter, SpannerDriver>, Closeable, AutoCloseable {
    private static final @Nonnull Logger logging = Logging.logger(SpannerManager.class);

    /** Keeps track of configured managers spawned by this manager. */
    private final @Nonnull ConcurrentMap<Integer, WeakReference<ConfiguredSpannerManager>> configuredManagers;

    /** Spanner manager singleton container. */
    private static final class SpannerManagerSingleton {
        // Global singleton.
        static volatile @Nullable SpannerManager __singleton = null;
    }

    /**
     * Default constructor.
     */
    private SpannerManager() {
        configuredManagers = new ConcurrentSkipListMap<>();
    }

    /**
     * Returns the full set of known configured Spanner managers, JVM-wide. This is mostly useful as a utility to shut
     * down connections globally when needed (for instance, during testing).
     *
     * @return Unmodifiable list of weak references to all known-active managers.
     */
    public static @Nonnull Collection<WeakReference<ConfiguredSpannerManager>> allManagers() {
        var target = acquire();
        if (logging.isTraceEnabled())
            logging.trace(
                "All `SpannerManager` instances requested. Total to return: {}.",
                target.configuredManagers.size());
        return Collections.unmodifiableCollection(target.configuredManagers.values());
    }

    /**
     * Acquire a singleton instance of the Spanner manager, which can be used safely across threads to interact with
     * Google Cloud Spanner, driven by {@link Message}-generated models.
     *
     * <p>The manager can then be used to request instances of {@link SpannerAdapter} specialized to a given model.
     * Adapter instances acquired in this way are not guaranteed to be new, and are safe to use across threads.</p>
     *
     * @return Singleton instance of the Spanner manager.
     */
    @Factory
    public static @Nonnull SpannerManager acquire() {
        var singleton = SpannerManagerSingleton.__singleton;
        if (singleton == null) {
            singleton = new SpannerManager();
            SpannerManagerSingleton.__singleton = singleton;
            if (logging.isTraceEnabled())
                logging.trace("Acquired fresh singleton for `SpannerManager` request.");
        }
        if (logging.isTraceEnabled())
            logging.trace("Acquired recycled singleton for `SpannerManager` request.");
        return singleton;
    }

    /**
     * Close all active Spanner connections tracked or controlled by this manager.
     *
     * @throws RuntimeException If the underlying connections raise IO exceptions.
     */
    @Override
    public void close() {
        if (logging.isInfoEnabled())
            logging.info("Shutting down all active `SpannerManager` instances...");
        allManagers().forEach((manager) -> {
            var target = manager.get();
            if (target != null) {
                target.close();
            }
        });
    }

    /** Intermediate builder which can gather settings for an eventually-immutable {@link ConfiguredSpannerManager}. */
    public final class Builder {
        /** Required/immutable: Main Spanner database this manager will interact with. */
        private final @Nonnull DatabaseId database;

        /** Private constructor. */
        Builder(@Nonnull DatabaseId database) {
            this.database = database;
        }

        /**
         * Build this builder into a configured and immutable {@link ConfiguredSpannerManager} instance, capable of
         * producing managed {@link SpannerAdapter}s specialized to {@link Message} instances.
         *
         * @return Configured and immutable Spanner manager.
         */
        public @Nonnull ConfiguredSpannerManager build() {
            var assignedId = configuredManagers.size();
            var manager = new ConfiguredSpannerManager(
                assignedId,
                database
            );

            try {
                configuredManagers.put(
                    assignedId,
                    new WeakReference<>(manager)
                );
                return manager;
            } finally {
                WeakReference.reachabilityFence(manager);
            }
        }
    }

    /**
     * Configure a vanilla Spanner manager instance for a given database.
     *
     * @param database Spanner database.
     * @return Spanner manager builder.
     */
    public @Nonnull Builder configureForDatabase(@Nonnull DatabaseId database) {
        return new Builder(database);
    }

    /**
     * Represents a configured version of a central {@link SpannerManager}, which has been sealed for immutable use at
     * runtime. Once built via {@link Builder}, fields on a configured manager cannot change.
     */
    @Immutable @ThreadSafe @Refreshable
    public final class ConfiguredSpannerManager implements
            DatabaseManager<SpannerAdapter, SpannerDriver>, Closeable, AutoCloseable {
        /** Main cache of adapters generated for concrete models. */
        private final @Nonnull ConcurrentMap<Integer,
                SpannerAdapter<? extends Message, ? extends Message>> adapterCache;

        /** Database we should interact with. */
        private final @Nonnull DatabaseId database;

        /** Whether this configured manager has closed, in which case it cannot spawn adapters or operations. */
        private final @Nonnull AtomicBoolean closed = new AtomicBoolean(false);

        /** ID assigned to this configured Spanner manager. */
        private final int id;

        /**
         * Package-private constructor.
         *
         * @param id Assigned ID for this manager.
         * @param database Database we should bind the resulting Spanner manager to.
         */
        ConfiguredSpannerManager(int id,
                                 @Nonnull DatabaseId database) {
            this.database = Objects.requireNonNull(database);
            this.adapterCache = new ConcurrentSkipListMap<>();
            this.id = id;
        }

        /**
         * Acquire a typed adapter instance specialized to the provided key and model types, which should derive from
         * schema-driven {@link Message} classes.
         *
         * <p>Adapters and backing drivers acquired via this route are not guaranteed to be new, which in most cases is
         * a performance benefit with ignorable costs. Since adapters and drivers are required to be threadsafe, they
         * can be re-used safely with no internal state involved.</p>
         *
         * <p>Alternatively, drivers/adapters can also be acquired directly, via methods like
         * {@link SpannerAdapter#acquire(Message, Message, DatabaseId)} and friends.</p>
         *
         * @param keyInstance Model key instance for which a specialized adapter should be returned.
         * @param modelInstance Model object instance for which a specialized adapter should be returned.
         * @param <Key> Key type to which the adapter will be specialized.
         * @param <Model> Model type to which the adapter will be specialized.
         * @throws IllegalArgumentException If the provided key or model instance is not duly marked as a key.
         * @throws IllegalStateException If the provided key or model instance is not duly marked with a table name.
         * @return New or recycled model adapter instance for the provided key and model types.
         */
        @Factory
        @SuppressWarnings("unchecked")
        public @Nonnull <Key extends Message, Model extends Message> SpannerAdapter<Key, Model> adapter(
                @Nonnull Key keyInstance,
                @Nonnull Model modelInstance) {
            Objects.requireNonNull(keyInstance);
            Objects.requireNonNull(modelInstance);
            if (this.getClosed())
                throw new IllegalStateException("Cannot spawn adapters with a closed manager.");
            if (logging.isDebugEnabled())
                logging.info("Acquiring `SpannerAdapter` for model '{}' (key: '{}').",
                        modelInstance.getDescriptorForType().getFullName(),
                        keyInstance.getDescriptorForType().getFullName());

            var modelFingerprint = keyInstance.getDescriptorForType().getFullName().concat(
                modelInstance.getDescriptorForType().getFullName()
            ).hashCode();
            if (logging.isTraceEnabled())
                logging.info("Model fingerprint for desired adapter: {}.", modelFingerprint);

            if (!adapterCache.containsKey(modelFingerprint)) {
                if (logging.isTraceEnabled())
                    logging.info("No cached adapter. Spawning new one for fingerprint '{}'...", modelFingerprint);

                // spawn a new adapter, place it in the cache
                var adapter = SpannerAdapter.acquire(
                    keyInstance,
                    modelInstance,
                    database
                );
                adapterCache.put(modelFingerprint, adapter);
                return adapter;
            } else if (logging.isTraceEnabled()) {
                logging.info("Cached adapter found for fingerprint '{}'. Returning.", modelFingerprint);
            }
            return (SpannerAdapter<Key, Model>)adapterCache.get(modelFingerprint);
        }

        /** @return Database bound to this manager. */
        public @Nonnull DatabaseId getDatabase() {
            return database;
        }

        /** @return Closed/open state of this manager. */
        public boolean getClosed() {
            return closed.get();
        }

        /**
         * Returns the full set of known configured Spanner managers, JVM-wide. This is mostly useful as a utility to
         * shut down connections globally when needed (for instance, during testing).
         *
         * @return Unmodifiable list of weak references to all known-active managers.
         */
        public @Nonnull Collection<SpannerAdapter> allAdapters() {
            if (logging.isTraceEnabled())
                logging.trace("All adapters requested from 'SpannerManager' at ID '{}'. Total: {}.",
                        this.id,
                        this.adapterCache.size());
            return Collections.unmodifiableCollection(this.adapterCache.values());
        }

        /**
         * Close all active Spanner connections tracked or controlled by this configured manager.
         *
         * @throws RuntimeException If the underlying connections raise IO exceptions.
         */
        @Override
        public void close() {
            if (logging.isTraceEnabled())
                logging.trace("Close requested for `SpannerManager` at ID '{}'.", this.id);
            if (this.getClosed()) {
                if (logging.isDebugEnabled())
                    logging.debug("Close requested, but but `SpannerManager` at ID '{}' is already closed.", this.id);
                return;
            }
            try {
                if (logging.isInfoEnabled())
                    logging.info("Closing `SpannerManager` at ID '{}'.", this.id);
                closed.compareAndSet(false, true);
                allAdapters().forEach(SpannerAdapter::close);
            } finally {
                adapterCache.clear();
                configuredManagers.remove(this.id);  // deregister self
            }
        }
    }
}
