package gust.backend.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * Defines an error case that was encountered while dealing with managed transport logic. This could include connection
 * acquisition, name resolution failures, and so on.
 */
public abstract class TransportException extends RuntimeException {
  /**
   * Package-private constructor for a regular exception.
   *
   * @param message Error message.
   */
  TransportException(@Nonnull String message) {
    super(message);
  }

  /**
   * Package-private constructor for a wrapped exception.
   *
   * @param message Error message.
   * @param thr Wrapped throwable.
   */
  TransportException(@Nonnull String message, @Nullable Throwable thr) {
    super(message, thr);
  }
}
