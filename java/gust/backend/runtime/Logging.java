package gust.backend.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Sugar bridge to SLF4J. */
public final class Logging {
  private Logging() { /* Disallow instantiation. */ }

  /**
   * Retrieve a logger for a particular Java class, named after the fully-qualified path to the class.
   *
   * @param cls Java class to get a logger for.
   * @return Logger for the specified class.
   */
  public static Logger logger(Class cls) { return LoggerFactory.getLogger(cls.getName());
  }
}
