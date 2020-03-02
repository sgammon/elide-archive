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
