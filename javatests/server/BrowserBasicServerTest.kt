package javatests.server

import com.google.testing.web.WebTest
import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import org.junit.Test
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.openqa.selenium.WebDriver

import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


/**
 * Performs the basic DOM test in Gecko and Chromium.
 */
@MicronautTest
@RunWith(JUnit4::class)
class BrowserBasicServerTest {
  // WebDriver instance.
  private var driver: WebDriver? = null

  // Private embedded server.
  private var server: EmbeddedServer = ApplicationContext.run(EmbeddedServer::class.java)

  // Private embedded client.
  private var client: HttpClient = HttpClient.create(server.url)

  @Before
  fun createDriver() {
    driver = WebTest().newWebDriverSession()
  }

  @After
  fun quitDriver() {
    try {
      driver!!.quit()
    } finally {
      driver = null
    }
  }

  @Test
  fun testBasicControllerFromKotlin() {
    assertNotNull("should have access to injected HTTP client") { client }
    assertNotNull("should have access to injected HTTP server") { server }
    val response = client.toBlocking().retrieve(HttpRequest.GET<String>("/kotlin"))
    assertEquals(
      "Hello from Kotlin!",
      response,
      "response from Kotlin controller should be expected string")
  }

  @Test
  fun testBasicControllerHTMLFromKotlin() {
    assertNotNull("should have access to injected HTTP client") { client }
    assertNotNull("should have access to injected HTTP server") { server }
    val response = client.toBlocking().retrieve(HttpRequest.GET<String>("/kotlin/html"))
    assertEquals(
      "<html><head><title>Hello from Kotlin!</title></head><b>Hello!</b></html>",
      response,
      "HTML response from Kotlin controller should be expected string")
  }

  @Test
  fun testWebDriverSessionFromKotlin() {
    val wt = WebTest()
    assertNotNull("should have access to WebDriver") { driver }
    driver?.get(wt.HTTPAddress().resolve("/healthz").toString())
    driver?.quit()
  }

  @Test
  fun testLoadKotlinFromBrowser() {
    val result = ApplicationContext.run().start()
    val wt = WebTest()
    assertNotNull(driver) { "should have access to WebDriver" }
    assertNotNull(server) { "should have access to injected HTTP server" }
    driver?.get(wt.HTTPAddress().resolve(server.url.toString() + "/kotlin/html").toString())
    assertEquals(
      "Hello from Kotlin!",
      driver?.title,
      "HTML page title from Kotlin controller should contain expected title string")
    driver?.quit()
    result.stop()
  }

  @Test
  fun testLoadVersionFromBrowser() {
    val result = ApplicationContext.run().start()
    val wt = WebTest()
    assertNotNull(driver) { "should have access to WebDriver" }
    assertNotNull(server) { "should have access to injected HTTP server" }
    driver?.get(wt.HTTPAddress().resolve(server.url.toString() + "/version").toString())
    assertNotEquals(
      "alpha",
      driver?.pageSource,
      "page source for version endpoint should not be default string")
    driver?.quit()
    result.stop()
  }
}
