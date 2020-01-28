
## -- Doc Targets

load(
    "//defs/toolchain:frontend.bzl",
    "js_test",
    "js_module",
    "js_app",
)

load(
    "//defs/toolchain:backend.bzl",
    "jdk_test",
    "jdk_binary",
    "jdk_library",
    "micronaut_test",
    "micronaut_library",
    "micronaut_application",
)

load(
    "//defs:proto.bzl",
    "proto",
    "proto_module",
)

# Any symbol loaded here will be added to the docs.
