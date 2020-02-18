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
