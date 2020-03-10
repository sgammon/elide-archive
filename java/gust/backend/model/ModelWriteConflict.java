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


/** Thrown when a write operation fails, because of some conflict situation. */
@SuppressWarnings("WeakerAccess")
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
