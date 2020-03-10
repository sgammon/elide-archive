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
    "@io_bazel_rules_closure//closure:defs.bzl",
     _closure_proto_library = "closure_proto_library",
    _closure_js_proto_library = "closure_js_proto_library",
)

load(
    "@rules_proto//proto:defs.bzl",
    _proto_library="proto_library"
)

load(
    "@io_grpc_java//:java_grpc_library.bzl",
    _java_grpc_library = "java_grpc_library"
)

JAVAPROTO_POSTFIX_ = "java_proto"
CLOSUREPROTO_POSTFIX_ = "closure_proto"
GRPCJAVA_POSTFIX_ = "grpc_java"
_PROTO_ROOT = "/proto"

_native_proto = _proto_library
_native_cc_proto = native.cc_proto_library
_native_java_proto = native.java_proto_library


INJECTED_PROTO_DEPS = [
    str(Label("@gust//gust/core:datamodel")),
]

INJECTED_SERVICE_DEPS = [
    str(Label("@gust//gust/api:services")),
    str(Label("@safe_html_types//:proto")),
]


def __declare_lang_protos(name, internal, service, kwargs):

    """ Declare Java and CC proto libraries. """

    ckwargs = {}
    ckwargs["name"] = "%s-%s" % (name, JAVAPROTO_POSTFIX_)
    ckwargs["deps"] = [":%s" % kwargs["name"]]
    _native_java_proto(
        **ckwargs
    )


def __declare_native(name, internal, service, kwargs):

    """ Declare a target as a native proto library. """

    kwargs["name"] = name
    if not internal:
        kwargs["deps"] = kwargs.get("deps", []) + INJECTED_PROTO_DEPS + (
            service and INJECTED_SERVICE_DEPS or [])
    _native_proto(
        **kwargs
    )

def __declare_closure_proto(name, internal, service, kwargs):

    """ Declare a target as a Closure proto library. """

    ckwargs = {}
    ckwargs["name"] = "%s-%s" % (kwargs["name"], CLOSUREPROTO_POSTFIX_)
    ckwargs["deps"] = [":%s" % kwargs["name"]]
    _closure_proto_library(
        **ckwargs
    )


def _proto(name,
           _internal = False,
           **kwargs):

    """
    Proxy individual proto declarations to relevant native and extension rules, which need to know about each individual
    proto. "Proto modules" are exported using the `_module` function in the same way. Keyword arguments are proxied in
    full, with selected entries being removed where needed. Positional arguments are not supported.

    :param name: Name of the proto target.
    :param _internal: Indicates that this is a built-in proto, and should not be injected with dependencies.
    :param kwargs: Keyword arguments to pass along.
    :returns: Nothing - defines rules instead.
    """

    __declare_native(name, _internal, False, kwargs)
    __declare_closure_proto(name, _internal, False, kwargs)
    __declare_lang_protos(name, _internal, False, kwargs)


def _module(name,
            _internal = False,
            **kwargs):

    """
    Proxy module proto declarations to relevant native and extension rules, which need to know about each grouping of
    protos. Individual protos are exported each using the `_proto` function in the same way. Keyword arguments are
    proxied in full, with selected entries being removed where needed. Positional arguments are not supported.

    :param kwargs: Keyword arguments to pass along.
    :returns: Nothing - defines rules instead.
    """

    __declare_native(name, _internal, False, kwargs)
    __declare_closure_proto(name, _internal, False, kwargs)
    __declare_lang_protos(name, _internal, False, kwargs)


def _service(name,
             flavor = "normal",
             **kwargs):

    """
    Define a service, contained in a Protobuf file, potentially with models to carry along as well. This injects
    additional dependencies and prepares targets related to RPC services.

    :param name: Name of the target.
    :param kwargs: Keyword arguments to pass along.
    """

    __declare_native(name, False, True, kwargs)
    __declare_closure_proto(name, False, True, kwargs)
    __declare_lang_protos(name, False, True, kwargs)

    _java_grpc_library(
        name = "%s-%s" % (name, GRPCJAVA_POSTFIX_),
        srcs = [":%s" % name],
        deps = [":%s-%s" % (name, JAVAPROTO_POSTFIX_)],
        flavor = flavor,
    )


model = _proto
service = _service
model_package = _module
