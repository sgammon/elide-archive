package gust.backend.model;

import com.google.protobuf.FieldMask;
import gust.backend.model.CacheOptions.EvictionMode;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for various options interfaces. */
public final class ModelOptionsTest {
  /** Default write disposition should remain {@code BLIND}. */
  @Test void testDefaultWriteDisposition() {
    assertFalse((new WriteOptions() {}).writeMode().isPresent(),
      "default write disposition should be empty");
  }

  /** Test each cache eviction mode for general compliance. */
  @Test void testCacheEvictionModes() {
    for (EvictionMode mode : EvictionMode.values()) {
      assertNotNull(mode.getLabel(),
        "eviction modes should expose a label");
      assertNotNull(mode.toString(),
        "mode should produce a `toString()`");
      assertTrue(mode.toString().contains(mode.name()),
        "mode description should contain the name");
      assertTrue(mode.toString().contains(mode.getLabel()),
        "mode description should contain the label");
    }
  }

  /** Test the default cache eviction mode. */
  @Test void testDefaultCacheEvictionMode() {
    Optional<EvictionMode> mode = (new CacheOptions() {}).cacheEvictionMode();
    assertNotNull(mode, "should not get `null` cache eviction mode");
    assertTrue(mode.isPresent(), "there should be a default cache eviction mode set");
    EvictionMode setting = mode.get();
    assertEquals(EvictionMode.TTL, setting, "TTL eviction setting should be enabled by default");
  }

  /** Test the default cache eviction time-to-live. It should be more than 1 minute, less than 1 day. */
  @Test void testDefaultCacheTTL() {
    Optional<Long> cacheTimeout = (new CacheOptions() {}).cacheDefaultTTL();
    TimeUnit cacheTimeUnit = (new CacheOptions() {}).cacheDefaultTTLUnit();
    assertNotNull(cacheTimeout, "should never get `null` from cache TTL setting");
    assertTrue(cacheTimeout.isPresent(), "there should be a default cache TTL set");
    assertNotNull(cacheTimeUnit, "should never get null for `cacheDefaultTTLUnit`");
    Long timeoutValue = cacheTimeout.get();
    long convertedTimeout = TimeUnit.SECONDS.convert(timeoutValue, cacheTimeUnit);
    assertTrue(convertedTimeout > 60,
      "default cache timeout should be greater than 60 seconds");
    assertTrue(convertedTimeout < 86400,
      "default cache timeout should be less than 1 day");
  }

  /** Test the default cache timeout, which governs how long the frameworks waits on the cache. */
  @Test void testDefaultCacheTimeout() {
    Optional<Long> cacheTimeout = (new CacheOptions() {}).cacheTimeout();
    TimeUnit cacheTimeoutUnit = (new CacheOptions() {}).cacheTimeoutUnit();
    assertNotNull(cacheTimeout, "should never get `null` from cache timeout setting");
    assertTrue(cacheTimeout.isPresent(), "there should be a default cache timeout set");
    assertNotNull(cacheTimeoutUnit, "should never get null for `cacheTimeoutUnit`");
    Long timeoutValue = cacheTimeout.get();
    long convertedTimeout = TimeUnit.SECONDS.convert(timeoutValue, cacheTimeoutUnit);
    assertTrue(convertedTimeout < 10,
      "default cache timeout should be less than 10 seconds");
    assertTrue(convertedTimeout > 0,
      "default cache timeout should be greater than 0 secons");
  }

  /** Test field masks when used with {@link FetchOptions}. */
  @Test void testDefaultFetchMask() {
    Optional<FieldMask> defaultMask = (new FetchOptions() {}).fieldMask();
    assertFalse(defaultMask.isPresent(), "there should be no default field mask on fetch");
    FetchOptions.MaskMode maskMode = (new FetchOptions() {}).fieldMaskMode();
    assertEquals(FetchOptions.MaskMode.INCLUDE, maskMode, "default mask mode should be INCLUDE");
  }

  /** Test that each fetch mask mode enumerated entry has a valid name and string representation. */
  @Test void testFetchMaskModeEnum() {
    for (FetchOptions.MaskMode mode : FetchOptions.MaskMode.values()) {
      assertNotNull(mode.name(), "every mask mode should have a name");
      assertTrue(mode.toString().contains(mode.name()), "mask mode representation should mention name");
    }
  }
}
