package gust.backend.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * Defines a class of exceptions which can be encountered when interacting with persistence tools, including internal
 * (built-in) data adapters.
 */
@SuppressWarnings("unused")
public abstract class PersistenceException extends RuntimeException {
  /**
   * Create a persistence exception with a string message.
   *
   * @param message Error message.
   */
  PersistenceException(@Nonnull String message) {
    super(message);
  }

  /**
   * Create a persistence exception with a throwable as a cause.
   *
   * @param cause Cause for the error.
   */
  PersistenceException(@Nonnull Throwable cause) {
    super(cause);
  }

  /**
   * Create a persistence exception with a throwable cause and an explicit error message.
   *
   * @param message Error message.
   * @param cause Cause for the error.
   */
  PersistenceException(@Nonnull String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
