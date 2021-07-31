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

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.spanner.*;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Message;
import gust.backend.model.*;
import gust.backend.transport.GoogleAPIChannel;
import gust.backend.transport.GoogleService;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.context.scope.Refreshable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.Executors;


/**
 * Implementation of a {@link DatabaseAdapter} backed by Google Cloud Spanner, capable of marshalling arbitrary
 * schema-generated {@link Message} objects back and forth from structured columnar storage.
 *
 * <p>This adapter is implemented by the {@link SpannerDriver} and related codec classes ({@link SpannerCodec},
 * {@link SpannerStructDeserializer} and {@link SpannerMutationSerializer}). Background execution and gRPC channels are
 * hooked into driver acquisition and may be managed by the developer, or by the framework automatically. See below for
 * a summary of application-level Spanner features supported by this engine:</p>
 *
 * <p><b>Caching</b> may be facilitated by any compliant model {@link CacheDriver}.</p>
 *
 * <p><b>Transactions</b> are supported under the hood, and can be controlled via Spanner-extended operation options
 * interfaces (such as {@link SpannerDriver.SpannerWriteOptions} and {@link SpannerDriver.SpannerFetchOptions}).
 * Invoking code may either opt-in to transactional protection automatically, or drive external transactions with this
 * adapter/driver by specifying an input transaction for a given operation.</p>
 *
 * <p><b>Collections</b> are supported by this engine, with additional support for nested models encoded via JSON. In
 * cases where model JSON is involved, {@link com.google.protobuf.util.JsonFormat} is used to produce and consume
 * compliant Proto-JSON.</p>
 *
 * @param <Key> Typed {@link Message} which implements a concrete model key structure, as defined and annotated by the
 *              core Gust annotations.
 * @param <Model> Typed {@link Message} which implements a concrete model object structure, as defined and annotated by
 *              the core Gust annotations.
 * @see SpannerManager Adapter instance manager and factory.
 * @see SpannerDriverSettings Driver-level settings specific to Spanner.
 * @see gust.backend.driver.firestore.FirestoreAdapter Similar adapter implementation, built on top of Cloud Firestore,
 *      which itself is implemented on top of Cloud Spanner.
 */
@Immutable @ThreadSafe
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "UnstableApiUsage"})
public final class SpannerAdapter<Key extends Message, Model extends Message>
        implements DatabaseAdapter<Key, Model, Struct, Mutation>, Closeable, AutoCloseable {
    /** Spanner database driver. */
    private final @Nonnull SpannerDriver<Key, Model> driver;

    /** Serializer and deserializer for this model. */
    private final @Nonnull ModelCodec<Model, Mutation, Struct> codec;

    /** Cache to use for model interactions through this adapter (optional). */
    private final @Nonnull Optional<CacheDriver<Key, Model>> cache;

    /**
     * Private constructor for a model-specialized Spanner adapter instance.
     *
     * @param driver Driver instance created to power this adapter.
     * @param codec Codec to use when marshalling objects with this adapter.
     * @param cache Optionally, cache to employ when interacting with the underlying driver.
     */
    private SpannerAdapter(@Nonnull SpannerDriver<Key, Model> driver,
                           @Nonnull ModelCodec<Model, Mutation, Struct> codec,
                           @Nonnull Optional<CacheDriver<Key, Model>> cache) {
        this.driver = driver;
        this.codec = codec;
        this.cache = cache;
    }

    /**
     * Create or resolve a {@link SpannerAdapter} for the pre-fabricated {@link SpannerDriver}.
     *
     * <p>Note: this method has no way to specify a cache. See below for alternatives.</p>
     *
     * @see #forModel(SpannerDriver, Optional) Variant of this method that allows invoking code to provide a
     *      compliant {@link CacheDriver} instance.
     * @param driver Pre-fabricated Spanner driver to wrap with an adapter instance.
     * @param <K> Typed model key structure which the resulting adapter should be specialized to.
     * @param <M> Typed model object structure which the resulting adapter should be specialized to.
     * @return Adapter instance, wrapping the provided driver.
     */
    public static @Nonnull <K extends Message, M extends Message> SpannerAdapter<K, M> forModel(
            @Nonnull SpannerDriver<K, M> driver) {
        return forModel(driver, Optional.empty());
    }

    /**
     * Create or resolve a {@link SpannerAdapter} for the pre-fabricated {@link SpannerDriver}, optionally using the
     * provided {@link CacheDriver}, if present.
     *
     * @param driver Pre-fabricated Spanner driver to wrap with an adapter instance.
     * @param cacheDriver Optional cache engine to employ when interacting with the provided driver.
     * @param <K> Typed model key structure which the resulting adapter should be specialized to.
     * @param <M> Typed model object structure which the resulting adapter should be specialized to.
     * @return Adapter instance, wrapping the provided driver.
     */
    public static @Nonnull <K extends Message, M extends Message> SpannerAdapter<K, M> forModel(
            @Nonnull SpannerDriver<K, M> driver,
            @Nonnull Optional<CacheDriver<K, M>> cacheDriver) {
        return new SpannerAdapter<>(
            driver,
            driver.codec(),
            cacheDriver
        );
    }

    /** Factory responsible for creating {@link SpannerAdapter} instances from injected dependencies. */
    @Factory static final class SpannerAdapterFactory {
        private SpannerAdapterFactory() { /* Disallow construction. */ }

        /**
         * Acquire a new instance of the Spanner adapter, using the specified component objects to facilitate model
         * serialization/deserialization, and transport communication with Cloud Spanner.
         *
         * @param driver Driver with which we should talk to Spanner.
         * @param cache Driver with which we should cache eligible data.
         * @return Spanner driver instance.
         */
        @Context
        @Refreshable
        public static @Nonnull <K extends Message, M extends Message> SpannerAdapter<K, M> acquire(
                @Nonnull SpannerDriver<K, M> driver,
                @Nonnull Optional<CacheDriver<K, M>> cache) {
            // resolve model builder from type
            return SpannerAdapter.forModel(
                driver,
                cache
            );
        }
    }

    /**
     * Create or acquire a {@link SpannerAdapter} and matching {@link SpannerDriver} for the provided generated model
     * key and object structures, working against the provided Spanner {@link DatabaseId}.
     *
     * <p>This method variant is the simplest invocation option for acquiring an adapter. Variants of this method
     * provide deeper control over interactions with the Spanner service. See below for alternatives if deeper control
     * is necessary. Generally, it is best to let the framework manage transport and stub settings.</p>
     *
     * @see #acquire(Message, Message, DatabaseId, ListeningScheduledExecutorService) For an option which lets invoking
     *      code specify a background executor for RPC transmission and followup.
     * @see #acquire(Message, Message, DatabaseId, SpannerOptions.Builder, ListeningScheduledExecutorService) Variant of
     *      this same method which offers control of the {@link SpannerOptions} used to spawn RPC clients.
     * @see #acquire(SpannerOptions.Builder, DatabaseId, TransportChannelProvider, Optional, Optional,
     *      GrpcTransportOptions, ListeningScheduledExecutorService, Message, Message, SpannerDriverSettings, Optional)
     *      Full control over creation of the Spanner adapter and driver.
     * @param keyInstance Generated key {@link Message} structure, for which the adapter should be specialized.
     * @param messageInstance Generated object {@link Message} structure, for which the adapter should be specialized.
     * @param defaultDatabase Default Spanner database to use when interacting with this adapter. This value may be
     *                        overridden on an individual operation basis via specifying custom
     *                        {@link SpannerDriver.SpannerOperationOptions} and descendents.
     * @param <K> Model key structure for which the resulting adapter should be specialized.
     * @param <M> Model object structure for which the resulting adapter should be specialized.
     * @return Spanner adapter instance, specialized to the provided model and key {@link Message}s.
     */
    public static @Nonnull <K extends Message, M extends Message> SpannerAdapter<K, M> acquire(
            @Nonnull K keyInstance,
            @Nonnull M messageInstance,
            @Nonnull DatabaseId defaultDatabase) {
        return acquire(
                SpannerOptions.newBuilder(),
                defaultDatabase,
                SpannerStubSettings.defaultTransportChannelProvider(),
                Optional.of(SpannerStubSettings.defaultCredentialsProviderBuilder().build()),
                Optional.empty(),
                GrpcTransportOptions.newBuilder().build(),
                MoreExecutors.listeningDecorator(
                    Executors.newScheduledThreadPool(3)
                ),
                keyInstance,
                messageInstance,
                SpannerDriverSettings.DEFAULTS,
                Optional.empty()
        );
    }

    /**
     * Create or acquire a {@link SpannerAdapter} and matching {@link SpannerDriver} for the provided generated model
     * key and object structures, working against the provided Spanner {@link DatabaseId}.
     *
     * <p>This method variant additionally allows the developer to specify a custom
     * {@link ListeningScheduledExecutorService} to use for background operation execution. This executor service is
     * injected directly into the {@link SpannerDriver} and underlying Spanner RPC client, and is used for both RPC
     * operational execution and async followup.</p>
     *
     * <p>Variants of this method provide deeper control over interactions with the Spanner service. See below for
     * alternatives.</p>
     *
     * @see #acquire(Message, Message, DatabaseId) For a simpler version of this method which uses managed driver
     *      settings and a sensible cached threadpool executor.
     * @see #acquire(Message, Message, DatabaseId, SpannerOptions.Builder, ListeningScheduledExecutorService) Variant of
     *      this same method which offers control of the {@link SpannerOptions} used to spawn RPC clients.
     * @see #acquire(SpannerOptions.Builder, DatabaseId, TransportChannelProvider, Optional, Optional,
     *      GrpcTransportOptions, ListeningScheduledExecutorService, Message, Message, SpannerDriverSettings, Optional)
     *      Full control over creation of the Spanner adapter and driver.
     * @param keyInstance Generated key {@link Message} structure, for which the adapter should be specialized.
     * @param messageInstance Generated object {@link Message} structure, for which the adapter should be specialized.
     * @param defaultDatabase Default Spanner database to use when interacting with this adapter. This value may be
     *                        overridden on an individual operation basis via specifying custom
     *                        {@link SpannerDriver.SpannerOperationOptions} and descendents.
     * @param executorService Executor service to use for primary RPC execution and related followup.
     * @param <K> Model key structure for which the resulting adapter should be specialized.
     * @param <M> Model object structure for which the resulting adapter should be specialized.
     * @return Spanner adapter instance, specialized to the provided model and key {@link Message}s.
     */
    public static @Nonnull <K extends Message, M extends Message> SpannerAdapter<K, M> acquire(
            @Nonnull K keyInstance,
            @Nonnull M messageInstance,
            @Nonnull DatabaseId defaultDatabase,
            @Nonnull ListeningScheduledExecutorService executorService) {
        return acquire(
            SpannerOptions.newBuilder(),
            defaultDatabase,
            SpannerStubSettings.defaultTransportChannelProvider(),
            Optional.of(SpannerStubSettings.defaultCredentialsProviderBuilder().build()),
            Optional.empty(),
            GrpcTransportOptions.newBuilder().build(),
            executorService,
            keyInstance,
            messageInstance,
            SpannerDriverSettings.DEFAULTS,
            Optional.empty()
        );
    }

    /**
     * Create or acquire a {@link SpannerAdapter} and matching {@link SpannerDriver} for the provided generated model
     * key and object structures, working against the provided Spanner {@link DatabaseId}.
     *
     * <p>This method variant is a balanced invocation which allows invoking code to control <i>most</i> settings,
     * without coupling too tightly to Google SDKs.</p>
     *
     * @see #acquire(Message, Message, DatabaseId, ListeningScheduledExecutorService) For an option which lets invoking
     *      code specify a background executor for RPC transmission and followup.
     * @see #acquire(Message, Message, DatabaseId, SpannerOptions.Builder, ListeningScheduledExecutorService) Variant of
     *      this same method which offers control of the {@link SpannerOptions} used to spawn RPC clients.
     * @see #acquire(SpannerOptions.Builder, DatabaseId, TransportChannelProvider, Optional, Optional,
     *      GrpcTransportOptions, ListeningScheduledExecutorService, Message, Message, SpannerDriverSettings, Optional)
     *      Full control over creation of the Spanner adapter and driver.
     * @param keyInstance Generated key {@link Message} structure, for which the adapter should be specialized.
     * @param messageInstance Generated object {@link Message} structure, for which the adapter should be specialized.
     * @param defaultDatabase Default Spanner database to use when interacting with this adapter. This value may be
     *                        overridden on an individual operation basis via specifying custom
     *                        {@link SpannerDriver.SpannerOperationOptions} and descendents.
     * @param executorService Executor service to use for primary RPC execution and related followup.
     * @param driverSettings Custom driver settings to apply. {@link SpannerDriverSettings#DEFAULTS} is a good start.
     * @param <K> Model key structure for which the resulting adapter should be specialized.
     * @param <M> Model object structure for which the resulting adapter should be specialized.
     * @return Spanner adapter instance, specialized to the provided model and key {@link Message}s.
     */
    public static @Nonnull <K extends Message, M extends Message> SpannerAdapter<K, M> acquire(
            @Nonnull K keyInstance,
            @Nonnull M messageInstance,
            @Nonnull DatabaseId defaultDatabase,
            @Nonnull Optional<ListeningScheduledExecutorService> executorService,
            @Nonnull Optional<SpannerDriverSettings> driverSettings,
            @Nonnull Optional<SpannerOptions.Builder> baseOptions,
            @Nonnull Optional<CacheDriver<K, M>> cacheDriver) {
        return acquire(
            baseOptions.orElse(SpannerOptions.newBuilder()),
            defaultDatabase,
            SpannerStubSettings.defaultTransportChannelProvider(),
            Optional.of(SpannerStubSettings.defaultCredentialsProviderBuilder().build()),
            Optional.empty(),
            GrpcTransportOptions.newBuilder().build(),
            executorService.orElseGet(() -> MoreExecutors.listeningDecorator(
                Executors.newScheduledThreadPool(3)
            )),
            keyInstance,
            messageInstance,
            driverSettings.orElse(SpannerDriverSettings.DEFAULTS),
            cacheDriver
        );
    }

    /**
     * Create or acquire a {@link SpannerAdapter} and matching {@link SpannerDriver} for the provided generated model
     * key and object structures, working against the provided Spanner {@link DatabaseId}.
     *
     * <p>This method variant additionally allows the developer to specify a custom
     * {@link ListeningScheduledExecutorService} to use for background operation execution, and a set of
     * {@link SpannerOptions} to use when spawning RPC clients. This executor service is injected directly into the
     * {@link SpannerDriver} and underlying Spanner RPC clients, and is used for both RPC operational execution and
     * async followup.</p>
     *
     * <p>Variants of this method provide either simpler invocation, or deeper control over interactions with the
     * Spanner service. See below for alternatives.</p>
     *
     * @see #acquire(Message, Message, DatabaseId) For a simpler version of this method which uses managed driver
     *      settings and a sensible cached threadpool executor.
     * @see #acquire(Message, Message, DatabaseId, ListeningScheduledExecutorService) For a simpler version of this
     *      method which uses managed driver settings.
     * @see #acquire(SpannerOptions.Builder, DatabaseId, TransportChannelProvider, Optional, Optional,
     *      GrpcTransportOptions, ListeningScheduledExecutorService, Message, Message, SpannerDriverSettings, Optional)
     *      Full control over creation of the Spanner adapter and driver.
     * @param keyInstance Generated key {@link Message} structure, for which the adapter should be specialized.
     * @param messageInstance Generated object {@link Message} structure, for which the adapter should be specialized.
     * @param defaultDatabase Default Spanner database to use when interacting with this adapter. This value may be
     *                        overridden on an individual operation basis via specifying custom
     *                        {@link SpannerDriver.SpannerOperationOptions} and descendents.
     * @param baseOptions Spanner options to use when spawning RPC clients.
     * @param executorService Executor service to use for primary RPC execution and related followup.
     * @param <K> Model key structure for which the resulting adapter should be specialized.
     * @param <M> Model object structure for which the resulting adapter should be specialized.
     * @return Spanner adapter instance, specialized to the provided model and key {@link Message}s.
     */
    public static @Nonnull <K extends Message, M extends Message> SpannerAdapter<K, M> acquire(
            @Nonnull K keyInstance,
            @Nonnull M messageInstance,
            @Nonnull DatabaseId defaultDatabase,
            @Nonnull SpannerOptions.Builder baseOptions,
            @Nonnull ListeningScheduledExecutorService executorService) {
        return acquire(
            baseOptions,
            defaultDatabase,
            SpannerStubSettings.defaultTransportChannelProvider(),
            Optional.of(SpannerStubSettings.defaultCredentialsProviderBuilder().build()),
            Optional.empty(),
            GrpcTransportOptions.newBuilder().build(),
            executorService,
            keyInstance,
            messageInstance,
            SpannerDriverSettings.DEFAULTS,
            Optional.empty()
        );
    }

    /**
     * Create or acquire a {@link SpannerAdapter} and matching {@link SpannerDriver} for the provided generated model
     * key and object structures, working against the provided Spanner {@link DatabaseId}.
     *
     * <p>This method variant additionally allows the developer to specify all custom settings available for the Spanner
     * driver and adapter.</p>
     *
     * <p>Variants of this method provide simpler invocation, for looser coupling with applications. See below to
     * consider these alternatives for situations where deep control isn't necessary.</p>
     *
     * @param baseOptions Spanner options to use when spawning RPC clients.
     * @param defaultDatabase Default Spanner database to use when interacting with this adapter. This value may be
     *                        overridden on an individual operation basis via specifying custom
     *                        {@link SpannerDriver.SpannerOperationOptions} and descendents.
     * @param spannerChannel Transport channel provider to use when spawning RPC connections to Spanner.
     * @param credentialsProvider Credentials provider to use when authorizing calls to Spanner.
     * @param callCredentialProvider Call-level credentials provider to use when authorizing calls to Spanner. Optional.
     * @param transportOptions Transport options to apply when interacting with Spanner services.
     * @param executorService Executor service to use for primary RPC execution and related followup.
     * @param keyInstance Generated key {@link Message} structure, for which the adapter should be specialized.
     * @param messageInstance Generated object {@link Message} structure, for which the adapter should be specialized.
     * @param driverSettings Settings to apply to the Spanner driver and adapter itself.
     * @param cacheDriver Cache engine to use when interacting with the underlying driver.
     * @param <K> Model key structure for which the resulting adapter should be specialized.
     * @param <M> Model object structure for which the resulting adapter should be specialized.
     * @return Spanner adapter instance, specialized to the provided model and key {@link Message}s.
     */
    public static @Nonnull <K extends Message, M extends Message> SpannerAdapter<K, M> acquire(
            @Nonnull SpannerOptions.Builder baseOptions,
            @Nonnull DatabaseId defaultDatabase,
            @Nonnull @GoogleAPIChannel(service = GoogleService.SPANNER) TransportChannelProvider spannerChannel,
            @Nonnull Optional<CredentialsProvider> credentialsProvider,
            @Nonnull Optional<SpannerOptions.CallCredentialsProvider> callCredentialProvider,
            @Nonnull GrpcTransportOptions transportOptions,
            @Nonnull ListeningScheduledExecutorService executorService,
            @Nonnull K keyInstance,
            @Nonnull M messageInstance,
            @Nonnull SpannerDriverSettings driverSettings,
            @Nonnull Optional<CacheDriver<K, M>> cacheDriver) {
        return SpannerAdapterFactory.acquire(
            SpannerDriver.SpannerDriverFactory.acquireDriver(
                baseOptions,
                defaultDatabase,
                spannerChannel,
                credentialsProvider,
                callCredentialProvider,
                transportOptions,
                executorService,
                keyInstance,
                messageInstance,
                driverSettings
            ),
            cacheDriver
        );
    }

    // -- API: Closeable -- //

    @Override
    public void close() {
        // Not yet implemented.
    }

    // -- Components -- //

    /** {@inheritDoc} */
    @Override
    public @Nonnull ModelCodec<Model, Mutation, Struct> codec() {
        return this.codec;
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull Optional<CacheDriver<Key, Model>> cache() {
        return this.cache;
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull DatabaseDriver<Key, Model, Struct, Mutation> engine() {
        return this.driver;
    }

    /** {@inheritDoc} */
    @Override
    public @Nonnull ListeningScheduledExecutorService executorService() {
        return driver.executorService();
    }

    // -- Spanner: Extended API -- //

    /**
     * Acquire the Spanner for Java client powering this adapter.
     *
     * @return Spanner client for Java.
     */
    public @Nonnull Spanner spanner() {
        return ((SpannerDriver<Key, Model>)engine()).engine;
    }
}
