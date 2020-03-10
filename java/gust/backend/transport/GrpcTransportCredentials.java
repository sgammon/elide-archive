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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;


/**
 * Transport-layer credentials configuration for use with managed gRPC connections. Specifies a credential provider for
 * use with auto-initialized and pooled RPC channels.
 */
public interface GrpcTransportCredentials extends TransportCredentials {
  /**
   * @return Credentials provider that should be active for managed RPC channel calls, with an empty set of scopes.
   */
  default @Nonnull Optional<CredentialsProvider> credentialsProvider() {
    return credentialsProvider(Optional.empty());
  }

  /**
   * @return Credentials provider that should be active for managed RPC channel calls.
   */
  default @Nonnull Optional<CredentialsProvider> credentialsProvider(@Nonnull Optional<List<String>> scopes) {
    return Optional.empty();
  }
}
