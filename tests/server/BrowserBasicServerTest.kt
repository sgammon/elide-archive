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
