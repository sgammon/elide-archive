
// @ts-ignore
const Core = goog.require("gust.Core");


/**
 * Main function, dispatched on page load.
 */
export function main() {
    console.log(`Hello from Gust ${Core.getVersion()}`);
}

// Bind `main` to page load.
window.addEventListener('load', main);
