package gust.backend.driver.inmemory;

import gust.backend.model.CacheDriver;
import gust.backend.model.GenericCacheDriverTest;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the builtin in-memory cache. */
public final class InMemoryCacheTest extends GenericCacheDriverTest {
  /** {@inheritDoc} */
  @Nonnull
  protected @Override CacheDriver cache() {
    return InMemoryCache.<PersonKey, Person>acquire();
  }

  /**
   * Implementation-specific driver acquisition test.
   */
  @Override
  protected void acquireDriver() {
    assertNotNull(InMemoryCache.<PersonKey, Person>acquire(),
      "should not get `null` when acquiring an in-memory cache driver");
  }
}
