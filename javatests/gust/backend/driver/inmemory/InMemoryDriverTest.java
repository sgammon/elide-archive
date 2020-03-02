package gust.backend.driver.inmemory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import gust.backend.model.*;
import gust.backend.model.GenericPersistenceDriverTest;
import gust.backend.runtime.ReactiveFuture;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the {@link InMemoryDriver}. */
@SuppressWarnings("UnstableApiUsage")
public final class InMemoryDriverTest extends GenericPersistenceDriverTest {
  private static ListeningScheduledExecutorService executorService;
  private static InMemoryDriver<PersonKey, Person> personDriver;
  private static CacheDriver<PersonKey, Person> NOOP_CACHE = new CacheDriver<>() {
    @Override
    public @Nonnull ReactiveFuture injectRecord(@Nonnull PersonKey personKey,
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
    personDriver = InMemoryAdapter
      .acquire(PersonKey.getDefaultInstance(), Person.getDefaultInstance(), executorService)
      .engine();
  }

  @AfterAll
  static void shutdownExecutor() throws InterruptedException {
    executorService.shutdownNow();
    executorService.awaitTermination(5, TimeUnit.SECONDS);
    executorService = null;
    personDriver = null;
  }

  // -- Driver Hook -- //
  @Override
  protected @Nonnull InMemoryDriver<PersonKey, Person> driver() {
    return personDriver;
  }

  // -- Tests -- //
  /** Implementation-specific driver acquisition test. */
  @Override
  protected void acquireDriver() {
    assertNotNull(InMemoryAdapter
      .acquire(PersonKey.getDefaultInstance(), Person.getDefaultInstance(), executorService)
      .engine(), "should be able to acquire a driver from an adapter");

    assertNotNull(InMemoryAdapter
      .acquire(PersonKey.getDefaultInstance(), Person.getDefaultInstance(), Optional.of(NOOP_CACHE), executorService)
      .engine(), "should be able to acquire a driver from an adapter, when providing a cache");
  }
}
