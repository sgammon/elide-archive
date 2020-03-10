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
package gust.backend.driver.inmemory;

import gust.backend.model.CacheDriver;
import gust.backend.model.GenericCacheDriverTest;
import gust.backend.model.PersonRecord.Person;
import gust.backend.model.PersonRecord.PersonKey;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.*;


/** Tests for the builtin in-memory cache. */
public final class InMemoryCacheTest extends GenericCacheDriverTest {
  /** {@inheritDoc} */
  @Nonnull
  protected @Override CacheDriver cache() {
    return InMemoryCache.<PersonKey, Person>acquire();
  }

  /**
   * Implementation-specific driver acquisition test.
   */
  @Override
  protected void acquireDriver() {
    assertNotNull(InMemoryCache.<PersonKey, Person>acquire(),
      "should not get `null` when acquiring an in-memory cache driver");
  }
}
