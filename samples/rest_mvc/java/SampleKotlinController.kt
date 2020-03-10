/*
 * Copyright © 2020, The Gust Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
package samples.rest_mvc.java

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
