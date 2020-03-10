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


/** Describes a generic operational failure that occurred within the persistence engine. */
@SuppressWarnings("WeakerAccess")
public final class PersistenceOperationFailed extends PersistenceException {
  /** Enumerated failure case. */
  private final @Nonnull PersistenceFailure failure;

  /**
   * Main private constructor.
   *
   * @param message Error message to apply.
   * @param cause Optional cause.
   */
  private PersistenceOperationFailed(@Nonnull PersistenceFailure failure,
                                     @Nonnull String message,
                                     @Nullable Throwable cause) {
    super(message, cause);
    this.failure = failure;
  }

  /**
   * Generate a persistence failure exception for a generic (known) failure case.
   *
   * @param failure Known failure case to spawn an exception for.
   * @return Exception object.
   */
  static @Nonnull PersistenceOperationFailed forErr(@Nonnull PersistenceFailure failure) {
    return new PersistenceOperationFailed(failure, failure.getMessage(), null);
  }

  /**
   * Generate a persistence failure exception for a generic (known) failure case, optionally applying an inner cause
   * exception to the built object.
   *
   * @param failure Known failure case to spawn an exception for.
   * @param cause Exception object to use as the inner cause.
   * @return Spawned persistence exception object.
   */
  static @Nonnull PersistenceOperationFailed forErr(@Nonnull PersistenceFailure failure,
                                                    @Nullable Throwable cause) {
    return new PersistenceOperationFailed(failure, failure.getMessage(), cause);
  }

  // -- Getters -- //

  /** @return Failure case that occurred. */
  public @Nonnull PersistenceFailure getFailure() {
    return failure;
  }
}
