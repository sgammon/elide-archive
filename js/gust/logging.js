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
 * Gust: Frontend logging.
 *
 * @fileoverview Provides tools for logging on the frontend. Allows logging to be enabled/disabled based on app
 *   configuration. Applies formatting like log-line prefixes, timestamps, etc.
 */

/*global goog */

goog.module('gust.logging');
const Core = goog.require('gust.Core');


/**
 * Send a log message to the console.
 *
 * @param {...*} var_args Arguments to log.
 * @public
 */
function log(var_args) {
  if (Core.isDebugMode() === true)
    console.log.apply(console, ['[GUST]'].concat(Array.from(arguments)));
}

/**
 * Send an INFO-level message to the console.
 *
 * @param {...*} var_args Arguments to log.
 * @public
 */
function info(var_args) {
  if (Core.isDebugMode() === true)
    console.info.apply(console, ['[GUST]'].concat(Array.from(arguments)));
}


/**
 * Send a WARN-level message to the console.
 *
 * @param {...*} var_args Arguments to log.
 * @public
 */
function warn(var_args) {
  console.warn.apply(console, ['[GUST]'].concat(Array.from(arguments)));
}


/**
 * Send an ERROR-level message to the console.
 *
 * @param {...*} var_args Arguments to log.
 * @public
 */
function error(var_args) {
  console.error.apply(console, ['[GUST]'].concat(Array.from(arguments)));
}


exports.log = log;
exports.info = info;
exports.warn = warn;
exports.error = error;
