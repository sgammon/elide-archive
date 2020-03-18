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
package gust;

import jsinterop.annotations.JsType;


/**
 * Provides core values, utility methods, etc, which can be used throughout the back- and front-end of a Gust-based
 * application. Some of these methods or properties will return different values based on where the application is
 * executed. So, accessing, say, {@link #getEngine()} will return {@code browser} when invoked from JavaScript on the
 * front-end, and one of {@code jvm} or {@code native} when running on the backend.
 */
@JsType
@SuppressWarnings("WeakerAccess")
public final class Core {
  /** Holds the current runtime engine. */
  static final String currentEngine = System.getProperty("gust.engine", "unknown");

  /** Version of the framework. Injected at build time. */
  static final String frameworkVersion = System.getProperty("gust.version", "alpha");

  /** Holds the current `dev` flag status. */
  static final Boolean devMode = Boolean.parseBoolean(System.getProperty("gust.dev", "true"));

  /** Holds the current `debug` flag status. */
  static final Boolean debugMode = Boolean.parseBoolean(System.getProperty("gust.debug", "true"));

  /** Holds the static prefix for dynamically routed assets. */
  static final String dynamicAssetPrefix = "/_/assets";

  private Core() { /* Disallow instantiation. */ }

  /**
   * Retrieve the application version setting, which is applied via the JVM system property <pre>gust.version</pre>.
   * This value also shows up in frontend libraries as <pre>gust.version</pre>. The default value for this property, if
   * left unspecified by the runtime, is `alpha`.
   *
   * @return Version assigned for the currently-running application.
   **/
  public static String getGustVersion() {
    return frameworkVersion;
  }

  /**
   * Retrieve the current engine running this code. By default, this is the string `unknown`. When running in frontend
   * contexts, this is generally overridden to be `browser`. On the backend, this value is generally either `jvm` or
   * `native`, depending on how the code is executing.
   *
   * @return Current execution engine.
   */
  public static String getEngine() {
    return currentEngine;
  }

  /**
   * Returns the current debug-mode flag state, which indicates whether we are running in a debugging-compatible mode or
   * not. If not, we can generally expect a stripped binary (on the front and back-end), and perhaps expect that symbol
   * renaming is active.
   *
   * @return Whether we are running in debug mode, or production mode (in which case this is `false`).
   */
  public static Boolean isDebugMode() {
    return debugMode;
  }

  /**
   * Returns the current dev-mode flag state, which indicates whether we are running locally/in a development context.
   * This is intentionally separate from {@link #isDebugMode()} to facilitate "production" binary testing in a local
   * setting. Flip this flag to `true` to enable local RPC calls, logging, and so on, but with a production-optimized
   * set of artifacts.
   *
   * @return Whether we are running in dev mode, or production mode (in which case this is `false`).
   */
  public static Boolean isDevMode() {
    return devMode;
  }

  /**
   * "Production mode" is so-called because both {@link #isDebugMode()} and {@link #isDevMode()} return `false`. In this
   * circumstance, we are running entirely in a mode for production use, in a production context. RPCs should be sent to
   * endpoints that write and read to/from production databases.
   *
   * @return Whether we are running in production mode or not (`true` when `dev` and `debug` are both `false`).
   */
  public static Boolean isProductionMode() {
    return !debugMode && !devMode;
  }

  /**
   * The <i>dynamic asset prefix</i> is a URL prefix under which frontend application assets are served, using a managed
   * file and token scheme, with a generated binary asset manifest living at the root of the JAR. Using this feature,
   * the server auto-detects managed assets and includes them in the page when directed.
   *
   * <p>All assets, stylesheets and scripts included, are served under this URL if they are "managed assets," meaning
   * they are defined in the asset manifest and enclosed in the JAR.</p>
   *
   * @return Dynamic frontend asset serving prefix.
   */
  public static String getDynamicAssetPrefix() {
    return dynamicAssetPrefix;
  }
}
