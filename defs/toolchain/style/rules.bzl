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
    "@io_bazel_rules_sass//:defs.bzl",
    _sass_binary = "sass_binary",
    _sass_library = "sass_library",
)

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    _closure_css_binary = "closure_css_binary",
    _closure_css_library = "closure_css_library",
)

load(
    "//defs/toolchain/style:postcss.bzl",
    _postcss_optimize = "postcss_optimize",
    _DEFAULT_POSTCSS_PLUGINS = "DEFAULT_POSTCSS_PLUGINS"
)

BASE_SASS_DEPS = [
    "//style:_vars.sass",
]

BASE_GSS_DEFS = [
]

OPTIMIZE_STYLES = False


def _style_library(name,
                   srcs = [],
                   deps = []):

    """ Wrap a style source in SASS/SCSS processing rules (if needed). Affix any dependencies that
        should be injected globally. """

    if srcs and (srcs[0].endswith(".sass") or srcs[0].endswith(".scss")):
        # structure as a SASS library regardless of dialect
        _sass_library(
            name = name,
            srcs = srcs,
            deps = deps,
        )
    else:
        # structure as a CSS/GSS library regardless of dialect
        _closure_css_library(
            name = name,
            srcs = srcs,
            deps = deps,
        )


def _style_opt(name,
               src,
               config = None,
               plugins = _DEFAULT_POSTCSS_PLUGINS,
               sourcemap = True):

    """ Wrap a built stylesheet target in an optimization routine, which passes it through PostCSS,
        including any plugins. """

    _postcss_optimize(
        name = name,
        src = src,
        plugins = plugins,
        config = config,
        sourcemap = sourcemap,
    )


def _style_binary(name,
                  src = None,
                  deps = [],
                  data = [],
                  debug = False,
                  config = None,
                  sourcemap = True,
                  output_name = None,
                  output_style = "expanded",  ## leave this be: helps with GSS compilation
                  plugins = _DEFAULT_POSTCSS_PLUGINS,
                  optimize = OPTIMIZE_STYLES,
                  defs = []):

    """ Wrap a style target in SASS/SCSS output rules (if needed). Gather and process the target
       with GSS after compilation, and then any applicable. post-processors. """

    renaming_state = select({
        "//defs/config:release": True,
        "//defs/config:debug": False,
        "//conditions:default": False
    })

    debug_state = select({
        "//defs/config:release": False,
        "//defs/config:debug": True,
        "//conditions:default": True
    })

    if src != None and (src.endswith(".sass") or src.endswith(".scss")):
        # process as normal SASS/SCSS
        _sass_binary(
            name = "%s-sass" % name,
            src = src,
            deps = deps,
            output_name = "%s-inter.css" % name,
            output_style = output_style,
            sourcemap = debug,
        )

        _closure_css_library(
            name = "%s-lib" % name,
            srcs = [":%s-sass" % name],
        )

        _closure_css_binary(
            name = "%s-bin" % name,
            deps = [":%s-lib" % name],
            defs = BASE_GSS_DEFS + defs,
            renaming = renaming_state,
            debug = debug_state,
        )

    elif src == None or (src.endswith(".gss") or src.endswith(".css")):
        # process as normal CSS/GSS
        _closure_css_binary(
            name = "%s-bin" % name,
            deps = deps,
            defs = BASE_GSS_DEFS + defs,
            renaming = renaming_state,
            debug = debug_state,
        )
    else:
        fail("Unrecognized style_binary src file.")

    if optimize:
        _style_opt(
            name = name,
            src = "%s-bin.css" % name,
            plugins = plugins,
            sourcemap = sourcemap,
            config = config,
        )
    else:
        native.alias(
            name = name,
            actual = "%s-bin.css" % name,
        )
        native.alias(
            name = "%s.css" % name,
            actual = "%s-bin.css" % name,
        )

    native.alias(
        name = "%s.css.json" % name,
        actual = ":%s-bin.css.js" % name,
    )


style_opt = _style_opt
style_binary = _style_binary
style_library = _style_library
