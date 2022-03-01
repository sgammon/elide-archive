##
# Copyright © 2022, The Elide Framework Authors. All rights reserved.
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

## Application version tag.
## ------------------------------------
## Displayed in certain areas of the app, mostly when `DEV` is active.
VERSION = "1.0.0-alpha8"

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
OUTPUT_TARGET = "ECMASCRIPT_2019"

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
GRAALVM_VERSION = "22.0.0.2"

## GraalVM JDK version.
## ------------------------------------
## Specifies the version of the underlying VM JDK.
GRAALVM_JDK_VERSION = "11"

## Java version.
## ------------------------------------
## Sets the language level for JVM output.
JAVA_LANGUAGE_LEVEL = "11"

## Kotlin language version.
## ------------------------------------
## Sets the Kotlin API version.
KOTLIN_LANGUAGE_LEVEL = "1.6"

## Kotlin SDK version.
## ------------------------------------
## Sets the Kotlin runtime version.
KOTLIN_SDK_VERSION = "1.6.10"

## React version.
## ------------------------------------
## Sets the version of React to use.
REACT_VERSION = "17.0.2"

## Kubernetes toolchain version.
## ------------------------------------
## Sets the version for the Kubectl tool, etc.
K8S_VERSION = "1.18.16"

## Kubernetes pinned version state.
## ------------------------------------
## Forces use of `kubectl` at the pinned Kubernetes version.
K8S_PINNED = False

## Protobuf toolchain version.
## ------------------------------------
## Sets the version enforced throughout for Protobuf.
PROTOBUF_VERSION = "3.19.2"

## gRPC toolchain version.
## ------------------------------------
## Sets the version enforced throughout for gRPC.
GRPC_VERSION = "1.44.0"

## gRPC Java version.
## ------------------------------------
## Sets the version enforced throughout for gRPC's Java layer.
GRPC_JAVA_VERSION = "1.44.0"

## Google API Extensions version.
## ------------------------------------
## Sets the version enforced throughout for GAX and GAX-gRPC.
GAX_VERSION = "2.5.0"

## Apache Beam SDK version.
## ------------------------------------
## Sets the version enforced throughout for Apache Beam and Cloud Dataflow.
BEAM_VERSION = "2.33.0"

## JVM-based app debug port.
## ------------------------------------
## Sets the port to wait/listen for remote JVM tools on (for launching a debugger).
JVM_DEBUG_PORT = "5005"

## Golang version to use.
## ------------------------------------
## Sets the version to use for Google's Go language.
GO_VERSION = "1.17.6"

## NodeJS version to use.
## ------------------------------------
## Sets the version of the Node JS runtime to use.
NODE_VERSION = "16.6.2"

## Yarn version to use.
## ------------------------------------
## Pins the version for the Yarn package manager for Node.
YARN_VERSION = "1.22.10"

## Firebase SDK version.
## ------------------------------------
## Version pin to use for Firebase JS, Java SDKs.
FIREBASE_VERSION = "8.3.2"

## Android SDK.
## ------------------------------------
## Whether to load the Android SDK.
ENABLE_ANDROID = False

## Google Cloud SDK.
## ------------------------------------
## Version of the Google Cloud SDK to use and install.
GCLOUD_VERSION = "373.0.0"

## Micronaut.
## ------------------------------------
## Server-side framework version.
MICRONAUT_VERSION = "3.3.3"

## iOS Version.
## ------------------------------------
## Minimum-supported version of iOS.
IOS_MINIMUM_VERSION = "14.0"
