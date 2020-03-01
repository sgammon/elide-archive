package javatests.gust.backend;

import gust.backend.ApplicationBoot;
import org.junit.Test;


/** Test failure logic when required app configs aren't present. */
public final class NoConfigTest {
  /** Test loading the app when no required configs are present at all. This should fail. */
  @Test(expected = RuntimeException.class) public void testLoadAppNoConfigs() {
    ApplicationBoot.load(false);
  }

  /** Test force-loading a specific config that does not exist. */
  @Test(expected = RuntimeException.class) public void testLoadConfigDoesNotExist() {
    ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", "hi-i-also-dont-exist.yml");
  }

  /** Test force-loading a specific config that does not exist, with no fallback. */
  @Test(expected = RuntimeException.class) public void testLoadConfigDoesNotExistNoFallback() {
    ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", null);
  }
}
