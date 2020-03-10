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
package samples.soy_ssr.src

import com.google.common.collect.ImmutableMap
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.View


/** Tests the rendering layer with Soy. */
@Controller
class SSRKotlinController {
  @View("samples.soy_ssr.pages.basic")
  @Get(produces = [MediaType.TEXT_HTML])
  fun index(): Map<String, Any> {
    return emptyMap()
  }

  @View("samples.soy_ssr.pages.salutation")
  @Get("/simple", produces = [MediaType.TEXT_HTML])
  fun simple(): Map<String, Any> {
    return ImmutableMap
      .of("name", "John Doe")
  }

  @View("samples.soy_ssr.pages.complex")
  @Get("/complex", produces = [MediaType.TEXT_HTML])
  fun complex(): Map<String, Any> {
    return ImmutableMap
      .of("context", ContextOuterClass.Context.newBuilder()
        .setSalutation(SalutationOuterClass.Salutation.newBuilder()
          .setName("Jane Doe"))
        .build())
  }
}
