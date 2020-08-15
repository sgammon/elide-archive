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

/**
 * Gust: Core module.
 *
 * @fileoverview Provides core declarations, used across platforms and modules.
 */

/*global goog,gust*/
goog.provide('gust');


/**
 * Defines whether we are operating in `dev` mode, which includes a bevy of diagnostic and development-related features
 * that should not be included in production builds.
 *
 * @public
 * @define {!boolean} gust.dev
 */
gust.dev = goog.define('gust.dev', true);


/**
 * Defines the current execution engine. This is always `browser` when running in a Closure-based context. On the
 * backend, this value changes via `Core`.
 *
 * @public
 * @define {!string} gust.engine
 */
gust.engine = goog.define('gust.engine', 'browser');


/**
 * Defines whether we are operating in `debug` mode, which forces the build not to include certain optimizations,
 * including property and style renaming.
 *
 * @public
 * @define {!boolean} gust.debug
 */
gust.debug = goog.define('gust.debug', true);


/**
 * Defines a variable which is overridden with the current Gust framework version, so it may be consulted in certain
 * circumstances by either internal logic or user code.
 *
 * @public
 * @define {!string} gust.version
 */
gust.version = goog.define('gust.version', 'alpha');


/**
 * Return the current development setting state.
 *
 * @public
 * @return {!boolean}
 */
gust.isDev = function() {
  return gust.dev;
};


/**
 * Return the current debug setting state.
 *
 * @public
 * @return {boolean}
 */
gust.isDebug = function() {
  return gust.debug;
};


/**
 * Return the current Gust framework version.
 *
 * @public
 * @return {string}
 */
gust.frameworkVersion = function() {
  return gust.version;
};
