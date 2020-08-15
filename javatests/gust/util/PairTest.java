package gust.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/** Test the {@link Pair} utility class. */
public final class PairTest {
  @Test void testConstructPair() {
    Pair<String, Long> pair = Pair.of("Hi", 5L);
    assertNotNull(pair, "wrapped pair should never be null");
    assertEquals("Hi", pair.getKey(), "key should be expected value in pair");
    assertEquals(5L, pair.getValue(), "vaule should be expected value in pair");
  }
}
