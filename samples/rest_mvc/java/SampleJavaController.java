package samples.rest_mvc.java;

import gust.Core;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;


/** Sample Micronaut app controller, written in Java. */
@Controller("/java")
public class SampleJavaController {
  /** Returns a simple HTML page. */
  @Get(produces = {MediaType.TEXT_HTML})
  public String index() {
    return (
      "<!doctype html>\n" +
      "<html>\n" +
      "<head>\n" +
      "  <meta charset=\"utf-8\">\n" +
      "  <title>Sample Java Controller</title>\n" +
      "</head>\n" +
      "<body>\n" +
      "  <b>This is a minimal REST/Micronaut example with Gust.</b>\n" +
      "</body>\n" +
      "</html>\n"
    );
  }

  /** Returns the current framework version, defined server-side. */
  @Get(value = "/version", produces = {MediaType.TEXT_PLAIN})
  public String version() {
    return Core.getGustVersion();
  }
}
