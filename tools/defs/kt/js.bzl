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
