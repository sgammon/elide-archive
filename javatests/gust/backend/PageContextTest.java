package javatests.gust.backend;

import com.google.common.collect.ImmutableMap;
import gust.backend.PageContext;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;


/** Tests the {@link PageContext} class for various behavioral contracts. */
public class PageContextTest {
  /** Test that a simple map context works fine with {@link PageContext}. */
  @Test
  public void testMapContext() {
    String hiprop = "hi";
    PageContext ctx = PageContext.fromMap(Collections.singletonMap(hiprop, 5));
    assertEquals("basic context property should be fetchable",
      5, ctx.getProperties().get(hiprop));

    PageContext ctx2 = PageContext.fromMap(
      ImmutableMap.of("hi", 10, "hey", 15));
    assertEquals("properties should not blend between instances",
      10, ctx2.getProperties().get("hi"));
    assertEquals("property map should work fine with multiple entries",
      15, ctx2.getProperties().get("hey"));
  }
}
