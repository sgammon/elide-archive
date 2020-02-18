package rest_mvc.java

import gust.Core
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get


/** Sample Micronaut app controller, written in Kotlin. */
@Controller("/kotlin")
class SampleKotlinController {
  /** Returns a simple HTML page. */
  @Get(produces = [MediaType.TEXT_HTML])
  fun index(): String {
    return """
      <!doctype html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>Hello, Gust!</title>
      </head>
      <body>
        <b>This is a minimal REST/Micronaut example with Gust.</b>
      </body>
      </html>""".trimIndent()
  }

  /** Returns the current framework version, defined server-side. */
  @Get("/version", produces = [MediaType.TEXT_PLAIN])
  fun version(): String = Core.getGustVersion()
}
