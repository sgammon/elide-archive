##
# Copyright © 2020, The Gust Framework Authors. All rights reserved.
#
# The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
# are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
# this code in object or source form requires and implies consent and agreement to that license in principle and
# practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
# Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
# Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
# by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
# is strictly forbidden except in adherence with assigned license requirements.
##


## Framework version tag.
## ------------------------------------
## Displayed in certain areas of the app, mostly when `DEV` is active.
VERSION = "1.0.0-alpha5"

## `Local` mode.
## ------------------------------------
## Set to `True` to build against local dependencies.
LOCAL = False

## `Debug` mode.
## ------------------------------------
## Set to `True` to shut off symbol rewriting and enable logging.
DEBUG = False

## `Dev` mode.
## ------------------------------------
## Set to `True` to enable in-page dev tools and UI, regardless of DEBUG mode.
DEV = False

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
PYTHON_TEMPLATES = False

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
GRAALVM_VERSION = "20.2.0"

## GraalVM JDK version.
## ------------------------------------
## Specifies the version of the underlying VM JDK.
GRAALVM_JDK_VERSION = "11"

## Java version.
## ------------------------------------
## Sets the language level for JVM output.
JAVA_LANGUAGE_LEVEL = "11"

## Kotlin version.
## ------------------------------------
## Sets the Kotlin API and runtime version.
KOTLIN_LANGUAGE_LEVEL = "1.4"

## Kubernetes toolchain version.
## ------------------------------------
## Sets the version for the Kubectl tool, etc.
K8S_VERSION = "1.15.9"

## Protobuf toolchain version.
## ------------------------------------
## Sets the version enforced throughout for Protobuf.
PROTOBUF_VERSION = "3.13.0"

## JVM-based app debug port.
## ------------------------------------
## Sets the port to wait/listen for remote JVM tools on (for launching a debugger).
JVM_DEBUG_PORT = "5005"
