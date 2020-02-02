package rest_mvc.java

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get


@Controller
class SampleController {
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
}
