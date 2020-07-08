package gust.backend.builtin;

import gust.backend.PageContextManager;
import gust.backend.PageRender;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;

import javax.annotation.Nonnull;


/**
 * Specifies the expected interface for an internal handler, which is responsible for responding to requests for a given
 * dynamic endpoint which is built-in to the framework.
 */
abstract class InternalHandler {
  /**
   * Respond to a request sent to an internal handler. Any annotation-based restrictions or modifications will be
   * applied by the time this method is dispatched.
   *
   * @param contextManager Page context manager.
   * @param request HTTP request to process with this handler.
   * @return HTTP response to send back.
   */
  public abstract @Nonnull HttpResponse<PageRender> respond(@Nonnull PageContextManager contextManager,
                                                            @Nonnull HttpRequest request);

  /**
   * Dispatch the internal handler defined by this object. Provided to the dispatcher is the HTTP request sent by the
   * client, and received by the main {@link BuiltinsController}.
   *
   * @param contextManager Page context manager.
   * @param request HTTP request to process with this handler.
   * @return Flowable which resolves to an HTTP response to send back to the requesting client.
   */
  @Nonnull HttpResponse<PageRender> dispatch(@Nonnull PageContextManager contextManager,
                                             @Nonnull HttpRequest request) {
    return respond(contextManager, request);
  }
}
