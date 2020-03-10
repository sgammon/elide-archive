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


import com.google.protobuf.FieldMask;

import javax.annotation.Nonnull;
import java.util.Optional;


/**
 * Specifies options which may be applied, generically, to model instance fetch operations implemented through the
 * {@link PersistenceDriver} interface.
 */
public interface FetchOptions extends CacheOptions, OperationOptions {
  /** Default set of fetch options. */
  FetchOptions DEFAULTS = new FetchOptions() {};

  /** Enumerates ways the {@code FieldMask} may be applied. */
  enum MaskMode {
    /** Only include fields mentioned in the field mask. */
    INCLUDE,

    /** Omit fields mentioned in the field mask. */
    EXCLUDE,

    /** Treat the field mask as a projection, for query purposes only. */
    PROJECTION
  }

  /** @return Field mask to apply when fetching properties. Fields not mentioned in the mask will be omitted. */
  default @Nonnull Optional<FieldMask> fieldMask() {
    return Optional.empty();
  }

  /** @return Mode to operate in when applying the affixed field mask, if any. */
  default @Nonnull MaskMode fieldMaskMode() {
    return MaskMode.INCLUDE;
  }
}
