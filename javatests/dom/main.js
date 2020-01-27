
goog.module('main');

const Core = goog.require('gust.Core');
const DomOperation = goog.require('javatests.dom.DomOperation');
const dom = goog.require('goog.dom');


/**
 * Main function, dispatched on page load.
 *
 * @private
 */
function main() {
  const root = document.body;
  const element = dom.createElement('div');
  dom.appendChild(root, DomOperation.mutate(element, `Gust ${Core.getVersion()}`));
}

// Bind `main` to page load.
window.addEventListener('load', main);
