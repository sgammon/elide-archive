package(
    default_visibility = ["//visibility:public"],
)

load("@rules_proto//proto:defs.bzl", "proto_library")


# Safe HTML Types
proto_library(
    name = "proto",
    srcs = ["proto/src/main/protobuf/webutil/html/types/html.proto"],
    strip_import_prefix = "proto/src/main/protobuf",
)
