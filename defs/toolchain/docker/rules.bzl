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
    "@io_bazel_rules_docker//container:image.bzl",
    _docker_container_image = "container_image",
)

load(
    "@io_bazel_rules_docker//container:push.bzl",
    _docker_container_push = "container_push",
)


def _container_image(name,
                     repository,
                     tag = None,
                     registry = "us.gcr.io",
                     image_format = "OCI",
                     **kwargs):

    """ Generate a regular Docker container image. """

    _docker_container_image(
        name = name,
        **kwargs
    )

    _docker_container_push(
        name = "%s-push" % name,
        image = ":%s" % name,
        tag = tag or "{BUILD_SCM_VERSION}",
        registry = registry,
        repository = repository,
        format = image_format,
    )


container_image = _container_image
