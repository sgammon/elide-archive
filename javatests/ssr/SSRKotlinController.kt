package javatests.ssr

import com.google.common.collect.ImmutableMap
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.View


/** Tests the rendering layer with Soy. */
@Controller
class SSRKotlinController {
  @View("javatests.ssr.basic")
  @Get(produces = [MediaType.TEXT_HTML])
  fun index(): Map<String, Any> {
    return emptyMap()
  }

  @View("javatests.ssr.salutation")
  @Get("/simple", produces = [MediaType.TEXT_HTML])
  fun simple(): Map<String, Any> {
    return ImmutableMap
      .of("name", "John Doe")
  }

  @View("javatests.ssr.complex")
  @Get("/complex", produces = [MediaType.TEXT_HTML])
  fun complex(): Map<String, Any> {
    return ImmutableMap
      .of("context", ContextOuterClass.Context.newBuilder()
        .setSalutation(SalutationOuterClass.Salutation.newBuilder()
          .setName("Jane Doe"))
        .build())
  }
}
