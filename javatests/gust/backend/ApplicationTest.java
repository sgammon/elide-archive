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
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


/** Test the default Micronaut application boot class. */
public final class ApplicationTest {
  /** Try loading a known-bad application yaml. */
  @Test void testLoadApplicationYamlError() {
    assertThrows(RuntimeException.class, () -> {
      ApplicationBoot.loadConfig("app", "bad.yml", "bad.yml");
    });
  }

  /** Try loading a known-good application yaml. */
  @Test void testLoadApplicationYamlValid() {
    ApplicationBoot.loadConfig("app", ApplicationBoot.rootConfig, ApplicationBoot.defaultConfig);
  }

  /** Try running the config-load routine which runs on server startup. It shouldn't fail. */
  @Test void testBasicConfigLoad() {
    ApplicationBoot.load();
  }

  /** Try reporting a fatal error that occurred during startup. This also shouldn't fail. */
  @Test void testBasicReportErrorStderr() {
    assertThrows(IllegalStateException.class, () -> {
      ApplicationBoot.reportStartupError(new IOException("Something happened"));
    });
  }

  /** Test fallback configuration loading. Should not produce an error, even though the first file doesn't exist. */
  @Test void testLoadConfigDoesNotExist() {
    ApplicationBoot.loadConfig("bunk", "some-nonexistent-name.yml", ApplicationBoot.rootConfig);
  }
}
