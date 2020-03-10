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
    @Nonnull
    @Override
    public ReactiveFuture flush(@Nonnull ListeningScheduledExecutorService executor) {
      return ReactiveFuture.cancelled();
    }

    @Nonnull
    @Override
    public ReactiveFuture evict(@Nonnull PersonKey personKey, @Nonnull ListeningScheduledExecutorService executor) {
      return ReactiveFuture.cancelled();
    }

    @Override
    public @Nonnull
    ReactiveFuture put(@Nonnull PersonKey personKey,
                       @Nonnull Person person,
                       @Nonnull ListeningScheduledExecutorService executor) {
      return ReactiveFuture.cancelled();
    }

    @Override
    public @Nonnull ReactiveFuture<Optional<Person>> fetch(@Nonnull PersonKey personKey,
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
