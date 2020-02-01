
load(
    "@rules_python//python:defs.bzl",
    _py_binary = "py_binary",
    _py_library = "py_library",
)

load(
    "@py//:requirements.bzl",
    _requirement = "requirement",
)

load(
    "@werkzeug//:requirements.bzl",
    _werkzeug_requirement = "requirement",
)


WERKZEUG_DEPS = [
    _werkzeug_requirement("werkzeug"),
    _requirement("protobuf"),
]


def _werkzeug_library(name,
                      srcs,
                      deps = []):

    """ Python library, containing some piece of code, which is used in conjunction with a
        Werkzeug-based backend. """

    _py_library(
        name = name,
        srcs = srcs,
        deps = (deps + WERKZEUG_DEPS),
    )


def _werkzeug_application(name,
                          entry_point,
                          deps = []):

    """ Wrap a Python library as a Werkzeug application entrypoint, with injected dependencies
        for Werkzeug, Redis, Soy, Protobuf, gRPC, and so on. """

    _py_binary(
        name = name,
        srcs = [entry_point],
        main = entry_point,
        deps = (deps + WERKZEUG_DEPS),
        python_version = "PY3",
    )


py_binary = _py_binary
py_library = _py_library
werkzeug_library = _werkzeug_library
werkzeug_application = _werkzeug_application
