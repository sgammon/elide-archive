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
package elide.driver.inmemory;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import elide.model.*;
import elide.model.PersonRecord.Person;
import elide.model.PersonRecord.PersonKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the {@link InMemoryAdapter}. */
@SuppressWarnings("UnstableApiUsage")
public final class InMemoryAdapterTest extends GenericPersistenceAdapterTest<InMemoryAdapter<PersonKey, Person>> {
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
  protected @Nonnull InMemoryAdapter<PersonKey, Person> adapter() {
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
