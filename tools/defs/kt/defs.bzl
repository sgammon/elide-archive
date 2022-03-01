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

"""Kotlin JVM and JS macros."""

load(
    "@io_bazel_rules_kotlin//kotlin:jvm.bzl",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_library = "kt_jvm_library",
)
load(
    "@io_bazel_rules_kotlin//kotlin:js.bzl",
    _kt_js_library = "kt_js_library",
)
load(
    "//tools/defs/java:java.bzl",
    _maven = "maven",
)
load(
    "//tools/defs/model:model.bzl",
    _javaproto = "javaproto",
)
load(
    "//tools/defs/model:service.bzl",
    _javagrpc = "javagrpc",
)

def kt_jvm_library(
        name,
        srcs = [],
        deps = [],
        **kwargs):
    """Designate a JVM-side Kotlin library."""
    _kt_jvm_library(
        name = name,
        srcs = srcs,
        deps = deps,
        **kwargs
    )

def kt_js_library(
        name,
        srcs = [],
        deps = [],
        **kwargs):
    """Designate a common Kotlin library."""
    _kt_js_library(
        name = name,
        srcs = srcs,
        deps = deps,
        **kwargs
    )

def kt_jvm_binary(
        name,
        **kwargs):
    """Designate a Kotlin-enabled Java server binary."""
    _kt_jvm_binary(
        name = name,
        **kwargs
    )

maven = _maven
javaproto = _javaproto
javagrpc = _javagrpc
