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

load(
    "@npm_bazel_jasmine//:index.bzl",
    _jasmine_node_test = "jasmine_node_test",
)

load(
    "//defs/toolchain/ts:tsc.bzl",
    _ts_module = "ts_module",
)


INJECTED_NODE_TEST_DEPS = [
    "@npm//jasmine",
    "@npm//@types/jasmine",
]

INJECTED_NODE_RUNTIME_TEST_DEPS = [
    # this space left intentionally blank
]


def _node_test(name,
               srcs,
               data = [],
               deps = [],
               config = str(Label("//jstests:jasmine_config.json")),
               **kwargs):

    """ Define a Jasmine-based unit test for JS use (server-side). """

    _jasmine_node_test(
        name = name,
        srcs = srcs,
        deps = (deps or []) + INJECTED_NODE_TEST_DEPS,
        data = (data or []) + INJECTED_NODE_RUNTIME_TEST_DEPS,
        config_file = config,
        coverage = True,
        **kwargs,
    )


def _node_ts_test(name,
                  srcs,
                  data = [],
                  deps = [],
                  **kwargs):

    """ Define a Jasmine-based unit test for TS use (server-side). """

    _ts_module(
        name = "%s-ts" % name,
        srcs = srcs,
        deps = (deps or []) + INJECTED_NODE_TEST_DEPS,
    )

    native.filegroup(
        name = "%s-es5.js" % name,
        srcs = [":%s-ts" % name],
        output_group = "es5_sources",
    )

    _jasmine_node_test(
        name = name,
        srcs = [":%s-es5.js" % name],
        deps = (deps or []) + INJECTED_NODE_TEST_DEPS,
        data = data,
        coverage = True,
        **kwargs
    )


node_test = _node_test
node_ts_test = _node_ts_test
