package javatests.gust;

import gust.Core;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;


/** Tests that J2CL classes can be used Java-side. */
public final class DualStackTest {
  /** Test that {@link Core} can be used as a dual-stack object. */
  @Test public void testDualStackObject() {
    assertNotNull(
      "Core object should produce version",
      Core.getGustVersion());
    assertNotEquals(
      "Core version should not be empty",
      "",
      Core.getGustVersion());
  }

  /** Test that {@link Core#getEngine()} responds as expected. */
  @Test
  public void testBackendExecutionEngine() {
    assertNotNull(
      "Engine tag should not be null",
      Core.getEngine());

    assertNotEquals(
      "Engine tag should not be empty when executing from Java",
      "",
      Core.getEngine());
  }

  /** Test that {@link Core#isDebugMode()} responds as expected. */
  @Test
  public void testDebugModeNonnull() {
    assertNotNull(
      "Debug mode provided by core should not be null",
      Core.isDebugMode());
  }

  /** Test that {@link Core#isDevMode()} responds as expected. */
  @Test
  public void testDevModeNonnull() {
    assertNotNull(
      "Dev mode provided by core should not be null",
      Core.isDevMode());
  }

  /** Test that {@link Core#isProductionMode()} responds as expected. */
  @Test
  public void testProdModeNonnull() {
    assertNotNull(
      "Production mode provided by core should not be null",
      Core.isProductionMode());
  }
}
