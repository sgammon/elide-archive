/*
 * Copyright © 2022, The Elide Framework Authors. All rights reserved.
 *
 * The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
 * are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
 * this code in object or source form requires and implies consent and agreement to that license in principle and
 * practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
 * Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
 * Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
 * by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
 * is strictly forbidden except in adherence with assigned license requirements.
 */
package elide.model;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import elide.model.PersonRecord.Person;
import elide.model.PersonRecord.PersonKey;
import elide.runtime.jvm.ReactiveFuture;
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
      dynamicTest(format("%s: `testInjectFetchHit`", subcase), this::testInjectFetchHit),
      dynamicTest(format("%s: `testInjectHitEvictMiss`", subcase), this::testInjectHitEvictMiss),
      dynamicTest(format("%s: `testMultiEvict`", subcase), this::testMultiEvict),
      dynamicTest(format("%s: `testFlush`", subcase), this::testFlush)
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
    ReactiveFuture<Optional<Person>> personRecord = cache().fetch(PersonKey.newBuilder()
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

    var injectOp = cache().put(injected.getKey(), injected, executorService);
    assertNotNull(injectOp, "should not get `null` from cache injection");
    injectOp.get(timeout(), timeoutUnit());
    assertTrue(injectOp.isDone(), "should be able to finish injection future");

    ReactiveFuture<Optional<Person>> personRecord = cache().fetch(PersonKey.newBuilder()
        .setId("sample123")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord, "should never get `null` from cache fetch");
    assertFalse(personRecord.isCancelled(), "future should not initially be cancelled");

    var personOpt = personRecord.get(timeout(), timeoutUnit());
    assertTrue(personOpt.isPresent(), "injected record should be present in the cache");
    assertEquals(injected.toString(), personOpt.get().toString(),
      "cache-resolved record should match original");
  }

  /** Test injecting a record, then fetching it, then evicting it. */
  protected void testInjectHitEvictMiss() throws InterruptedException, TimeoutException, ExecutionException {
    Person injected = Person.newBuilder()
      .setKey(PersonKey.newBuilder().setId("sample123"))
      .setName("Jane Doe")
      .build();

    var injectOp = cache().put(injected.getKey(), injected, executorService);
    assertNotNull(injectOp, "should not get `null` from cache injection");
    injectOp.get(timeout(), timeoutUnit());
    assertTrue(injectOp.isDone(), "should be able to finish injection future");

    ReactiveFuture<Optional<Person>> personRecord = cache().fetch(PersonKey.newBuilder()
        .setId("sample123")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord, "should never get `null` from cache fetch");
    assertFalse(personRecord.isCancelled(), "future should not initially be cancelled");

    var personOpt = personRecord.get(timeout(), timeoutUnit());
    assertTrue(personOpt.isPresent(), "injected record should be present in the cache");
    assertEquals(injected.toString(), personOpt.get().toString(),
      "cache-resolved record should match original");
    Optional<PersonKey> personKeyOpt = ModelMetadata.key(personOpt.get());
    assertTrue(personKeyOpt.isPresent());
    PersonKey personKey = personKeyOpt.get();
    assertNotNull(personKey, "resolved person key should not be null");

    ReactiveFuture evictOp = cache().evict(personKey, executorService);
    assertNotNull(evictOp, "evict operation should not be `null`");
    assertFalse(evictOp.isCancelled(), "evict operation should not be initially cancelled");
    evictOp.get(timeout(), timeoutUnit());
    assertTrue(evictOp.isDone(), "evict operation should be done after resolving");

    // try to re-fetch from the cache
    ReactiveFuture<Optional<Person>> personRecord2 = cache().fetch(PersonKey.newBuilder()
        .setId("sample123")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord2, "should never get `null` from cache fetch");
    assertFalse(personRecord2.isCancelled(), "future should not initially be cancelled");

    Optional<Person> personOpt2 = personRecord2.get(timeout(), timeoutUnit());
    assertNotNull(personOpt2, "should not get `null` optional from `.get` after cache fetch");
    assertFalse(personOpt2.isPresent(), "should not find model in the cache after evicting it");
  }

  /** Test evicting more than one key in a single call. */
  protected void testMultiEvict() throws InterruptedException, TimeoutException, ExecutionException {
    Person injected = Person.newBuilder()
      .setKey(PersonKey.newBuilder().setId("sample125"))
      .setName("Jane Doe")
      .build();

    Person injected2 = Person.newBuilder()
      .setKey(PersonKey.newBuilder().setId("sample126"))
      .setName("Jane Doe")
      .build();

    assertEquals("sample125", injected.getKey().getId(), "key IDs should match");
    var injectOp = cache().put(injected.getKey(), injected, executorService);
    assertNotNull(injectOp, "should not get `null` from cache injection");
    injectOp.get(timeout(), timeoutUnit());
    assertTrue(injectOp.isDone(), "should be able to finish injection future");

    assertEquals("sample126", injected2.getKey().getId(), "key IDs should match");
    var injectOp2 = cache().put(injected2.getKey(), injected2, executorService);
    assertNotNull(injectOp2, "should not get `null` from cache injection");
    injectOp2.get(timeout(), timeoutUnit());
    assertTrue(injectOp2.isDone(), "should be able to finish injection future");

    ReactiveFuture<Optional<Person>> personRecord = cache().fetch(PersonKey.newBuilder()
        .setId("sample125")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord, "should never get `null` from cache fetch");
    assertFalse(personRecord.isCancelled(), "future should not initially be cancelled");

    var personOpt = personRecord.get(timeout(), timeoutUnit());
    assertTrue(personOpt.isPresent(), "injected record should be present in the cache");
    assertEquals(injected.toString(), personOpt.get().toString(),
      "cache-resolved record should match original");
    Optional<PersonKey> personKeyOpt = ModelMetadata.key(personOpt.get());
    assertTrue(personKeyOpt.isPresent());
    PersonKey personKey = personKeyOpt.get();
    assertNotNull(personKey, "resolved person key should not be null");

    ReactiveFuture<Optional<Person>> personRecord2 = cache().fetch(PersonKey.newBuilder()
        .setId("sample126")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord2, "should never get `null` from cache fetch");
    assertFalse(personRecord2.isCancelled(), "future should not initially be cancelled");

    var personOpt2 = personRecord2.get(timeout(), timeoutUnit());
    assertTrue(personOpt2.isPresent(), "injected record should be present in the cache");
    assertEquals(injected2.toString(), personOpt2.get().toString(),
      "cache-resolved record should match original");
    Optional<PersonKey> personKeyOpt2 = ModelMetadata.key(personOpt2.get());
    assertTrue(personKeyOpt2.isPresent());
    PersonKey personKey2 = personKeyOpt2.get();
    assertNotNull(personKey2, "resolved person key should not be null");

    // evict both records in one go
    ReactiveFuture evictions = cache().evict(Arrays.asList(personKey, personKey2), executorService);
    assertNotNull(evictions, "should not get `null` from multi-key `evict`");
    assertFalse(evictions.isCancelled(), "multi-key evict should not initially be cancelled");
    evictions.get(timeout(), timeoutUnit());
    assertTrue(evictions.isDone(), "multi-key evict should be done after `get`");

    // try to re-fetch
    ReactiveFuture<Optional<Person>> personRecordFail = cache().fetch(PersonKey.newBuilder()
        .setId("sample125")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecordFail, "should never get `null` from cache fetch");
    assertFalse(personRecordFail.isCancelled(), "future should not initially be cancelled");
    var personOptFail = personRecordFail.get(timeout(), timeoutUnit());
    assertNotNull(personOptFail, "should never get `null` from cache fetch");
    assertFalse(personOptFail.isPresent(), "should not find model in cache after eviction");

    ReactiveFuture<Optional<Person>> personRecordFail2 = cache().fetch(PersonKey.newBuilder()
        .setId("sample126")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecordFail2, "should never get `null` from cache fetch");
    assertFalse(personRecordFail2.isCancelled(), "future should not initially be cancelled");
    var personOptFail2 = personRecordFail2.get(timeout(), timeoutUnit());
    assertNotNull(personOptFail2, "should never get `null` from cache fetch");
    assertFalse(personOptFail2.isPresent(), "should not find model in cache after eviction");
  }

  /** Test evicting the entire cache in a single call. */
  protected void testFlush() throws InterruptedException, TimeoutException, ExecutionException {
    Person injected = Person.newBuilder()
      .setKey(PersonKey.newBuilder().setId("sample125"))
      .setName("Jane Doe")
      .build();

    Person injected2 = Person.newBuilder()
      .setKey(PersonKey.newBuilder().setId("sample126"))
      .setName("Jane Doe")
      .build();

    assertEquals("sample125", injected.getKey().getId(), "key IDs should match");
    var injectOp = cache().put(injected.getKey(), injected, executorService);
    assertNotNull(injectOp, "should not get `null` from cache injection");
    injectOp.get(timeout(), timeoutUnit());
    assertTrue(injectOp.isDone(), "should be able to finish injection future");

    assertEquals("sample126", injected2.getKey().getId(), "key IDs should match");
    var injectOp2 = cache().put(injected2.getKey(), injected2, executorService);
    assertNotNull(injectOp2, "should not get `null` from cache injection");
    injectOp2.get(timeout(), timeoutUnit());
    assertTrue(injectOp2.isDone(), "should be able to finish injection future");

    ReactiveFuture<Optional<Person>> personRecord = cache().fetch(PersonKey.newBuilder()
        .setId("sample125")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord, "should never get `null` from cache fetch");
    assertFalse(personRecord.isCancelled(), "future should not initially be cancelled");

    var personOpt = personRecord.get(timeout(), timeoutUnit());
    assertTrue(personOpt.isPresent(), "injected record should be present in the cache");
    assertEquals(injected.toString(), personOpt.get().toString(),
      "cache-resolved record should match original");
    Optional<PersonKey> personKeyOpt = ModelMetadata.key(personOpt.get());
    assertTrue(personKeyOpt.isPresent());
    PersonKey personKey = personKeyOpt.get();
    assertNotNull(personKey, "resolved person key should not be null");

    ReactiveFuture<Optional<Person>> personRecord2 = cache().fetch(PersonKey.newBuilder()
        .setId("sample126")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecord2, "should never get `null` from cache fetch");
    assertFalse(personRecord2.isCancelled(), "future should not initially be cancelled");

    var personOpt2 = personRecord2.get(timeout(), timeoutUnit());
    assertTrue(personOpt2.isPresent(), "injected record should be present in the cache");
    assertEquals(injected2.toString(), personOpt2.get().toString(),
      "cache-resolved record should match original");
    Optional<PersonKey> personKeyOpt2 = ModelMetadata.key(personOpt2.get());
    assertTrue(personKeyOpt2.isPresent());
    PersonKey personKey2 = personKeyOpt2.get();
    assertNotNull(personKey2, "resolved person key should not be null");

    // flush the cache
    ReactiveFuture flush = cache().flush(executorService);
    assertNotNull(flush, "should not get `null` from cache flush");
    assertFalse(flush.isCancelled(), "flush should not initially be cancelled");
    flush.get(timeout(), timeoutUnit());
    assertTrue(flush.isDone(), "flush should be done after `get`");

    // try to re-fetch
    ReactiveFuture<Optional<Person>> personRecordFail = cache().fetch(PersonKey.newBuilder()
        .setId("sample127")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecordFail, "should never get `null` from cache fetch");
    assertFalse(personRecordFail.isCancelled(), "future should not initially be cancelled");
    var personOptFail = personRecordFail.get(timeout(), timeoutUnit());
    assertNotNull(personOptFail, "should never get `null` from cache fetch");
    assertFalse(personOptFail.isPresent(), "should not find model in cache after flush");

    ReactiveFuture<Optional<Person>> personRecordFail2 = cache().fetch(PersonKey.newBuilder()
        .setId("sample128")
        .build(),
      FetchOptions.DEFAULTS,
      executorService);

    assertNotNull(personRecordFail2, "should never get `null` from cache fetch");
    assertFalse(personRecordFail2.isCancelled(), "future should not initially be cancelled");
    var personOptFail2 = personRecordFail2.get(timeout(), timeoutUnit());
    assertNotNull(personOptFail2, "should never get `null` from cache fetch");
    assertFalse(personOptFail2.isPresent(), "should not find model in cache after flush");
  }
}
