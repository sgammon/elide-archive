
goog.module('gust');


/**
 * Defines whether we are operating in `dev` mode, which includes a bevy of diagnostic and development-related features
 * that should not be included in production builds.
 *
 * @public
 * @define {!boolean} gust.dev
 */
const dev = goog.define('GUST_DEV', false);


/**
 * Defines whether we are operating in `debug` mode, which forces the build not to include certain optimizations,
 * including property and style renaming.
 *
 * @public
 * @define {!boolean} gust.debug
 */
const debug = goog.define('GUST_DEBUG', false);


/**
 * Defines a variable which is overridden with the current Gust framework version, so it may be consulted in certain
 * circumstances by either internal logic or user code.
 *
 * @public
 * @define {!string} gust.version
 */
const version = goog.define('GUST_VERSION', 'alpha');


/**
 * Return the current development setting state.
 *
 * @public
 * @return {!boolean}
 */
function isDev() {
  return dev;
}


/**
 * Return the current debug setting state.
 *
 * @public
 * @return {boolean}
 */
function isDebug() {
  return debug;
}


/**
 * Return the current Gust framework version.
 *
 * @public
 * @return {string}
 */
function frameworkVersion() {
  return version;
}


exports = {
  dev: dev,
  debug: debug,
  version: version,
  isDev: isDev,
  isDebug: isDebug,
  frameworkVersion: frameworkVersion
};
