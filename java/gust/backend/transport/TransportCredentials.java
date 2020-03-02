package gust.backend.transport;

import javax.annotation.Nonnull;


/**
 * Specifies the generic notion of "transport credentials," as configuration or logic. Credentials of this nature are
 * generally used during external connection establishment via {@link TransportManager} implementations.
 */
public interface TransportCredentials {
  /**
   * @return Whether a transport requires credentials. This defaults to <code>false</code>.
   */
  default @Nonnull Boolean requiresCredentials() {
    return false;
  }
}
