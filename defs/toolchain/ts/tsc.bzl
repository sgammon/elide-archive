
load(
    "@npm_bazel_typescript//:index.bzl",
    _ts_library = "ts_library",
)

load(
    "//defs/toolchain/js:closure.bzl",
    _js_module = "js_module",
)


def _ts_module(name,
               srcs = None,
               deps = None,
               **kwargs):

    """ Build a TypeScript library, and translate it into Closure-compatible structures
        by leveraging Clutz/Tsickle. """

#    _ts_library(
#        name = "%s" % name,
#        srcs = srcs or [],
#        deps = deps or [],
#        **kwargs
#    )

    #_js_module(
    #    name = "%s-closure" % name,
    #    srcs = ["%s.js" % name],
    #)


ts_module = _ts_module
