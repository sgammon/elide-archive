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
import com.google.api.gax.core.GoogleCredentialsProvider;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Provides sensible defaults and additional configuration when applying transport settings specifically to Google-
 * provided or hosted services. These generally do not need to be changed.
 *
 * <p>Namely, this enforces authentication by default, and resolves Application Default Credentials. This behavior can
 * be overridden by specifying configuration properties in your <code>application.yml</code>. See
 * {@link GoogleTransportManager} for more information.</p>
 *
 * @see GoogleTransportManager for information about transport credential settings.
 */
public interface GoogleTransportConfig extends GrpcTransportConfig {
  /**
   * @return Whether a transport requires credentials. This defaults to <code>true</code> for
   *         {@link GoogleTransportConfig} and descendents.
   */
  @Override
  default @Nonnull Boolean requiresCredentials() {
    return true;
  }

  /**
   * Resolve a credentials provider bound to the specified auth requirements.
   *
   * @param scopes Authorization scopes to request.
   * @return Credentials provider that should be active for managed RPC channel calls. By default, for configurations
   *         that inherit from {@link GoogleTransportConfig}, this will read and use Application Default Credentials.
   */
  @Override
  default @Nonnull Optional<CredentialsProvider> credentialsProvider(@Nonnull Optional<List<String>> scopes) {
    return Optional.of(GoogleCredentialsProvider.newBuilder()
      .setScopesToApply(scopes.isPresent() ? scopes.get() : Collections.emptyList())
      .build());
  }
}
