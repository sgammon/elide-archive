package javatests.gust;

import gust.Core;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Tests that J2CL classes can be used Java-side.
 */
public class DualStackTest {
  @Test
  public void testDualStackObject() {
    assertNotNull(
      "Core object should produce version",
      Core.getGustVersion());
    assertNotEquals(
      "Core version should not be empty",
      "",
      Core.getGustVersion());
  }

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

  @Test
  public void testDebugModeNonnull() {
    assertNotNull(
      "Debug mode provided by core should not be null",
      Core.isDebugMode());
  }

  @Test
  public void testDevModeNonnull() {
    assertNotNull(
      "Dev mode provided by core should not be null",
      Core.isDevMode());
  }

  @Test
  public void testProdModeNonnull() {
    assertNotNull(
      "Production mode provided by core should not be null",
      Core.isProductionMode());
  }
}
