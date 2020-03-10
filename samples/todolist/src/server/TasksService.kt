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

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import io.micronaut.grpc.annotation.GrpcService
import tools.elide.samples.todolist.schema.TasksGrpc
import javax.inject.Singleton


/**
 * Defines the server-side implementation of the [TasksGrpc] service. This API service supports all backend-enabled
 * functionality related to the Todolist app, including (1) management of user tasks, (2) task attachment upload and
 * management, and (3) task lifecycle (i.e. completion, clearing, and so on).
 *
 * Although the API is designed with the frontend application's general access pattern in mind, there are a number of
 * alternate ways to consume this service (listed exhaustively below, including the general case):
 *
 * - **Direct access**. When build an implementation of a Todolist client app, the API may be accessed directly via
 *   *gRPC* or *REST*. In REST access contexts, the API supports exchanging JSON.
 *
 * - **Cross-origin access**. Currently, CORS-based access is disabled on the API, because it is mostly usable in one of
 *   the other two access methodologies listed. It may be enabled in `api.yml`.
 *
 * - **GraphQL endpoint**. Through Micronaut, there is support for Rejoiner-based GraphQL access, both for data reads
 *   and writes. This endpoint makes use of the same schemas as *Direct access* circumstances, just translated to edges
 *   in the GraphQL schema available at the endpoint.
 */
@Singleton
@GrpcService
class TasksService: TasksGrpc.TasksImplBase() {
  /**
   * Implements the `Health` service method, which simply checks the health of the Tasks API itself, and reports back
   * whether there are any known issues, or that the service is working as intended. Transient issues with database
   * connections and backend services may cause this endpoint to report bad health. This way, routing is transparent
   * around issues with deep-backend layers when operating Todolist in a multi-homed fashion.
   *
   * This method is unary, so we are expected to prepare one (and only one) response and then send it. If the service
   * is not working correctly, we yield an error and explain why (instead of a regular response).
   *
   * This method can generally be invoked via `HTTP GET` at the endpoint `/v1/health`.
   *
   * @param request Empty protocol message, indicating a request for system health.
   * @param observer Observer for responses or errors which should be relayed back to the invoking client.
   */
  override fun health(request: Empty, observer: StreamObserver<Empty>) {
    observer.onNext(Empty.getDefaultInstance())
    observer.onCompleted()
  }
}
