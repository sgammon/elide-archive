
load(
    "@npm_bazel_typescript//:index.bzl",
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
