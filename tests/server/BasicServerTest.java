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
package tests.server;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.http.client.HttpClient;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Basic test of a Micronaut/Gust application, which starts up the server and loads an endpoint, from at least one Java
 * controller and one Kotlin controller.
 */
@MicronautTest
public class BasicServerTest {
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
  public void testApplicationContext() {
    assertNotNull("should be able to access embedded server", server);
    assertNotNull("should be able to access default client", client);
  }

  @Test
  public void testBasicJavaController() {
    assertNotNull("should have access to injected HTTP client", client);
    assertNotNull("should have access to injected HTTP server", server);
    String response = client.toBlocking().retrieve(HttpRequest.GET("/java"));
    assertEquals("response from Java controller should be expected string", "Hello from Java!", response);
  }

  @Test
  public void testBasicKotlinController() {
    assertNotNull("should have access to injected HTTP client", client);
    assertNotNull("should have access to injected HTTP server", server);
    String response = client.toBlocking().retrieve(HttpRequest.GET("/kotlin"));
    assertEquals("response from Kotlin controller should be expected string", "Hello from Kotlin!", response);
  }

  @Test
  public void testVersionBackend() {
    assertNotNull("should have access to injected HTTP client", client);
    assertNotNull("should have access to injected HTTP server", server);
    String response = client.toBlocking().retrieve(HttpRequest.GET("/version"));
    assertNotEquals("response from version output controller should not be default", "alpha", response);
  }
}
