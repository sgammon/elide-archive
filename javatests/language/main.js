
goog.module('main');

const Core = goog.require('gust.Core');
const JavaObject = goog.require('javatests.language.JavaObject');


/**
 * Main function, dispatched on page load.
 *
 * @private
 */
function main() {
  console.log(`Hello from Gust, version ${Core.getGustVersion()}!`);
  console.log(`Also, hello from ${JavaObject.hello()}`);
}

// Bind `main` to page load.
window.addEventListener('load', main);
