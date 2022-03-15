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

"""Provides target template definitions for multi-language API services via gRPC."""

load(
    "@elide//tools/defs/model:util.bzl",
    _target_name = "target_name",
)

KTGRPC_POSTFIX = "grpckt"
JAVAGRPC_POSTFIX = "grpcjava"
SWIFTGRPC_POSTFIX = "grpcswift"
TYPESCRIPTGRPC_POSTFIX = "grpcts"

def ktgrpc(target):
    """Calculate a target path for a Kotlin gRPC service."""
    return _target_name(target, KTGRPC_POSTFIX)

def javagrpc(target):
    """Calculate a target path for a Java gRPC service."""
    return _target_name(target, JAVAGRPC_POSTFIX)

def swiftgrpc(target):
    """Calculate a target path for a Swift gRPC service."""
    return _target_name(target, SWIFTGRPC_POSTFIX)

def tsgrpc(target):
    """Calculate a target path for a TypeScript gRPC service."""
    return _target_name(target, TYPESCRIPTGRPC_POSTFIX)
