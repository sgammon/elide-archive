
load(
    "//defs/toolchain/java:rules.bzl",
    _jdk_binary = "jdk_binary",
    _jdk_library = "jdk_library",
    _micronaut_library = "micronaut_library",
    _micronaut_application = "micronaut_application",
)

load(
    "//defs/toolchain/java:testing.bzl",
    _jdk_test = "jdk_test",
    _micronaut_test = "micronaut_test",
)


jdk_test = _jdk_test
jdk_binary = _jdk_binary
jdk_library = _jdk_library
micronaut_test = _micronaut_test
micronaut_library = _micronaut_library
micronaut_application = _micronaut_application
