package gust.backend.model;

import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/** Thrown when a model fails to write. Should be extended by more specific error cases. */
@SuppressWarnings("unused")
public class ModelWriteFailure extends PersistenceException {
  /** Key for the model that failed to write. */
  private final @Nullable Object key;

  /** Model that failed to write. */
  private final @Nonnull Message model;

  /**
   * Create a model write exception with a throwable as a cause.
   *
   * @param key Key for the record that failed to write.
   * @param model Model that failed to write.
   */
  ModelWriteFailure(@Nullable Object key, @Nonnull Message model) {
    super(String.format("Failed to write model at key '%s'.", key));
    this.key = key;
    this.model = model;
  }

  /**
   * Create a model write exception with a throwable as a cause.
   *
   * @param key Key for the record that failed to write.
   * @param model Model that failed to write.
   * @param cause Cause for the error.
   */
  ModelWriteFailure(@Nullable Object key, @Nonnull Message model, @Nonnull Throwable cause) {
    super(String.format("Failed to write model at key '%s': %s.", key, cause.getMessage()), cause);
    this.key = key;
    this.model = model;
  }

  /**
   * Create a model write exception with a custom error message.
   *
   * @param key Key for the record that failed to write.
   * @param model Model that failed to write.
   * @param errorMessage Custom error message.
   */
  ModelWriteFailure(@Nullable Object key, @Nonnull Message model, @Nonnull String errorMessage) {
    super(errorMessage);
    this.key = key;
    this.model = model;
  }

  /**
   * Create a model write exception with a throwable and a custom error message.
   *
   * @param key Key for the record that failed to write.
   * @param model Model that failed to write.
   * @param cause Cause for the error.
   * @param errorMessage Custom error message.
   */
  ModelWriteFailure(@Nullable Object key,
                    @Nonnull Message model,
                    @Nonnull Throwable cause,
                    @Nonnull String errorMessage) {
    super(errorMessage, cause);
    this.key = key;
    this.model = model;
  }

  // -- Getters -- //
  /** @return Key for the model that failed to write. */
  public @Nullable Object getKey() {
    return key;
  }

  /** @return Model that failed to write. */
  public @Nonnull Message getModel() {
    return model;
  }
}
