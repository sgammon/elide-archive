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

import gust.backend.transport.GoogleTransportManager;
import gust.backend.transport.GrpcTransportConfig;
import io.micronaut.context.annotation.ConfigurationProperties;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;


/**
 * Specifies configuration property bindings for a managed transport channel interacting with Cloud Firestore. Also in
 * charge of supplying a sensible set of defaults, if no config properties are specified.
 *
 * <p>Configurable aspects of the framework's connection to Firestore include keepalive timings, retries, connection
 * pooling, connection refreshing, and more. All of these may be customized via a number of code paths:
 * <ul>
 *   <li><b>Environment:</b> By default, Micronaut will automatically merge config with environment vars.</li>
 *   <li><b>Config:</b> You can configure things under the <pre>transport.google.firestore</pre> prefix.</li>
 *   <li><b>Bean events:</b> You can watch for a bean event creating this object, and use the methods on it to change
 *       configured values before they are used.</li>
 * </ul></p>
 */
@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal"})
@ConfigurationProperties(FirestoreTransportConfig.CONFIG_PREFIX)  // `transport.google.firestore`
public final class FirestoreTransportConfig implements GrpcTransportConfig {
  // -- Paths -- //

  /** Token under `transport.google` to look for Firestore config. */
  private final static String CONFIG_TOKEN = "firestore";

  /** Path prefix at which the Firestore transport layer may be configured. */
  public final static String CONFIG_PREFIX = GoogleTransportManager.CONFIG_PREFIX + "." + CONFIG_TOKEN;

  // -- Defaults -- //

  /** Specifies the default number of connections to maintain to Firestore. */
  public final static int DEFAULT_POOL_SIZE = 2;

  /** Whether to enable keepalive features by default. */
  public final static boolean DEFAULT_ENABLE_KEEPALIVE = true;

  /** Length of time in between keepalive pings. */
  public final static Duration DEFAULT_KEEPALIVE_TIME = Duration.ofMinutes(3);

  /** Amount of time to wait before ending the keepalive stream. */
  public final static Duration DEFAULT_KEEPALIVE_TIMEOUT = Duration.ofMinutes(15);

  /** Whether to keep the connection alive, even if there is no activity. */
  public final static boolean DEFAULT_KEEPALIVE_NO_ACTIVITY = true;

  /** Default Firestore endpoint. */
  public final static String DEFAULT_FIRESTORE_ENDPOINT = "firestore.googleapis.com";

  // -- Configuration Properties -- //

  /** Size of the connection pool for Firestore. */
  private @Min(1) int poolSize = DEFAULT_POOL_SIZE;

  /** Whether to enable keepalive features. */
  private boolean keepaliveEnabled = DEFAULT_ENABLE_KEEPALIVE;

  /** Length of time in between keepalive pings. */
  private @Nonnull Duration keepaliveTime = DEFAULT_KEEPALIVE_TIME;

  /** Amount of time to wait before ending the keepalive stream. */
  private @Nonnull Duration keepaliveTimeout = DEFAULT_KEEPALIVE_TIMEOUT;

  /** Whether to keep the connection alive, even if there is no activity. */
  private boolean keepaliveNoActivity = DEFAULT_KEEPALIVE_NO_ACTIVITY;

  /** Endpoint at which to connect to Firestore. */
  private @NotBlank @Nonnull String firestoreEndpoint = DEFAULT_FIRESTORE_ENDPOINT;

  // -- Interface Compliance -- //
  /** @return gRPC endpoint at which to connect to the target service. */
  @Override
  public @Nonnull String endpoint() {
    return firestoreEndpoint;
  }

  /** @return Retrieve the desired connection pool size. */
  @Override
  public @Nonnull Integer getPoolSize() {
    return poolSize;
  }

  /** @return Whether to enable keepalive features. */
  @Override
  public @Nonnull Boolean getKeepaliveEnabled() {
    return keepaliveEnabled;
  }

  /** @return Keep-alive time. */
  @Override
  public @Nonnull Duration getKeepaliveTime() {
    return keepaliveTime;
  }

  /** @return Keep-alive timeout. */
  @Override
  public @Nonnull Duration getKeepaliveTimeout() {
    return keepaliveTimeout;
  }

  /** @return Whether to keep-alive even when there is no activity. */
  @Override
  public @Nonnull Boolean getKeepAliveNoActivity() {
    return keepaliveNoActivity;
  }
}
