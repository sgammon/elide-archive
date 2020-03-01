package javatests.gust.backend;

import gust.backend.ApplicationBoot;
import org.junit.Test;

import java.io.IOException;


/** Test the default Micronaut application boot class. */
public final class ApplicationTest {
  /** Try loading a known-bad application yaml. */
  @Test(expected = RuntimeException.class) public void testLoadApplicationYamlError() {
    ApplicationBoot.loadConfig("app", "bad.yml", "bad.yml");
  }

  /** Try loading a known-good application yaml. */
  @Test public void testLoadApplicationYamlValid() {
    ApplicationBoot.loadConfig("app", ApplicationBoot.rootConfig, ApplicationBoot.defaultConfig);
  }

  /** Try running the config-load routine which runs on server startup. It shouldn't fail. */
  @Test public void testBasicConfigLoad() {
    ApplicationBoot.load(false);
  }

  /** Try reporting a fatal error that occurred during startup. This also shouldn't fail. */
  @Test(expected = IllegalStateException.class) public void testBasicReportErrorStderr() {
    ApplicationBoot.reportStartupError(new IOException("Something happened"));
  }

  /** Test fallback configuration loading. Should not produce an error, even though the first file doesn't exist. */
  @Test public void testLoadConfigDoesNotExist() {
    ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", ApplicationBoot.rootConfig);
  }
}
