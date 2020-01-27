
goog.module('javatests.dom.dom_test');
goog.setTestOnly();

const DomOperation = goog.require('javatests.dom.DomOperation');
const TagName = goog.require('goog.dom.TagName');
const dom = goog.require('goog.dom');
const testSuite = goog.require('goog.testing.testSuite');


testSuite({
  testDomOperation() {
    const el = dom.createElement(TagName.DIV);
    const mutated = DomOperation.mutate(el, 'Testsuite');

    assertEquals(
        'mutated dom node should contain injected text from Java',
        'Hello from Testsuite!',
        mutated ? mutated.textContent : null);
  }
});
