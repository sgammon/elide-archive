package javatests.ssr;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests basic SSR functionality.
 */
@MicronautTest
public class SSRBasicTest {
  // Private embedded server.
  private EmbeddedServer server;

  // Private embedded client.
  private HttpClient client;

  @Before
  public void setup() {
    server = ApplicationContext.run(EmbeddedServer.class).start();
    client = HttpClient.create(server.getURL());
  }

  @Test
  public void testLoadBasic() {
    assertNotNull("should be able to access embedded server", server);
    assertNotNull("should be able to access default client", client);

    final String basic = client.toBlocking().retrieve(HttpRequest.GET("/"));
    assertEquals(
      "response from SSR basic endpoint should be expected rendered Soy string",
      "<b>Hello from static Soy!</b>",
      basic);
  }

  @Test
  public void testLoadWithContext() {
    assertNotNull("should be able to access embedded server", server);
    assertNotNull("should be able to access default client", client);
    final String simple = client.toBlocking().retrieve(HttpRequest.GET("/simple"));
    assertEquals(
      "response from SSR simple endpoint should be expected rendered Soy string",
      "<b>Hello John Doe!</b>",
      simple);
  }

  @Test
  public void testLoadWithProto() {
    assertNotNull("should be able to access embedded server", server);
    assertNotNull("should be able to access default client", client);
    final String complex = client.toBlocking().retrieve(HttpRequest.GET("/complex"));
    assertEquals(
      "response from SSR complex endpoint should be expected rendered Soy string",
      "<b>Hello Jane Doe!</b><b>Hello Jane Doe!</b>",
      complex);
  }
}
