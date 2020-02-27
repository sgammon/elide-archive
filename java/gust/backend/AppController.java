package gust.backend;


import javax.annotation.Nonnull;
import javax.inject.Inject;


/**
 * Describes a framework application controller, which is pre-loaded with convenient access to tooling, and any injected
 * application logic. Alternatively, if the invoking developer wishes to use their own base class, they can acquire most
 * or all of the enclosed functionality via injection.
 *
 * <p>Various <pre>protected</pre> properties are made available which allow easy access to stuff:</p>
 * <ul>
 *   <li><b><pre>context</pre>:</b> This property exposes a builder for the current flow's page context. This can be
 *       manipulated to provide/inject properties, and then subsequently used in Soy.</li>
 * </ul>
 *
 * <p>To make use of this controller, simply inherit from it in your own <pre>@Controller</pre>-annotated class. When a
 * request method is invoked, the logic provided by this object will have been initialized and will be ready to use.</p>
 */
public class AppController extends BaseController {
  /**
   * Private constructor, which accepts injected manager objects. Some or all of these are passed up to
   * {@link BaseController}.
   *
   * @param pageContextManager Page context manager.
   */
  @Inject public AppController(@Nonnull PageContextManager pageContextManager) {
    super(pageContextManager);
  }
}
