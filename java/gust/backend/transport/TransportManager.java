package gust.backend.transport;

import io.micronaut.aop.MethodInterceptor;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;


/**
 * Defines the interface by which "transport manager" objects must comply. These objects are used to construct and
 * manage connections to other machines or systems, usually via higher-order service layers like gRPC.
 *
 * @param <A> Annotation qualifier type, which is responsible for marking types that need injection from a given
 *           implementing manager.
 * @param <E> Enumerated connection type, or service type. An instance of this enumerated type is required when
 *           acquiring a connection.
 * @param <C> Connection implementation. Should match the object that a given manager hands back when a connection is
 *           acquired for use.
 */
public interface TransportManager<A extends Annotation, E extends Enum<E>, C> extends MethodInterceptor<Object, C> {
  /** Config prefix under which transport settings are specified. */
  String ROOT_CONFIG_PREFIX = "transport";

  /**
   * Acquire a connection from this transport manager. The connection provided may or may not be freshly-created,
   * depending on the underlying implementation, but it should never be <pre>null</pre> (exceptions are raised instead).
   *
   * @param type Type of connection to acquire. Defined by the implementation.
   * @return Connection instance.
   * @throws TransportException If the connection could not be acquired.
   */
  @Nonnull C acquire(@Nonnull E type) throws TransportException;
}
