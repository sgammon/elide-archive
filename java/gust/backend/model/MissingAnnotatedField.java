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

import com.google.protobuf.Descriptors;
import tools.elide.core.FieldType;

import javax.annotation.Nonnull;


/** Specifies that a model was missing a required annotated-field for a given operation. */
@SuppressWarnings("WeakerAccess")
public final class MissingAnnotatedField extends PersistenceException {
  /** Specifies the descriptor that violated the required field constraint. */
  private final @Nonnull Descriptors.Descriptor violatingSchema;

  /** Field type that was required but not found. */
  private final @Nonnull FieldType requiredField;

  /**
   * Create an exception describing a missing, but required, annotated schema field.
   *
   * @param descriptor Descriptor for the message type in question.
   * @param type Field type that was required but missing.
   */
  MissingAnnotatedField(@Nonnull Descriptors.Descriptor descriptor, @Nonnull FieldType type) {
    super(String.format(
      "Model type '%s' failed to be processed, because it is missing the annotated field '%s', " +
      "which was required for the requested operation.",
      descriptor.getName(),
      type.name()));

    this.violatingSchema = descriptor;
    this.requiredField = type;
  }

  // -- Getters -- //

  /** @return Descriptor for the schema type which violated the required-field constraint. */
  public @Nonnull Descriptors.Descriptor getViolatingSchema() {
    return violatingSchema;
  }

  /** @return The type of field that must be added to pass the failing constraint. */
  public @Nonnull FieldType getRequiredField() {
    return requiredField;
  }
}
