package tests.server;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;


/**
 * Basic Java-language Micronaut controller, used for testing.
 */
@Controller("/java")
public class JavaController {
  @Get(produces = {MediaType.TEXT_PLAIN})
  public String index() {
    return "Hello from Java!";
  }
}
