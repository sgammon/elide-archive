package server

import com.google.common.collect.ImmutableMap
import gust.backend.AppController
import gust.backend.PageContext
import gust.backend.PageContextManager
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.views.View
import javax.inject.Inject


/**
 * Todolist homepage controller - responsible for serving the homepage, with a little preview of the app, with the
 * ability to use it (anonymously / ephemerally). It also offers the ability to sign in and persist one's tasks. The
 * homepage UI is defined in Soy, and styled in SASS.
 */
@Controller
@Secured("isAnonymous()")
class HomeController @Inject constructor (ctx: PageContextManager): AppController(ctx) {
  /**
   * `/` (`HTTP GET`): Handler for the root homepage for Todolist - i.e. `/`. Serves the preview page if the user isn't
   * logged in, or the regular app page & container if they are.
   *
   * The content for this page depends conditionally on the user's login status. If they aren't logged in, we render a
   * page with a sign-in button and a little toy copy of Todolist. The toy copy is identical to the real one, but it
   * offers no persistence of tasks beyond the current browser session.
   *
   * If the user opts to make use of the anonymous/toy version, and then later chooses to sign in, their previous tasks
   * are preserved, along with their user ID (using anonymous user upgrade via Firebase). This flow happens via the
   * client-side app, so we don't need to worry about it here. Similarly, if the user hits the homepage without being
   * logged in, and then logs in, that flow is also handled by the re-hydrated CSR frontend.
   */
  @Get("/", produces = [MediaType.TEXT_HTML])
  @View("todolist.home.page")
  fun home(@QueryValue("name", defaultValue = "World") name: String): HttpResponse<PageContext> {
    return HttpResponse.ok(PageContext.fromMap(ImmutableMap.of("name", name) as Map<String, Any>))
  }
}
