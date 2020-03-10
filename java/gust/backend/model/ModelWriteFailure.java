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
