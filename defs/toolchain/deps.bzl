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

def _closure_path(*path):

    """ Computes a Closure Library dependency path, based on the
        `goog.provide`/`goog.module` path desired. """

    genpath = ""
    if len(path) == 1:
        genpath = "%s:%s" % (path[0], path[0])  # like `asserts` -> `asserts:asserts`
    elif len(path) > 1:
        genpath = "%s:%s" % ("/".join(path[0:-1]), path[-1].lower())  # like `some,deep,obj` -> `some/deep:obj`
    else:
        fail("Failed to figure out closure dependency path: '%s'." % str(path))

    return "@io_bazel_rules_closure//closure/library/%s" % genpath


def _closure_service(target, type = "binary"):

    """ Computes a target name, postfixed with a sentinel indicating
        a request for a Closure-based gRPC client. """

    return "%s-%s-%s" % (target, "grpc_js", type)


def _closure_proto(target):

    """ Computes a target name, postfixed with a sentinel indicating
        a request for a Closure-based JS proto. """

    return "%s-%s" % (target, "closure_proto")


def _maven(path, root="maven"):

    """ Computes a Maven dependency path, based on the coordinates
        for the artifact. """

    return ("@" + root + "//:" + path
            .replace(":", "_")
            .replace(".", "_")
            .replace("-", "_"))


def _javaproto(path):

    """ Computes a Java protobuf path, by appending the appropriate
        prefix to the handed-in proto target path. """

    return "%s-%s" % (path, "java_proto")  # todo(sgammon): don't hardcode this


maven = _maven
javaproto = _javaproto
closure = _closure_path
js_proto = _closure_proto
js_service = _closure_service
