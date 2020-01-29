
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


def _style_library(name,
                   srcs,
                   deps = []):

    """ Wrap a style source in SASS/SCSS processing rules (if needed). Affix any dependencies that
        should be injected globally. """

    # structure as a SASS library regardless of dialect
    _sass_library(
        name = name,
        srcs = srcs,
        deps = deps,
    )


def _style_opt(name,
               src,
               plugins = _DEFAULT_POSTCSS_PLUGINS,
               sourcemap = True):

    """ Wrap a built stylesheet target in an optimization routine, which passes it through PostCSS,
        including any plugins. """

    _postcss_optimize(
        name = name,
        src = src,
        plugins = plugins,
        sourcemap = sourcemap,
    )


def _style_binary(name,
                  src,
                  deps = [],
                  data = [],
                  debug = False,
                  sourcemap = True,
                  output_name = None,
                  output_style = "expanded",  ## leave this be: helps with GSS compilation
                  plugins = _DEFAULT_POSTCSS_PLUGINS,
                  defs = []):

    """ Wrap a style target in SASS/SCSS output rules (if needed). Gather and process the target
       with GSS after compilation, and then any applicable. post-processors. """

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
        renaming = select({
            "//defs/config:release": True,
            "//defs/config:debug": False,
            "//conditions:default": False
        }),
        debug = select({
            "//defs/config:release": False,
            "//defs/config:debug": True,
            "//conditions:default": True
        }),
    )

    _style_opt(
        name = name,
        src = "%s-bin.css" % name,
        plugins = plugins,
        sourcemap = sourcemap,
    )


style_opt = _style_opt
style_binary = _style_binary
style_library = _style_library
