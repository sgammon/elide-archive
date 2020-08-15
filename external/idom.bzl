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

suppressions = [
    "JSC_REQUIRES_NOT_SORTED",
    "JSC_UNKNOWN_EXPR_TYPE",
    "JSC_IMPLICITLY_NULLABLE_JSDOC"]


## Files: TSC Config
filegroup(
    name = "idom-tsconfig",
    srcs = ["tsconfig.json"])


## Files: Closure Dist
closure_js_library(
    name = "idom-types-js",
    srcs = ["dist/closure/types.js"],
    suppress = ["JSC_USELESS_EMPTY_STATEMENT", "JSC_IMPLICITLY_NULLABLE_JSDOC"])

closure_js_library(
    name = "idom-global-js",
    srcs = ["dist/closure/global.js"])

closure_js_library(
    name = "idom-assertions-js",
    srcs = ["dist/closure/assertions.js"],
    deps = [
        ":idom-global-js",
        ":idom-types-js"],
    suppress = suppressions)

closure_js_library(
    name = "idom-symbols-js",
    srcs = ["dist/closure/symbols.js"])

closure_js_library(
    name = "idom-util-js",
    srcs = ["dist/closure/util.js"],
    suppress = ["JSC_UNKNOWN_EXPR_TYPE"])

closure_js_library(
    name = "idom-attributes-js",
    srcs = ["dist/closure/attributes.js"],
    deps = [
        ":idom-types-js",
        ":idom-symbols-js",
        ":idom-util-js"],
    suppress = suppressions)

closure_js_library(
    name = "idom-changes-js",
    srcs = ["dist/closure/changes.js"],
    deps = [":idom-util-js"],
    suppress = suppressions)

closure_js_library(
    name = "idom-notifications-js",
    srcs = ["dist/closure/notifications.js"])

closure_js_library(
    name = "idom-context-js",
    srcs = ["dist/closure/context.js"],
    deps = [":idom-notifications-js"],
    suppress = suppressions)

closure_js_library(
    name = "idom-dom_util-js",
    srcs = ["dist/closure/dom_util.js"],
    deps = [":idom-notifications-js"],
    suppress = ["JSC_UNKNOWN_EXPR_TYPE"])

closure_js_library(
    name = "idom-node_data-js",
    srcs = ["dist/closure/node_data.js"],
    deps = [
        ":idom-assertions-js",
        ":idom-dom_util-js",
        ":idom-global-js",
        ":idom-types-js"],
    suppress = suppressions)

closure_js_library(
    name = "idom-nodes-js",
    srcs = ["dist/closure/nodes.js"],
    deps = [
        ":idom-node_data-js",
        ":idom-types-js"],
    suppress = suppressions)

closure_js_library(
    name = "idom-core-js",
    srcs = ["dist/closure/core.js"],
    deps = [
        ":idom-assertions-js",
        ":idom-context-js",
        ":idom-dom_util-js",
        ":idom-global-js",
        ":idom-node_data-js",
        ":idom-nodes-js",
        ":idom-types-js"],
    suppress = suppressions)

closure_js_library(
    name = "idom-diff-js",
    srcs = ["dist/closure/diff.js"],
    deps = [
        ":idom-util-js"],
    suppress = suppressions)

closure_js_library(
    name = "idom-virtual_elements-js",
    srcs = ["dist/closure/virtual_elements.js"],
    deps = [
        ":idom-assertions-js",
        ":idom-attributes-js",
        ":idom-core-js",
        ":idom-global-js",
        ":idom-node_data-js",
        ":idom-types-js",
        ":idom-util-js"],
    suppress = suppressions + ["JSC_WRONG_NUMBER_OF_PARAMS"])

closure_js_library(
    name = "idom-index-js",
    srcs = ["dist/closure/index.js"],
    deps = [
        ":idom-attributes-js",
        ":idom-core-js",
        ":idom-global-js",
        ":idom-node_data-js",
        ":idom-notifications-js",
        ":idom-symbols-js",
        ":idom-virtual_elements-js"])

closure_js_library(
    name = "idom-js",
    exports = [":idom-index-js"])