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
