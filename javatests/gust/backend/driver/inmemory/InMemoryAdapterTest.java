package gust.backend.driver.inmemory;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import gust.backend.model.*;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the {@link InMemoryAdapter}. */
@SuppressWarnings("UnstableApiUsage")
public final class InMemoryAdapterTest extends GenericPersistenceAdapterTest {
  private static ListeningScheduledExecutorService executorService;
  private static InMemoryAdapter<PersonKey, Person> personAdapter;

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
    InMemoryAdapter<PersonKey, Person> personAdapter = InMemoryAdapter.acquire(
      PersonKey.getDefaultInstance(),
      Person.getDefaultInstance(),
      executorService);
    assertNotNull(personAdapter, "should not get `null` for adapter acquire");
  }
}
