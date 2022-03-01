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
package elide.runtime.jvm;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureToListenableFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Adapts future/async value containers from different frameworks (namely, Reactive Java, Guava, and the JDK).
 *
 * <p>Create a new {@link ReactiveFuture} by using any of the {@code wrap)} factory methods. The resulting object is
 * usable as a {@link Publisher}, {@link ListenableFuture}, or {@link ApiFuture}. This object simply wraps whatever
 * inner object is provided, and as such instances are lightweight; there is no default functionality after immediate
 * construction in most cases.</p>
 *
 * <p><b>Caveat:</b> when using a {@link Publisher} as a {@link ListenableFuture} (i.e. wrapping a {@link Publisher} and
 * then using any of the typical future methods, like {@link ListenableFuture#addListener(Runnable, Executor)}), the
 * underlying publisher may not publish more than one value. This is to prevent dropping intermediate values on the
 * floor, silently, before dispatching the future's callbacks, which generally only accept one value. Other than this,
 * things should work "as expected" whether you're looking at them from a Guava, JDK, or Reactive perspective.</p>
 *
 * @see Publisher Reactive Java type adapted by this object.
 * @see ListenableFuture Guava's extension of the JDK's basic {@link Future}, which adds listener support.
 * @see ApiFuture Lightweight Guava-like future meant to avoid dependencies on Java in API libraries.
 * @see #wrap(Publisher) To wrap a {@link Publisher}.
 * @see #wrap(ListenableFuture, Executor) To wrap a {@link ListenableFuture}.
 * @see #wrap(ApiFuture, Executor) To wrap an {@link ApiFuture}.
 */
@Immutable
@ThreadSafe
public final class ReactiveFuture<R> implements Publisher<R>, ListenableFuture<R>, ApiFuture<R> {
  /** Inner future, if one is set. Otherwise {@link Optional#empty()}. */
  private final @Nonnull Optional<ListenableFuture<R>> future;

  /** If a `publisher` is present, this object adapts it to a `future`. */
  private final @Nullable PublisherListenableFuture<R> publisherAdapter;

  /** If a `future` is present, this object adapts it to a `publisher`. */
  private final @Nullable ListenableFuturePublisher<R> futureAdapter;

  /** If a `future` is present, this object adapts it to a `publisher`. */
  private final @Nullable CompletableFuturePublisher<R> javaFutureAdapter;

  /**
   * Spawn a reactive/future adapter in a reactive context, from a {@link Publisher}. Constructing a reactive future in
   * this manner causes the object to operate in a "publisher-backed" mode.
   *
   * @param publisher Publisher to work with.
   */
  private ReactiveFuture(@Nonnull Publisher<R> publisher) {
    this.future = Optional.empty();
    this.futureAdapter = null;
    this.publisherAdapter = new PublisherListenableFuture<>(publisher);
    this.javaFutureAdapter = null;
  }

  /**
   * Spawn a reactive/future adapter in a future context, from a {@link ListenableFuture}. Constructing a reactive
   * future in this manner causes the object to operate in a "future-backed" mode.
   *
   * @param future Future to work with.
   * @param executor Executor to use when running callbacks.
   */
  private ReactiveFuture(@Nonnull ListenableFuture<R> future, @Nonnull Executor executor) {
    this.future = Optional.of(future);
    this.futureAdapter = new ListenableFuturePublisher<>(future, executor);
    this.publisherAdapter = null;
    this.javaFutureAdapter = null;
  }

  /**
   * Spawn a reactive/future adapter in a future context, from a {@link CompletableFuture}. Constructing a reactive
   * future in this manner causes the object to operate in a "future-backed" mode.
   *
   * @param future Future to work with.
   * @param executor Executor to use when running callbacks.
   */
  private ReactiveFuture(@Nonnull CompletableFuture<R> future, @Nonnull Executor executor) {
    this.future = Optional.empty();
    this.futureAdapter = null;
    this.publisherAdapter = null;
    this.javaFutureAdapter = new CompletableFuturePublisher<>(future, executor);
  }

  /** @return Internal future representation. */
  private @Nonnull ListenableFuture<R> resolveFuture() {
    if (this.publisherAdapter != null)
      return this.publisherAdapter;
    else if (this.javaFutureAdapter != null)
      return this.javaFutureAdapter;
    //noinspection OptionalGetWithoutIsPresent
    return this.future.get();
  }

  /** @return Internal publisher representation. */
  private @Nonnull Publisher<R> resolvePublisher() {
    if (this.futureAdapter != null)
      return this.futureAdapter;
    else if (this.javaFutureAdapter != null)
      return this.javaFutureAdapter;
    return Objects.requireNonNull(this.publisherAdapter);
  }

  // -- Public API -- //
  /**
   * Wrap a Reactive Java {@link Publisher} in a universal {@link ReactiveFuture}, such that it may be used with any
   * interface requiring a supported async or future value.
   *
   * <p>The resulting object is usable as any of {@link ListenableFuture}, {@link Publisher}, or {@link ApiFuture}. See
   * class docs for more information.</p>
   *
   * <p><b>Note:</b> to use a {@link Publisher} as a {@link Future} (or any descendent thereof), the {@link Publisher}
   * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and
   * accessed as a {@link Future}, to prevent silently dropping intermediate values on the floor.</p>
   *
   * @see #wrap(ListenableFuture, Executor) Wraps a {@link ListenableFuture} from Guava.
   * @param publisher Reactive publisher to wrap.
   * @param <R> Return or emission type of the publisher.
   * @return Wrapped reactive future object.
   * @throws IllegalArgumentException If the passed `publisher` is `null`.
   */
  public static @Nonnull <R> ReactiveFuture<R> wrap(@Nonnull Publisher<R> publisher) {
    //noinspection ConstantConditions
    if (publisher == null) throw new IllegalArgumentException("Cannot wrap `null` publisher.");
    return new ReactiveFuture<>(publisher);
  }

  /**
   * Wrap a regular Java {@link CompletableFuture} in a universal {@link ReactiveFuture}, such that it may be used with
   * any interface requiring support for that class.
   *
   * <p>The resulting object is usable as any of {@link ListenableFuture}, {@link Publisher}, or {@link ApiFuture}. See
   * class docs for more information.</p>
   *
   * <p><b>Note:</b> to use a {@link Publisher} as a {@link Future} (or any descendent thereof), the {@link Publisher}
   * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and
   * accessed as a {@link Future}, to prevent silently dropping intermediate values on the floor.</p>
   *
   * <p><b>Warning:</b> this method uses {@link MoreExecutors#directExecutor()} for callback execution. You should only
   * do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily
   * recommended to use the variants of {@code wrap} that accept an {@link Executor}. For instance, the corresponding
   * method to this one is {@link #wrap(ListenableFuture, Executor)}.</p>
   *
   * @param future Completable future to wrap.
   * @param <R> Return or emission type of the future.
   * @return Wrapped reactive future object.
   */
  public static @Nonnull <R> ReactiveFuture<R> wrap(@Nonnull CompletableFuture<R> future) {
    //noinspection ConstantConditions
    if (future == null) throw new IllegalArgumentException("Cannot wrap `null` publisher.");
    return wrap(future, MoreExecutors.directExecutor());
  }

  /**
   * Wrap a regular Java {@link CompletableFuture} in a universal {@link ReactiveFuture}, such that it may be used with
   * any interface requiring support for that class.
   *
   * <p>The resulting object is usable as any of {@link ListenableFuture}, {@link Publisher}, or {@link ApiFuture}. See
   * class docs for more information.</p>
   *
   * <p><b>Note:</b> to use a {@link Publisher} as a {@link Future} (or any descendent thereof), the {@link Publisher}
   * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and
   * accessed as a {@link Future}, to prevent silently dropping intermediate values on the floor.</p>
   *
   * @param future Completable future to wrap.
   * @param executor Executor to use.
   * @param <R> Return or emission type of the future.
   * @return Wrapped reactive future object.
   */
  public static @Nonnull <R> ReactiveFuture<R> wrap(@Nonnull CompletableFuture<R> future, @Nonnull Executor executor) {
    //noinspection ConstantConditions
    if (future == null) throw new IllegalArgumentException("Cannot wrap `null` future.");
    //noinspection ConstantConditions
    if (executor == null) throw new IllegalArgumentException("Cannot wrap future with `null` executor.");
    return new ReactiveFuture<>(future, executor);
  }

  /**
   * Wrap a Guava {@link ListenableFuture} in a universal {@link ReactiveFuture}, such that it may be used with any
   * interface requiring a supported async or future value.
   *
   * <p><b>Warning:</b> this method uses {@link MoreExecutors#directExecutor()} for callback execution. You should only
   * do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily
   * recommended to use the variants of {@code wrap} that accept an {@link Executor}. For instance, the corresponding
   * method to this one is {@link #wrap(ListenableFuture, Executor)}.</p>
   *
   * <p>The resulting object is usable as any of {@link ListenableFuture}, {@link Publisher}, or {@link ApiFuture}. See
   * class docs for more information.</p>
   *
   * <p><b>Note:</b> to use a {@link Publisher} as a {@link Future} (or any descendent thereof), the {@link Publisher}
   * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and
   * accessed as a {@link Future}, to prevent silently dropping intermediate values on the floor.</p>
   *
   * @see #wrap(Publisher) Wraps a Reactive Java {@link Publisher}.
   * @param future Future value to wrap.
   * @param <R> Return value type for the future.
   * @return Wrapped reactive future object.
   * @throws IllegalArgumentException If the passed `future` is `null`.
   */
  public static @Nonnull <R> ReactiveFuture<R> wrap(@Nonnull ListenableFuture<R> future) {
    return wrap(future, MoreExecutors.directExecutor());
  }

  /**
   * Wrap a Guava {@link ListenableFuture} in a universal {@link ReactiveFuture}, such that it may be used with any
   * interface requiring a supported async or future value.
   *
   * <p>The resulting object is usable as any of {@link ListenableFuture}, {@link Publisher}, or {@link ApiFuture}. See
   * class docs for more information.</p>
   *
   * <p><b>Note:</b> to use a {@link Publisher} as a {@link Future} (or any descendent thereof), the {@link Publisher}
   * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and
   * accessed as a {@link Future}, to prevent silently dropping intermediate values on the floor.</p>
   *
   * @see #wrap(Publisher) Wraps a Reactive Java {@link Publisher}.
   * @param future Future value to wrap.
   * @param executor Executor to dispatch callbacks with.
   * @param <R> Return value type for the future.
   * @return Wrapped reactive future object.
   * @throws IllegalArgumentException If the passed `future` is `null`.
   */
  public static @Nonnull <R> ReactiveFuture<R> wrap(@Nonnull ListenableFuture<R> future, @Nonnull Executor executor) {
    //noinspection ConstantConditions
    if (future == null) throw new IllegalArgumentException("Cannot wrap `null` future.");
    //noinspection ConstantConditions
    if (executor == null) throw new IllegalArgumentException("Cannot wrap future with `null` executor.");
    return new ReactiveFuture<>(future, executor);
  }

  /**
   * Wrap a Google APIs {@link ApiFuture} in a universal {@link ReactiveFuture}, such that it may be used with any
   * interface requiring a supported async or future value.
   *
   * <p>The resulting object is usable as any of {@link ListenableFuture}, {@link Publisher}, or {@link ApiFuture}. See
   * class docs for more information.</p>
   *
   * <p><b>Note:</b> to use a {@link Publisher} as a {@link Future} (or any descendent thereof), the {@link Publisher}
   * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and
   * accessed as a {@link Future}, to prevent silently dropping intermediate values on the floor.</p>
   *
   * @see #wrap(Publisher) Wraps a Reactive Java {@link Publisher}.
   * @see #wrap(ListenableFuture, Executor) Wraps a regular Guava {@link ListenableFuture}.
   * @param apiFuture API future to wrap.
   * @param executor Executor to run callbacks with.
   * @param <R> Return value type for the future.
   * @return Wrapped reactive future object.
   * @throws IllegalArgumentException If the passed `apiFuture` is `null`.
   */
  public static @Nonnull <R> ReactiveFuture<R> wrap(@Nonnull ApiFuture<R> apiFuture, @Nonnull Executor executor) {
    //noinspection ConstantConditions
    if (apiFuture == null) throw new IllegalArgumentException("Cannot wrap `null` API future.");
    return wrap(new ApiFutureToListenableFuture<>(apiFuture), executor);
  }

  /**
   * Wrap a Google APIs {@link ApiFuture} in a universal {@link ReactiveFuture}, such that it may be used with any
   * interface requiring a supported async or future value.
   *
   * <p><b>Warning:</b> this method uses {@link MoreExecutors#directExecutor()} for callback execution. You should only
   * do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily
   * recommended to use the variants of {@code wrap} that accept an {@link Executor}. For instance, the corresponding
   * method to this one is {@link #wrap(ListenableFuture, Executor)}.</p>
   *
   * <p>The resulting object is usable as any of {@link ListenableFuture}, {@link Publisher}, or {@link ApiFuture}. See
   * class docs for more information.</p>
   *
   * <p><b>Note:</b> to use a {@link Publisher} as a {@link Future} (or any descendent thereof), the {@link Publisher}
   * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and
   * accessed as a {@link Future}, to prevent silently dropping intermediate values on the floor.</p>
   *
   * @see #wrap(Publisher) Wraps a Reactive Java {@link Publisher}.
   * @see #wrap(ListenableFuture, Executor) Wraps a regular Guava {@link ListenableFuture}.
   * @param apiFuture API future to wrap.
   * @param <R> Return value type for the future.
   * @return Wrapped reactive future object.
   * @throws IllegalArgumentException If the passed `apiFuture` is `null`.
   */
  public static @Nonnull <R> ReactiveFuture<R> wrap(@Nonnull ApiFuture<R> apiFuture) {
    return wrap(apiFuture, MoreExecutors.directExecutor());
  }

  /**
   * Create an already-resolved future, wrapping the provided value. The future will present as done as soon as it is
   * returned from this method.
   *
   * <p>Under the hood, this is simply a {@link ReactiveFuture} wrapping a call to
   * {@link Futures#immediateFuture(Object)}.</p>
   *
   * @param value Value to wrap in an already-completed future.
   * @param <R> Return value generic type.
   * @return Reactive future wrapping a finished value.
   */
  public static @Nonnull <R> ReactiveFuture<R> done(@Nonnull R value) {
    return wrap(Futures.immediateFuture(value));
  }

  /**
   * Create an already-failed future, wrapping the provided exception instance. The future will present as one as soon
   * as it is returned from this method.
   *
   * <p>Calling {@link Future#get(long, TimeUnit)} or {@link Future#get()} on a failed future will surface the
   * associated exception where invocation occurs. Under the hood, this is simply a {@link ReactiveFuture} wrapping a
   * call to {@link Futures#immediateFailedFuture(Throwable)}.</p>
   *
   * @param error Error to wrap in an already-failed future.
   * @param <R> Return value generic type.
   * @return Reactive future wrapping a finished value.
   */
  public static @Nonnull <R> ReactiveFuture<R> failed(@Nonnull Throwable error) {
    return wrap(Futures.immediateFailedFuture(error));
  }

  /**
   * Create an already-cancelled future. The future will present as both done and cancelled as soon as it is returned
   * from this method.
   *
   * <p>Under the hood, this is simply a {@link ReactiveFuture} wrapping a call to
   * {@link Futures#immediateCancelledFuture()}.</p>
   *
   * @param <R> Return value generic type.
   * @return Reactive future wrapping a cancelled operation.
   */
  public static @Nonnull <R> ReactiveFuture<R> cancelled() {
    return wrap(Futures.immediateCancelledFuture());
  }

  // -- Compliance: Publisher -- //
  /**
   * Request {@link Publisher} to start streaming data.
   *
   * <p>This is a "factory method" and can be called multiple times, each time starting a new {@link Subscription}. Each
   * {@link Subscription} will work for only a single {@link Subscriber}. A {@link Subscriber} should only subscribe
   * once to a single {@link Publisher}. If the {@link Publisher} rejects the subscription attempt or otherwise fails it
   * will signal the error via {@link Subscriber#onError}.</p>
   *
   * @param subscriber the {@link Subscriber} that will consume signals from this {@link Publisher}.
   */
  @Override
  public void subscribe(Subscriber<? super R> subscriber) {
    resolvePublisher().subscribe(subscriber);
  }

  // -- Compliance: Listenable Future -- //
  /**
   * Registers a listener to be {@linkplain Executor#execute(Runnable) run} on the given executor. The listener will run
   * when the {@code Future}'s computation is {@linkplain Future#isDone() complete} or, if the computation is already
   * complete, immediately.
   *
   * <p>There is no guaranteed ordering of execution of listeners, but any listener added through this method is
   * guaranteed to be called once the computation is complete.</p>
   *
   * <p>Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown during
   * {@code Executor.execute} (e.g., a {@code RejectedExecutionException} or an exception thrown by
   * {@linkplain MoreExecutors#directExecutor direct execution}) will be caught and logged.</p>
   *
   * <p>Note: For fast, lightweight listeners that would be safe to execute in any thread, consider
   * {@link MoreExecutors#directExecutor}. Otherwise, avoid it. Heavyweight {@code directExecutor} listeners can cause
   * problems, and these problems can be difficult to reproduce because they depend on timing. For example:</p>
   * <ul>
   *   <li>The listener may be executed by the caller of {@code addListener}. That caller may be a
   *       UI thread or other latency-sensitive thread. This can harm UI responsiveness.
   *   <li>The listener may be executed by the thread that completes this {@code Future}. That
   *       thread may be an internal system thread such as an RPC network thread. Blocking that
   *       thread may stall progress of the whole system. It may even cause a deadlock.
   *   <li>The listener may delay other listeners, even listeners that are not themselves {@code
   *       directExecutor} listeners.
   * </ul>
   *
   * <p>This is the most general listener interface. For common operations performed using listeners, see
   * {@link Futures}. For a simplified but general listener interface, see
   * {@link Futures#addCallback addCallback()}.</p>
   *
   * <p>Memory consistency effects: Actions in a thread prior to adding a listener <a
   * href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5"><i>happen-before</i></a> its
   * execution begins, perhaps in another thread.</p>
   *
   * <p>Guava implementations of {@code ListenableFuture} promptly release references to listeners after executing
   * them.</p>
   *
   * @param listener the listener to run when the computation is complete.
   * @param executor the executor to run the listener in
   * @throws RejectedExecutionException if we tried to execute the listener immediately but the executor rejecte it.
   */
  @Override
  public void addListener(@Nonnull Runnable listener, @Nonnull Executor executor) throws RejectedExecutionException {
    resolveFuture().addListener(listener, executor);
  }

  /**
   * Attempts to cancel execution of this task.  This attempt will fail if the task has already completed, has already
   * been cancelled, or could not be cancelled for some other reason. If successful, and this task has not started when
   * {@code cancel} is called, this task should never run.  If the task has already started, then the
   * {@code mayInterruptIfRunning} parameter determines whether the thread executing this task should be interrupted in
   * an attempt to stop the task.
   *
   * <p>After this method returns, subsequent calls to {@link #isDone} will always return {@code true}.  Subsequent
   * calls to {@link #isCancelled} will always return {@code true} if this method returned {@code true}.
   *
   * @param mayInterruptIfRunning {@code true} if the thread executing this task should be interrupted; otherwise,
   *                              in-progress tasks are allowed to complete
   * @return {@code false} if the task could not be cancelled, typically because it has already completed normally;
   *         {@code true} otherwise.
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return resolveFuture().cancel(mayInterruptIfRunning);
  }

  /**
   * Returns {@code true} if this task was cancelled before it completed normally. This defers to the underlying future,
   * or a wrapped object if using a {@link Publisher}.
   *
   * @return {@code true} if this task was cancelled before it completed
   */
  @Override
  public boolean isCancelled() {
    return resolveFuture().isCancelled();
  }

  /**
   * Returns {@code true} if this task completed. This defers to the underlying future, or a wrapped object if using a
   * Reactive Java {@link Publisher}.
   *
   * Completion may be due to normal termination, an exception, or cancellation -- in all of these cases, this method
   * will return {@code true}.
   *
   * @return {@code true} if this task completed.
   */
  @Override
  public boolean isDone() {
    return resolveFuture().isDone();
  }

  /**
   * Waits if necessary for the computation to complete, and then retrieves its result.
   *
   * <p>It is generally recommended to use the variant of this method which specifies a timeout - one must handle the
   * additional {@link TimeoutException}, but on the other hand the computation can never infinitely block if an async
   * value does not materialize.</p>
   *
   * @see #get(long, TimeUnit) For a safer version of this method, which allows specifying a timeout.
   * @return the computed result.
   * @throws CancellationException if the computation was cancelled
   * @throws ExecutionException    if the computation threw an exception
   * @throws InterruptedException  if the current thread was interrupted while waiting
   */
  @Override
  public R get() throws InterruptedException, ExecutionException {
    return resolveFuture().get();
  }

  /**
   * Waits if necessary for at most the given time for the computation to complete, and then retrieves its result, if
   * available.
   *
   * @param timeout the maximum time to wait
   * @param unit    the time unit of the timeout argument
   * @return the computed result
   * @throws CancellationException if the computation was cancelled
   * @throws ExecutionException    if the computation threw an exception
   * @throws InterruptedException  if the current thread was interrupted while waiting
   * @throws TimeoutException      if the wait timed out
   */
  @Override
  public R get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return resolveFuture().get(timeout, unit);
  }

  /**
   * Structure that adapts a {@link Publisher} to a {@link ListenableFuture} interface. We accomplish this by
   * immediately subscribing to the publisher with a callback that dispatches a {@link SettableFuture}.
   *
   * <p>This object is used in the specific circumstance of wrapping a {@link Publisher}, and then using the wrapped
   * object as a {@link ListenableFuture} (or any descendent or compliant implementation thereof).</p>
   *
   * @param <T> Generic type returned by the future.
   */
  @Immutable
  @ThreadSafe
  public final static class PublisherListenableFuture<T> implements ListenableFuture<T>, Publisher<T> {
    /** Whether we have received a value. */
    private final @Nonnull AtomicBoolean received = new AtomicBoolean(false);

    /** Whether we have completed acquiring a value. */
    private final @Nonnull AtomicBoolean completed = new AtomicBoolean(false);

    /** Whether we have been cancelled. */
    private final @Nonnull AtomicBoolean cancelled = new AtomicBoolean(false);

    /** Describes the list of proxied subscribers. */
    private final @Nonnull Map<String, Subscriber<? super T>> subscribers = new ConcurrentHashMap<>();

    /** Converted/pass-through future value. */
    private final @Nonnull SettableFuture<T> future;

    /** Subscription, so we can propagate cancellation. */
    private volatile Subscription subscription;

    /**
     * Private constructor.
     *
     * @param publisher Publisher to wrap.
     */
    private PublisherListenableFuture(@Nonnull Publisher<T> publisher) {
      this.future = SettableFuture.create();
      publisher.subscribe(new Subscriber<T>() {
        @Override
        public void onSubscribe(Subscription s) {
          PublisherListenableFuture.this.subscription = s;
        }

        @Override
        public void onNext(T t) {
          if (received.compareAndSet(false, true)) {
            PublisherListenableFuture.this.proxyExecute((sub) -> sub.onNext(t));
            future.set(t);
            return;
          }
          this.onError(new IllegalStateException(
            "Cannot publish multiple items through `ReactiveFuture`."));
        }

        @Override
        public void onError(Throwable t) {
          if (!completed.get()) {
            PublisherListenableFuture.this.proxyExecute((sub) -> sub.onError(t));
            future.setException(t);
          }
        }

        @Override
        public void onComplete() {
          if (completed.compareAndSet(false, true)) {
            PublisherListenableFuture.this.proxyExecute(Subscriber::onComplete);
            PublisherListenableFuture.this.clear();
          }
        }
      });
    }

    /**
     * Call something on each proxied publisher subscription, if any.
     *
     * @param operation Operation to execute. Called for each subscriber.
     */
    private void proxyExecute(@Nonnull Consumer<Subscriber<? super T>> operation) {
      if (!this.subscribers.isEmpty()) {
        this.subscribers.values().forEach(operation);
      }
    }

    /**
     * Remove all subscribers and clear references to futures/publishers/listeners.
     */
    private void clear() {
      this.subscribers.clear();
      this.subscription = null;
    }

    /**
     * Drop a subscription (after proxied {@link Subscription#cancel()} is called).
     *
     * @param id ID of the subscription to drop.
     */
    private void dropSubscription(@Nonnull String id) {
      this.subscribers.get(id).onComplete();
      this.subscribers.remove(id);
    }

    // -- Interface Compliance: Publisher -- //

    @Override
    public void subscribe(Subscriber<? super T> s) {
      final String id = String.valueOf(this.subscribers.size());
      Subscription sub = new Subscription() {
        @Override
        public void request(long n) {
          PublisherListenableFuture.this.subscription.request(n);
        }

        @Override
        public void cancel() {
          // kill self
          PublisherListenableFuture.this.dropSubscription(id);
        }
      };
      this.subscribers.put(id, s);
      s.onSubscribe(sub);
    }

    // -- Interface Compliance: Listenable Future -- //

    @Override
    public void addListener(@Nonnull Runnable runnable, @Nonnull Executor executor) {
      this.future.addListener(runnable, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      boolean cancelled = false;
      if (!this.completed.get() && this.cancelled.compareAndSet(false, true)) {
        this.proxyExecute(Subscriber::onComplete);  // dispatch `onComplete` for any subscribers
        this.subscription.cancel();  // cancel upwards
        cancelled = this.future.cancel(mayInterruptIfRunning);  // cancel future
        this.clear();  // clear references
      }
      return cancelled;
    }

    @Override
    public boolean isCancelled() {
      return this.cancelled.get();
    }

    @Override
    public boolean isDone() {
      return this.completed.get() || this.cancelled.get();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      return this.future.get();
    }

    @Override
    public T get(long timeout, @Nonnull TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return this.future.get(timeout, unit);
    }
  }

  /**
   * Structure that adapts Java's {@link CompletableFuture} to a Reactive Java {@link Publisher}, which publishes one
   * item - either the result of the computation, or an error.
   *
   * <p>This object is used in the specific circumstance that a {@link CompletableFuture} is wrapped by a
   * {@link ReactiveFuture}, and then used within the Reactive Java or Guava ecosystems as a {@link Publisher} or a
   * {@link ListenableFuture} (or {@link ApiFuture}), or a descendent thereof. As in {@link ListenableFuturePublisher},
   * we simply set the callback for the future value, upon item-request (one cycle is allowed), and propagate any events
   * received to the publisher.</p>
   *
   * @param <T> Emit type for this adapter. Matches the future it wraps.
   */
  public final static class CompletableFuturePublisher<T>
      implements Publisher<T>, ListenableFuture<T>, CompletionStage<T> {
    private final @Nonnull CompletableFuture<T> future;
    private final @Nonnull CompletionStage<T> stage;
    private final @Nonnull Executor callbackExecutor;

    /**
     * Construct an adapter that propagates signals from a {@link CompletableFuture} to a {@link Publisher}.
     *
     * @param future Completable future to wrap.
     * @param callbackExecutor Callback executor to use.
     */
    private CompletableFuturePublisher(@Nonnull CompletableFuture<T> future,
                                       @Nonnull Executor callbackExecutor) {
      this.future = future;
      this.stage = future;
      this.callbackExecutor = callbackExecutor;
    }

    /* == `Future`/`ListenableFuture` Interface Compliance == */

    /** @inheritDoc */
    @Override public final void subscribe(Subscriber<? super T> subscriber) {
      Objects.requireNonNull(subscriber, "Subscriber cannot be null");
      subscriber.onSubscribe(new CompletableFutureSubscription(this.future, subscriber, this.callbackExecutor));
    }

    /** @inheritDoc */
    @Override public void addListener(Runnable runnable, Executor executor) {
      future.thenRunAsync(runnable, executor);
    }

    /** @inheritDoc */
    @Override public boolean cancel(boolean mayInterruptIfRunning) {
      return future.cancel(mayInterruptIfRunning);
    }

    /** @inheritDoc */
    @Override public boolean isCancelled() {
      return future.isCancelled();
    }

    /** @inheritDoc */
    @Override public boolean isDone() {
      return future.isDone();
    }

    /** @inheritDoc */
    @Override public T get() throws InterruptedException, ExecutionException {
      return future.get();
    }

    /** @inheritDoc */
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return future.get(timeout, unit);
    }

    /* == `CompletionStage` Interface Compliance == */

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
      return stage.thenApply(fn);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
      return stage.thenApplyAsync(fn);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
      return stage.thenApplyAsync(fn, executor);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
      return stage.thenAccept(action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
      return stage.thenAcceptAsync(action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
      return stage.thenAcceptAsync(action, executor);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> thenRun(Runnable action) {
      return stage.thenRun(action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> thenRunAsync(Runnable action) {
      return stage.thenRunAsync(action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
      return stage.thenRunAsync(action, executor);
    }

    /** @inheritDoc */
    @Override public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
                                                           BiFunction<? super T, ? super U, ? extends V> fn) {
      return stage.thenCombine(other, fn);
    }

    /** @inheritDoc */
    @Override public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                                BiFunction<? super T, ? super U, ? extends V> fn) {
      return stage.thenCombineAsync(other, fn);
    }

    /** @inheritDoc */
    @Override public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                                BiFunction<? super T, ? super U, ? extends V> fn,
                                                                Executor executor) {
      return stage.thenCombineAsync(other, fn, executor);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                                                              BiConsumer<? super T, ? super U> action) {
      return stage.thenAcceptBoth(other, action);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                                   BiConsumer<? super T, ? super U> action) {
      return stage.thenAcceptBothAsync(other, action);
    }

    /** @inheritDoc */
    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                         BiConsumer<? super T, ? super U> action,
                                                         Executor executor) {
      return stage.thenAcceptBothAsync(other, action, executor);
    }

    /** @inheritDoc */
    @Override
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
      return stage.runAfterBoth(other, action);
    }

    /** @inheritDoc */
    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
      return stage.runAfterBothAsync(other, action);
    }

    /** @inheritDoc */
    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
      return stage.runAfterBothAsync(other, action, executor);
    }

    /** @inheritDoc */
    @Override
    public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
      return stage.applyToEither(other, fn);
    }

    /** @inheritDoc */
    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
      return stage.applyToEitherAsync(other, fn);
    }

    /** @inheritDoc */
    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                                     Function<? super T, U> fn,
                                                     Executor executor) {
      return stage.applyToEitherAsync(other, fn, executor);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other,
                                                        Consumer<? super T> action) {
      return stage.acceptEither(other, action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                                             Consumer<? super T> action) {
      return stage.acceptEitherAsync(other, action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                                             Consumer<? super T> action,
                                                             Executor executor) {
      return stage.acceptEitherAsync(other, action, executor);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
      return stage.runAfterEither(other, action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
      return stage.runAfterEitherAsync(other, action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
      return stage.runAfterEitherAsync(other, action, executor);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
      return stage.thenCompose(fn);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
      return stage.thenComposeAsync(fn);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
                                                             Executor executor) {
      return stage.thenComposeAsync(fn, executor);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
      return stage.handle(fn);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
      return stage.handleAsync(fn);
    }

    /** @inheritDoc */
    @Override public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
                                                        Executor executor) {
      return stage.handleAsync(fn, executor);
    }

    /** @inheritDoc */
    @Override public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
      return stage.whenComplete(action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
      return stage.whenCompleteAsync(action);
    }

    /** @inheritDoc */
    @Override public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
                                                          Executor executor) {
      return stage.whenCompleteAsync(action, executor);
    }

    /** @inheritDoc */
    @Override public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
      return stage.exceptionally(fn);
    }

    /** @inheritDoc */
    @Override public CompletableFuture<T> toCompletableFuture() {
      return stage.toCompletableFuture();
    }

    /**
     * Models a Reactive Java {@link Subscription}, which is responsible for propagating events from a
     * Concurrent Java {@link CompletableFuture} to a {@link Subscriber}.
     *
     * <p>This object is generally used internally by the {@link CompletableFuturePublisher}, once a {@link Subscriber}
     * attaches itself to a {@link Publisher} that is actually a wrapped {@link CompletableFuture}. Error (exception)
     * events and value events are both propagated. Subscribers based on this wrapping will only ever receive a maximum
     * of <b>one value</b> or <b>one error</b>.</p>
     */
    @Immutable
    @ThreadSafe
    public final class CompletableFutureSubscription implements Subscription {
      private final AtomicBoolean completed = new AtomicBoolean(false);
      private final @Nonnull Subscriber<? super T> subscriber;
      private final @Nonnull CompletableFuture<T> future;
      private final @Nonnull Executor executor;

      /**
       * Private constructor, meant for use by {@link CompletableFuturePublisher} only.
       *
       * @param future Future value to adapt.
       * @param subscriber The subscriber.
       * @param executor Executor to run callbacks with.
       */
      CompletableFutureSubscription(@Nonnull CompletableFuture<T> future,
                                    @Nonnull Subscriber<? super T> subscriber,
                                    @Nonnull Executor executor) {
        this.future = Objects.requireNonNull(future);
        this.subscriber = Objects.requireNonNull(subscriber);
        this.executor = Objects.requireNonNull(executor);
      }

      /**
       * Request the specified number of items from the underlying {@link Subscription}. This must <b>always be
       * <pre>1</pre></b>.
       *
       * @param n Number of elements to request to the upstream (must always be <pre>1</pre>).
       * @throws IllegalArgumentException If any value other than <pre>1</pre> is passed in.
       */
      public synchronized void request(long n) {
        if (n == 1 && !completed.get()) {
          try {
            CompletableFuture<T> future = this.future;
            future.thenAcceptAsync(t -> {
              T val = null;
              Throwable err = null;
              try {
                val = future.get();
              } catch (Exception exc) {
                err = exc;
              }

              if (completed.compareAndSet(false, true)) {
                if (err != null) {
                  subscriber.onError(err);
                } else {
                  if (val != null) {
                    subscriber.onNext(val);
                  }
                  subscriber.onComplete();
                }
              }
            }, executor);

          } catch (Exception e) {
            subscriber.onError(e);
          }
        } else if (n != 1) {
          IllegalArgumentException ex = new IllegalArgumentException(
              "Cannot request more or less than 1 item from a ReactiveFuture-wrapped publisher.");
          subscriber.onError(ex);
        }
      }

      /**
       * Request the publisher to stop sending data and clean up resources.
       */
      public synchronized void cancel() {
        if (completed.compareAndSet(false, true)) {
          subscriber.onComplete();
          future.cancel(false);
        }
      }
    }
  }

  /**
   * Structure that adapts Guava's {@link ListenableFuture} to a Reactive Java {@link Publisher}, which publishes one
   * item - either the result of the computation, or an error.
   *
   * <p>This object is used in the specific circumstance that a {@link ListenableFuture} is wrapped by a
   * {@link ReactiveFuture}, and then used within the Reactive Java ecosystem as a {@link Publisher}. We simply set a
   * callback for the future value, upon item-request (one cycle is allowed), and propagate any events received to the
   * publisher.</p>
   *
   * @param <T> Emit type for this adapter. Matches the publisher it wraps.
   */
  public final static class ListenableFuturePublisher<T> implements Publisher<T> {
    private final @Nonnull ListenableFuture<T> future;
    private final @Nonnull Executor callbackExecutor;

    /**
     * Wrap a {@link ListenableFuture}. Private constructor for use by {@link ReactiveFuture} only.
     *
     * @param future The future to convert or wait on.
     * @param callbackExecutor Executor to run the callback on.
     */
    private ListenableFuturePublisher(@Nonnull ListenableFuture<T> future,
                                      @Nonnull Executor callbackExecutor) {
      this.future = future;
      this.callbackExecutor = callbackExecutor;
    }

    @Override
    public final void subscribe(Subscriber<? super T> subscriber) {
      Objects.requireNonNull(subscriber, "Subscriber cannot be null");
      subscriber.onSubscribe(new ListenableFutureSubscription(this.future, subscriber, this.callbackExecutor));
    }

    /**
     * Models a Reactive Java {@link Subscription}, which is responsible for propagating events from a
     * {@link ListenableFuture} to a {@link Subscriber}.
     *
     * <p>This object is generally used internally by the {@link ListenableFuturePublisher}, once a {@link Subscriber}
     * attaches itself to a {@link Publisher} that is actually a wrapped {@link ListenableFuture}. Error (exception)
     * events and value events are both propagated. Subscribers based on this wrapping will only ever receive a maximum
     * of <b>one value</b> or <b>one error</b>.</p>
     */
    @Immutable
    @ThreadSafe
    public final class ListenableFutureSubscription implements Subscription {
      private final AtomicBoolean completed = new AtomicBoolean(false);
      private final @Nonnull Subscriber<? super T> subscriber;
      private final @Nonnull ListenableFuture<T> future; // to allow cancellation
      private final @Nonnull Executor executor;  // executor to use when dispatching the callback

      /**
       * Private constructor, meant for use by {@link ListenableFuturePublisher} only.
       *
       * @param future Future value to adapt.
       * @param subscriber The subscriber.
       * @param executor Executor to run callbacks with.
       */
      ListenableFutureSubscription(@Nonnull ListenableFuture<T> future,
                                   @Nonnull Subscriber<? super T> subscriber,
                                   @Nonnull Executor executor) {
        this.future = Objects.requireNonNull(future);
        this.subscriber = Objects.requireNonNull(subscriber);
        this.executor = Objects.requireNonNull(executor);
      }

      /**
       * Request the specified number of items from the underlying {@link Subscription}. This must <b>always be
       * <pre>1</pre></b>.
       *
       * @param n Number of elements to request to the upstream (must always be <pre>1</pre>).
       * @throws IllegalArgumentException If any value other than <pre>1</pre> is passed in.
       */
      public synchronized void request(long n) {
        if (n == 1 && !completed.get()) {
          try {
            ListenableFuture<T> future = this.future;
            future.addListener(() -> {
              T val = null;
              Throwable err = null;
              try {
                val = this.future.get();
              } catch (Exception exc) {
                err = exc;
              }

              if (completed.compareAndSet(false, true)) {
                if (err != null) {
                  subscriber.onError(err);
                } else {
                  if (val != null) {
                    subscriber.onNext(val);
                  }
                  subscriber.onComplete();
                }
              }
            }, this.executor);
          } catch (Exception e) {
            subscriber.onError(e);
          }
        } else if (n != 1) {
          IllegalArgumentException ex = new IllegalArgumentException(
            "Cannot request more or less than 1 item from a ReactiveFuture-wrapped publisher.");
          subscriber.onError(ex);
        }
      }

      /**
       * Request the publisher to stop sending data and clean up resources.
       */
      public synchronized void cancel() {
        if (completed.compareAndSet(false, true)) {
          subscriber.onComplete();
          future.cancel(false);
        }
      }
    }
  }
}
