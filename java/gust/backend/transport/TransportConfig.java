package gust.backend.transport;

import javax.annotation.Nonnull;
import java.time.Duration;


/**
 * Specifies base configuration properties generally supported by all transport configurations. This includes properties
 * like keepalive timeouts and timings, pooling settings, and so on.
 */
public interface TransportConfig {
  /**
   * @return Whether to enable keepalive features.
   */
  @Nonnull Boolean getKeepaliveEnabled();

  /**
   * @return Keep-alive time.
   */
  @Nonnull Duration getKeepaliveTime();

  /**
   * @return Keep-alive timeout.
   */
  @Nonnull Duration getKeepaliveTimeout();
}
