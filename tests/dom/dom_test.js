
goog.module('javatests.dom.dom_test');
goog.setTestOnly();

const Core = goog.require('gust.Core');
const DomOperation = goog.require('javatests.dom.DomOperation');
const TagName = goog.require('goog.dom.TagName');
const dom = goog.require('goog.dom');
const gust = goog.require('gust');


describe('Framework JS core', function() {
  it('should support basic mutations via Elemental2', function() {
    const el = dom.createElement(TagName.DIV);
    const mutated = DomOperation.mutate(el, 'Testsuite');

    expect(mutated ? mutated.textContent : null)
        .toBe('Hello from Testsuite!');
  });

  it('should provide a non-null version', function() {
    // tests framework version from JS
    expect(gust.version).not.toBeNull();
  });

  it('should provide an accurate version number', function() {
    expect(gust.version).not.toBe('alpha');
    expect(gust.version).toBeTruthy();
    expect(gust.version).toBe(Core.getGustVersion());

    // tests framework version from cross-lib
    const el = dom.createElement(TagName.DIV);
    const mutated = DomOperation.mutateVersion(el);
    expect(mutated.textContent).toBeTruthy();
    expect(mutated.textContent).toBe(gust.version);
  });
});
