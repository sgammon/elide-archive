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
package gust;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/** Tests that J2CL classes can be used Java-side. */
public final class DualStackTest {
  /** Test that {@link Core} can be used as a dual-stack object. */
  @Test void testDualStackObject() {
    assertNotNull(Core.getGustVersion(),
      "core object should produce version");
    assertNotEquals("", Core.getGustVersion(),
      "core version should not be empty");
  }

  /** Test that {@link Core#getEngine()} responds as expected. */
  @Test void testBackendExecutionEngine() {
    assertNotNull(Core.getEngine(),
      "engine tag should not be null");

    assertNotEquals("", Core.getEngine(),
      "engine tag should not be empty when executing from Java");
  }

  /** Test that {@link Core#isDebugMode()} responds as expected. */
  @Test void testDebugModeNonnull() {
    assertNotNull(Core.isDebugMode(),
      "debug mode provided by core should not be null");
  }

  /** Test that {@link Core#isDevMode()} responds as expected. */
  @Test void testDevModeNonnull() {
    assertNotNull(Core.isDevMode(),
      "dev mode provided by core should not be null");
  }

  /** Test that {@link Core#isProductionMode()} responds as expected. */
  @Test void testProdModeNonnull() {
    assertNotNull(Core.isProductionMode(),
      "production mode provided by core should not be null");
  }
}
