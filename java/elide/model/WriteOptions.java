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
import java.util.Optional;


/** Describes options involved with operations to persist model entities. */
public interface WriteOptions extends OperationOptions {
  /** Default set of write operation options. */
  WriteOptions DEFAULTS = new WriteOptions() {};

  /** Enumerates write attitudes with regard to existing record collisions. */
  enum WriteDisposition {
    /** We don't care. Just write it. */
    BLIND,

    /** The record must exist for the write to proceed (an <i>update</i> operation). */
    MUST_EXIST,

    /** The record must <b>not</b> exist for the write to proceed (a <i>create</i> operation). */
    MUST_NOT_EXIST
  }

  /** @return Specifies the write mode for an operation. Overridden by some methods (for instance, {@code create}). */
  default @Nonnull Optional<WriteDisposition> writeMode() {
    return Optional.empty();
  }
}
