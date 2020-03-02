package gust.backend.driver.firestore;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.Message;
import gust.backend.model.*;
import gust.backend.runtime.Logging;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.context.scope.Refreshable;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;


/**
 * Defines a built-in database adapter for interacting with Google Cloud Firestore, using business-data models defined
 * through Protobuf annotated with framework-provided metadata.
 *
 * <p>This adapter makes use of a specialized {@link DatabaseDriver} ({@link FirestoreDriver}), and supports a custom
 * configuration class ({@link FirestoreTransportConfig}) which is loaded from application config during service channel
 * initialization. Connections are pooled and cached against a caching executor by the active transport manager.</p>
 *
 * <p>Instantiation is disallowed to facilitate restriction of the active {@link ModelCodec} to Firestore's own model
 * codec, which uses {@link gust.backend.model.ObjectModelCodec} to produce generic collapsed messages during
 * serialization, and to translate from proto-maps during deserialization. Optionally, a compliant instance of
 * {@link CacheDriver} may be provided at construction time, which enables caching against that driver for calls that
 * are eligible (according, again, to settings from {@link FirestoreTransportConfig}).</p>
 *
 * @param <Model> Model type which this adapter adapts to Firestore.
 * @see FirestoreDriver Driver for speaking to Firestore.
 * @see FirestoreTransportConfig Transport configuration for Firestore.
 */
@Immutable
@ThreadSafe
@SuppressWarnings({"WeakerAccess", "unused", "UnstableApiUsage"})
public final class FirestoreAdapter<Key extends Message, Model extends Message>
  implements DatabaseAdapter<Key, Model, CollapsedMessage> {
  /** Private log pipe. */
  private static final Logger logging = Logging.logger(FirestoreAdapter.class);

  /** Firestore database driver. */
  private final @Nonnull FirestoreDriver<Key, Model> driver;

  /** Serializer and deserializer for this model. */
  private final @Nonnull ModelCodec<Model, CollapsedMessage> codec;

  /** Cache to use for model interactions through this adapter (optional). */
  private final @Nonnull Optional<CacheDriver<Key, Model>> cache;

  /**
   * Setup a new Firestore adapter from scratch. Generally instances of this class are acquired through injection, or
   * static factory methods also listed on this class.
   *
   * @param driver Database driver to use when speaking to Firestore.
   * @param codec Serializer and deserializer to use.
   * @param cache Cache to use when reading data from Firestore (optional).
   */
  private FirestoreAdapter(@Nonnull FirestoreDriver<Key, Model> driver,
                           @Nonnull ModelCodec<Model, CollapsedMessage> codec,
                           @Nonnull Optional<CacheDriver<Key, Model>> cache) {
    this.driver = driver;
    this.codec = codec;
    this.cache = cache;
  }

  /**
   * Create or otherwise resolve a {@link FirestoreAdapter} for the provided model type and builder. This additionally
   * resolves a model codec, driver, and optionally a caching engine as well (although one may be provided explicitly at
   * the invoking developer's discretion - see {@link #forModel(Message.Builder, FirestoreDriver, Optional)}).
   *
   * <p>The resulting adapter may not be created fresh for the task at hand, but it is threadsafe and shares no direct
   * state with any other operation.</p>
   *
   * @see #forModel(Message.Builder, FirestoreDriver, Optional) To provide an explicit cache driver for this type.
   * @param <M> Model type for which we are requesting a Firestore adapter instance.
   * @param builder Model builder instance, which the engine will clone for each retrieve operation.
   * @param driver Driver which we should use when handling instances of <code>M</code>.
   * @return Pre-fabricated (or otherwise resolved) Firestore adapter for the requested model.
   */
  public static @Nonnull <K extends Message, M extends Message> FirestoreAdapter<K, M> forModel(
    @Nonnull Message.Builder builder,
    @Nonnull FirestoreDriver<K, M> driver) {
    return forModel(builder, driver, Optional.empty());
  }

  /**
   * Create or otherwise resolve a {@link FirestoreAdapter} for the provided model type and builder. This additionally
   * resolves a model codec, driver, and optionally a caching engine as well.
   *
   * <p>The resulting adapter may not be created fresh for the task at hand, but it is threadsafe and shares no direct
   * state with any other operation.</p>
   *
   * @param <M> Model type for which we are requesting a Firestore adapter instance.
   * @param driver Driver which we should use when handling instances of <code>M</code>.
   * @param builder Model builder instance, which the engine will clone for each retrieve operation.
   * @return Pre-fabricated (or otherwise resolved) Firestore adapter for the requested model.
   */
  public static @Nonnull <K extends Message, M extends Message> FirestoreAdapter<K, M> forModel(
    @Nonnull Message.Builder builder,
    @Nonnull FirestoreDriver<K, M> driver,
    @Nonnull Optional<CacheDriver<K, M>> cacheDriver) {
    return new FirestoreAdapter<>(
      driver,
      CollapsedMessageCodec.forModel(builder),
      cacheDriver);
  }

  /** Factory responsible for creating {@link FirestoreAdapter} instances from injected dependencies. */
  @Factory
  final static class FirestoreAdapterFactory {
    /**
     * Acquire a new instance of the Firestore adapter, using the specified component objects to facilitate model
     * serialization/deserialization, and transport communication with Firestore.
     *
     * @param messageInstance Empty message instance to infer type information from.
     * @param driver Driver with which we should talk to Firestore.
     * @param cache Driver with which we should cache eligible data.
     * @return Firestore driver instance.
     */
    @Context
    @Refreshable
    public static @Nonnull <K extends Message, M extends Message> FirestoreAdapter<K, M> acquire(
      @Nonnull Message messageInstance,
      @Nonnull FirestoreDriver<K, M> driver,
      @Nonnull Optional<CacheDriver<K, M>> cache) {
      // resolve model builder from type
      return FirestoreAdapter.forModel(
        messageInstance.newBuilderForType(),
        driver,
        cache);
    }
  }

  // -- Components -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ModelCodec<Model, CollapsedMessage> codec() {
    return this.codec;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull Optional<CacheDriver<Key, Model>> cache() {
    return this.cache;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull DatabaseDriver<Key, Model, CollapsedMessage> engine() {
    return this.driver;
  }

  /** {@inheritDoc} */
  @Override
  public @Nonnull ListeningScheduledExecutorService executorService() {
    return driver.executorService();
  }
}
