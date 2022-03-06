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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Operational options that can be applied to individual calls into the {@link ModelAdapter} framework. See individual
 * options interfaces for more information.
*/
@SuppressWarnings("UnstableApiUsage")
public interface OperationOptions {
  /** @return Value to apply to the operation timeout. If left unspecified, the global default is used. */
  default @Nonnull Optional<Long> timeoutValue() {
    return Optional.empty();
  }

  /** @return Unit to apply to the operation timeout. If left unspecified, the global default is used. */
  default @Nonnull Optional<TimeUnit> timeoutUnit() {
    return Optional.empty();
  }

  /** @return Executor service that should be used for calls that reference this option set. */
  default @Nonnull Optional<ListeningScheduledExecutorService> executorService() {
    return Optional.empty();
  }

  /** @return Set a precondition for the precise time (in microseconds) that a record was updated. */
  default @Nonnull Optional<Long> updatedAtMicros() {
    return Optional.empty();
  }

  /** @return Set a precondition for the precise time (in seconds) that a record was updated. */
  default @Nonnull Optional<Long> updatedAtSeconds() {
    return Optional.empty();
  }

  /** @return Number of retries, otherwise the default is used. */
  default @Nonnull Optional<Integer> retries() {
    return Optional.empty();
  }

  /** @return Whether to run in a transaction. */
  default @Nonnull Optional<Boolean> transactional() {
    return Optional.empty();
  }
}
