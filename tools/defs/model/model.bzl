##
# Copyright © 2022, The Elide Framework Authors. All rights reserved.
#
# The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
# are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
# this code in object or source form requires and implies consent and agreement to that license in principle and
# practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
# Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
# Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
# by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
# is strictly forbidden except in adherence with assigned license requirements.
##

"""Provides macros for defining unified data model targets via Protocol Buffers."""

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
