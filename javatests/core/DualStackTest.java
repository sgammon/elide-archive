package javatests.core;

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
}
