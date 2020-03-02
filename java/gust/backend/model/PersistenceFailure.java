package gust.backend.model;


/** Enumerates common kinds of persistence failures. Goes well with {@link PersistenceOperationFailed}. */
public enum PersistenceFailure {
  /** The operation timed out. */
  TIMEOUT,

  /** The operation was cancelled. */
  CANCELLED,

  /** The operation was interrupted. */
  INTERRUPTED,

  /** An unknown internal error occurred. */
  INTERNAL;

  /** @return Error message for the selected case. */
  String getMessage() {
    switch (this) {
      case TIMEOUT: return "The operation timed out.";
      case CANCELLED: return "The operation was cancelled.";
      case INTERRUPTED: return "The operation was interrupted.";
    }
    return "An unknown internal error occurred.";
  }
}
