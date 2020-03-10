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
package gust.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/** Test failure logic when required app configs aren't present. */
public final class NoConfigTest {
  /** Test loading the app when no required configs are present at all. This should fail. */
  @Test void testLoadAppNoConfigs() {
    assertThrows(RuntimeException.class, ApplicationBoot::load);
  }

  /** Test force-loading a specific config that does not exist. */
  @Test void testLoadConfigDoesNotExist() {
    assertThrows(RuntimeException.class, () -> {
      ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", "hi-i-also-dont-exist.yml");
    });
  }

  /** Test force-loading a specific config that does not exist, with no fallback. */
  @Test void testLoadConfigDoesNotExistNoFallback() {
    assertThrows(RuntimeException.class, () -> {
      ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", null);
    });
  }
}
