package gust.backend;

import org.junit.jupiter.api.Test;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


/** Test the default Micronaut application boot class. */
public final class ApplicationTest {
  /** Try loading a known-bad application yaml. */
  @Test void testLoadApplicationYamlError() {
    assertThrows(RuntimeException.class, () -> {
      ApplicationBoot.loadConfig("app", "bad.yml", "bad.yml");
    });
  }

  /** Try loading a known-good application yaml. */
  @Test void testLoadApplicationYamlValid() {
    ApplicationBoot.loadConfig("app", ApplicationBoot.rootConfig, ApplicationBoot.defaultConfig);
  }

  /** Try running the config-load routine which runs on server startup. It shouldn't fail. */
  @Test void testBasicConfigLoad() {
    ApplicationBoot.load();
  }

  /** Try reporting a fatal error that occurred during startup. This also shouldn't fail. */
  @Test void testBasicReportErrorStderr() {
    assertThrows(IllegalStateException.class, () -> {
      ApplicationBoot.reportStartupError(new IOException("Something happened"));
    });
  }

  /** Test fallback configuration loading. Should not produce an error, even though the first file doesn't exist. */
  @Test void testLoadConfigDoesNotExist() {
    ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", ApplicationBoot.rootConfig);
  }
}
