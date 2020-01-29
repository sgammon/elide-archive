#!/usr/bin/env node

const DEBUG = true;

const args = require('args');
const postcss = require('postcss');
const fs = require('fs');


/**
 * Optimize a given stylesheet `source`, placing the result in the `target` file specified.
 * This runs PostCSS, with any configured plugins (see above).
 *
 * @param {!string} config Configuration file to load.
 * @param {!string} source Source file to compile/optimize.
 * @param {!string} target Target file, in which to put the compiled output.
 * @param {!string} plugins Comma-separated list of plugins to load.
 * @param {!boolean} debug Whether the build is running in debug mode.
 * @param {!boolean} opt Whether the build is running in release mode.
 * @param {!boolean} sourcemap Whether to generate sourcemaps alongside target outputs.
 */
function execute(config, source, target, plugins, debug, opt, sourcemap) {
  if (DEBUG)
    console.log('PostCSS Invoked', {
      config, source, target, plugins, debug, opt, sourcemap});

  const configFile = fs.readFile(config, (configErr, configJson) => {
    if (configErr) {
      console.error(`Failed to load PostCSS config file at '${config}'.`);
      process.exitCode = 1;
    } else {
      const configData = JSON.parse(configJson);

      // step one: load plugins
      const resolvedPlugins = [].map.call(plugins, (pluginName) => {
        try {
          if (DEBUG)
            console.log(`Loading PostCSS plugin "${pluginName}"...`);
          return require(pluginName)(Object.assign({},
              configData ? (configData[pluginName] || {}) : {}));
        } catch (err) {
          console.error(`Failed to load PostCSS plugin "${pluginName}". Is it installed?`);
          process.exit(1);
        }
      });

      fs.readFile(source, (err, css) => {
        if (err) {
          console.error(`Failed to load source file: ${err}.`);
          process.exitCode = 1;
        } else {
          // noinspection JSCheckFunctionSignatures
          postcss(resolvedPlugins)
              .process(css, { from: source, to: target })
              .then(result => {
                if (DEBUG)
                  console.log('Post CSS invocation complete.', {
                    css: result.css
                  });
                fs.writeFile(target, result.css, () => true);
                if (sourcemap === true) {
                  if (result.map) {
                    fs.writeFile(`${target}.map`, result.map, () => true);
                  }
                }
              }, err => {
                console.error(`Failed to optimize stylsheet with PostCSS. Error:\n${err}`);
                process.exitCode = 1;
              });
        }
      });
    }
  });
}

// noinspection JSUnresolvedFunction
args
    .option('source', 'Source file to optimize.')
    .option('target', 'Target file in which to place the optimized style code.')
    .option('config', 'Configuration file to load and apply to enabled plugins.')
    .option('plugins', 'Selection of plugins to `require()` and add to the PostCSS build pipeline.', [])
    .option('debug', 'Whether we are operating in debug mode. Defaults to `false`.', false)
    .option('opt', 'Whether we are operating in release mode. Defaults to `false`.', false)
    .option('sourcemap', 'Whether we should generate source maps alongside targets. Defaults to `true`.', true);

const flags = args.parse(process.argv);
execute(flags.config, flags.source, flags.target, flags.plugins, flags.debug, flags.opt, flags.sourcemap);
