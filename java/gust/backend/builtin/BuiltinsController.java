package gust.backend.builtin;

import gust.backend.BaseController;
import gust.backend.PageContextManager;
import gust.backend.PageRender;
import io.micronaut.context.BeanContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.validation.Validated;
import io.micronaut.views.View;
import io.micronaut.views.ViewsFilterOrderProvider;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * Specifies implementation logic which dispatches to {@link BuiltinHandler} classes from the methods on this central
 * controller. Each built-in endpoint is implemented in its own {@link InternalHandler}, but dispatched through here.
 */
@Controller
public class BuiltinsController extends BaseController {
  @Filter({"/_/site/*.xml"})
  public static class XMLFilter implements HttpServerFilter {
    public XMLFilter(@Nullable ViewsFilterOrderProvider orderProvider) {
      /* no-op */
    }

    public int getOrder() {
      return -9999;
    }

    public Publisher<MutableHttpResponse<?>> doFilter(@Nonnull HttpRequest<?> request,
                                                      @Nonnull ServerFilterChain chain) {
      return Flowable.fromPublisher(chain.proceed(request))
          .doOnNext(response -> {
            response.contentType(MediaType.APPLICATION_XML_TYPE);
            response.getHeaders().remove("Content-Security-Policy");
            response.getHeaders().remove("Content-Security-Policy-Report-Only");
          });
    }
  }

  /** Handler for site map renders. */
  private final @Nonnull SitemapHandler sitemapHandler;

  /**
   * Create a new builtins controller.
   *
   * @param sitemapHandler Site map handler.
   */
  BuiltinsController(@Nonnull SitemapHandler sitemapHandler,
                     @Nonnull PageContextManager pageContextManager) {
    super(pageContextManager);
    this.sitemapHandler = sitemapHandler;
  }

  /**
   * Main GET serving endpoint for dynamic sitemap requests, with responses issued in XML. This file can be consumed by
   * search engines (Google, Bing, Yahoo and others) to automatically crawl your site.
   *
   * <p>We accomplish this by (1) querying the {@link BeanContext} for controllers marked with the
   * {@link gust.backend.annotations.Page} annotation. After reading related annotation info, we can generate the site-
   * map dynamically.</p>
   *
   * @return Response containing a rendered sitemap. If no sitemap information can be produced, HTTP error `404` is
   *         returned instead.
   */
  @Validated
  @View("gust.builtins.sitemap.sitemapFile")
  @Get(value = "/_/site/map.xml", produces = MediaType.TEXT_HTML)
  public @Nonnull HttpResponse<PageRender> sitemap(@Nonnull HttpRequest request) {
    return this.sitemapHandler.dispatch(this.context, request);
  }
}
