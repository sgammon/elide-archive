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
    "@com_google_j2cl//build_defs:rules.bzl",
    _j2cl_application = "j2cl_application",
)

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    _closure_js_library = "closure_js_library",
    _closure_js_binary = "closure_js_binary",
    _closure_js_template_library = "closure_js_template_library",
    _closure_js_deps = "closure_js_deps",
)

load(
    "//defs/toolchain/context:props.bzl",
    _annotate_defs_dict = "annotate_defs_dict",
    _annotate_defs_flags = "annotate_defs_flags",
)


def _js_module(name, srcs = None, deps = None, **kwargs):

    """ Build a closure JS library. """

    _closure_js_library(
        name = name,
        srcs = srcs or [],
        deps = deps or [],
        **kwargs
    )


def _js_app(name,
            entry_points,
            modules = None,
            deps = None,
            defs = {},
            flags = [],
            css = None,
            **kwargs):

    """ Build a combined frontend application target. """

    overlay_defs = _annotate_defs_dict(defs)

    if css != None:
        css_target = "%s-bin" % css
    else:
        css_target = None

    _j2cl_application(
        name = name,
        entry_points = entry_points,
        deps = deps,
        rewrite_polyfills = True,
#        dependency_mode = "PRUNE",
        extra_flags = flags,
        closure_defines = overlay_defs,
        css = css_target,
        **kwargs
    )


js_app = _js_app
js_module = _js_module
