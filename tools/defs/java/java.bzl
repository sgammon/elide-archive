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
    _java_library = "java_library",
    _java_binary = "java_binary"
)

MAVEN_REPO = "@maven"

def _transform_dep(target):
    """Transform a Maven dependency target."""
    return target.replace(".", "_").replace("-", "_").replace(":", "_")

def maven(target):
    """Calculate a Maven target."""
    return "%s//:%s" % (
        MAVEN_REPO,
        _transform_dep(target),
    )

java_library = _java_library
java_binary = _java_binary
