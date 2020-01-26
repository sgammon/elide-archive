
load(
    "@com_google_j2cl//build_defs:rules.bzl",
    _j2cl_application = "j2cl_application",
)

load("@io_bazel_rules_closure//closure:defs.bzl",
    _closure_js_test = "closure_js_test",
    _closure_js_library = "closure_js_library",
    _closure_js_binary = "closure_js_binary",
    _closure_js_template_library = "closure_js_template_library",
    _closure_js_deps = "closure_js_deps",
)


def _js_test(name, srcs = None, deps = None, **kwargs):

    """ Build a closure JS test. """

    _closure_js_test(
        name = name,
        srcs = srcs or [],
        deps = deps or [],
        **kwargs
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
            **kwargs):

    """ Build a combined frontend application target. """

    _j2cl_application(
        name = name,
        entry_points = entry_points,
        deps = deps,
        rewrite_polyfills = True,
        dependency_mode = "PRUNE",
        **kwargs
    )


js_app = _js_app
js_test = _js_test
js_module = _js_module
