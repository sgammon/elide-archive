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
    "@io_bazel_rules_closure//closure/private/rules:soy_library.bzl",
    _soy_library = "soy_library",
)

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    _closure_js_template_library = "closure_js_template_library",
    _closure_py_template_library = "closure_py_template_library",
    _closure_java_template_library = "closure_java_template_library",
)

load(
    "//defs/toolchain:schema.bzl",
    "JAVAPROTO_POSTFIX_",
    "CLOSUREPROTO_POSTFIX_",
)

load(
    "//defs:config.bzl",
    _JS_TEMPLATES = "JS_TEMPLATES",
    _JAVA_TEMPLATES = "JAVA_TEMPLATES",
    _PYTHON_TEMPLATES = "PYTHON_TEMPLATES",
)

INJECTED_SSR_SOY_DEPS = [
    "//gust/page:page_soy",
]

INJECTED_SSR_PROTO_DEPS = [
    "//gust/page:page_proto",
]


def _template_library(name,
                      srcs,
                      soy_deps = [],
                      js_deps = [],
                      py_deps = [],
                      java_deps = [],
                      proto_deps = [],
                      style_deps = [],
                      js = _JS_TEMPLATES,
                      java = _JAVA_TEMPLATES,
                      python = _PYTHON_TEMPLATES,
                      java_package = None,
                      precompile = True):

    """ Declare a universal, cross-platform template library, making use of the built-in
        Soy integration. """

    _soy_library(
        name = name,
        srcs = srcs,
        deps = soy_deps,
        proto_deps = proto_deps,
    )

    if js:
        _closure_js_template_library(
            name = "%s-js" % name,
            srcs = srcs,
            deps = js_deps + [("%s-%s" % (dep, CLOSUREPROTO_POSTFIX_)) for dep in proto_deps] + style_deps,
        )

    if python:
        _closure_py_template_library(
            name = "%s-py" % name,
            srcs = srcs,
            deps = soy_deps + style_deps,
            proto_deps = [("%s-%s" % (p, CLOSUREPROTO_POSTFIX_)) for p in proto_deps],
        )

    if java:
        _closure_java_template_library(
            name = "%s-java" % name,
            srcs = srcs,
            deps = soy_deps,
            java_deps = (
              [("%s-%s" % (p, JAVAPROTO_POSTFIX_)) for p in proto_deps] +
              [("%s-java_jcompiled" % p) for p in soy_deps]),
            proto_deps = [("%s-%s" % (p, CLOSUREPROTO_POSTFIX_)) for p in proto_deps],
            precompile = precompile,
            java_package = java_package,
        )


def _ssr_library(name,
                 srcs,
                 soy_deps = [],
                 js_deps = [],
                 py_deps = [],
                 java_deps = [],
                 proto_deps = [],
                 style_deps = [],
                 java = _JAVA_TEMPLATES,
                 python = _PYTHON_TEMPLATES,
                 java_package = None,
                 precompile = True,
                 **kwargs):

    """ Declare a template for use exclusively during SSR (Server-Side Rendering). This
        also injects additional SSR-related dependencies automatically. """

    _template_library(
        name = name,
        srcs = srcs,
        soy_deps = (soy_deps or []) + INJECTED_SSR_SOY_DEPS,
        proto_deps = (proto_deps or []) + INJECTED_SSR_PROTO_DEPS,
        java_package = None,
        js = False,
    )


ssr_library = _ssr_library
template_library = _template_library
