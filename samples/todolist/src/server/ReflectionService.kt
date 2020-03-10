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
package server

import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import org.slf4j.LoggerFactory
import javax.inject.Singleton


/**
 * Observes for a [ServerBuilder] to be created in the bean context, and when it is, it mounts the built-in
 * [ProtoReflectionService] so that we may use the gRPC CLI and other tools.
 */
@Singleton
class ReflectionService: BeanCreatedEventListener<ServerBuilder<*>> {
  private val logging = LoggerFactory.getLogger(ReflectionService::class.java)

  override fun onCreated(event: BeanCreatedEvent<ServerBuilder<*>>): ServerBuilder<*> {
    // skip `TasksService`/`TodolistInterceptor` - they are installed by default
    // because they are members of the bean context.
    logging.info("Mounting gRPC reflection service...")
    return event.bean.addService(ProtoReflectionService.newInstance())
  }
}
