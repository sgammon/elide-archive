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
