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

"""Provides macros for defining universal service targets via gRPC."""

load(
    "@rules_proto//proto:defs.bzl",
    _proto_library = "proto_library",
)
load(
    "@io_grpc_grpc_java//:java_grpc_library.bzl",
    _java_grpc_library = "java_grpc_library",
)
load(
    "@build_bazel_rules_swift//swift:swift.bzl",
    _swift_grpc_library = "swift_grpc_library",
)
load(
    "//tools/defs/model:util.bzl",
    _target_name = "target_name",
)
load(
    "//tools/defs/model:model.bzl",
    _javaproto = "javaproto",
    _swiftproto = "swiftproto",
    _tsproto = "tsproto",
    _model = "model",
)

SWIFT = True
TYPESCRIPT = True
JAVAGRPC_POSTFIX = "grpcjava"
SWIFTGRPC_POSTFIX = "grpcswift"
TYPESCRIPTGRPC_POSTFIX = "grpcts"

def service(name, srcs = [], **kwargs):
    """Declare a Protocol Buffer service."""

    base = ":%s" % name

    _model(
        name = name,
        srcs = srcs,
        **kwargs
    )

    # gRPC: Java.
    _java_grpc_library(
        name = _target_name(name, JAVAGRPC_POSTFIX),
        srcs = [base],
        deps = [_javaproto(base)],
    )

    if SWIFT:
        # gRPC: Swift.
        _swift_grpc_library(
            name = _target_name(name, SWIFTGRPC_POSTFIX),
            flavor = "client",
            srcs = [base],
            deps = [_swiftproto(base)],
        )

    if TYPESCRIPT:
        native.alias(
            name = _target_name(name, TYPESCRIPTGRPC_POSTFIX),
            actual = _tsproto(name),
        )

def javagrpc(target):
    """Calculate a target path for a Java gRPC service."""
    return _target_name(target, JAVAGRPC_POSTFIX)

def swiftgrpc(target):
    """Calculate a target path for a Swift gRPC service."""
    return _target_name(target, SWIFTGRPC_POSTFIX)

def tsgrpc(target):
    """Calculate a target path for a TypeScript gRPC service."""
    return _target_name(target, TYPESCRIPTGRPC_POSTFIX)
