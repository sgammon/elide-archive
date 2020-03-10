##
# Copyright © 2020, The Gust Framework Authors. All rights reserved.
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
    "@bazel_tools//tools/build_defs/pkg:pkg.bzl",
    _pkg_tar = "pkg_tar",
)


load(
    "@rules_pkg//pkg:pkg.bzl",
    _pkg_zip = "pkg_zip",
)


def _archive(name,
             srcs,
             deps = None,
             format = "tar",
             **kwargs):

    """ Create a generic tarball or zip archive. """

    if format == "tar":
        _pkg_tar(
            name = name,
            srcs = srcs,
            deps = deps,
            **kwargs
        )
    elif format == "zip":
        _pkg_zip(
            name = name,
            srcs = srcs,
            deps = deps,
            **kwargs
        )
    else:
        fail("Unrecognized package format: '%s'." % format)


zipball = _pkg_zip
tarball = _pkg_tar
archive = _archive
