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
