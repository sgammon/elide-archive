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


/** Provides an interface for virtualized object writes during transactions or hierarchical serialization. */
public interface WriteProxy<Reference> {
  /**
   * Prepare a database reference, based on the provided `path`.
   *
   * <p>"Paths" in the model layer refer to hierarchically-stored entities. Typically, a path contains "collections" -
   * i.e. buckets full of similarly shaped data, and "documents" within those collections - i.e., individual bags of
   * schema-less key value associations.</p>
   *
   * @param path Path to prep a database reference for.
   * @return Prepped database reference corresponding to the provided `path`.
   */
  default @Nonnull Reference ref(@Nonnull String path) {
    return this.ref(path, null);
  }

  /**
   * Prepare a database reference, based on the provided `path`, and prepend the provided transaction-wide `prefix`.
   *
   * <p>"Paths" in the model layer refer to hierarchically-stored entities. Typically, a path contains "collections" -
   * i.e. buckets full of similarly shaped data, and "documents" within those collections - i.e., individual bags of
   * schema-less key value associations.</p>
   *
   * @param path Path to prep a database reference for.
   * @param prefix Transaction-wide prefix for this reference.
   * @return Prepped database reference corresponding to the provided `path`.
   */
  @Nonnull Reference ref(@Nonnull String path, @Nullable String prefix);

  /**
   * Save a collapsed `message` in underlying storage, referenced by `reference`.
   *
   * This method specifies the interface that corresponds with the {@link ModelSerializer.WriteDisposition#BLIND} mode,
   * wherein a write occurs without regard to underlying state.
   *
   * @param reference Reference / key for the serialized message.
   * @param message Serialized message (i.e. serialized hierarchical data payload for the message).
   */
  void put(@Nonnull Reference reference, @Nonnull SerializedModel message);

  /**
   * Create a collapsed `message` in underlying storage, referenced by `reference`. If the record is determined to
   * already exist in underlying storage, the operation fails.
   *
   * This method specifies the interface that corresponds with the {@link ModelSerializer.WriteDisposition#CREATE} mode,
   * wherein a write occurs if-and-only-if the entity does not already exist.
   *
   * @param reference Reference / key for the serialized message.
   * @param message Serialized message (i.e. serialized hierarchical data payload for the message).
   */
  void create(@Nonnull Reference reference, @Nonnull SerializedModel message);

  /**
   * Update a collapsed `message` in underlying storage, referenced by `reference`. If the record is determined to be
   * non-existent in underlying storage, the operation fails.
   *
   * This method specifies the interface that corresponds with the {@link ModelSerializer.WriteDisposition#UPDATE} mode,
   * wherein a write occurs if-and-only-if the underlying entity exists during transaction execution.
   *
   * @param reference Reference / key for the serialized message.
   * @param message Serialized message (i.e. serialized hierarchical data payload for the message).
   */
  void update(@Nonnull Reference reference, @Nonnull SerializedModel message);
}
