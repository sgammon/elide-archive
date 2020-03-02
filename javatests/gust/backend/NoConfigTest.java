package gust.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/** Test failure logic when required app configs aren't present. */
public final class NoConfigTest {
  /** Test loading the app when no required configs are present at all. This should fail. */
  @Test void testLoadAppNoConfigs() {
    assertThrows(RuntimeException.class, ApplicationBoot::load);
  }

  /** Test force-loading a specific config that does not exist. */
  @Test void testLoadConfigDoesNotExist() {
    assertThrows(RuntimeException.class, () -> {
      ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", "hi-i-also-dont-exist.yml");
    });
  }

  /** Test force-loading a specific config that does not exist, with no fallback. */
  @Test void testLoadConfigDoesNotExistNoFallback() {
    assertThrows(RuntimeException.class, () -> {
      ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", null);
    });
  }
}
