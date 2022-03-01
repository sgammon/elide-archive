load(
    "@rules_proto//proto:defs.bzl",
    _proto_library = "proto_library",
)
load(
    "@npm//@bazel/labs/grpc_web:ts_proto_library.bzl",
    _ts_proto_library = "ts_proto_library"
)
load(
    "@rules_java//java:defs.bzl",
    _java_proto_library = "java_proto_library",
)
load(
    "@build_bazel_rules_swift//swift:swift.bzl",
    _swift_proto_library = "swift_proto_library",
)
load(
    "@com_google_protobuf//:protobuf.bzl",
    _py_proto_library = "py_proto_library",
)
load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    _closure_proto_library = "closure_proto_library",
)
load(
    "//tools/defs/model:util.bzl",
    _target_name = "target_name",
)

SWIFT = True
PYTHON = False
TYPESCRIPT = True
JAVA_POSTFIX = "javaproto"
SWIFT_POSTFIX = "swiftproto"
PYTHON_POSTFIX = "pyproto"
CLOSURE_POSTFIX = "closureproto"
TYPESCRIPT_POSTFIX = "tsproto"

def model(name, **kwargs):
    """Declare a Protocol Buffer model."""

    base = ":%s" % name

    _proto_library(
        name = name,
        **kwargs
    )

    # Proto: Java.
    _java_proto_library(
        name = _target_name(name, JAVA_POSTFIX),
        deps = [base],
    )

    # Proto: Closure.
    _closure_proto_library(
        name = _target_name(name, CLOSURE_POSTFIX),
        deps = [base],
    )

    if SWIFT:
        # Proto: Swift.
        _swift_proto_library(
            name = _target_name(name, SWIFT_POSTFIX),
            deps = [base],
        )

    if PYTHON:
        # Proto: Python.
        _py_proto_library(
            name = _target_name(name, PYTHON_POSTFIX),
            deps = [base],
        )

    if TYPESCRIPT:
        # Proto: TypeScript.
        _ts_proto_library(
            name = _target_name(name, TYPESCRIPT_POSTFIX),
            proto = base,
        )

def javaproto(target):
    """Calculate a target name for a Java protocol buffer."""
    return _target_name(target, JAVA_POSTFIX)

def swiftproto(target):
    """Calculate a target name for a Swift protocol buffer."""
    return _target_name(target, SWIFT_POSTFIX)

def pyproto(target):
    """Calculate a target name for a Python protocol buffer."""
    return _target_name(target, PYTHON_POSTFIX)

def jsproto(target):
    """Calculate a target name for a Closure protocol buffer."""
    return _target_name(target, CLOSURE_POSTFIX)

def tsproto(target):
    """Calculate a target name for a TypeScript protocol buffer."""
    return _target_name(target, TYPESCRIPT_POSTFIX)
