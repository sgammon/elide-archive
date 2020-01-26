
## `Local` mode.
## ------------------------------------
## Set to `True` to build against local dependencies.
LOCAL = False

## `Debug` mode.
## ------------------------------------
## Set to `True` to shut off symbol rewriting and enable logging.
DEBUG = True

## `Dev` mode.
## ------------------------------------
## Set to `True` to enable in-page dev tools and UI, regardless of DEBUG mode.
DEV = True

## Application version tag.
## ------------------------------------
## Displayed in certain areas of the app, mostly when `DEV` is active.
VERSION = "alpha-1b0"

## `Renaming` mode.
## ------------------------------------
## Set to `True` to obfuscate styles.
RENAMING = False

## Browser target year.
## ------------------------------------
## Tune JS and add polyfills based on this feature-set year.
BROWSER_FEATURE_YEAR = 2017

## Closure JS target.
## ------------------------------------
## Describes the language level we should output JS in.
OUTPUT_TARGET = "ECMASCRIPT5_STRICT"

## `IDOM` mode.
## ------------------------------------
## Set to `True` to use incremental DOM as the rendering engine.
IDOM = True

## `JS_TEMPLATES` enable/disable flag.
## ------------------------------------
## Set to `True` to generate client-side templates by default.
JS_TEMPLATES = True

## `JAVA_TEMPLATES` enable/disable flag.
## ------------------------------------
## Set to `True` to generate server-side templates for Java by default.
JAVA_TEMPLATES = True

## `PYTHON_TEMPLATES` enable/disable flag.
## ------------------------------------
## Set to `True` to generate server-side templates for Python by default.
PYTHON_TEMPLATES = True
