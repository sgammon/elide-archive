
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


def _template_library(name,
                      srcs,
                      soy_deps = [],
                      js_deps = [],
                      py_deps = [],
                      java_deps = [],
                      proto_deps = [],
                      js = _JS_TEMPLATES,
                      java = _JAVA_TEMPLATES,
                      python = _PYTHON_TEMPLATES,
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
            deps = js_deps + [("%s-%s" % (dep, CLOSUREPROTO_POSTFIX_)) for dep in proto_deps],
        )

    if python:
        _closure_py_template_library(
            name = "%s-py" % name,
            srcs = srcs,
            deps = soy_deps,
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
        )


template_library = _template_library
