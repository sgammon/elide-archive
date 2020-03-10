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

import javax.inject.Singleton


/**
 * Defines application logic for the Todolist app. This logic is exposed via gRPC services (like [TasksService]), or,
 * alternatively, made use of directly via dependency injection in server-side controllers (i.e. [HomeController]). The
 * task lifecycle is defined and implemented within this logic, so it is safe for external callers (i.e. services or
 * controllers) to make assumptions about the data they are handed.
 *
 * In this manner, validation logic in the Todolist app is centralized here. Some aspects of the logic contained herein
 * are packaged separately to allow sharing that logic with the frontend, which must perform a subset of the tasks
 * incumbent on the server (for instance, basic validation of data before submission to the service, which is performed
 * in duplicate on the server-side).
 *
 * Methods in this implementation logic may only be executed by authenticated/authorized users. Anonymous interactions
 * with the Todolist app do not persist beyond the user's local browser memory.
 */
@Singleton
class TodolistLogic {
}
