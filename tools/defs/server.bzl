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

load(
    "@rules_java//java:defs.bzl",
    _java_binary = "java_binary",
    _java_library = "java_library",
)
load(
    "@rules_pkg//pkg:pkg.bzl",
    _pkg_tar = "pkg_tar",
)
load(
    "@rules_graal//graal:graal.bzl",
    _graal_binary = "graal_binary",
)
load(
    "@rules_pkg//pkg:mappings.bzl",
    _strip_prefix = "strip_prefix",
)
load(
    "//tools/defs/kt:defs.bzl",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_library = "kt_jvm_library",
    _micronaut_library = "micronaut_library",
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

ENABLE_KT_BINARY = False
DEFAULT_STRIP_PREFIX = "elide/backend/"

GRAAL_IMAGE_ARGS = [
    "-O1",
    "--auto-fallback",
    "--enable-http",
    "--enable-https",
    "--language:nfi",
    "--language:regex",
    "--language:llvm",
    "-Duser.country=US",
    "-Duser.language=en",
    "-H:IncludeLocales=en",
    "-H:-EnableSecurityServicesFeature",
]

GRAAL_IMAGE_FEATURES = [
    "com.google.cloud.nativeimage.features.ProtobufMessageFeature",
    "com.google.cloud.nativeimage.features.core.GrpcNettyFeature",
    "com.google.cloud.nativeimage.features.core.GoogleJsonClientFeature",
    "com.google.cloud.nativeimage.features.core.OpenCensusFeature",
]

NATIVE_IMAGE_COMPILE_DEPS = [
    _maven("com.google.cloud:native-image-support"),
]

def server_binary(
        name,
        srcs = [],
        deps = [],
        runtime_deps = [],
        resources = [],
        assets = [],
        main_class = None,
        micronaut = True,
        optimized = False,
        resource_strip_prefix = DEFAULT_STRIP_PREFIX,
        native_image_features = GRAAL_IMAGE_FEATURES,
        graal_extra_args = GRAAL_IMAGE_ARGS,
        **kwargs):
    """Wraps a Kotlin-enabled JVM server binary target."""
    libdeps = []
    if len(assets) > 0:
        _java_library(
            name = "%s.assets" % name,
            resource_strip_prefix = resource_strip_prefix,
            resources = assets,
        )
        libdeps.append(
            ":%s.assets" % name,
        )
    if len(resources) > 0:
        _java_library(
            name = "%s.resources" % name,
            resource_strip_prefix = resource_strip_prefix or _strip_prefix.from_pkg(),
            resources = resources,
        )
        libdeps.append(
            ":%s.resources" % name,
        )

    libdef = _kt_jvm_library
    if micronaut:
        libdef = _micronaut_library
    if len(srcs) > 0:
        libdef(
            name = "%s.entry" % name,
            srcs = srcs,
            deps = deps,
            runtime_deps = runtime_deps,
        )
        libdeps.append(
            ":%s.entry" % name,
        )

    binargs = {
        "name": "%s.jvm" % name,
        "main_class": main_class,
        "runtime_deps": runtime_deps + libdeps,
    }
    binargs.update(kwargs)

    if ENABLE_KT_BINARY:
        _kt_jvm_binary(
            srcs = srcs,
            deps = deps,
            **binargs
        )
    else:
        _java_binary(
            **binargs
        )

    _graal_binary(
        name = "%s.native" % name,
        main_class = main_class,
        include_resources = ".*",
        native_image_features = native_image_features,
        graal_extra_args = graal_extra_args,
        deps = [":%s.jvm" % name] + libdeps + NATIVE_IMAGE_COMPILE_DEPS,
    )
    if optimized:
        native.alias(
            name = name,
            actual = "%s.native" % name,
        )
    else:
        native.alias(
            name = name,
            actual = "%s.jvm" % name,
        )

def server_assets(
        name,
        srcs = [],
        resource_strip_prefix = None,
        compress = False,
        **kwargs):
    """Wraps server-side assets in a `java_library` target."""
    extension = "tar"
    if compress:
        extension = "tar.gz"
    _pkg_tar(
        name = name,
        srcs = srcs,
        strip_prefix = resource_strip_prefix or _strip_prefix.from_pkg(),
        **kwargs
    )

maven = _maven
javagrpc = _javagrpc
javaproto = _javaproto
java_library = _java_library
kt_jvm_library = _kt_jvm_library
micronaut_library = _micronaut_library
