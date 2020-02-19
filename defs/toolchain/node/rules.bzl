
load(
    "@com_google_j2cl//build_defs:rules.bzl",
    _j2cl_application = "j2cl_application",
)

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    _closure_js_library = "closure_js_library",
    _closure_js_binary = "closure_js_binary",
)

load(
    "//defs/toolchain/ts:tsc.bzl",
    _ts_module = "ts_module",
)


def _feathers_library(name, srcs = None, deps = None, **kwargs):

    """ Build a closure JS library, used for metadata for a FeathersJS/ExpressJS target. """

    if srcs != None and len(srcs) > 0 and srcs[0].endswith(".ts"):
        _ts_module(
            name = "%s-ts" % name,
            srcs = srcs,
            deps = deps,
        )

        _closure_js_library(
            name = name,
            srcs = "%s.js" % name,
            deps = deps or [],
            **kwargs
        )
    else:
        _closure_js_library(
            name = name,
            srcs = srcs or [],
            deps = deps or [],
            **kwargs
        )


def _feathers_app(name,
                  controllers = [],
                  deps = None,
                  **kwargs):

    """ Build a combined backend JS application target, for FeathersJS/ExpressJS. """

#    _j2cl_application(
#        name = name,
#        entry_points = entry_points,
#        deps = deps,
#        rewrite_polyfills = True,
#        dependency_mode = "PRUNE",
#        extra_flags = flags,
#        closure_defines = overlay_defs,
#        **kwargs
#    )


feathers_app = _feathers_app
feathers_library = _feathers_library
