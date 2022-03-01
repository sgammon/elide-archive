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

"""Defines macros and targets relating to Kotlin JS."""

load(
    "@io_bazel_rules_kotlin//kotlin:js.bzl",
    _kt_js_library = "kt_js_library",
)

def kt_js_library(name, **kwargs):
    """Macro for defining a Kotlin JavaScript target."""
    _kt_js_library(
        name = name,
        **kwargs
    )

def _ktjs(name, type = "wrapper"):
    """Calculate a Kotlin JavaScript dependency target, for an extension or a wrapper."""
    base = "//third_party/kotlin/js"
    if type == "wrapper":
        base = "//third_party/kotlin/js/wrappers"
    return "%s:%s" % (
        base, name
    )

ktjs = _ktjs
