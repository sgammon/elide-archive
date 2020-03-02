package gust.backend.driver.inmemory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import gust.backend.model.CacheDriver;
import gust.backend.model.FetchOptions;
import gust.backend.model.GenericPersistenceAdapterTest;
import gust.backend.model.ModelAdapter;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;
import gust.backend.runtime.ReactiveFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/** Tests the in-memory adapter with a no-op cache. */
@SuppressWarnings("UnstableApiUsage")
public final class InMemoryAdapterNoopCacheTest extends GenericPersistenceAdapterTest {
  private static ListeningScheduledExecutorService executorService;
  private static InMemoryAdapter<PersonKey, Person> personAdapter;
  private static CacheDriver<PersonKey, Person> NOOP_CACHE = new CacheDriver<>() {
    @Override
    public @Nonnull
    ReactiveFuture injectRecord(@Nonnull PersonKey personKey,
                                @Nonnull Person person,
                                @Nonnull ListeningScheduledExecutorService executor) {
      return ReactiveFuture.cancelled();
    }

    @Override
    public @Nonnull ReactiveFuture<Optional<Person>> fetchCached(@Nonnull PersonKey personKey,
                                                                 @Nonnull FetchOptions option,
                                                                 @Nonnull ListeningScheduledExecutorService executor) {
      return ReactiveFuture.wrap(Futures.immediateFuture(Optional.empty()), executorService);
    }
  };

  @BeforeAll
  static void initExecutor() {
    executorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(3));
    personAdapter = InMemoryAdapter.acquire(
      PersonKey.getDefaultInstance(),
      Person.getDefaultInstance(),
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
  protected @Nonnull ModelAdapter adapter() {
    return personAdapter;
  }

  /** {@inheritDoc} */
  @Override
  protected void acquireDriver() {
    assertNotNull(InMemoryAdapter.acquire(
      PersonKey.getDefaultInstance(),
      Person.getDefaultInstance(),
      Optional.of(NOOP_CACHE),
      executorService));
  }
}
