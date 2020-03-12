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

package(default_visibility = ["//visibility:public"])

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    "closure_js_library",
)


## Base: TS Config
alias(name = "tsconfig.json",
      actual = "config/tsconfig.base.json")


## Package: App Types
alias(name = "tsconfig.apptypes.json",
      actual = "packages/app-types/tsconfig.json")

## Package: Util
alias(name = "tsconfig.util.json",
      actual = "@io_bloombox_labs_NEURON//external/firebase:tsconfig.util.json")

## Package: App
alias(name = "tsconfig.app.json",
      actual = "packages/app/tsconfig.json")


## Source Groups: Externs
filegroup(
    name = "firebase-app-externs-js",
    srcs = ["packages/firebase/externs/firebase-app-externs.js"])

filegroup(
    name = "firebase-auth-externs-js",
    srcs = ["packages/firebase/externs/firebase-auth-externs.js"])

filegroup(
    name = "firebase-client-auth-externs-js",
    srcs = ["packages/firebase/externs/firebase-client-auth-externs.js"])

filegroup(
    name = "firebase-database-externs-js",
    srcs = ["packages/firebase/externs/firebase-database-externs.js"])

filegroup(
    name = "firebase-error-externs-js",
    srcs = ["packages/firebase/externs/firebase-error-externs.js"])

filegroup(
    name = "firebase-firestore-externs-js",
    srcs = ["packages/firebase/externs/firebase-firestore-externs.js"])

filegroup(
    name = "firebase-messaging-externs-js",
    srcs = ["packages/firebase/externs/firebase-messaging-externs.js"])

filegroup(
    name = "firebase-storage-externs-js",
    srcs = ["packages/firebase/externs/firebase-storage-externs.js"])

filegroup(
    name = "firebase-externs-js",
    srcs = [
        ":firebase-app-externs-js",
        ":firebase-auth-externs-js",
        ":firebase-client-auth-externs-js",
        ":firebase-database-externs-js",
        ":firebase-error-externs-js",
        ":firebase-firestore-externs-js",
        ":firebase-messaging-externs-js",
        ":firebase-storage-externs-js"])
