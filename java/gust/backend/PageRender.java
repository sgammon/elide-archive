package gust.backend;

import io.micronaut.views.soy.SoyContextMediator;
import tools.elide.page.Context;

import javax.annotation.Nonnull;


/**
 * Interface by which protobuf-driven Soy render context can be managed and orchestrated by a custom {@link PageContext}
 * object. Provides the ability to specify variables for <pre>@param</pre> and <pre>@inject</pre> Soy declarations, and
 * the ability to override objects like the {@link io.micronaut.views.soy.SoyNamingMapProvider}.
 *
 * @author Sam Gammon (sam@momentum.io)
 * @see PageContext Default implementation of this interface
 */
public interface PageRender extends SoyContextMediator {
  /** Name at which proto-context is injected. */
  String PAGE_CONTEXT_IJ_NAME = "page";

  /**
   * Retrieve serializable server-side-rendered page context, which should be assigned to the render flow bound to this
   * context mediator.
   *
   * @return Server-side rendered page context.
   */
  @Nonnull Context getPageContext();
}
