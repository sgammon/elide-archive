package javatests.server

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get


/**
 * Basic Kotlin-language Micronaut controller, used for testing.
 */
@Controller("/kotlin")
class BasicKotlinController {
  @Get(produces = [MediaType.TEXT_PLAIN])
  fun index(): String {
    return "Hello from Kotlin!"
  }

  @Get("/html", produces = [MediaType.TEXT_HTML])
  fun html(): String {
    return "<html><head><title>Hello from Kotlin!</title></head><b>Hello!</b></html>"
  }
}
