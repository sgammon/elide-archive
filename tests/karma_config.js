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
module.exports = function(config) {
  config.set({
    browsers: ['ChromeHeadless'],
    reporters: ['progress', 'dots', 'kjhtml', 'coverage'],
    preprocessors: {
      '**/gust/**/!(*_test|*amd*).js': ['coverage']
    },
    coverageInstrumenter: {
      esModules: false,
      produceSourceMap: true
    },
    coverageReporter: {
      dir: '_coverage',
      subdir: function(browser) {
        return browser.toLowerCase().split(/[ /-]/)[0].replace('headless', '');
      },
      reporters: [
        { type: 'lcovonly', file: 'coverage.dat' },
        { type: 'text', file: 'coverage.txt' },
        { type: 'text-summary', file: 'coverage-summary.txt' }
      ],
      instrumenterOptions: {
        istanbul: {
          esModules: false,
          produceSourceMap: true,
          noCompact: true
        }
      }
    }
  });
};
