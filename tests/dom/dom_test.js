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
goog.module('tests.dom.dom_test');
goog.setTestOnly();

const Core = goog.require('gust.Core');
const DomOperation = goog.require('tests.dom.DomOperation');
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
