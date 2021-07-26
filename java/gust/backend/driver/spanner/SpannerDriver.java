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
import com.google.cloud.spanner.SpannerOptions;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.util.FieldMaskUtil;
import gust.backend.model.*;
import gust.backend.runtime.Logging;
import gust.backend.runtime.ReactiveFuture;
import gust.backend.transport.GoogleAPIChannel;
import gust.backend.transport.GoogleService;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.context.scope.Refreshable;
import org.slf4j.Logger;
import tools.elide.core.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static gust.backend.driver.spanner.SpannerUtil.*;
import static com.google.common.util.concurrent.Futures.transformAsync;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static com.google.common.util.concurrent.Futures.withTimeout;
import static gust.backend.runtime.ReactiveFuture.wrap;
import static gust.backend.model.ModelMetadata.*;
import static java.lang.String.format;


/**
 * Provides a {@link DatabaseDriver} implementation which enables seamless Protocol Buffer persistence with Google Cloud
 * Spanner, for any {@link Message}-derived (schema-driven) business model in a given Gust app's ecosystem.
 *
 * <p>Model storage can be deeply customized on a per-model basis, thanks to the built-in proto annotations available
 * in <code>gust.core</code>. The Spanner adapter supports basic persistence (i.e. as a regular
 * <pre>PersistenceDriver</pre>), but also supports generic, object index-style queries.</p>
 *
 * <p>See the main {@link SpannerAdapter} for a full description of supported application-level functionality.</p>
 *
 * @param <Key> Typed {@link Message} which implements a concrete model key structure, as defined and annotated by the
 *              core Gust annotations.
 * @param <Model> Typed {@link Message} which implements a concrete model object structure, as defined and annotated by
 *              the core Gust annotations.
 * @see SpannerAdapter Main typed adapter interface for Spanner.
 * @see SpannerManager Adapter instance manager and factory.
 * @see SpannerDriverSettings Driver-level settings specific to Spanner.
 * @see gust.backend.driver.firestore.FirestoreDriver Similar driver implementation, built on top of Cloud Firestore,
 *      which itself is implemented on top of Cloud Spanner.
 */
@Immutable @ThreadSafe
@SuppressWarnings({"UnstableApiUsage", "OptionalUsedAsFieldOrParameterType"})
public final class SpannerDriver<Key extends Message, Model extends Message>
        implements DatabaseDriver<Key, Model, Struct, Mutation> {
    /** Private log pipe. */
    private static final Logger logging = Logging.logger(SpannerDriver.class);

    /** Executor service to use for async calls. */
    private final @Nonnull ListeningScheduledExecutorService executorService;

    /** Codec to use for serializing/de-serializing models. */
    private final @Nonnull ModelCodec<Model, Mutation, Struct> codec;

    /** Default database ID to interact with. */
    private final @Nonnull DatabaseId defaultDatabase;

    /** Settings for the Spanner driver. */
    private final @Nonnull SpannerDriverSettings driverSettings;

    /** Cloud Spanner client engine. */
    private final @Nonnull Spanner engine;

    /** Defines generic Spanner operation-specific options. */
    interface SpannerOperationOptions extends OperationOptions {
        /** @return Database to use when connecting to Spanner. */
        default @Nonnull Optional<DatabaseId> databaseId() {
            return Optional.empty();
        }
    }

    /** Defines Spanner-specific options for all read and query operations. */
    interface SpannerReadOptions extends SpannerOperationOptions, FetchOptions {
        /** @return Manual field projection, as applicable. Defaults to any present field mask. */
        default @Nonnull Optional<FieldMask> projection() {
            return this.fieldMask();
        }
    }

    /** Defines Spanner-specific options for all write and mutation operations. */
    interface SpannerWriteOptions extends SpannerOperationOptions, WriteOptions {
        // Nothing yet.
    }

    /** Defines Spanner-specific fetch options. */
    interface SpannerFetchOptions extends SpannerReadOptions {
        /** Default set of fetch options. */
        SpannerFetchOptions DEFAULTS = new SpannerFetchOptions() {};

        /** @return Timestamp boundary for single-use reads. */
        default @Nonnull Optional<TimestampBound> timestampBound() {
            return Optional.empty();
        }
    }

    /** Defines Spanner-specific mutative write options. */
    interface SpannerMutationOptions extends SpannerWriteOptions {
        /** Default set of mutation options. */
        SpannerMutationOptions DEFAULTS = new SpannerMutationOptions() {};
    }

    /** Defines Spanner-specific mutative delete options. */
    interface SpannerDeleteOptions extends SpannerWriteOptions {
        /** Default set of delete options. */
        SpannerDeleteOptions DEFAULTS = new SpannerDeleteOptions() {};
    }

    /**
     * Construct a new Spanner driver from scratch.
     *
     * @param baseOptions Base options to apply to the Spanner driver.
     * @param channelProvider Managed gRPC channel to use for Spanner RPCAPI interactions.
     * @param credentialsProvider Transport credentials provider.
     * @param defaultDatabase Default Spanner database to use and interact with.
     * @param callCredentialProvider RPC call credential provider.
     * @param transportOptions Options to apply to the transport layer.
     * @param executorService Executor service to use when executing calls.
     * @param codec Model codec to use with this driver.
     * @param driverSettings Settings for the Spanner driver itself.
     */
    private SpannerDriver(@Nonnull SpannerOptions.Builder baseOptions,
                          @Nonnull TransportChannelProvider channelProvider,
                          @Nonnull DatabaseId defaultDatabase,
                          @Nonnull Optional<CredentialsProvider> credentialsProvider,
                          @Nonnull Optional<SpannerOptions.CallCredentialsProvider> callCredentialProvider,
                          @Nonnull GrpcTransportOptions transportOptions,
                          @Nonnull ListeningScheduledExecutorService executorService,
                          @Nonnull ModelCodec<Model, Mutation, Struct> codec,
                          @Nonnull SpannerDriverSettings driverSettings) {
        this.codec = codec;
        this.defaultDatabase = defaultDatabase;
        this.executorService = executorService;
        this.driverSettings = driverSettings;
        SpannerOptions.Builder options = baseOptions
                .setChannelProvider(channelProvider)
                .setTransportOptions(transportOptions);

        callCredentialProvider.ifPresent(options::setCallCredentialsProvider);
        credentialsProvider.ifPresent((credentialProvider) -> options
                .getSpannerStubSettingsBuilder()
                .setCredentialsProvider(credentialProvider));

        if (logging.isDebugEnabled())
            logging.debug(String.format("Initializing Spanner driver with options:\n%s",
                    options.build().getService().getOptions().toString()));
        this.engine = options.build().getService();
    }

    /** Factory responsible for creating {@link SpannerDriver} instances from injected dependencies. */
    @Factory static final class SpannerDriverFactory {
        private SpannerDriverFactory() { /* Disallow construction. */ }

        /**
         * Acquire a new instance of the Spanner driver, using the specified configuration settings, and the specified
         * injected channel.
         *
         * @param baseOptions Base options to apply to the Spanner driver.
         * @param spannerChannel Managed gRPC channel provider.
         * @param credentialsProvider Transport credentials provider.
         * @param callCredentialProvider RPC call credential provider.
         * @param transportOptions Options to apply to the Spanner channel.
         * @param executorService Executor service to use when executing calls.
         * @param keyInstance Key model class we are binding this driver to.
         * @param modelInstance Default instance of the model we wish to make a driver for.
         * @param driverSettings Settings for the Spanner driver.
         * @return Spanner driver instance.
         */
        @Context
        @Refreshable
        public static @Nonnull <K extends Message, M extends Message> SpannerDriver<K, M> acquireDriver(
                @Nonnull SpannerOptions.Builder baseOptions,
                @Nonnull DatabaseId defaultDatabase,
                @Nonnull @GoogleAPIChannel(service = GoogleService.SPANNER) TransportChannelProvider spannerChannel,
                @Nonnull Optional<CredentialsProvider> credentialsProvider,
                @Nonnull Optional<SpannerOptions.CallCredentialsProvider> callCredentialProvider,
                @Nonnull GrpcTransportOptions transportOptions,
                @Nonnull ListeningScheduledExecutorService executorService,
                @SuppressWarnings("unused") @Nonnull K keyInstance,
                @Nonnull M modelInstance,
                @Nonnull SpannerDriverSettings driverSettings) {
            return new SpannerDriver<>(
                baseOptions,
                spannerChannel,
                defaultDatabase,
                credentialsProvider,
                callCredentialProvider,
                transportOptions,
                executorService,
                SpannerCodec.forModel(
                    modelInstance,
                    SpannerMutationSerializer.forModel(
                        modelInstance,
                        driverSettings
                    ),
                    SpannerStructDeserializer.forModel(
                        modelInstance,
                        driverSettings
                    )
                ),
                driverSettings
            );
        }
    }

    /** @inheritDoc */
    @Override
    public @Nonnull ListeningScheduledExecutorService executorService() {
        return executorService;
    }

    /** @inheritDoc */
    @Override
    public @Nonnull ModelCodec<Model, Mutation, Struct> codec() {
        return codec;
    }

    /** @inheritDoc */
    @Override
    public @Nonnull ReactiveFuture<Optional<Model>> retrieve(@Nonnull Key key,
                                                             @Nonnull FetchOptions options) {
        // null check all inputs
        Objects.requireNonNull(key, "Cannot fetch model with `null` for key.");
        Objects.requireNonNull(options, "Cannot fetch model without `options`.");
        enforceRole(key, DatapointType.OBJECT_KEY);
        var keyId = id(key).orElseThrow(() ->
            new IllegalArgumentException("Cannot fetch model with empty key."));

        // resolve the table where we should look for this entity
        var table = resolveTableName(key);

        // next, resolve the executor, database we should operate on, and corresponding client
        ListeningScheduledExecutorService exec = options.executorService().orElseGet(this::executorService);
        SpannerFetchOptions spannerOpts;

        if (options.getClass().isAssignableFrom(SpannerFetchOptions.class)) {
            spannerOpts = ((SpannerFetchOptions) options);
        } else {
            spannerOpts = SpannerFetchOptions.DEFAULTS;
        }

        DatabaseId db = spannerOpts.databaseId().orElse(defaultDatabase);
        var client = engine.getDatabaseClient(db);
        boolean transactional = spannerOpts.transactional().isPresent() && spannerOpts.transactional().get();

        // with the DB client in hand, resolve the raw Spanner result
        ReadContext context;
        if (spannerOpts.timestampBound().isPresent()) {
            if (transactional) {
                context = client.readOnlyTransaction(spannerOpts.timestampBound().get());
            } else {
                context = client.singleUse(spannerOpts.timestampBound().get());
            }
        } else {
            if (transactional) {
                context = client.readOnlyTransaction();
            } else {
                context = client.singleUse();
            }
        }

        // calculate the fields we should read from Spanner. because Spanner is a SQL-style DB with tables, we must
        // enumerate each field we wish to load into the result set. this set of fields can either be specified via a
        // field mask attached to the call options, or by generating a set of fields from the top-level model.
        List<String> fieldsToRead;
        if (spannerOpts.projection().isPresent()) {
            fieldsToRead = FieldMaskUtil.normalize(spannerOpts.projection().get())
                    .getPathsList();
        } else {
            fieldsToRead = calculateDefaultFields(
                this.codec().instance().getDescriptorForType(),
                driverSettings
            );
        }

        var op = wrap(context.readRowAsync(
            table,
            com.google.cloud.spanner.Key.of(id(key).orElseThrow()),
            fieldsToRead
        ));

        return wrap(transformAsync(withTimeout(op, 120, TimeUnit.SECONDS, exec), (result) -> {
            if (result == null) {
                if (logging.isDebugEnabled())
                    logging.debug("Query option result was `null`. Returning empty result.");
                return immediateFuture(Optional.empty());

            } else {
                if (logging.isDebugEnabled())
                    logging.debug("Received non-null `Struct` result from Spanner. Deserializing...");

                // deserialize the model
                var deserialized = codec.deserialize(result);
                if (logging.isDebugEnabled())
                    logging.debug(format(
                        "Found and deserialized model at ID '%s' from Spanner. Record follows:\n%s",
                        keyId,
                        deserialized));

                return immediateFuture(Optional.of(
                    spliceKey(applyMask(deserialized, options), Optional.of(key))
                ));
            }
        }, exec));
    }

    /** @inheritDoc */
    @Override
    public @Nonnull ReactiveFuture<Model> persist(@Nullable Key key,
                                                  @Nonnull Model model,
                                                  @Nonnull WriteOptions options) {
        // enforce model constraints
        Objects.requireNonNull(key, "Cannot write model with `null` for key.");
        Objects.requireNonNull(model, "Cannot write model which is, itself, `null`.");
        Objects.requireNonNull(options, "Cannot write model without `options`.");
        enforceRole(key, DatapointType.OBJECT_KEY);

        // resolve executor
        ListeningScheduledExecutorService exec = options.executorService().orElseGet(this::executorService);

        // resolve existing ID, if any. if none can be resolved, generate one, and splice it into the key, and then
        // likewise splice the key into the model. if an explicit ID is present, it is assumed that it is mounted
        // correctly on the model.
        Optional<Object> existingId = id(key);
        Object modelId = existingId.orElseGet(() -> this.generateId(model));

        if (existingId.isEmpty()) {
            spliceKey(
                model,
                Optional.of(spliceId(
                    key,
                    Optional.of(modelId)
                ))
            );
        }

        try {
            // resolve extended spanner mutation options
            SpannerMutationOptions spannerOpts;
            if (options.getClass().isAssignableFrom(SpannerMutationOptions.class)) {
                spannerOpts = ((SpannerMutationOptions) options);
            } else {
                spannerOpts = SpannerMutationOptions.DEFAULTS;
            }

            // resolve the table where we should look for this entity
            var table = resolveTableName(key);
            DatabaseId db = spannerOpts.databaseId().orElse(defaultDatabase);
            var client = engine.getDatabaseClient(db);
            boolean transactional = spannerOpts.transactional().isPresent() ?
                    spannerOpts.transactional().get() :
                    options.transactional().orElse(false);

            var writeMode = spannerOpts.writeMode().isPresent() ?
                    spannerOpts.writeMode().get() :
                    options.writeMode().orElse(WriteOptions.WriteDisposition.BLIND);

            if (logging.isDebugEnabled())
                logging.debug("Mode '{}' determined for Spanner write.", writeMode.name());

            Mutation.WriteBuilder mutation;
            switch (writeMode) {
                case BLIND: mutation = Mutation.newInsertOrUpdateBuilder(table); break;
                case MUST_EXIST: mutation = Mutation.newUpdateBuilder(table); break;
                case MUST_NOT_EXIST: mutation = Mutation.newInsertBuilder(table); break;
                default: mutation = Mutation.newReplaceBuilder(table); break;
            }

            if (codec instanceof SpannerCodec) {
                if (logging.isTraceEnabled())
                    logging.trace("Serializing model to Spanner `Mutation`: {}", model.toString());

                // fill in the mutation
                var serialized = ((SpannerCodec<Model>) codec).serialize(mutation, model);

                return wrap(exec.submit(() -> {
                    if (transactional) {
                        // @TODO(sgammon): support for write transactions
                        throw new IllegalStateException("Write transactions are not supported yet.");

                    } else {
                        // it's time to actually write the model
                        var write = client.writeAtLeastOnce(Collections.singleton(serialized));
                        Objects.requireNonNull(write, "write result from Spanner should never be null");
                        return model;

                    }
                }));

            } else {
                throw new IllegalStateException("Cannot serialize Spanner model without `SpannerCodec`.");
            }

        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    /** @inheritDoc */
    @Override
    public @Nonnull ReactiveFuture<Key> delete(@Nonnull Key key,
                                               @Nonnull DeleteOptions options) {
        Objects.requireNonNull(key, "cannot delete null key from Spanner");
        Objects.requireNonNull(options, "cannot delete without valid Spanner options");
        enforceRole(key, DatapointType.OBJECT_KEY);
        Object keyId = id(key).orElseThrow(() ->
                new IllegalArgumentException("Cannot delete key with empty or missing ID."));

        // prep for an async delete action
        ListeningScheduledExecutorService exec = options.executorService().orElseGet(this::executorService);

        // resolve extended spanner mutation options
        SpannerDeleteOptions spannerOpts;
        if (options.getClass().isAssignableFrom(SpannerDeleteOptions.class)) {
            spannerOpts = ((SpannerDeleteOptions) options);
        } else {
            spannerOpts = SpannerDeleteOptions.DEFAULTS;
        }

        // next, resolve the table we should work with, and any override DB
        var table = resolveTableName(key);
        DatabaseId db = spannerOpts.databaseId().orElse(defaultDatabase);
        var client = engine.getDatabaseClient(db);
        boolean transactional = spannerOpts.transactional().isPresent() ?
                spannerOpts.transactional().get() :
                options.transactional().orElse(false);

        // prep the delete operation and fire it off
        var deleteOperation = Mutation.delete(table, com.google.cloud.spanner.Key.of(keyId));
        if (logging.isDebugEnabled())
            logging.debug("Deleting model at ID `{}` in table `{}`.", keyId, table);
        return wrap(exec.submit(() -> {
            if (transactional) {
                // @TODO(sgammon): support for delete transactions
                throw new IllegalStateException("Write transactions are not supported yet.");
            } else {
                var result = client.write(Arrays.asList(deleteOperation));
                Objects.requireNonNull(result, "delete result from Spanner should never be null");
                return key;
            }
        }));
    }
}
