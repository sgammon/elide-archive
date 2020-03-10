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

import org.junit.jupiter.api.TestFactory;

import javax.annotation.Nonnull;


/** Abstract test factory, which is responsible for testing/enforcing {@link ModelAdapter} surfaces. */
public abstract class GenericPersistenceAdapterTest<Adapter extends ModelAdapter>
  extends GenericPersistenceDriverTest<Adapter> {
  /**
   * @return Replace the tested driver with the subject adapter.
   */
  @Override
  protected @Nonnull Adapter driver() {
    return adapter();
  }

  /**
   * Adapter hook-point for the generic {@link ModelAdapter} suite of unit compliance tests. Checks that the adapter
   * exposes required components, and can perform all common driver tasks, both as a {@link ModelAdapter} and a
   * {@link PersistenceDriver}.
   *
   * <p>Automatically spawns tests via {@link TestFactory} (using JUnit 5). These tests may be customized on a per-
   * driver basis by overriding individual test methods, such as {@link #testDriverCodec()}. At runtime, during testing,
   * these cases are dynamically generated and run against each driver.</p>
   *
   * @return Persistence adapter to execute tests against.
   */
  protected abstract @Nonnull Adapter adapter();
}
