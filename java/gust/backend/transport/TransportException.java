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
