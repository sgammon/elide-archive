
load(
    "@io_bazel_rules_closure//closure:defs.bzl",
     _closure_proto_library = "closure_proto_library",
    _closure_js_proto_library = "closure_js_proto_library",
)

load(
    "@rules_proto//proto:defs.bzl",
    _proto_library="proto_library"
)

_JSPROTO_POSTFIX = "jsproto"
_JAVAPROTO_POSTFIX = "java_proto"
_CLOSUREPROTO_POSTFIX = "closure_proto"
_PROTO_ROOT = "/proto"

_native_proto = _proto_library
_native_cc_proto = native.cc_proto_library
_native_java_proto = native.java_proto_library


def __declare_lang_protos(name, kwargs):

    """ Declare Java and CC proto libraries. """

    ckwargs = {}
    ckwargs["name"] = "%s-%s" % (name, _JAVAPROTO_POSTFIX)
    ckwargs["deps"] = [":%s" % kwargs["name"]]
    _native_java_proto(
        **ckwargs
    )


def __declare_native(name, kwargs):

    """ Declare a target as a native proto library. """

    kwargs["name"] = name
    _native_proto(
        **kwargs
    )

def __declare_closure_proto(name, kwargs):

    """ Declare a target as a Closure proto library. """

    ckwargs = {}
    ckwargs["name"] = "%s-%s" % (kwargs["name"], _CLOSUREPROTO_POSTFIX)
    ckwargs["deps"] = [":%s" % kwargs["name"]]
    _closure_proto_library(
        **ckwargs
    )

def _proto(name, **kwargs):

    """
    Proxy individual proto declarations to relevant native and extension rules, which need to know about each individual
    proto. "Proto modules" are exported using the `_module` function in the same way. Keyword arguments are proxied in
    full, with selected entries being removed where needed. Positional arguments are not supported.

    :param kwargs: Keyword arguments to pass along.
    :returns: Nothing - defines rules instead.
    """

    if "strip_import_prefix" not in kwargs:
        kwargs["strip_import_prefix"] = "/proto"
    __declare_native(name, kwargs)
    __declare_closure_proto(name, kwargs)
    __declare_lang_protos(name, kwargs)


def _module(name, **kwargs):

    """
    Proxy module proto declarations to relevant native and extension rules, which need to know about each grouping of
    protos. Individual protos are exported each using the `_proto` function in the same way. Keyword arguments are
    proxied in full, with selected entries being removed where needed. Positional arguments are not supported.

    :param kwargs: Keyword arguments to pass along.
    :returns: Nothing - defines rules instead.
    """

    if "strip_import_prefix" not in kwargs:
        kwargs["strip_import_prefix"] = "/proto"
    __declare_native(name, kwargs)
    __declare_closure_proto(name, kwargs)
    __declare_lang_protos(name, kwargs)


proto = _proto
module = _module
