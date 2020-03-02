package gust.backend.model;

import com.google.protobuf.Message;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/** Thrown when a write operation fails, because of some conflict situation. */
public final class ModelWriteConflict extends ModelWriteFailure {
  /** Expectation that was violated during the write operation. */
  private final @Nonnull WriteOptions.WriteDisposition failedExpectation;

  /**
   * Create a model write exception with a throwable as a cause.
   *
   * @param key   Key for the record that failed to write.
   * @param model Model that failed to write.
   * @param expectation Expectation that failed to be met.
   */
  public ModelWriteConflict(@Nullable Object key,
                            @Nonnull Message model,
                            @Nonnull WriteOptions.WriteDisposition expectation) {
    super(key, model, String.format(
      "Cannot write to the specified model. Key %s did not meet expectation %s.",
      key,
      expectation.name()));
    this.failedExpectation = expectation;
  }

  // -- Getters -- //
  /** @return Expectation that was violated, when the error was encountered. */
  public @Nonnull WriteOptions.WriteDisposition getFailedExpectation() {
    return failedExpectation;
  }
}
