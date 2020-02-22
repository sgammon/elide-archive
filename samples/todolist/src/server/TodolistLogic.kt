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
