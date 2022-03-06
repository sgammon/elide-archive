/*
 * Copyright © 2022, The Elide Framework Authors. All rights reserved.
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
package elide.model;

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
