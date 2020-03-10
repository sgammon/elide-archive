/*
 * Copyright © 2020, The Gust Framework Authors. All rights reserved.
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
package gust.backend.runtime;

import com.google.api.core.ApiFuture;
import com.google.api.core.ListenableFutureToApiFuture;
import com.google.api.gax.longrunning.OperationFutures;
import com.google.api.gax.longrunning.OperationSnapshot;
import com.google.api.gax.rpc.StatusCode;
import com.google.common.util.concurrent.*;
import io.reactivex.Flowable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for {@link ReactiveFuture}, which adapts Guava-based concurrency classes and ReactiveJava. */
@SuppressWarnings({"UnstableApiUsage", "DuplicatedCode", "CodeBlock2Expr"})
public final class ReactiveFutureTest {
  private static ListeningScheduledExecutorService executorService = null;
  private static ExecutorService directExecutor = MoreExecutors.newDirectExecutorService();

  @BeforeAll static void setupReactiveFutureTesting() {
    executorService = MoreExecutors.listeningDecorator(
      Executors.newScheduledThreadPool(3));
  }

  @AfterAll static void teardownReactiveFutureTesting() {
    try {
      executorService.shutdown();
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException ine) {
      throw new IllegalStateException(ine);
    }
  }

  @Test void testHasExecutorService() {
    assertNotNull(executorService, "test should have a valid executor service");
  }

  @Test void testRegularAsyncTask() throws InterruptedException, ExecutionException, TimeoutException {
    final AtomicBoolean flip = new AtomicBoolean(false);

    ListenableFuture<String> task = executorService.submit(() -> {
      flip.compareAndSet(false, true);
      return "Hello";
    });

    task.get(30, TimeUnit.SECONDS);
    assertTrue(flip.get(), "atomic boolean should have been flipped through regular task");
    assertEquals("Hello", task.get(30, TimeUnit.SECONDS),
      "returned value from async task should be expected string");
  }

  @Test void testWrapPublisherFailsWithNull() {
    assertThrows(IllegalArgumentException.class, () -> {
      //noinspection ConstantConditions
      ReactiveFuture.wrap((Publisher<?>)null);
    }, "should throw illegal argument exception when wrapping a `null` publisher");
  }

  @Test void testWrapListenableFutureFailsWithNull() {
    assertThrows(IllegalArgumentException.class, () -> {
      //noinspection ConstantConditions
      ReactiveFuture.wrap((ListenableFuture<?>)null);
    }, "should throw illegal argument exception when wrapping a `null` future");

    assertThrows(IllegalArgumentException.class, () -> {
      //noinspection ConstantConditions
      ReactiveFuture.wrap((ListenableFuture<?>)null, executorService);
    }, "should throw illegal argument exception when wrapping a `null` future");
  }

  @Test void testWrapApiFutureFailsWithNull() {
    assertThrows(IllegalArgumentException.class, () -> {
      //noinspection ConstantConditions
      ReactiveFuture.wrap((ApiFuture<?>)null);
    }, "should throw illegal argument exception when passing a `null` API future");

    assertThrows(IllegalArgumentException.class, () -> {
      //noinspection ConstantConditions
      ReactiveFuture.wrap((ApiFuture<?>)null, executorService);
    }, "should throw illegal argument exception when passing a `null` API future");
  }

  @Test void testWrapListenableFutureFailsWithNullExecutor() {
    assertThrows(IllegalArgumentException.class, () -> {
      //noinspection ConstantConditions
      ReactiveFuture.wrap((ListenableFuture<?>) Futures.immediateFuture(true), null);
    }, "should throw illegal argument exception when passing a `null` executor");
  }

  @Test void testWrapApiFutureFailsWithNullExecutor() {
    assertThrows(IllegalArgumentException.class, () -> {
      //noinspection ConstantConditions
      ReactiveFuture.wrap((ApiFuture<?>) new ListenableFutureToApiFuture<>(
        Futures.immediateFuture(true)), null);
    }, "should throw illegal argument exception when wrapping a `null` executor");
  }

  @Test void testWrapApiFuture() {
    // setup an empty API future
    final String testString = "yoooo";
    final ApiFuture<String> future = OperationFutures.immediateOperationFuture(new OperationSnapshot() {
      @Override
      public String getName() {
        return null;
      }

      @Override
      public Map<String, String> getMetadata() {
        return null;
      }

      @Override
      public boolean isDone() {
        return true;
      }

      @Override
      public String getResponse() {
        return testString;
      }

      @Override
      public StatusCode getErrorCode() {
        return null;
      }

      @Override
      public String getErrorMessage() {
        return null;
      }
    });

    ReactiveFuture wrapped = ReactiveFuture.wrap(future);
    assertNotNull(wrapped, "should get a non-null `ReactiveFuture` for #wrap(`ApiFuture`)");
    ReactiveFuture wrapped2 = ReactiveFuture.wrap(future, directExecutor);
    assertNotNull(wrapped2, "should get a non-null `ReactiveFuture` for #wrap(`ApiFuture`)");
  }

  @Test void testImmediateDone() throws InterruptedException, ExecutionException, TimeoutException {
    ReactiveFuture<Integer> done = ReactiveFuture.done(5);
    assertTrue(done.isDone(), "immediate-done future should immediately present as done");
    assertFalse(done.isCancelled(), "immediate-done future should not present as cancelled");
    assertEquals(5, done.get(1, TimeUnit.MICROSECONDS),
      "value should immediately be available and match");
  }

  @Test void testImmediateFailure() {
    String testVal = "test123123";
    Throwable x = new IllegalStateException(testVal);
    ReactiveFuture err = ReactiveFuture.failed(x);
    assertTrue(err.isDone(), "immediate-err future should immediately present as done");
    assertFalse(err.isCancelled(), "immediate-err future should not present as cancelled");
    assertThrows(ExecutionException.class, () -> {
      err.get(1, TimeUnit.MICROSECONDS);
    });
  }

  @Test void testImmediateCancelled() {
    ReactiveFuture cancelled = ReactiveFuture.cancelled();
    assertTrue(cancelled.isDone(), "immediate-cancelled future should report as done");
    assertTrue(cancelled.isCancelled(), "immediate-cancelled future should report as cancelled");
  }

  @Test void testWrappedImmediateFutureValue() throws ExecutionException, InterruptedException, TimeoutException {
    final String testVal = "hello hello test";
    ListenableFuture<String> immediateVal = ReactiveFuture.wrap(
      Futures.immediateFuture(testVal), directExecutor);
    final AtomicBoolean called = new AtomicBoolean(false);
    final ArrayList<String> values = new ArrayList<>(1);
    final ArrayList<Throwable> errs = new ArrayList<>(1);

    immediateVal.addListener(() -> {
      try {
        called.compareAndSet(false, true);
        values.add(immediateVal.get(30, TimeUnit.SECONDS));
      } catch (Throwable thr) {
        errs.add(thr);
      }
    }, directExecutor);

    assertTrue(immediateVal.isDone(), "immediate future should already be done");
    assertFalse(immediateVal.isCancelled(), "immediate future should not be cancelled");
    final String outVal = immediateVal.get(30, TimeUnit.SECONDS);
    final String outVal2 = immediateVal.get();

    assertTrue(called.get(), "listenable future callback should be called");
    assertTrue(errs.isEmpty(), "should not get an error dispatching a value through a future");
    assertEquals(1, values.size(), "array of received values should have length of exactly 1");
    assertNotNull(outVal, "should get non-null value from future.get with timeout");
    assertEquals(testVal, outVal, "value from future.get with timeout should match expected value");
    assertNotNull(outVal2, "should get non-null value from future.get with no timeout");
    assertEquals(testVal, outVal2, "value from future.get with no timeout should match expected value");
  }

  @Test void testWrappedImmediateFutureErr() {
    final String errVal = "hello hello error";
    ListenableFuture<String> immediateFail = ReactiveFuture.wrap(
      Futures.immediateFailedFuture(new IllegalStateException(errVal)), executorService);
    final AtomicBoolean called = new AtomicBoolean(false);
    final ArrayList<String> values = new ArrayList<>(1);
    final ArrayList<Throwable> errs = new ArrayList<>(1);

    immediateFail.addListener(() -> {
      try {
        called.compareAndSet(false, true);
        values.add(immediateFail.get(30, TimeUnit.SECONDS));
      } catch (Throwable thr) {
        errs.add(thr);
      }
    }, directExecutor);

    assertTrue(immediateFail.isDone(), "immediate fail future should already be done");
    assertFalse(immediateFail.isCancelled(), "immediate fail future should not be cancelled");

    boolean caught = false;
    try {
      immediateFail.get(30, TimeUnit.SECONDS);
    } catch (ExecutionException | InterruptedException | TimeoutException thr) {
      caught = true;
      assertNotNull(thr, "we should never get a null err");
    }
    assertTrue(caught, "should have caught surfaced exception from wrapped future.get with timeout");

    boolean caught2 = false;
    try {
      immediateFail.get();
    } catch (ExecutionException | InterruptedException thr) {
      caught2 = true;
      assertNotNull(thr, "we should never get a null err");
    }
    assertTrue(caught2, "should have caught surfaced exception from wrapped future.get with no timeout");

    assertTrue(called.get(), "listenable future callback should be called");
    assertFalse(errs.isEmpty(), "should get an error if one is propagated from future execution");
    assertEquals(values.size(), 0, "array of received values should have length of exactly 0");
  }

  @Test void testFutureCancellationBeforeRunning() {
    final String testValue = "yoyoyoyoyoyoy";
    Callable<String> longrunningOp = () -> {
      Thread.sleep(5 * 1000);
      return testValue;
    };

    // schedule operation, then cancel it before execution
    ListenableFuture<String> shouldNeverRun = executorService
      .schedule(longrunningOp, 30, TimeUnit.DAYS);

    final AtomicBoolean called = new AtomicBoolean(false);
    shouldNeverRun.addListener(() -> called.compareAndSet(false, true), directExecutor);

    // test initial state on future
    assertFalse(called.get(), "callback should not be dispatched until a value is ready");
    assertFalse(shouldNeverRun.isDone(), "future should initially present as not-done");
    assertFalse(shouldNeverRun.isCancelled(), "future should initially present as not-cancelled");

    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(shouldNeverRun, executorService);
    assertFalse(wrapped.isDone(), "reactive future should initially present as not-done");
    assertFalse(wrapped.isCancelled(), "reactive future should initially present as not-cancelled");

    // cancel the future directly
    boolean cancelled = shouldNeverRun.cancel(false);

    // the underlying + wrapped futures should both now appear cancelled
    assertTrue(called.get(), "callback should be called if cancelled");
    assertTrue(cancelled, "should be able to cancel a future before it runs");
    assertTrue(shouldNeverRun.isCancelled(), "future should present as cancelled after cancellation");
    assertTrue(wrapped.isCancelled(), "should be able to notice underlying cancellation through wrapper");
  }

  @Test void testFutureCancellationBeforeRunningThroughWrapper() {
    final String testValue = "yoyoyoyoyoyoy";
    Callable<String> longrunningOp = () -> {
      Thread.sleep(5 * 1000);
      return testValue;
    };

    // schedule operation, then cancel it before execution
    ListenableFuture<String> shouldNeverRun = executorService
      .schedule(longrunningOp, 30, TimeUnit.DAYS);

    final AtomicBoolean called = new AtomicBoolean(false);
    shouldNeverRun.addListener(() -> called.compareAndSet(false, true), directExecutor);

    // test initial state on future
    assertFalse(called.get(), "callback should not be dispatched until a value is ready");
    assertFalse(shouldNeverRun.isDone(), "future should initially present as not-done");
    assertFalse(shouldNeverRun.isCancelled(), "future should initially present as not-cancelled");

    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(shouldNeverRun, executorService);
    assertFalse(wrapped.isDone(), "reactive future should initially present as not-done");
    assertFalse(wrapped.isCancelled(), "reactive future should initially present as not-cancelled");

    // cancel the future through the wrapper
    boolean cancelled = wrapped.cancel(false);

    // the underlying + wrapped futures should both now appear cancelled
    assertTrue(called.get(), "callback should be called if cancelled");
    assertTrue(cancelled, "should be able to cancel a future before it runs");
    assertTrue(shouldNeverRun.isCancelled(), "future should present as cancelled after cancellation");
    assertTrue(wrapped.isCancelled(), "should be able to notice underlying cancellation through wrapper");
  }

  @Test void testFutureCancellationWhileRunning() throws InterruptedException {
    final String testValue = "yoyoyoyoyoyoy";
    AtomicBoolean running = new AtomicBoolean(false);
    Callable<String> longrunningOp = () -> {
      try {
        running.compareAndSet(false, true);
        Thread.sleep(30 * 60 * 1000);
        return testValue;
      } catch (InterruptedException ixe) {
        running.compareAndSet(true, false);
        throw ixe;
      }
    };

    // schedule operation, then cancel it before execution
    ListenableFuture<String> shouldBeRunning = executorService.submit(longrunningOp);

    final AtomicBoolean called = new AtomicBoolean(false);
    shouldBeRunning.addListener(() -> called.compareAndSet(false, true), executorService);

    if (!running.get()) {
      // wait for the executor to run the task
      int waits = 0;
      while (!running.get()) {
        Thread.sleep(100);  // sleep for 100ms
        waits++;
        if (waits > 25) {
          throw new InterruptedException("Failed to enqueue async long-running task.");
        }
      }
    }

    // test initial state on future
    assertTrue(running.get(), "executor should be running a long-running operation");
    assertFalse(called.get(), "callback should not be dispatched until a value is ready");
    assertFalse(shouldBeRunning.isDone(), "future should initially present as not-done");
    assertFalse(shouldBeRunning.isCancelled(), "future should initially present as not-cancelled");

    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(shouldBeRunning, executorService);
    assertFalse(wrapped.isDone(), "reactive future should initially present as not-done");
    assertFalse(wrapped.isCancelled(), "reactive future should initially present as not-cancelled");

    // cancel the future directly (but without interrupting - should fail)
    boolean cancelled = shouldBeRunning.cancel(true);

    Thread.sleep(100);

    // test cancellation failure
    assertFalse(running.get(), "should not be running anymore");
    assertTrue(cancelled, "should be able to cancel task");
    assertTrue(shouldBeRunning.isDone(), "future should now present as done");
    assertTrue(shouldBeRunning.isCancelled(), "future should now present as cancelled");
    assertTrue(wrapped.isDone(), "wrapped should now present as done");
    assertTrue(wrapped.isCancelled(), "wrapped should now present as cancelled");
    assertTrue(called.get(), "callback should be called if cancelled while running (without interruption)");
  }

  @Test void testWrappedPublisher() {
    final AtomicBoolean subscribed = new AtomicBoolean(false);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean completed = new AtomicBoolean(false);
    final ArrayList<String> values = new ArrayList<>(1);
    final String testValue = "Hello Hello";

    Publisher<String> pub = Flowable.just(testValue);
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(pub);

    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscribed.compareAndSet(false, true);
        s.request(1);  // request one element
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
        values.add(s);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
      }

      @Override
      public void onComplete() {
        completed.compareAndSet(false, true);
      }
    });

    assertTrue(subscribed.get(), "subscriber's `onSubscribe` should have been called");
    assertTrue(next.get(), "subscriber's `onNext` should have been called");
    assertFalse(error.get(), "subscriber's `onError` should NOT have been called");
    assertTrue(completed.get(), "subscriber's `onCompleted` should have been called");
    assertEquals(1, values.size(), "array of received publisher values should have length of exactly 1");
    assertEquals(testValue, values.get(0), "emitted value to publisher should match expected value");
  }

  @Test void testWrappedCancelPublisherAsFuture() {
    final AtomicBoolean subscribed = new AtomicBoolean(false);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean completed = new AtomicBoolean(false);
    final ArrayList<Throwable> futureErrs = new ArrayList<>(1);
    final ArrayList<Subscription> subs = (
      new ArrayList<>(1));
    final String testValue = "Hello Hello";

    Publisher<String> pub = Flowable.just(testValue);
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(pub);

    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscribed.compareAndSet(false, true);
        subs.add(s);
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
      }

      @Override
      public void onComplete() {
        completed.compareAndSet(false, true);
      }
    });

    // use it as a future
    wrapped.addListener(() -> {
      try {
        String val = wrapped.get(30, TimeUnit.SECONDS);
        String valNoTimeout = wrapped.get();
        assertEquals(val, valNoTimeout,
          "val should match whether a timeout is specified or not");

      } catch (ExecutionException | TimeoutException | InterruptedException err) {
        futureErrs.add(err);
      }
    }, directExecutor);

    // test initial state: publisher
    assertTrue(subscribed.get(), "`onSubscribe` should be called immediately after subscription");
    assertFalse(next.get(), "`onNext` should not be called until a value is ready");
    assertFalse(error.get(), "`onError` should not be called until an error is ready");
    assertFalse(error.get(), "`onCompleted` should not be called until a value is ready");

    // test initial state: future
    assertFalse(wrapped.isDone(), "future should initially present as not-done");
    assertFalse(wrapped.isCancelled(), "future should initially present as not-cancelled");

    // trigger the value by priming the subscription
    final Subscription sub = subs.get(0);
    assertEquals(subs.size(), 1, "should have exactly 1 subscription immediately after subscribing");
    sub.cancel();

    // test value state: publisher
    assertEquals(subs.size(), 1, "`onSubscribe` should not be called again after values appear");
    assertFalse(next.get(), "`onNext` should not be called if a value is never ready");
    assertFalse(error.get(), "`onError` should not be called unless an error occurs");
    assertTrue(completed.get(), "`onCompleted` should be called even if cancelled before a value is ready");

    // test value state: future
    assertFalse(wrapped.isDone(), "future should not present as done after single subscriber cancellation");
    assertFalse(wrapped.isCancelled(), "future should not be cancelled if individual subscribers cancel");
    assertTrue(futureErrs.isEmpty(), "future should encounter no error after subscriber cancellation");
  }

  @Test void testWrappedPublisherAsFuture() {
    final AtomicBoolean subscribed = new AtomicBoolean(false);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean completed = new AtomicBoolean(false);
    final ArrayList<String> values = new ArrayList<>(1);
    final ArrayList<String> futureValues = new ArrayList<>(1);
    final ArrayList<Throwable> futureErrs = new ArrayList<>(1);
    final ArrayList<Subscription> subs = new ArrayList<>(1);
    final String testValue = "Hello Hello";

    Publisher<String> pub = Flowable.just(testValue);
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(pub);

    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscribed.compareAndSet(false, true);
        subs.add(s);
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
        values.add(s);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
      }

      @Override
      public void onComplete() {
        completed.compareAndSet(false, true);
      }
    });

    // use it as a future
    wrapped.addListener(() -> {
      try {
        String val = wrapped.get(30, TimeUnit.SECONDS);
        futureValues.add(val);

        String valNoTimeout = wrapped.get();
        assertEquals(val, valNoTimeout,
          "val should match whether a timeout is specified or not");

      } catch (ExecutionException | TimeoutException | InterruptedException err) {
        futureErrs.add(err);
      }
    }, directExecutor);

    // test initial state: publisher
    assertTrue(subscribed.get(), "`onSubscribe` should be called immediately after subscription");
    assertFalse(next.get(), "`onNext` should not be called until a value is ready");
    assertFalse(error.get(), "`onError` should not be called until an error is ready");
    assertFalse(error.get(), "`onCompleted` should not be called until a value is ready");

    // test initial state: future
    assertFalse(wrapped.isDone(), "future should initially present as not-done");
    assertFalse(wrapped.isCancelled(), "future should initially present as not-cancelled");

    // trigger the value by priming the subscription
    final Subscription sub = subs.get(0);
    assertEquals(subs.size(), 1, "should have exactly 1 subscription immediately after subscribing");
    sub.request(1);

    // test value state: publisher
    assertEquals(subs.size(), 1, "`onSubscribe` should not be called again after values appear");
    assertTrue(next.get(), "`onNext` should be called when a value is ready");
    assertEquals(1, values.size(), "`onNext` should be called exactly once per value");
    assertFalse(error.get(), "`onError` should not be called unless an error occurs");
    assertTrue(completed.get(), "`onCompleted` should be called after a single value is ready");
    assertEquals(testValue, values.get(0), "publisher should receive exactly one value, the expected value");

    // test value state: future
    assertTrue(wrapped.isDone(), "future should present as done after a value is ready");
    assertFalse(wrapped.isCancelled(), "future should not present as cancelled unless instructed to do so");
    assertTrue(futureErrs.isEmpty(), "future should not encounter an error with a static value from a publisher");
    assertEquals(1, futureValues.size(), "future callback should receive exactly one call");
    assertEquals(testValue, futureValues.get(0), "future value from callback should be the expected value");
  }

  @Test void testWrappedFutureAsPublisher() throws InterruptedException, ExecutionException, TimeoutException {
    final AtomicBoolean subscribed = new AtomicBoolean(false);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean completed = new AtomicBoolean(false);
    final ArrayList<String> values = new ArrayList<>(1);
    final String testValue = "Hello Hello";

    SettableFuture<String> future = SettableFuture.create();
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(future, directExecutor);
    assertNotNull(wrapped, "should never get null for a wrapped future value");

    // subscribe to it as a publisher
    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscribed.compareAndSet(false, true);
        s.request(1);  // request one element
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
        values.add(s);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
      }

      @Override
      public void onComplete() {
        completed.compareAndSet(false, true);
      }
    });

    // trigger available value
    future.set(testValue);
    String val = future.get(30, TimeUnit.SECONDS);
    assertNotNull(val, "should get value back from wrapped future");
    assertEquals(testValue, val, "value we get back from future should be expected");
    assertTrue(subscribed.get(), "subscriber's `onSubscribe` should have been called");
    assertTrue(next.get(), "subscriber's `onNext` should have been called");
    assertFalse(error.get(), "subscriber's `onError` should NOT have been called");
    assertTrue(completed.get(), "subscriber's `onCompleted` should have been called");
    assertEquals(values.size(), 1, "array of received publisher values should have length of exactly 1");
    assertEquals(testValue, values.get(0), "emitted value to publisher should match expected value");
    assertTrue(future.isDone(), "future should still mark itself as complete");
  }

  @Test void testWrappedFutureAsPublisherRequestInvalid() {
    final AtomicBoolean subscribed = new AtomicBoolean(false);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean completed = new AtomicBoolean(false);
    final ArrayList<Throwable> errs = new ArrayList<>(1);
    final String testValue = "Hello Hello";

    SettableFuture<String> future = SettableFuture.create();
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(future, directExecutor);
    assertNotNull(wrapped, "should never get null for a wrapped future value");

    // subscribe to it as a publisher
    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscribed.compareAndSet(false, true);
        s.request(2);  // request two elements (invalid)
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
        errs.add(t);
      }

      @Override
      public void onComplete() {
        completed.compareAndSet(false, true);
      }
    });
    future.set(testValue);
    assertTrue(error.get(), "subscriber's `onError` should have been called");
    assertEquals(errs.size(), 1, "should get exactly one call to `onError`");
  }

  @Test void testDisallowPublisherMultipleValues() {
    Publisher<String> multipleItems = Flowable.just("hello", "hi");
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(multipleItems);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean complete = new AtomicBoolean(false);
    final ArrayList<Throwable> errs = new ArrayList<>();

    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        s.request(2);
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
        errs.add(t);
      }

      @Override
      public void onComplete() {
        complete.compareAndSet(false, true);
      }
    });

    assertTrue(next.get(), "`onNext` should get called the first time");
    assertTrue(error.get(), "`onError` should get called after that");
    assertEquals(1, errs.size(), "`onError` should only get called once");
    assertTrue(complete.get(), "`onComplete` should still get called");
  }

  @Test void testPublisherCancelSubscription() {
    Publisher<String> singleItem = Flowable.just("hello");
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(singleItem);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean complete = new AtomicBoolean(false);
    final ArrayList<Throwable> errs = new ArrayList<>();

    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        s.cancel();
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
        errs.add(t);
      }

      @Override
      public void onComplete() {
        complete.compareAndSet(false, true);
      }
    });

    assertFalse(next.get(), "`onNext` should not get called if cancelled before items are received");
    assertFalse(error.get(), "`onError` should not get called if cancelled before items are receive");
    assertTrue(complete.get(), "`onComplete` should still get called");
    assertEquals(0, errs.size(), "we should have zero errors at the end");
  }

  @Test void testPublisherCancelFuture() {
    Publisher<String> singleItem = Flowable.just("hello");
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(singleItem);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean complete = new AtomicBoolean(false);
    final ArrayList<Throwable> errs = new ArrayList<>();

    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        /* do nothing */
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
        errs.add(t);
      }

      @Override
      public void onComplete() {
        complete.compareAndSet(false, true);
      }
    });

    // test initial state: publisher
    assertFalse(next.get(), "`onNext` should not get called if cancelled before items are received");
    assertFalse(error.get(), "`onError` should not get called if cancelled before items are receive");
    assertEquals(0, errs.size(), "there should be 0 errors during initial state");

    // test initial state: future
    assertFalse(wrapped.isDone(), "future should not present as done until there is a terminal event");
    assertFalse(wrapped.isCancelled(), "future should not present as cancelled until there is a terminal event");

    // cancel as a future
    wrapped.cancel(true);

    // test cancelled state: publisher
    assertFalse(next.get(), "`onNext` should not get called if cancelled before items are received");
    assertFalse(error.get(), "`onError` should not get called if cancelled before items are receive");
    assertEquals(0, errs.size(), "there should be 0 errors during cancellation-before-value state");

    // test initial state: future
    assertTrue(wrapped.isDone(), "future should present as done after there is a terminal event");
    assertTrue(wrapped.isCancelled(), "future should not as cancelled after there is a terminal event");

    assertTrue(complete.get(), "`onComplete` should still get called");
  }

  @Test void testCancelPublisherAsFutureBeforeValue() {
    Publisher<String> singleItem = Flowable.just("hello");
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(singleItem);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean complete = new AtomicBoolean(false);
    final ArrayList<Throwable> errs = new ArrayList<>();

    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        /* don't request a value */
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
        errs.add(t);
      }

      @Override
      public void onComplete() {
        complete.compareAndSet(false, true);
      }
    });
    boolean cancelled = wrapped.cancel(true);

    assertTrue(cancelled, "should be able to cancel a wrapped publisher as a future");
    assertFalse(next.get(), "`onNext` should not get called if cancelled before items are received");
    assertFalse(error.get(), "`onError` should not get called if cancelled before items are receive");
    assertEquals(0, errs.size(), "we should have zero errors at the end");
    assertTrue(complete.get(), "`onComplete` should still get called");
  }

  @Test void testWrappedFutureAsPublisherErrorPropagation() {
    final AtomicBoolean subscribed = new AtomicBoolean(false);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean completed = new AtomicBoolean(false);
    final ArrayList<Throwable> errs = new ArrayList<>(1);
    final String testValue = "Hello Hello";

    SettableFuture<String> future = SettableFuture.create();
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(future, directExecutor);
    assertNotNull(wrapped, "should never get null for a wrapped future value");

    // subscribe to it as a publisher
    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscribed.compareAndSet(false, true);
        s.request(1);  // request two elements (invalid)
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
        errs.add(t);
      }

      @Override
      public void onComplete() {
        completed.compareAndSet(false, true);
      }
    });
    future.setException(new IllegalStateException(testValue));
    assertTrue(error.get(), "subscriber's `onError` should have been called");
    assertEquals(errs.size(), 1, "should get exactly one call to `onError`");
    assertEquals("ExecutionException", errs.get(0).getClass().getSimpleName(),
      "exception message wrap type should be expected value");
    assertEquals(testValue, errs.get(0).getCause().getMessage(),
      "exception message should be expected value");
    assertEquals("IllegalStateException", errs.get(0).getCause().getClass().getSimpleName(),
      "exception message type should be expected value");
  }

  @Test void testWrappedFutureAsPublisherCancel() {
    final AtomicBoolean subscribed = new AtomicBoolean(false);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean completed = new AtomicBoolean(false);
    final ArrayList<Throwable> errs = new ArrayList<>(1);
    final ArrayList<Subscription> subs = new ArrayList<>(1);

    SettableFuture<String> future = SettableFuture.create();
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(future, directExecutor);
    assertNotNull(wrapped, "should never get null for a wrapped future value");

    // subscribe to it as a publisher
    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscribed.compareAndSet(false, true);
        subs.add(s);
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
        errs.add(t);
      }

      @Override
      public void onComplete() {
        completed.compareAndSet(false, true);
      }
    });

    // should have gotten a subscription
    assertEquals(1, subs.size(), "should get a subscription after subscribing");
    Subscription sub = subs.get(0);
    sub.cancel();

    // cancelling the main subscription should cancel the future
    assertTrue(future.isDone(),
      "future should present as done after cancellation through channel subscription");
    assertTrue(future.isCancelled(),
      "future should present as cancelled after cancellation through channel subscription");
    assertTrue(wrapped.isDone(),
      "wrap should present as done after cancellation through channel subscription");
    assertTrue(wrapped.isCancelled(),
      "wrap should present as cancelled after cancellation through channel subscription");

    assertEquals(errs.size(), 0, "should get exactly zero calls to `onError`");
    assertTrue(completed.get(), "`onComplete` should still get called");
  }

  @Test void testWrappedFutureAsPublisherListenerErrorPropagation() {
    final AtomicBoolean subscribed = new AtomicBoolean(false);
    final AtomicBoolean next = new AtomicBoolean(false);
    final AtomicBoolean error = new AtomicBoolean(false);
    final AtomicBoolean completed = new AtomicBoolean(false);
    final ArrayList<Throwable> errs = new ArrayList<>(1);
    final String testValue = "Hello Hello";

    ListenerErrorFuture<String> future = new ListenerErrorFuture<>();
    ReactiveFuture<String> wrapped = ReactiveFuture.wrap(future, directExecutor);
    assertNotNull(wrapped, "should never get null for a wrapped future value");

    // subscribe to it as a publisher
    wrapped.subscribe(new Subscriber<>() {
      @Override
      public void onSubscribe(Subscription s) {
        subscribed.compareAndSet(false, true);
        s.request(1);  // request two elements (invalid)
      }

      @Override
      public void onNext(String s) {
        next.compareAndSet(false, true);
      }

      @Override
      public void onError(Throwable t) {
        error.compareAndSet(false, true);
        errs.add(t);
      }

      @Override
      public void onComplete() {
        completed.compareAndSet(false, true);
      }
    });
    future.set(testValue);
    assertTrue(error.get(), "subscriber's `onError` should have been called");
    assertEquals(errs.size(), 1, "should get exactly one call to `onError`");
    assertEquals("some error happened", errs.get(0).getMessage(),
      "exception message should be expected value");
    assertEquals("IllegalStateException", errs.get(0).getClass().getSimpleName(),
      "exception message type should be expected value");
  }

  private final static class ListenerErrorFuture<V> extends AbstractFuture<V> {
    @Override
    public boolean set(@Nullable V v) {
      return super.set(v);
    }

    @Override
    public void addListener(@Nonnull Runnable runnable, @Nonnull Executor executor) {
      throw new IllegalStateException("some error happened");
    }
  }
}
