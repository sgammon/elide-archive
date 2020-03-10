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
