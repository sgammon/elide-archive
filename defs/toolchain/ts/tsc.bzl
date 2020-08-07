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
    "@npm//@bazel/typescript:index.bzl",
    _ts_library = "ts_library"
)

load(
    "@build_bazel_rules_nodejs//:index.bzl",
    "nodejs_binary",
)

load(
    "//defs/toolchain/js:rules.bzl",
    _js_module = "js_module",
)


def _ts_module(name,
               srcs = None,
               deps = None,
               data = [],
               **kwargs):

    """ Establish a TypeScript library target. """

    _ts_library(
        name = "%s" % name,
        srcs = srcs or [],
        deps = deps or [],
        data = data,
        **kwargs
    )


ts_module = _ts_module
