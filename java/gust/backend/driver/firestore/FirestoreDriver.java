package gust.backend.driver.firestore;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.common.base.Function;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;


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

  // -- API: Key Generation -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull Key generateKey(@Nonnull Message instance) {
    return null;
  }

  // -- API: Fetch -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Optional<Model>> retrieve(@Nonnull Key key, @Nonnull FetchOptions opts) {
    ExecutorService exec = opts.executorService().orElseGet(this::executorService);
    return ReactiveFuture.wrap(Futures.transform(ReactiveFuture.wrap(engine.getAll(), exec), new Function<>() {
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
  public @Nonnull ReactiveFuture<Model> persist(@Nonnull Key documentReference,
                                                @Nonnull Model model,
                                                @Nonnull WriteOptions options) {
    return null;
  }
}
