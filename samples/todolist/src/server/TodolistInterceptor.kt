package server

import io.grpc.*
import javax.annotation.concurrent.Immutable
import javax.inject.Singleton


/**
 * Intercepts server-side calls to the [TasksService], and performs steps to authenticate the calls before allowing them
 * to proceed. If a given call does not meet the requirements for authentication/authorization, it is rejected with a
 * `403 Forbidden`/`PERMISSION_DENIED` status. If the call is missing authentication credentials entirely, it is
 * rejected with `403 Forbidden`/`AUTHORIZATION_REQUIRED`.
 *
 * Meeting the following requirements constitute valid authentication for use of the Tasks API:
 * - **API key.** The frontend application invoking the service call must affix an API key, which is valid and un-
 *   revoked, and passes any associated validation (for instance, referrer restrictions, for web app keys, and so on).
 *
 * - **Authorization token.** The frontend application invoking the service call must affix an `Authorization` header,
 *   which specifies a `Bearer` token containing a valid, un-expired, and un-revoked Firebase authorization JWT. To
 *   learn more about Firebase Auth, see [here](https://firebase.google.com/docs/auth). To learn more about *JSON Web
 *   Tokens*, head over to [jwt.io](https://jwt.io/).
 */
@Singleton
@Immutable
class TodolistInterceptor: ServerInterceptor {
  /**
   * Performs the interception of RPC traffic through the [TasksService], enforces authentication requirements like API
   * key presence and validity, and loads the active user through any affixed `Authorization` header. If *any* of the
   * described steps fail, the request is rejected. How it is rejected is based on the circumstances, but generally the
   * HTTP status failure code is always `403`.
   *
   * If the interceptor is able to load authentication and authorization credentials are properly validate them, it then
   * prepares the loaded values for downstream use by attaching them to the active gRPC context (see [io.grpc.Context]).
   * Keys for injected context items are exposed statically on this class, so that downstream actors may easily
   * reference them.
   *
   * @param call Server-side call being intercepted in this interceptor invocation.
   * @param metadata Metadata for the call, which roughly equates to the request's HTTP headers.
   * @param handler Tip of the handler chain for this call, which we should pass the call to, in order to continue RPC
   *        processing. This invokes the service method, any associated logic, etc., and hopefully completes the call.
   * @return Listener, which wraps the provided server [call] and [metadata], and eventually dispatches [handler] if
   *         auth details are processed and applied successfully.
   */
  override fun <Request: Any, Response: Any> interceptCall(call: ServerCall<Request, Response>,
                                                           metadata: Metadata,
                                                           handler: ServerCallHandler<Request, Response>):
                                                                                          ServerCall.Listener<Request> {
    return handler.startCall(call, metadata)
  }
}
