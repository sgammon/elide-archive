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
    "@rules_pkg//pkg:pkg.bzl",
    _pkg_tar = "pkg_tar",
    _pkg_zip = "pkg_zip",
)

load(
    "@io_bazel_rules_k8s//k8s:object.bzl",
    _k8s_object = "k8s_object",
)

load(
    "@io_bazel_rules_k8s//k8s:objects.bzl",
    _k8s_objects = "k8s_objects",
)



def _k8s_config(name,
                kind = None,
                template = None,
                deps = None,
                **kwargs):

    """ Generate targets for a generic Kubernetes config file. """

    if deps != None and template != None:
        fail("Cannot specify both `deps` and `template` for k8s_config. Please use `deps=` for groupings of " +
             "Kubernetes objects.")

    native.filegroup(
        name = "%s-files" % name,
        srcs = (template and [template] or []) + (deps or []),
    )

    if deps != None:
        _pkg_tar(
            name = "%s-tar" % name,
            srcs = [":%s-files" % name],
            deps = [
                ("%s-tar" % n) for n in (deps or [])
            ],
        )

        _pkg_tar(
            name = "%s-zip" % name,
            srcs = [":%s-files" % name],
            deps = [
                ("%s-zip" % n) for n in (deps or [])
            ],
        )

        _k8s_objects(
            name = name,
            objects = deps,
            **kwargs
        )

    else:
        _k8s_object(
            name = name,
            kind = kind,
            template = template,
            **kwargs
        )


k8s_config = _k8s_config
