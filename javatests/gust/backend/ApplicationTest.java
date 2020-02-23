package javatests.gust.backend;

import gust.backend.Application;
import org.junit.Test;

import java.io.IOException;


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

  /** Try running the config-load routine which runs on server startup. It shouldn't fail. */
  @Test public void testBasicConfigLoad() {
    Application.load(false);
  }

  /** Try reporting a fatal error that occurred during startup. This also shouldn't fail. */
  @Test(expected = RuntimeException.class) public void testBasicReportErrorStderr() {
    Application.reportStartupError(new IOException("Something happened"), false);
  }

  /** Test fallback configuration loading. Should not produce an error, even though the first file doesn't exist. */
  @Test public void testLoadConfigDoesNotExist() {
    Application.loadConfig("bunk", "some-nonexistent-name.yml", Application.rootConfig);
  }
}
