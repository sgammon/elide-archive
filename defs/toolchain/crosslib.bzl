
load(
    "@rules_java//java:defs.bzl",
    "java_library",
)

load(
    "@com_google_j2cl//build_defs:rules.bzl",
    "j2cl_library",
)

def _cross_java_lib(name, srcs, **kwargs):

    """ Prepare a library for both Java (server-side) and J2CL (client-side). """

    java_library(
        name = name,
        srcs = srcs,
        deps = [
            "@com_google_j2cl//:jsinterop-annotations",
        ],
    )

    j2cl_library(
        name = "%s-j2cl" % name,
        srcs = srcs,
        deps = [
            "@com_google_j2cl//:jsinterop-annotations-j2cl",
        ],
    )


cross_lib = _cross_java_lib
