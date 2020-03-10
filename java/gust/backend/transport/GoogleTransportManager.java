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
package gust.backend.transport;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.grpc.*;
import gust.Core;
import gust.backend.runtime.Logging;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.runtime.context.scope.Refreshable;
import org.slf4j.Logger;
import org.threeten.bp.Duration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Supplies a {@link TransportManager} implementation for dealing with Google Cloud APIs via gRPC and Protobuf. In this
 * object, we make heavy use of the Google API Extensions for Java ("GAX"), in order to centrally manage channels, on-
 * demand, for downstream service use.
 *
 * <p>Connection instances may be requested from this object like any other transport manager, but if there is an
 * existing managed channel for the provided service ID, it will provide the existing instance (or one from a pool of
 * existing instances) rather than creating a new one.</p>
 *
 * <p>To request a connection instance from this transport manager, annotate an injectable method parameter (or
 * constructor parameter) of type {@link Channel} with {@link GoogleAPIChannel}. For example:
 * <code>
 *   public void doSomething(@GoogleAPIChannel(Service.PUBSUB) Channel pubsubChannel) {
 *     // ...
 *   }
 * </code></p>
 *
 * <p><b>Configuration:</b> Each Google service has a specified <i>name</i>, included on the docs in
 * {@link GoogleService}, at which that service may be configured in <pre>application.yml</pre>. For example, the
 * following code configures the Cloud Pubsub client's keepalive and pool size settings:
 * <code>
 *   transport:
 *     google:
 *       pubsub:
 *         poolSize: 3
 *         keepAliveTime: 15s
 *         keepAliveTimeout: 30s
 *         keepAliveWithoutCalls: true
 * </code></p>
 */
@Context
@Singleton
@Immutable
@ThreadSafe
@Refreshable
@SuppressWarnings("unused")
@ConfigurationProperties(GoogleTransportManager.CONFIG_PREFIX)  // `transport.google`
public final class GoogleTransportManager implements TransportManager<GoogleAPIChannel, GoogleService, Channel> {
  /** Prefix under which Google services may be configured. */
  public final static String CONFIG_PREFIX = ROOT_CONFIG_PREFIX + ".google";

  /** Logging pipe. */
  private final static Logger logging = Logging.logger(GoogleTransportManager.class);

  /** Maximum number of concurrent connections per service. */
  private final static int DEFAULT_MAX_POOL_SIZE = 3;

  /** Tag to append to the user-agent on outgoing calls. */
  private final static String GUST_TAG = "gust/" + Core.getGustVersion();

  /** Map of pooled connections, grouped per-service. */
  private final ConcurrentMap<GoogleService, ManagedChannelPool> poolMap;

  /** Maximum size of any one service connection pool. */
  private volatile int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

  /** Executor service to use for RPC traffic. */
  private volatile @Nonnull ScheduledExecutorService executorService;

  /** Thrown when required transport configuration is missing. */
  private static final class TransportConfigMissing extends TransportException {
    /**
     * Instantiate a new `TransportConfigMissing` exception.
     *
     * @param svc Service we are missing config for.
     */
    private TransportConfigMissing(@Nonnull GoogleService svc) {
      super(String.format(
        "Google transport configuration could not be resolved for service %s.", svc.getToken()));
    }
  }

  /** Thrown when credentials are required, but missing. */
  private static final class TransportCredentialsMissing extends TransportException {
    /**
     * Instantiate a new `TransportConfigMissing` exception.
     *
     * @param svc Service we are missing config for.
     */
    private TransportCredentialsMissing(@Nonnull GoogleService svc, @Nullable IOException ioe) {
      super(String.format(
        "Google transport credentials could not be resolved for service %s.", svc.getToken()), ioe);
    }
  }

  /** Exception thrown when a channel could not be established. */
  private static final class ChannelEstablishFailed extends TransportException {
    /**
     * Instantiate a new `ChannelEstablishException` exception.
     *
     * @param svc Service that failed to establish a connection.
     * @param ioe Wrapped IO exception.
     */
    private ChannelEstablishFailed(@Nonnull GoogleService svc, @Nonnull IOException ioe) {
      super(String.format(
        "Failed to establish a connection to Google service '%s'.", svc.getToken()), ioe);
    }
  }

  /** Client header interceptor for injecting the framework `User-Agent` header. */
  @Immutable
  private static final class UAInterceptor extends GrpcHeaderInterceptor {
    /** Singleton interceptor instance. */
    private static final UAInterceptor INSTANCE = new UAInterceptor();

    private UAInterceptor() {
      super(Collections.singletonMap("user-agent", GUST_TAG));
    }
  }

  /** Wrapper object that manages a set of pooled connections. */
  @Immutable
  private final static class ManagedChannelPool {
    /** Known service we will be managing connections for. */
    private final GoogleService service;

    /** Holds the set of channels managed by this pool. */
    private final InstantiatingGrpcChannelProvider provider;

    /**
     * Initialize a new managed connection pool for the provided service configuration.
     *
     * @param maxPoolSize Maximum per-service pool size.
     * @param executorService Executor service to use for spawning work.
     * @param svc Service which we will be managing connections for.
     * @param config Configuration by which to initialize gRPC channels.
     */
    private ManagedChannelPool(int maxPoolSize,
                               @Nonnull ScheduledExecutorService executorService,
                               @Nonnull GoogleService svc,
                               @Nonnull GoogleTransportConfig config) {
      this.service = svc;
      this.provider = buildProvider(maxPoolSize, svc, executorService, config);
    }

    /**
     * Acquire a managed channel from this pool, potentially blocking until one is ready or otherwise free. If all
     * channels are busy, and the maximum number of channels has not yet been reached, one may be established fresh for
     * the invoking caller, which may also incur delays.
     *
     * @return Managed channel acquired for the service configured with this pool.
     * @throws ChannelEstablishFailed If an I/O error occurs establishing or resolving the requested channel.
     */
    @Nonnull GrpcTransportChannel acquire() throws ChannelEstablishFailed {
      try {
        if (logging.isDebugEnabled())
          logging.debug(String.format("Acquiring connection for service '%s'...", this.service.getToken()));

        // acquire the channel
        GrpcTransportChannel channel = (GrpcTransportChannel)this.provider.getTransportChannel();
        if (channel == null || channel.isShutdown())
          throw new IllegalStateException("Failed to acquire gRPC channel (or was initially shut down).");
        else if (logging.isDebugEnabled())
          logging.debug(String.format("gRPC channel acquired ('%s').", channel.getChannel().authority()));
        return channel;

      } catch (IOException ioe) {
        logging.error(String.format("Failed to establish managed channel (error: '%s').", ioe.getMessage()));
        throw new ChannelEstablishFailed(this.service, ioe);
      }
    }
  }

  /**
   * Build the provided {@link GoogleTransportConfig} into a gRPC channel provider, which applies the configuration when
   * instantiating channels according to needs invoked through the active {@link TransportManager}.
   *
   * @param maxPoolSize Maximum size of the channel pool.
   * @param service Service for which we are building a channel provider.
   * @param executorService Executor service to make use of when executing RPCs.
   * @param config gRPC configuration to apply when instantiating new channels for this service.
   * @return Pre-fabricated provider instance to use when instantiating new channels.
   */
  private static InstantiatingGrpcChannelProvider buildProvider(int maxPoolSize,
                                                                @Nonnull GoogleService service,
                                                                @Nonnull ScheduledExecutorService executorService,
                                                                @Nonnull GoogleTransportConfig config) {
    if (logging.isTraceEnabled())
      logging.trace("Setting up new gRPC channel provider...");
    // begin setting up provider
    InstantiatingGrpcChannelProvider.Builder builder = InstantiatingGrpcChannelProvider.newBuilder()
      // target
      .setEndpoint(Objects.requireNonNull(config.endpoint(),
        "Managed channel endpoint cannot be null."))

      // message sizes
      .setMaxInboundMessageSize(Objects.requireNonNull(config.maxInboundMessageSize(),
        "Maximum inbound message size cannot be null."))
      .setMaxInboundMetadataSize(Objects.requireNonNull(config.maxInboundMetadataSize(),
        "Maximum inbound metadata size cannot be null."))

      // pooling & execution
      .setPoolSize(Math.min(
        Objects.requireNonNull(config.getPoolSize(),
          "Cannot set `null` for max pool size."),
        maxPoolSize))

      .setExecutorProvider(new ExecutorProvider() {
        @Override
        public boolean shouldAutoClose() {
          return false; /* this executor is shared, so don't auto-close per-service. */
        }

        @Override
        public ScheduledExecutorService getExecutor() {
          return executorService;
        }
      });

    // interceptors
    if (logging.isTraceEnabled()) logging.trace("Applying extra interceptors...");
    Optional<List<ClientInterceptor>> extraInterceptors = config.getExtraInterceptors();
    if (extraInterceptors.isPresent()) {
      List<ClientInterceptor> extraInterceptorsList = extraInterceptors.get();
      final ArrayList<ClientInterceptor> composedInterceptors = new ArrayList<>(
        extraInterceptorsList.size() + 1);
      composedInterceptors.add(UAInterceptor.INSTANCE);
      composedInterceptors.addAll(extraInterceptorsList);
      builder.setInterceptorProvider(() -> Collections.unmodifiableList(composedInterceptors));
      if (logging.isDebugEnabled())
        logging.debug(String.format(
          "Custom interceptors added (%s), along with `UAInterceptor`.", extraInterceptorsList.size()));
    } else {
      builder.setInterceptorProvider(() -> Collections.singletonList(UAInterceptor.INSTANCE));
      if (logging.isDebugEnabled())
        logging.debug("No custom interceptors detected. Added `UAInterceptor`.");
    }

    // keepalive
    if (logging.isTraceEnabled()) logging.trace("Applying keepalive settings...");
    if (Objects.requireNonNull(config.getKeepaliveEnabled())) {
      builder
        .setKeepAliveTime(Duration.ofSeconds(config.getKeepaliveTime().getSeconds()))
        .setKeepAliveTimeout(Duration.ofSeconds(config.getKeepaliveTimeout().getSeconds()))
        .setKeepAliveWithoutCalls(config.getKeepAliveNoActivity());
      if (logging.isDebugEnabled())
        logging.debug(String.format(
          "Managed channel keepalive is ENABLED with (%s) time, (%s) timeout.",
          config.getKeepaliveTime().toString(),
          config.getKeepaliveTimeout().toString()));
    } else {
      if (logging.isDebugEnabled())
        logging.debug("Managed channel keepalive is disabled.");
    }

    // credentials
    if (logging.isTraceEnabled()) logging.trace("Applying credential settings...");
    Optional<CredentialsProvider> maybeProvider = config.credentialsProvider();
    if (Objects.requireNonNull(config.requiresCredentials()) && !maybeProvider.isPresent()) {
      logging.error(String.format(
        "Failed to initialize gRPC service '%s': credentials were required, but could not be obtained.",
        service.getToken()));
      throw new TransportCredentialsMissing(service, null);
    }
    if (maybeProvider.isPresent()) {
      if (logging.isTraceEnabled()) logging.trace("Found credential provider. Resolving...");
      try {
        builder.setCredentials(maybeProvider.get().getCredentials());
        if (logging.isDebugEnabled()) logging.debug("Credentials were resolved and attached.");
      } catch (IOException ioe) {
        logging.error(String.format(
          "Failed to initialize gRPC service '%s': credentials were specified, but failed to load.",
          service.getToken()));
        throw new TransportCredentialsMissing(service, ioe);
      }
    }

    // connection priming
    if (Objects.requireNonNull(config.enablePrimer())) {
      if (logging.isDebugEnabled()) logging.debug("Connection priming ENABLED.");
      builder.setChannelPrimer(managedChannel -> primeManagedChannel(service, config, managedChannel));
    } else if (logging.isDebugEnabled()) {
      logging.debug("Connection priming DISABLED.");
    }
    return builder.build();
  }

  /**
   * Prime a managed gRPC channel, once it has been established by the underlying GAX tooling.
   *
   * @param service Service which we are instantiating a channel for.
   * @param config Configuration for our gRPC service channel.
   * @param channel Instantiated/established connection and higher-order RPC channel.
   */
  private static void primeManagedChannel(@Nonnull GoogleService service,
                                          @Nonnull GoogleTransportConfig config,
                                          @Nonnull ManagedChannel channel) {
    throw new IllegalStateException("channel priming is not yet supported");
  }

  /**
   * Initialize a new Google Transport Manager.
   *
   * @param executorService Scheduled executor against which to execute RPC traffic.
   */
  @Inject
  GoogleTransportManager(@Nonnull ScheduledExecutorService executorService) {
    if (logging.isTraceEnabled())
      logging.trace(String.format("Initializing `GoogleTransportManager` (version tag: '%s').", GUST_TAG));
    if (logging.isDebugEnabled())
      logging.debug(String.format("`GoogleTransportManager` executor: '%s'.", executorService.getClass().getName()));
    this.poolMap = new ConcurrentSkipListMap<>();  // initialize pool map
    this.executorService = executorService;
  }

  /**
   * Generate or otherwise resolve a transport-layer configuration for the provided <pre>service</pre>. This includes
   * stuff like the actual endpoint to connect to, keepalive configuration, retries, pooling, and so on.
   *
   * @param service Service for which we should generate or acquire a transport config.
   * @return Transport configuration for the specified service.
   * @throws TransportConfigMissing If configuration cannot be resolved.
   */
  private static @Nonnull GoogleTransportConfig configForService(@Nonnull GoogleService service)
      throws TransportConfigMissing {
    if (logging.isTraceEnabled())
      logging.trace(String.format("Resolving configuration type for Google service '%s'...", service.getToken()));
    Optional<Class<GoogleTransportConfig>> cfgType = service.getConfigType();
    if (cfgType.isPresent()) {
      Class<GoogleTransportConfig> cfgTypeClass = cfgType.get();
      if (logging.isDebugEnabled())
        logging.debug(String.format("Configuration type resolved as '%s'.", cfgType.get().getName()));
      // create config instance
      try {
        return cfgTypeClass.newInstance();
      } catch (InstantiationException | IllegalAccessException err) {
        logging.error(
          String.format("Failed to resolve configuration for Google service '%s'.", service.getToken()));
        throw new TransportConfigMissing(service);
      }
    }
    throw new IllegalStateException(
      "Failed to resolve configuration type for service '%s'. It may not be implemented.");
  }

  /**
   * Resolve a managed channel for the provided Google API service, according to the provided transport configuration,
   * which contains transport-layer settings to apply when establishing new connections for this service.
   *
   * @param service Service to establish or otherwise resolve a connection for.
   * @param config Configuration to apply when establishing connections for this service.
   * @return Managed channel, established for the provided service.
   * @throws ChannelEstablishFailed If a channel cannot be established.
   */
  private @Nonnull GrpcTransportChannel resolveChannel(@Nonnull GoogleService service,
                                                       @Nonnull GoogleTransportConfig config) throws TransportException {
    if (logging.isTraceEnabled())
      logging.trace(String.format("Resolving managed gRPC channel for Google service '%s'.", service.getToken()));

    final @Nonnull ManagedChannelPool pool;
    if (!poolMap.containsKey(service)) {
      if (logging.isDebugEnabled()) logging.debug(String.format(
          "Connection pool not found for service '%s'. Establishing...", service.getToken()));

      // establish initial pool of connections
      pool = new ManagedChannelPool(maxPoolSize, executorService, service, config);
      poolMap.put(service, pool);
    } else {
      if (logging.isTraceEnabled()) logging.trace(String.format(
        "Connection pool found for service '%s'. Using existing.", service.getToken()));
      pool = Objects.requireNonNull(poolMap.get(service));
    }
    return pool.acquire();
  }

  // -- Getters & Setters -- //

  /** @return Maximum size of any one service connection pool. */
  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  // -- Public API -- //
  /**
   * Acquire a connection from this transport manager. The connection provided may or may not be freshly-created,
   * depending on the underlying implementation, but it should never be <pre>null</pre> (exceptions are raised instead).
   *
   * @param type Type of connection to acquire. Defined by the implementation.
   * @return Connection instance for the desired service, potentially fresh, potentially re-used.
   * @throws TransportException If the connection could not be acquired.
   */
  @Override
  public @Nonnull Channel acquire(@Nonnull GoogleService type) throws TransportException {
    //noinspection ConstantConditions
    if (type == null) throw new IllegalArgumentException("Cannot resolve connection for `null` service.");
    return resolveChannel(type, configForService(type)).getChannel();
  }

  /**
   * Provide acquired connections via injection-annotated {@link Channel} method or constructor parameters. This
   * essentially proxies to {@link #acquire(GoogleService)}, passing in the service specified in the context of the
   * {@link GoogleAPIChannel} annotation.
   *
   * @param context Intercepted method execution context.
   * @return Resulting object.
   * @throws TransportException If the connection could not be acquired.
   */
  @Override
  public Channel intercept(MethodInvocationContext<Object, Channel> context) {
    if (!context.hasAnnotation(GoogleAPIChannel.class)) {
      throw new IllegalArgumentException(
        "Must annotate method with @GoogleAPIChannel to inject from GoogleTransportManager.");
    }
    // fetch annotation and resolve desired service
    @Nonnull AnnotationValue<GoogleAPIChannel> anno = Objects.requireNonNull(
      context.getAnnotation(GoogleAPIChannel.class));
    Optional<GoogleService> desiredService = anno.enumValue("service", GoogleService.class);
    if (!desiredService.isPresent())
      throw new IllegalArgumentException("Must provide desired service to GoogleTransportManager.");
    return acquire(desiredService.get());
  }
}
