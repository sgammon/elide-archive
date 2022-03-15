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

"""Macros for defining gRPC APIs via Cloud Endpoints."""

load(
    "//tools/defs/model:model.bzl",
    _descriptor = "descriptor",
)
load(
    "@rules_pkg//pkg:pkg.bzl",
    _pkg_tar = "pkg_tar",
)
load(
    "@rules_pkg//pkg:mappings.bzl",
    _strip_prefix = "strip_prefix",
)

def api_endpoint(name, version, config, service, extension = "tar.gz", active = True):
    """Wire together targets for an API bundle, based on a gRPC descriptor and YAML configuration."""
    _descriptor(
        name = "%s.%s.descriptor" % (name, version),
        proto_library = service,
        out = "%s.%s.pb" % (name, version),
    )
    native.filegroup(
        name = "%s.%s.config" % (name, version),
        srcs = [config],
    )
    _pkg_tar(
        name = "%s.%s" % (name, version),
        strip_prefix = _strip_prefix.from_pkg(),
        extension = extension,
        srcs = [
            ":%s.%s.descriptor" % (name, version),
            ":%s.%s.config" % (name, version),
        ],
    )

    if active:
        native.alias(
            name = name,
            actual = "%s.%s" % (name, version),
        )

def apiconfig(endpoint, version = "v1"):
    """Generate a target path for an API configuration."""
    return "%s.%s.config" % (
        endpoint,
        version,
    )

def descriptor(endpoint, version = "v1"):
    """Generate a target path for an API descriptor."""
    return "%s.%s.descriptor" % (
        endpoint,
        version,
    )
