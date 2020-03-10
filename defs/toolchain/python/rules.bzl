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
    "@rules_python//python:defs.bzl",
    _py_binary = "py_binary",
    _py_library = "py_library",
)

#load(
#    "@py//:requirements.bzl",
#    _requirement = "requirement",
#)

#load(
#    "@werkzeug//:requirements.bzl",
#    _werkzeug_requirement = "requirement",
#)


#WERKZEUG_DEPS = [
#    _werkzeug_requirement("werkzeug"),
#    _requirement("protobuf"),
#]


def _werkzeug_library(name,
                      srcs,
                      deps = []):

    """ Python library, containing some piece of code, which is used in conjunction with a
        Werkzeug-based backend. """

#    _py_library(
#        name = name,
#        srcs = srcs,
#        deps = (deps + WERKZEUG_DEPS),
#    )


def _werkzeug_application(name,
                          entry_point,
                          deps = []):

    """ Wrap a Python library as a Werkzeug application entrypoint, with injected dependencies
        for Werkzeug, Redis, Soy, Protobuf, gRPC, and so on. """

#    _py_binary(
#        name = name,
#        srcs = [entry_point],
#        main = entry_point,
#        deps = (deps + WERKZEUG_DEPS),
#        python_version = "PY3",
#    )


py_binary = _py_binary
py_library = _py_library
werkzeug_library = _werkzeug_library
werkzeug_application = _werkzeug_application
