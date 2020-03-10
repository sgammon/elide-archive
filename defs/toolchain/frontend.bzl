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
    "//defs/toolchain/js:rules.bzl",
    _js_app = "js_app",
    _js_module = "js_module",
)

load(
    "//defs/toolchain/js:testing.bzl",
    _js_test = "js_test",
)

load(
    "//defs/toolchain/ts:tsc.bzl",
    _ts_module = "ts_module",
)

load(
    "//defs/toolchain/style:rules.bzl",
    _style_binary = "style_binary",
    _style_library = "style_library",
)


js_app = _js_app
js_test = _js_test
js_module = _js_module
ts_module = _ts_module
style_binary = _style_binary
style_library = _style_library
