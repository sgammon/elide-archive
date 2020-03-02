package gust.backend.transport;


import javax.annotation.Nonnull;


/** Specifies configuration properties related to pooling of managed transport connections. */
public interface PooledTransportConfig extends TransportConfig {
  /**
   * @return Retrieve the desired connection pool size.
   */
  @Nonnull Integer getPoolSize();
}
