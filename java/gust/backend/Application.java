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

import io.micronaut.runtime.Micronaut;


/**
 * Main application class, which bootstraps a backend Gust app via Micronaut, including any configured controllers,
 * services, or assets. This is where execution starts when running on a JVM. Micronaut uses build-time annotation
 * processing, and a number of other techniques, to pre-initialize and wire up the app before it ever gets to the
 * server to be executed at runtime.
 */
public final class Application {
  /**
   * Main entrypoint into a Gust backend application, powered by Micronaut. This function will pre-load any static stuff
   * that needs to be bootstrapped, and then it initializes the app via Micronaut.
   *
   * @param args Arguments passed on the command line.
   */
  public static void main(String[] args) {
    try {
      ApplicationBoot.load();
      Micronaut.run(Application.class);
    } catch (Throwable thr) {
      ApplicationBoot.reportStartupError(thr);
    }
  }

  private Application() { /* Disallow instantiation. */ }
}
