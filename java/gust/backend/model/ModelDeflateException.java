package gust.backend.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/** Describes an error that occurred while serializing a model. */
public final class ModelDeflateException extends PersistenceException {
  /**
   * Main package-private constructor.
   *
   * @param message Error message to apply.
   * @param cause Optional cause.
   */
  ModelDeflateException(@Nonnull String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  /**
   * Create a persistence exception with a throwable as a cause.
   *
   * @param cause Cause for the error.
   */
  ModelDeflateException(@Nonnull Throwable cause) {
    super(cause);
  }
}
