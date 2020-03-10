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
package gust.backend.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the {@link PersistenceDriver} interface definition. */
public final class PersistenceDriverTest {
  @Test void testSwallowExceptions() {
    assertDoesNotThrow(() -> {
      PersistenceDriver.Internals.swallowExceptions(() -> {
        throw new IllegalStateException("testing testing");
      });
    });
  }

  @Test void testConvertAsyncExceptions() {
    assertThrows(PersistenceOperationFailed.class, () -> {
      PersistenceDriver.Internals.convertAsyncExceptions(() -> {
        throw new InterruptedException("operation interrupted");
      });
    });

    assertThrows(PersistenceOperationFailed.class, () -> {
      PersistenceDriver.Internals.convertAsyncExceptions(() -> {
        throw new TimeoutException("operation timed out");
      });
    });

    assertThrows(PersistenceOperationFailed.class, () -> {
      PersistenceDriver.Internals.convertAsyncExceptions(() -> {
        throw new ExecutionException(new IllegalStateException("something happened in another thread"));
      });
    });

    assertThrows(PersistenceOperationFailed.class, () -> {
      PersistenceDriver.Internals.convertAsyncExceptions(() -> {
        throw new IllegalArgumentException("some arbitrary exception");
      });
    });
  }
}
