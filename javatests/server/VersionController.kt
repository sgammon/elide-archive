package javatests.server

import gust.Core
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get


/** Produces the Gust framework version as a string. */
@Controller("/version")
class VersionController {
  @Get(produces = [MediaType.TEXT_PLAIN])
  fun index(): String {
    return Core.getGustVersion()
  }
}
