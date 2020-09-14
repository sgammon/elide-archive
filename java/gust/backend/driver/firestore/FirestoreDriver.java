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
package gust.backend.driver.firestore;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.Message;
import gust.backend.model.*;
import gust.backend.runtime.Logging;
import gust.backend.runtime.ReactiveFuture;
import gust.backend.transport.GoogleAPIChannel;
import gust.backend.transport.GoogleService;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.context.scope.Refreshable;
import org.slf4j.Logger;
import tools.elide.core.DatapointType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static gust.backend.model.ModelMetadata.enforceRole;
import static gust.backend.model.ModelMetadata.id;


/**
 * Defines a built-in framework {@link DatabaseDriver} for interacting seamlessly with Google Cloud Firestore. This
 * enables Firestore-based persistence for any {@link Message}-derived (schema-driven) business model in a given Gust
 * app's ecosystem.
 *
 * <p>Model storage can be deeply customized on a per-model basis, thanks to the built-in proto annotations available
 * in <code>gust.core</code>. The Firestore adapter supports basic persistence (i.e. as a regular
 * <pre>PersistenceDriver</pre>), but also supports generic, object index-style queries.</p>
 *
 * <p><b>Caching</b> may be facilitated by any compliant cache driver, via the main Firestore adapter.</p>
 *
 * @see FirestoreAdapter main adapter interface for Firestore.
 * @see FirestoreManager logic and connection manager for Firestore.
 * @see FirestoreTransportConfig configuration class for Firestore access.
 */
@Immutable
@ThreadSafe
@SuppressWarnings("UnstableApiUsage")
public final class FirestoreDriver<Key extends Message, Model extends Message>
  implements DatabaseDriver<Key, Model, CollapsedMessage> {
  /** Private log pipe. */
  private static final Logger logging = Logging.logger(FirestoreDriver.class);

  /** Executor service to use for async calls. */
  private final ListeningScheduledExecutorService executorService;

  /** Codec to use for serializing/de-serializing models. */
  private final ModelCodec<Model, CollapsedMessage> codec;

  /** Firestore client engine. */
  private final Firestore engine;

  /** Factory responsible for creating {@link FirestoreDriver} instances from injected dependencies. */
  @Factory
  final static class FirestoreDriverFactory {
    /**
     * Acquire a new instance of the Firestore driver, using the specified configuration settings, and the specified
     * injected channel.
     *
     * @param firestoreChannel Managed gRPC channel provider.
     * @param credentialsProvider Transport credentials provider. Generally calls into ADC.
     * @param transportOptions Options to apply to the Firestore channel.
     * @param executorService Executor service to use when executing calls.
     * @return Firestore driver instance.
     */
    @Context
    @Refreshable
    public static @Nonnull <K extends Message, M extends Message> FirestoreDriver<K, M> acquireDriver(
      @Nonnull @GoogleAPIChannel(service = GoogleService.FIRESTORE) TransportChannelProvider firestoreChannel,
      @Nonnull CredentialsProvider credentialsProvider,
      @Nonnull GrpcTransportOptions transportOptions,
      @Nonnull ListeningScheduledExecutorService executorService,
      @Nonnull Message.Builder builder) {
      return new FirestoreDriver<>(
        firestoreChannel,
        credentialsProvider,
        transportOptions,
        executorService,
        CollapsedMessageCodec.forModel(builder));
    }
  }

  /**
   * Construct a new Firestore driver from scratch.
   *
   * @param channelProvider Managed gRPC channel to use for Firestore RPCAPI interactions.
   * @param credentialsProvider Transport credentials provider.
   * @param transportOptions Options to apply to the transport layer.
   * @param executorService Executor service to use when executing calls.
   * @param codec Model codec to use with this driver.
   */
  private FirestoreDriver(TransportChannelProvider channelProvider,
                          CredentialsProvider credentialsProvider,
                          GrpcTransportOptions transportOptions,
                          ListeningScheduledExecutorService executorService,
                          ModelCodec<Model, CollapsedMessage> codec) {
    this.codec = codec;
    this.executorService = executorService;
    FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
      .setChannelProvider(channelProvider)
      .setCredentialsProvider(credentialsProvider)
      .setTransportOptions(transportOptions)
      .build();

    if (logging.isDebugEnabled())
      logging.debug(String.format("Initializing Firestore driver with options:\n%s", firestoreOptions));
    this.engine = firestoreOptions.getService();
  }

  /**
   * Deserialize the provided document snapshot, into an instance of the message we manage through this instance of the
   * {@link FirestoreDriver}.
   *
   * @param snapshot Document snapshot to de-serialize.
   * @return Inflated object record, or {@link Optional#empty()}.
   */
  private @Nonnull Model deserialize(@Nonnull DocumentSnapshot snapshot) {
    throw new IllegalStateException("not yet implemented: " + snapshot.toString());
  }

  // -- Getters -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ListeningScheduledExecutorService executorService() {
    return this.executorService;
  }

  /** {@inheritDoc} */
  @Nonnull
  @Override
  public ModelCodec<Model, CollapsedMessage> codec() {
    return this.codec;
  }

  /**
   * Convert a model `Key` into a Firestore {@link DocumentReference}.
   *
   * @param keyInstance Key instance to convert into a ref.
   * @return Computed document reference.
   */
  private @Nonnull DocumentReference ref(@Nonnull Key keyInstance) {
    throw new IllegalStateException("not yet implemented");
  }

  /**
   * Convert a path and prefix into a Firestore {@link DocumentReference}.
   *
   * @param path Path in Firestore for the document.
   * @param prefix Global prefix to apply to the path.
   * @return Computed document reference.
   */
  private @Nonnull DocumentReference ref(@Nonnull String path, @Nullable String prefix) {
    if (prefix != null) {
      return engine.document(prefix + path);
    } else {
      return engine.document(path);
    }
  }

  // -- API: Key Generation -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull Key generateKey(@Nonnull Message instance) {
    var fieldPointer = ModelMetadata.keyField(instance);
    if (fieldPointer.isEmpty())
      throw new IllegalArgumentException("Failed to resolve key field for message '"
          + instance.getDescriptorForType().getFullName() + "'.");

    var keyBuilder = instance.toBuilder().getFieldBuilder(fieldPointer.get().getField());
    var idPointer = ModelMetadata.idField(keyBuilder.getDescriptorForType());
    if (idPointer.isEmpty())
      throw new IllegalArgumentException("Failed to resolve key ID field for key message type '"
          + keyBuilder.getDescriptorForType().getFullName() + "'.");

    ModelMetadata.spliceBuilder(
        keyBuilder,
        idPointer.get(),
        Optional.of(UUID.randomUUID().toString().toUpperCase()));

    //noinspection unchecked
    return (Key)keyBuilder.build();
  }

  // -- API: Fetch -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Optional<Model>> retrieve(@Nonnull Key key, @Nonnull FetchOptions opts) {
    Objects.requireNonNull(key, "Cannot fetch model with `null` for key.");
    Objects.requireNonNull(opts, "Cannot fetch model without `options`.");
    enforceRole(key, DatapointType.OBJECT_KEY);
    final var id = id(key).orElseThrow(() -> new IllegalArgumentException("Cannot fetch model with empty key."));

    ExecutorService exec = opts.executorService().orElseGet(this::executorService);
    return ReactiveFuture.wrap(Futures.transform(ReactiveFuture.wrap(engine.getAll(ref(key)), exec), new Function<>() {
      @Override
      public @Nonnull Optional<Model> apply(@Nullable List<DocumentSnapshot> documentSnapshots) {
        if (documentSnapshots == null || documentSnapshots.isEmpty()) {
          return Optional.empty();

        } else if (documentSnapshots.size() > 1) {
          throw new IllegalStateException("Unexpectedly encountered more than 1 result.");

        } else {
          return Optional.of(deserialize(
            Objects.requireNonNull(
              documentSnapshots.get(0),
              "Unexpected null `DocumentReference`.")));
        }
      }
    }, exec), exec);
  }

  // -- API: Persist -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Model> persist(@Nonnull Key key,
                                                @Nonnull Model model,
                                                @Nonnull WriteOptions options) {
    Objects.requireNonNull(key, "Cannot write model with `null` for key.");
    Objects.requireNonNull(model, "Cannot write model which is, itself, `null`.");
    Objects.requireNonNull(options, "Cannot write model without `options`.");
    enforceRole(key, DatapointType.OBJECT_KEY);

    try {
      // collapse the model
      var serialized = codec.serialize(model);
      var that = this;

      return ReactiveFuture.wrap(engine.runTransaction(transaction -> {
        serialized.persist(null, new WriteProxy<DocumentReference>() {
          @Override
          public @Nonnull DocumentReference ref(@Nonnull String path, @Nullable String prefix) {
            return that.ref(path, prefix);
          }

          @Override
          public void put(@Nonnull DocumentReference key, @Nonnull SerializedModel message) {
            transaction.set(key, ImmutableMap.copyOf(message.getData()));
          }

          @Override
          public void create(@Nonnull DocumentReference key, @Nonnull SerializedModel message) {
            transaction.create(key, ImmutableMap.copyOf(message.getData()));
          }

          @Override
          public void update(@Nonnull DocumentReference key, @Nonnull SerializedModel message) {
            transaction.update(key, ImmutableMap.copyOf(message.getData()));
          }
        });
        return model;
      }));

    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
  }

  // -- API: Delete -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Key> delete(@Nonnull Key key, @Nonnull DeleteOptions options) {
    return null;
  }
}
