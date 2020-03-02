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
