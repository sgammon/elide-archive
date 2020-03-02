package gust.backend.transport;

import io.micronaut.aop.Introduction;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Type;

import javax.annotation.Nonnull;
import java.lang.annotation.*;


/**
 * Specifies an injection qualifier for a managed transport supporting the service specified by this annotation.
 *
 * <p>To use this annotation, decorate a {@link io.grpc.ManagedChannel} parameter with <code>@GoogleAPIChannel</code>,
 * and pass in the enumerated service you wish to receive a connection for. The connection will be initialized and kept
 * in a pool according to current settings and load.</p>
 *
 * <p>For more information about how connections are managed and integrated with Micronaut,
 * see {@link GoogleTransportManager}.</p>
 *
 * @see GoogleTransportManager
 **/
@Bean
@Documented
@Introduction
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Type(GoogleTransportManager.class)
public @interface GoogleAPIChannel {
  /**
   * Service for which this annotation is requesting a {@link io.grpc.ManagedChannel} instance.
   *
   * <p>If no connection to the requested service exists, one will be initialized before being handed back to the user.
   * Otherwise, the user may get an existing singleton connection instance, or a connection instance from a pool of
   * connections, depending on the implementing {@link TransportManager} (usually {@link GoogleTransportManager}).</p>
   *
   * @return Service to return a managed connection for.
   */
  @Nonnull GoogleService service();
}
