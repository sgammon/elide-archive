package javatests.gust.backend;

import gust.backend.Application;
import org.junit.Test;


/** Test the default Micronaut application runner class. */
public final class ApplicationTest {
  /** Try loading a known-bad application yaml. */
  @Test(expected = RuntimeException.class) public void testLoadApplicationYamlError() {
    Application.loadConfig("app", "bad.yml", "bad.yml");
  }

  /** Try loading a known-good application yaml. */
  @Test public void testLoadApplicationYamlValid() {
    Application.loadConfig("app", Application.rootConfig, Application.defaultConfig);
  }
}
