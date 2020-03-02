package gust.backend.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/** Describes an error that occurred while de-serializing a model. */
public final class ModelInflateException extends PersistenceException {
  /**
   * Main package-private constructor.
   *
   * @param message Error message to apply.
   * @param cause Optional cause.
   */
  ModelInflateException(@Nonnull String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  /**
   * Create a persistence exception with a throwable as a cause.
   *
   * @param cause Cause for the error.
   */
  ModelInflateException(@Nonnull Throwable cause) {
    super(cause);
  }
}
