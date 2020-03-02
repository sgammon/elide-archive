package gust.backend.model;


import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;
import gust.backend.runtime.ReactiveFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;


/** Sets up an abstract set of tests for a cache driver. */
@SuppressWarnings({"WeakerAccess", "UnstableApiUsage", "DuplicatedCode"})
public abstract class GenericCacheDriverTest<Driver extends CacheDriver<PersonKey, Person>> {
  private static ListeningScheduledExecutorService executorService = null;

  @BeforeAll
  static void setupReactiveFutureTesting() {
    executorService = MoreExecutors.listeningDecorator(
      Executors.newScheduledThreadPool(1));
  }

  @AfterAll
  static void teardownReactiveFutureTesting() {
    try {
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException ine) {
      throw new IllegalStateException(ine);
    }
  }

  @TestFactory
  protected Iterable<DynamicTest> driverTests() {
    final String subcase = this.getClass().getSimpleName();
    List<DynamicTest> tests = Arrays.asList(
      dynamicTest(format("%s: `acquireDriver`", subcase), this::acquireDriver),
      dynamicTest(format("%s: `testFetchMiss`", subcase), this::testFetchMiss),
      dynamicTest(format("%s: `testInjectFetchHit`", subcase), this::testInjectFetchHit)
    );

    tests.addAll(subclassTests().orElse(Collections.emptyList()));
    return tests;
  }

  /** Default timeout to apply to async operations. */
  protected @Nonnull Long timeout() {
    return 30L;
  }

  /** Default time unit to apply to async operations. */
  protected @Nonnull TimeUnit timeoutUnit() {
    return TimeUnit.SECONDS;
  }

  protected @Nonnull Optional<List<DynamicTest>> subclassTests() {
    return Optional.empty();
  }

  /**
   * Driver hook-point for the generic {@link CacheDriver} suite of unit compliance tests. Checks that the cache driver
   * exposes required components, and can perform all common driver tasks.
   *
   * <p>Automatically spawns tests via {@link TestFactory} (using JUnit 5). These tests may be customized on a per-
   * driver basis by overriding individual test methods, such as {@link #acquireDriver()}. At runtime, during testing,
   * these cases are dynamically generated and run against each driver.</p>
   *
   * @return Cache driver to execute tests against.
   */
  protected abstract @Nonnull Driver cache();

  // -- Abstract Tests -- //

  /** Implementation-specific driver acquisition test. */
  protected abstract void acquireDriver();

  /** Test fetching an item from the cache, that we know isn't there. */
  protected void testFetchMiss() throws InterruptedException, TimeoutException, ExecutionException {
    ReactiveFuture<Optional<Person>> personRecord = cache().fetchCached(PersonKey.newBuilder()
      .setId("i-do-not-exist")
      .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord, "should never get `null` from cache fetch");
    assertFalse(personRecord.isCancelled(), "future should not initially be cancelled");

    var personOpt = personRecord.get(timeout(), timeoutUnit());
    assertFalse(personOpt.isPresent(), "made up record should not be present in the cache");
  }

  /** Test injecting a record, then fetching it, when we know it's there. */
  protected void testInjectFetchHit() throws InterruptedException, TimeoutException, ExecutionException {
    Person injected = Person.newBuilder()
      .setKey(PersonKey.newBuilder().setId("sample123"))
      .setName("Jane Doe")
      .build();

    var injectOp = cache().injectRecord(injected.getKey(), injected, executorService);
    assertNotNull(injectOp, "should not get `null` from cache injection");
    injectOp.get(timeout(), timeoutUnit());
    assertTrue(injectOp.isDone(), "should be able to finish injection future");

    ReactiveFuture<Optional<Person>> personRecord = cache().fetchCached(PersonKey.newBuilder()
        .setId("sample123")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord, "should never get `null` from cache fetch");
    assertFalse(personRecord.isCancelled(), "future should not initially be cancelled");

    var personOpt = personRecord.get(timeout(), timeoutUnit());
    assertTrue(personOpt.isPresent(), "made up record should not be present in the cache");
    assertEquals(injected.toString(), personOpt.get().toString(),
      "cache-resolved record should match original");
  }
}
