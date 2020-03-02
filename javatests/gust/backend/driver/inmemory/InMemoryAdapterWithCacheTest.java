package gust.backend.driver.inmemory;


import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import gust.backend.model.GenericPersistenceAdapterTest;
import gust.backend.model.ModelAdapter;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/** Tests the in-memory adapter with an in-memory cache in front of it. */
@SuppressWarnings("UnstableApiUsage")
public final class InMemoryAdapterWithCacheTest extends GenericPersistenceAdapterTest {
  private static ListeningScheduledExecutorService executorService;
  private static InMemoryAdapter<PersonKey, Person> personAdapter;
  private static InMemoryCache<PersonKey, Person> personCache;

  @BeforeAll
  static void initExecutor() {
    executorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(3));
    personCache = InMemoryCache.acquire();
    personAdapter = InMemoryAdapter.acquire(
      PersonKey.getDefaultInstance(),
      Person.getDefaultInstance(),
      Optional.of(personCache),
      executorService);
  }

  @AfterAll
  static void shutdownExecutor() throws InterruptedException {
    executorService.shutdownNow();
    executorService.awaitTermination(5, TimeUnit.SECONDS);
    executorService = null;
    personAdapter = null;
  }

  /** {@inheritDoc} */
  @Override
  protected @Nonnull
  ModelAdapter adapter() {
    return personAdapter;
  }

  /** {@inheritDoc} */
  @Override
  protected void acquireDriver() {
    assertNotNull(InMemoryAdapter.acquire(
      PersonKey.getDefaultInstance(),
      Person.getDefaultInstance(),
      Optional.of(personCache),
      executorService));
  }
}
