
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

## Framework version tag.
## ------------------------------------
## Displayed in certain areas of the app, mostly when `DEV` is active.
VERSION = "1.0.0-alpha1"

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

## `CHROMIUM` enable/disable flag.
## ------------------------------------
## Set to `True` to run tests on Chromium via WebDriver.
CHROMIUM = True

## `FIREFOX` enable/disable flag.
## ------------------------------------
## Set to `True` to run tests on Firefox via WebDriver.
FIREFOX = True

## `SAUCE` enable/disable flag.
## ------------------------------------
## Set to `True` to run tests on SauceLabs, as configured.
SAUCE = True

## Browser package version.
## ------------------------------------
## Assigned to the latest version at: https://github.com/bazelbuild/rules_webtesting/blob/master/web/versioned.
BROWSERS_VERSION = "0.3.2"

## GraalVM version.
## ------------------------------------
## Assigned to the latest available CE VM.
GRAALVM_VERSION = "19.3.1"

## GraalVM JDK version.
## ------------------------------------
## Specifies the version of the underlying VM JDK.
GRAALVM_JDK_VERSION = "8"

## Java version.
## ------------------------------------
## Sets the language level for JVM output.
JAVA_LANGUAGE_LEVEL = "11"

## Kotlin version.
## ------------------------------------
## Sets the Kotlin API and runtime version.
KOTLIN_LANGUAGE_LEVEL = "1.3"
