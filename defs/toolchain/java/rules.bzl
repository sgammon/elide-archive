
load(
    "@rules_java//java:defs.bzl",
    _java_binary = "java_binary",
    _java_library = "java_library",
)

load(
    "@io_bazel_rules_kotlin//kotlin:kotlin.bzl",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_library = "kt_jvm_library",
)

load(
    "//defs/toolchain:deps.bzl",
    "maven",
)


INJECTED_MICRONAUT_DEPS = [
    "//defs/toolchain/java/plugins:micronaut",
    maven("io.micronaut:micronaut-aop"),
    maven("io.micronaut:micronaut-core"),
    maven("io.micronaut:micronaut-http"),
    maven("io.micronaut:micronaut-runtime"),
    maven("io.micronaut:micronaut-http-client"),
    maven("io.micronaut:micronaut-inject"),
    maven("io.micronaut:micronaut-inject-java"),
    maven("io.micronaut:micronaut-validation"),
    maven("io.micronaut:micronaut-http-server"),
    maven("io.micronaut:micronaut-http-server-netty"),
    maven("io.micronaut:micronaut-graal"),
    maven("io.micronaut:micronaut-views"),
    maven("io.micronaut:micronaut-router"),
    maven("io.micronaut:micronaut-session"),
    maven("io.micronaut:micronaut-tracing"),
    maven("io.micronaut:micronaut-security"),
    maven("io.micronaut:micronaut-multitenancy"),
]

INJECTED_MICRONAUT_RUNTIME_DEPS = [
    maven("io.micronaut:micronaut-runtime"),
]


def _dedupe_deps(deps):

    """ Deduplicate a set of string labels in a deps argument. """

    if deps == None:
        return deps
    depindex = []
    for i in deps:
        if i not in depindex:
            depindex.append(i)
    return depindex


def _ensure_types(srcs, ext):

    """ Ensure that all srcs are of a certain file extension, or fail. """

    if any([(not s.endswith(ext)) for s in srcs]):
        fail("All sources must either be Kotlin or Java. " +
             "Expected file extension was '%s'." % ext)


def _jdk_binary(name,
                srcs = [],
                deps = [],
                data = [],
                **kwargs):

    """ Generate a JDK binary, with built-in support for Kotlin. """

    if len(srcs) > 0 and srcs[0].endswith(".kt"):
        # process as kotlin
        _ensure_types(srcs, ".kt")
        _kt_jvm_binary(
            name = name,
            srcs = srcs,
            deps = _dedupe_deps(deps),
            data = data,
            **kwargs
        )

    else:
        # process as java
        _ensure_types(srcs, ".java")
        _java_binary(
            name = name,
            srcs = srcs,
            deps = _dedupe_deps(deps),
            data = data,
            **kwargs
        )


def _jdk_library(name,
                 srcs,
                 deps = [],
                 data = [],
                 **kwargs):

    """ Generate a JDK binary, with support for both Java and Kotlin. """

    if srcs[0].endswith(".kt"):
        # process as kotlin
        _ensure_types(srcs, ".kt")
        _kt_jvm_library(
            name = name,
            srcs = srcs,
            deps = _dedupe_deps(deps),
            data = data,
            **kwargs
        )

    else:
        # process as java
        _ensure_types(srcs, ".java")
        _java_library(
            name = name,
            srcs = srcs,
            deps = _dedupe_deps(deps),
            data = data,
            **kwargs
        )


def _micronaut_library(name,
                       srcs,
                       deps = [],
                       runtime_deps = [],
                       data = [],
                       **kwargs):

    """ Wraps a regular JDK library with injected Micronaut dependencies and plugins. """

    _jdk_library(
        name = name,
        srcs = srcs,
        deps = _dedupe_deps(deps + INJECTED_MICRONAUT_DEPS),
        runtime_deps = _dedupe_deps(runtime_deps + INJECTED_MICRONAUT_RUNTIME_DEPS),
        data = data,
        **kwargs
    )


def _micronaut_application(name,
                           main_class,
                           srcs = [],
                           deps = [],
                           data = [],
                           runtime_deps = [],
                           **kwargs):

    """ Wraps a regular JDK application with injected Micronaut dependencies and plugins. """

    if len(srcs) > 0:
        computed_deps = _dedupe_deps(deps + INJECTED_MICRONAUT_DEPS)
    else:
        computed_deps = None

    _jdk_binary(
        name = name,
        srcs = srcs,
        deps = computed_deps,
        runtime_deps = _dedupe_deps(runtime_deps + INJECTED_MICRONAUT_RUNTIME_DEPS),
        data = data,
        main_class = main_class,
        **kwargs
    )


## Meant for private use.
dedupe_deps_ = _dedupe_deps
ensure_types_ = _ensure_types

jdk_binary = _jdk_binary
jdk_library = _jdk_library
micronaut_library = _micronaut_library
micronaut_application = _micronaut_application
