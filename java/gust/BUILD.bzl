package(
    default_visibility = ["//visibility:public"],
)

load("@com_google_j2cl//build_defs:rules.bzl", "j2cl_library")


java_library(
    name = "Core",
    srcs = ["Core.java"],
    deps = [
        "@com_google_j2cl//:jsinterop-annotations",
    ],
)

j2cl_library(
    name = "Core-j2cl",
    srcs = ["Core.java"],
    deps = [
        "@com_google_j2cl//:jsinterop-annotations-j2cl",
    ],
)

