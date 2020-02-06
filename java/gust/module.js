
goog.provide('gust');


/**
 * Defines whether we are operating in `dev` mode, which includes a bevy of diagnostic and development-related features
 * that should not be included in production builds.
 *
 * @public
 * @define {!boolean} gust.dev
 */
gust.dev = goog.define('gust.dev', false);


/**
 * Defines whether we are operating in `debug` mode, which forces the build not to include certain optimizations,
 * including property and style renaming.
 *
 * @public
 * @define {!boolean} gust.debug
 */
gust.debug = goog.define('gust.debug', false);


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
