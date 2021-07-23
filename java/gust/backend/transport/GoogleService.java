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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;


/** Enumerates Google Cloud services with built-in managed transport support. */
public enum GoogleService {
  /** Google Cloud Pub-Sub (token: <pre>pubsub</pre>). */
  PUBSUB("pubsub", null),

  /** Google Cloud Storage (token: <pre>storage</pre>). */
  STORAGE("storage", null),

  /** Google Cloud Firestore (token: <pre>firestore</pre>) */
  FIRESTORE("firestore", null),

  /** Google Cloud Firestore (token: <pre>spanner</pre>) */
  SPANNER("spanner", null);

  /** Prefix at which the specified service may be configured. */
  private final @Nonnull String token;

  /** Config bindings class for the provided service. */
  private final @Nullable Class<GoogleTransportConfig> configType;

  /**
   * Private constructor.
   *
   * @param token Configuration binding prefix.
   * @param configType Configuration class type.
   */
  GoogleService(@Nonnull String token,
                @Nullable Class<GoogleTransportConfig> configType) {
    this.token = token;
    this.configType = configType;
  }

  // -- Getters -- //
  /**
   * @return Prefix at which the specified service can be configured.
   */
  public @Nonnull String getToken() {
    return token;
  }

  /**
   * @return Configuration bindings class for the provided service, if supported.
   */
  public @Nonnull Optional<Class<GoogleTransportConfig>> getConfigType() {
    if (configType == null)
      return Optional.empty();
    return Optional.of(configType);
  }
}
