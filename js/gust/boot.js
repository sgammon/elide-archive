
goog.module('gust.boot');

const Core = goog.require('gust.Core');


/**
 * Main function, dispatched on page load.
 *
 * @private
 */
function main() {
    console.log(`Hello from Gust, version ${Core.getVersion()}!`);
}

// Bind `main` to page load.
window.addEventListener('load', main);
