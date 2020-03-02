package gust.backend.runtime;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;


/** Tests framework logging features, built atop SLF4J and Logback. */
public final class LoggingTest {
  /** Acquire a logger for a given class. */
  @Test void testAcquireLogger() {
    Logger logger = Logging.logger(LoggingTest.class);
    assertNotNull(logger, "acquired logger should never be null");
    assertEquals(LoggingTest.class.getName(), logger.getName(),
      "acquired logger should have correct name");
  }
}
