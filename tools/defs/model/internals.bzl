"""Internal tools for Protocol Buffer-related functions."""

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
    "@com_github_grpc_grpc_kotlin//:kt_jvm_grpc.bzl",
    _kt_jvm_proto_library = "kt_jvm_proto_library",
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
KOTLIN = True
TYPESCRIPT = True
KT_POSTFIX = "ktproto"
JAVA_POSTFIX = "javaproto"
SWIFT_POSTFIX = "swiftproto"
PYTHON_POSTFIX = "pyproto"
CLOSURE_POSTFIX = "closureproto"
TYPESCRIPT_POSTFIX = "tsproto"

def _paths(files):
    """Gather a set of paths."""
    return [f.path for f in files]

def _descriptor_impl(ctx):
    """Build a full-source and all-imports descriptor."""
    descriptors = ctx.attr.proto_library[ProtoInfo].transitive_descriptor_sets.to_list()
    ctx.actions.run_shell(
      inputs=descriptors,
      outputs=[ctx.outputs.out],
      command='cat %s > %s' % (
          ' '.join(_paths(descriptors)), ctx.outputs.out.path))

def declare_model(name, **kwargs):
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

    if KOTLIN:
        # Proto: Kotlin.
        _kt_jvm_proto_library(
            name = _target_name(name, KT_POSTFIX),
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

def well_known(name, actual, **kwargs):
    """Import a well-known Protocol Buffer definition, wrapping it in the appropriate language-specific rules supported
    by the framework.

    :param name: Name of the well-known proto target.
    :param actual: Well-known protobuf library target.
    :param kwargs: Keyword arguments to pass along.
    """
    # setup an alias to the native proto lib
    native.alias(
        name = "%s_wellknown" % name,
        actual = actual,
    )
    native.alias(
        name = name,
        actual = "%s_wellknown" % name,
    )

def ktproto(target):
    """Calculate a target name for a Kotlin protocol buffer."""
    return _target_name(target, KT_POSTFIX)

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

descriptor = rule(
  implementation=_descriptor_impl,
  attrs = {
    "proto_library": attr.label(),
    "out": attr.output(mandatory=True),
  }
)

proto_library = _proto_library
