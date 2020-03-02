package gust.backend.transport;

import io.grpc.ClientInterceptor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;


/**
 * Specifies transport configuration properties specific to gRPC {@link io.grpc.ManagedChannel} objects.
 */
public interface GrpcTransportConfig extends PooledTransportConfig, GrpcTransportCredentials {
  /** Default maximum inbound message size, if no other value is specified. */
  Integer DEFAULT_MAX_INBOUND_MESSAGE_SIZE = 1;

  /** Default maximum inbound metadata size, if no other value is specified. */
  Integer DEFAULT_MAX_INBOUND_METADATA_SIZE = 1;

  /** Whether to prime managed gRPC connections by default. */
  Boolean DEFAULT_PRIME_CONNECTIONS = true;

  /**
   * @return gRPC endpoint at which to connect to the target service.
   */
  @Nonnull String endpoint();

  /**
   * @return Whether to keep-alive even when there is no activity.
   */
  @Nonnull Boolean getKeepAliveNoActivity();

  /**
   * @return Max inbound message size, in bytes.
   */
  default @Nonnull Integer maxInboundMessageSize() {
    return DEFAULT_MAX_INBOUND_MESSAGE_SIZE;
  }

  /**
   * @return Max inbound metadata stanza size, in bytes.
   */
  default @Nonnull Integer maxInboundMetadataSize() {
    return DEFAULT_MAX_INBOUND_METADATA_SIZE;
  }

  /**
   * @return Additional client-side call interceptors to install.
   */
  default @Nonnull Optional<List<ClientInterceptor>> getExtraInterceptors() {
    return Optional.empty();
  }

  /**
   * @return Whether to prime managed gRPC connections.
   */
  default @Nonnull Boolean enablePrimer() {
    return DEFAULT_PRIME_CONNECTIONS;
  }
}
