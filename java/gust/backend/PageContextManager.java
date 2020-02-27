package gust.backend;

import io.micronaut.http.HttpRequest;
import io.micronaut.runtime.http.scope.RequestScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.elide.page.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.Closeable;


/**
 * Manages the process of filling out {@link PageContext} objects before they are sealed, and delivered to Closure/Soy
 * to be reduced and rendered into content.
 *
 * <p>This object may be used from controllers via dependency injection, or used via the base controller classes provided
 * as part of the framework.</p>
 */
@RequestScope
@SuppressWarnings("unused")
public class PageContextManager implements Closeable, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(PageContextManager.class);

  /** Factory to create request-scoped {@link PageContextManager} instances. */
  @io.micronaut.context.annotation.Factory
  public final static class Factory {
    /**
     * Create a new request-bound {@link PageContextManager}. This is our chance to accept any injected values and init
     * the object with them.
     *
     * @param request Current HTTP request.
     * @return Page context manager instance.
     */
    @RequestScope
    public PageContextManager managerFactory(@Nonnull HttpRequest request) {
      return new PageContextManager(request);
    }
  }

  /** HTTP request tied to this flow. */
  private final @Nonnull HttpRequest request;

  /** Page context builder. */
  private final @Nonnull Context.Builder context;

  /** Built context: assembled when we "close" the page context manager. */
  private @Nullable Context builtContext = null;

  /** Whether we have closed context building or not. */
  private boolean closed = false;

  /**
   * Package-private constructor to restrict initialization to {@link Factory}.
   *
   * @param request Current HTTP request.
   */
  @Inject PageContextManager(@Nonnull HttpRequest request) {
    if (LOG.isDebugEnabled())
      LOG.debug(String.format("Initializing `PageContextManager` for request %s.", request.toString()));
    this.request = request;
    this.context = Context.newBuilder();
  }

  /** @return The active HTTP request for this flow. */
  @Nonnull public HttpRequest getRequest() {
    return request;
  }

  /** @return The current page context builder. */
  @Nonnull public Context.Builder getContext() {
    if (this.closed)
      throw new IllegalStateException("Cannot access mutable context after closing page manager state.");
    return context;
  }

  /** @return Built context. After calling this method the first time, context may no longer be mutated. */
  @Nonnull public Context export() {
    if (this.closed) {
      assert this.builtContext != null;
      return this.builtContext;
    } else {
      this.close();
      if (LOG.isDebugEnabled())
        LOG.debug("Exporting page context...");
      this.builtContext = this.context.build();
      assert this.builtContext != null;
      if (LOG.isDebugEnabled())
        LOG.debug(String.format("Exported page context: %s", this.builtContext));
    }
    return this.builtContext;
  }

  /**
   * Closes this stream and releases any system resources associated with it. If the stream is already closed then
   * invoking this method has no effect.
   *
   * <p>As noted in {@link AutoCloseable#close()}, cases where the close may fail require careful attention. It is
   * strongly advised to relinquish the underlying resources and to internally <em>mark</em> the {@code Closeable} as
   * closed, prior to throwing the {@code IOException}.
   */
  @Override
  public void close() {
    this.closed = true;
  }
}
