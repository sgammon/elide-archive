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
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;
import gust.backend.model.*;
import gust.backend.runtime.Logging;
import gust.backend.runtime.ReactiveFuture;
import gust.backend.transport.GoogleAPIChannel;
import gust.backend.transport.GoogleService;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.context.scope.Refreshable;
import org.slf4j.Logger;
import tools.elide.core.Datamodel;
import tools.elide.core.DatapointType;
import tools.elide.core.FieldType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static gust.backend.model.ModelMetadata.enforceRole;
import static gust.backend.model.ModelMetadata.modelAnnotation;
import static gust.backend.model.ModelMetadata.idField;
import static gust.backend.model.ModelMetadata.keyField;
import static gust.backend.model.ModelMetadata.spliceBuilder;
import static gust.backend.model.ModelMetadata.annotatedField;
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
  implements DatabaseDriver<Key, Model, DocumentSnapshot, CollapsedMessage> {
  /** Private log pipe. */
  private static final Logger logging = Logging.logger(FirestoreDriver.class);

  /** Whether to run operations in a transactional by default. */
  private static final Boolean defaultTransactional = true;

  /** Executor service to use for async calls. */
  private final ListeningScheduledExecutorService executorService;

  /** Codec to use for serializing/de-serializing models. */
  private final ModelCodec<Model, CollapsedMessage, DocumentSnapshot> codec;

  /** Firestore client engine. */
  private final Firestore engine;

  /** Deserializes Firestore {@link DocumentSnapshot} instances to {@link Message} instances. */
  final static class DocumentSnapshotDeserializer<M extends Message> implements ModelDeserializer<DocumentSnapshot, M> {
    /** Encapsulated object deserializer. */
    private final ObjectModelDeserializer<M> objectDeserializer;

    /**
     * Private constructor.
     *
     * @param instance Model instance to deserialize.
     */
    private DocumentSnapshotDeserializer(@Nonnull M instance) {
      this.objectDeserializer = ObjectModelDeserializer.defaultInstance(instance);
    }

    /**
     * Construct a {@link DocumentSnapshot} deserializer for the provided <b>instance</b>.
     *
     * @param instance Model instance to acquire a snapshot deserializer for.
     * @param <M> Model type to deserialize.
     * @return Snapshot deserializer instance.
     */
    static <M extends Message> DocumentSnapshotDeserializer<M> forModel(@Nonnull M instance) {
      return new DocumentSnapshotDeserializer<>(instance);
    }

    /** @inheritDoc */
    @Override
    public @Nonnull M inflate(@Nonnull DocumentSnapshot documentSnapshot) throws ModelInflateException {
      return objectDeserializer.inflate(Objects.requireNonNull(documentSnapshot.getData()));
    }
  }

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
      @Nonnull M instance) {
      return new FirestoreDriver<>(
        firestoreChannel,
        credentialsProvider,
        transportOptions,
        executorService,
        CollapsedMessageCodec.forModel(instance, DocumentSnapshotDeserializer.forModel(instance)));
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
                          ModelCodec<Model, CollapsedMessage, DocumentSnapshot> codec) {
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
    try {
      return this.codec.deserialize(snapshot);
    } catch (IOException ioe) {
      var buf = new StringWriter();
      var printer = new PrintWriter(buf);
      ioe.printStackTrace(printer);
      logging.error("Failed to deserialize model: '" + ioe.getMessage() + "'.\n" + buf);
      throw new RuntimeException(ioe);
    }
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
  public ModelCodec<Model, CollapsedMessage, DocumentSnapshot> codec() {
    return this.codec;
  }

  /**
   * Convert a model `Key` into a Firestore {@link DocumentReference}.
   *
   * @param keyInstance Key instance to convert into a ref.
   * @return Computed document reference.
   */
  private @Nonnull DocumentReference ref(@Nonnull Message keyInstance) {
    if (logging.isDebugEnabled())
      logging.debug("Creating Firestore ref from key instance '" + keyInstance.toString() + "'.");
    enforceRole(keyInstance, DatapointType.OBJECT_KEY);
    var keyDescriptor = keyInstance.getDescriptorForType();

    // first: resolve the key's model path
    String resolvedPath;
    var explicitPath = modelAnnotation(keyDescriptor, Datamodel.db, false);

    if (explicitPath.isPresent() && !explicitPath.get().getPath().isEmpty()) {
      resolvedPath = explicitPath.get().getPath();
      if (logging.isTraceEnabled())
        logging.trace("Explicit path found for type '"
            + keyDescriptor.getFullName() + "'. Using '" + resolvedPath + "'.");
    } else {
      // `PersonKey` -> `persons`
      resolvedPath = keyInstance
          .getDescriptorForType()
          .getName()
          .toLowerCase()
          .replace("key", "")
          + "s";

      if (logging.isTraceEnabled())
        logging.trace("No explicit path found for type '"
            + keyDescriptor.getFullName() + "'. Resolved as '" + resolvedPath + "'.");
    }

    // second: resolve the key's ID
    var resolvedId = id(keyInstance);
    Optional<String> targetId = resolvedId.map(Object::toString);

    // third: resolve the model's parent, if applicable
    var parentField = annotatedField(
        keyDescriptor,
        Datamodel.field,
        false,
        Optional.of((field) -> field.getType() == FieldType.PARENT));

    if (parentField.isPresent()) {
      var parentInstance = ModelMetadata.pluck(keyInstance, parentField.get());
      if (parentInstance.getValue().isPresent()) {
        var parentKey = this.ref((Message)parentInstance.getValue().get());

        DocumentReference ref = targetId
            .map(s -> parentKey.collection(resolvedPath).document(s))
            .orElseGet(() -> parentKey.collection(resolvedPath).document());

        if (logging.isDebugEnabled())
          logging.debug("Generated document reference with parent: '" + ref.toString() + "'.");
        return ref;
      } else {
        // no parent present when one is required: fail
        throw new IllegalStateException("Cannot persist key with missing parent when one is requred.");
      }
    } else {
      // build a document reference with no parent
      DocumentReference ref = targetId
          .map(s -> engine.collection(resolvedPath).document(s))
          .orElseGet(() -> engine.collection(resolvedPath).document());

      if (logging.isDebugEnabled())
        logging.debug("Generated document reference with no parent: '" + ref.toString() + "'.");
      return ref;
    }
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
    var fieldPointer = keyField(instance);
    if (fieldPointer.isEmpty())
      throw new IllegalArgumentException("Failed to resolve key field for message '"
          + instance.getDescriptorForType().getFullName() + "'.");

    var keyBuilder = instance.toBuilder().getFieldBuilder(fieldPointer.get().getField());
    var idPointer = idField(keyBuilder.getDescriptorForType());
    if (idPointer.isEmpty())
      throw new IllegalArgumentException("Failed to resolve key ID field for key message type '"
          + keyBuilder.getDescriptorForType().getFullName() + "'.");

    spliceBuilder(
        keyBuilder,
        idPointer.get(),
        Optional.of(UUID.randomUUID().toString().toUpperCase()));

    //noinspection unchecked
    return (Key)keyBuilder.build();
  }

  /** @return Converted Firestore field mask from the provided Protobuf mask. */
  private @Nullable com.google.cloud.firestore.FieldMask convertMask(@Nonnull FieldMask originalMask) {
    ProtocolStringList paths = originalMask.getPathsList();
    if (!paths.isEmpty()) {
      if (logging.isDebugEnabled())
        logging.debug("Applying field mask for Firestore operation: \n" + originalMask.toString());

      ArrayList<String> pathsList = new ArrayList<>(paths.size());
      var count = originalMask.getPathsCount();
      for (int i = 0; i < count; i++) {
        pathsList.add(originalMask.getPaths(i));
      }

      String[] pathsArr = new String[pathsList.size()];
      pathsList.toArray(pathsArr);

      return com.google.cloud.firestore.FieldMask.of(pathsArr);
    }
    return null;
  }

  /** @return Preconditions for the provided operational options. */
  private @Nonnull Precondition generatePreconditions(@Nonnull OperationOptions options) {
      var updatedTimestamp = options.updatedAtMicros()
          .map(Timestamp::ofTimeMicroseconds)
          .orElseGet(() -> options.updatedAtSeconds()
              .map((secs) -> Timestamp.ofTimeSecondsAndNanos(secs, 0))
              .orElse(null));

      if (updatedTimestamp == null) {
        return Precondition.NONE;
      } else {
        return Precondition.updatedAt(updatedTimestamp);
      }
  }

  /** @return Fetched model, with the provided field mask enforced. */
  private @Nonnull Model enforceMask(@Nonnull Model instance, @Nullable Optional<FieldMask> mask) {
    // nothing from nothing
    Objects.requireNonNull(instance, "model instance should not be null for mask enforcement");
    // @TODO: field mask enforcement
    return instance;
  }

  // -- API: Fetch -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Optional<Model>> retrieve(@Nonnull Key key, @Nonnull FetchOptions opts) {
    Objects.requireNonNull(key, "Cannot fetch model with `null` for key.");
    Objects.requireNonNull(opts, "Cannot fetch model without `options`.");
    enforceRole(key, DatapointType.OBJECT_KEY);
    id(key).orElseThrow(() -> new IllegalArgumentException("Cannot fetch model with empty key."));

    ExecutorService exec = opts.executorService().orElseGet(this::executorService);
    DocumentReference[] refs = {ref(key)};
    FieldMask mask = opts.fieldMask().orElse(null);
    var that = this;

    if (opts.transactional().orElse(defaultTransactional)) {
      return ReactiveFuture.wrap(Futures.transform(ReactiveFuture.wrap(engine.runAsyncTransaction((txn) ->
          txn.getAll(refs, mask != null ? this.convertMask(mask) : null),
          TransactionOptions.createReadOnlyOptionsBuilder()
              .setExecutor(exec)
              .setReadTime(opts.snapshot()
                  .map((secs) -> com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(secs))
                  .orElse(null))
              .build())), documentSnapshots -> {
        // check for results
        if (documentSnapshots != null && !documentSnapshots.isEmpty()) {
          return Optional.of(that.deserialize(documentSnapshots.get(0)));
        } else {
          // otherwise return an empty optional
          return Optional.empty();
        }
      }, exec));

    } else {
      return ReactiveFuture.wrap(Futures.transform(ReactiveFuture.wrap(
          engine.getAll(
              refs,
              mask != null ? this.convertMask(mask) : null
          ),
          exec), new Function<>() {
        @Override
        public @Nonnull Optional<Model> apply(@Nullable List<DocumentSnapshot> documentSnapshots) {
          if (documentSnapshots == null || documentSnapshots.isEmpty() || (
              documentSnapshots.size() == 1 && !documentSnapshots.get(0).exists())) {
            return Optional.empty();

          } else if (documentSnapshots.size() > 1) {
            throw new IllegalStateException("Unexpectedly encountered more than 1 result.");

          } else {
            return Optional.of(that.enforceMask(deserialize(
                Objects.requireNonNull(
                    documentSnapshots.get(0),
                    "Unexpected null `DocumentReference`.")), opts.fieldMask()));
          }
        }
      }, exec), exec);
    }
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
    ExecutorService exec = options.executorService().orElseGet(this::executorService);

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
      }, TransactionOptions.createReadWriteOptionsBuilder()
          .setExecutor(exec)
          .setNumberOfAttempts(options.retries().orElse(2))
          .build()));

    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
  }

  // -- API: Delete -- //
  /** {@inheritDoc} */
  @Override
  public @Nonnull ReactiveFuture<Key> delete(@Nonnull Key key, @Nonnull DeleteOptions options) {
    Objects.requireNonNull(key, "Cannot delete model with `null` for key.");
    Objects.requireNonNull(options, "Cannot delete model without `options`.");
    enforceRole(key, DatapointType.OBJECT_KEY);
    id(key).orElseThrow(() -> new IllegalArgumentException("Cannot delete model with empty key."));
    ExecutorService exec = options.executorService().orElseGet(this::executorService);

    return ReactiveFuture.wrap(engine.runTransaction(transaction -> {
      transaction.delete(ref(key), generatePreconditions(options));
      return key;
    }, TransactionOptions.createReadWriteOptionsBuilder()
      .setExecutor(exec)
      .setNumberOfAttempts(options.retries().orElse(2))
      .build()));
  }
}
