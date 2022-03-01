load(
    "@io_bazel_rules_kotlin//kotlin:jvm.bzl",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_library = "kt_jvm_library",
)
load(
    "@io_bazel_rules_kotlin//kotlin:js.bzl",
    _kt_js_library = "kt_js_library",
)
load(
    "//tools/defs/java:java.bzl",
    _maven = "maven",
)
load(
    "//tools/defs/model:model.bzl",
    _javaproto = "javaproto",
)
load(
    "//tools/defs/model:service.bzl",
    _javagrpc = "javagrpc",
)

MICRONAUT_DEPS = [
    "//tools/micronaut",
    _maven("com.google.guava:guava"),
]

MICRONAUT_RUNTIME_DEPS = [
    # None yet.
]

MICRONAUT_SERVICE_DEPS = [
    "@io_grpc_grpc_java//context",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//stub",
    _maven("io.micronaut.grpc:micronaut-grpc-runtime"),
    _maven("io.micronaut.grpc:micronaut-grpc-server-runtime"),
]

MICRONAUT_KT_PLUGINS = [
    "//tools/defs/kt/plugins:serialization",
]

def kt_jvm_library(
        name,
        srcs = [],
        deps = [],
        **kwargs):
    """Designate a JVM-side Kotlin library."""
    _kt_jvm_library(
        name = name,
        srcs = srcs,
        deps = deps,
        **kwargs
    )

def kt_js_library(
        name,
        srcs = [],
        deps = [],
        **kwargs):
    """Designate a common Kotlin library."""
    _kt_js_library(
        name = name,
        srcs = srcs,
        deps = deps,
        **kwargs
    )

def kt_jvm_binary(
        name,
        **kwargs):
    """Designate a Kotlin-enabled Java server binary."""
    _kt_jvm_binary(
        name = name,
        **kwargs
    )

def micronaut_library(
        name,
        srcs = [],
        deps = [],
        runtime_deps = [],
        plugins = [],
        **kwargs):
    """Designate a Micronaut (JVM) library."""
    kt_jvm_library(
        name = name,
        srcs = srcs,
        runtime_deps = runtime_deps + MICRONAUT_RUNTIME_DEPS,
        deps = deps + MICRONAUT_DEPS,
        plugins = plugins + MICRONAUT_KT_PLUGINS,
    )

def micronaut_service(
        name,
        srcs = [],
        deps = [],
        runtime_deps = [],
        plugins = [],
        protos = [],
        services = [],
        **kwargs):
    """Designate a Micronaut (JVM) library."""
    micronaut_library(
        name = name,
        srcs = srcs,
        runtime_deps = runtime_deps,
        deps = deps + MICRONAUT_SERVICE_DEPS + [
            _javaproto(p)
            for p in (protos + services)
        ] + [
            _javagrpc(s)
            for s in services
        ],
        plugins = plugins,
    )

maven = _maven
javaproto = _javaproto
javagrpc = _javagrpc
