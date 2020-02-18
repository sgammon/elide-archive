
goog.module('javatests.dom.dom_test');
goog.setTestOnly();

const Core = goog.require('gust.Core');
const DomOperation = goog.require('javatests.dom.DomOperation');
const TagName = goog.require('goog.dom.TagName');
const dom = goog.require('goog.dom');
const gust = goog.require('gust');
const testSuite = goog.require('goog.testing.testSuite');


testSuite({
  testDomOperation() {
    const el = dom.createElement(TagName.DIV);
    const mutated = DomOperation.mutate(el, 'Testsuite');

    assertEquals(
        'mutated dom node should contain injected text from Java',
        'Hello from Testsuite!',
        mutated ? mutated.textContent : null);
  },

  testFrameworkVersion() {
    // tests framework version from JS
    assert(!!gust.version);
  },

  testCompareFrameworkVersion() {
    assertNotEquals(
        'config app version should not be default',
        'alpha',
        gust.version);
    assertEquals(
        'config app versions should match',
        gust.version,
        Core.getGustVersion());
  },

  testRenderFrameworkVersion() {
    // tests framework version from cross-lib
    const el = dom.createElement(TagName.DIV);
    const mutated = DomOperation.mutateVersion(el);
    assertEquals(
        'mutated dom node should contain identical version to expected framework version',
        gust.version,
        mutated.textContent);
  }
});
