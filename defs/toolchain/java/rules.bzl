
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
    "//defs/toolchain:schema.bzl",
    "JAVAPROTO_POSTFIX_",
    "CLOSUREPROTO_POSTFIX_",
)

load(
    "//defs/toolchain/context:props.bzl",
    _annotate_defs_dict = "annotate_defs_dict",
    _annotate_jvm_flags = "annotate_jvm_flags",
)

load(
    "//defs/toolchain:deps.bzl",
    "maven",
)


INJECTED_MICRONAUT_DEPS = [
    "@gust//defs/toolchain/java/plugins:micronaut",
    maven("com.google.guava:guava"),
    maven("com.google.template:soy"),
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
    maven("io.micronaut.configuration:micronaut-redis-lettuce"),
]

INJECTED_MICRONAUT_RUNTIME_DEPS = [
    "@gust//java:entrypoint",
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
                deps = None,
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
                 srcs = [],
                 deps = None,
                 data = [],
                 **kwargs):

    """ Generate a JDK binary, with support for both Java and Kotlin. """

    if len(srcs) > 0 and srcs[0].endswith(".kt"):
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
                       srcs = [],
                       deps = [],
                       proto_deps = [],
                       runtime_deps = [],
                       data = [],
                       templates = [],
                       exports = [],
                       **kwargs):

    """ Wraps a regular JDK library with injected Micronaut dependencies and plugins. """

    _jdk_library(
        name = name,
        srcs = srcs,
        deps = _dedupe_deps(deps + INJECTED_MICRONAUT_DEPS) + [("%s-%s" % (
           p, JAVAPROTO_POSTFIX_
        )) for p in proto_deps],
        runtime_deps = _dedupe_deps(runtime_deps + INJECTED_MICRONAUT_RUNTIME_DEPS + [
          ("%s-java" % t) for t in templates
        ] + [
          ("%s-java_jcompiled" % t) for t in templates
        ]),
        exports = _dedupe_deps([("%s-%s" % (
          p, JAVAPROTO_POSTFIX_
        )) for p in proto_deps] + [
          ("%s-java" % t) for t in templates
        ] + [
          ("%s-java_jcompiled" % t) for t in templates
        ] + exports),
        data = data,
        **kwargs
    )


def _micronaut_controller(name,
                          srcs,
                          deps = [],
                          protos = [],
                          templates = [],
                          proto_deps = [],
                          runtime_deps = [],
                          data = [],
                          **kwargs):

    """ Wraps a regular Micronaut JDK library which is intended to be used as a
        controller, potentially with templates to inject. """

    _micronaut_library(
        name = name,
        srcs = srcs,
        proto_deps = protos,
        templates = templates,
        runtime_deps = runtime_deps,
        data = data,
        **kwargs
    )


def _micronaut_application(name,
                           main_class = None,
                           config = str(Label("@gust//java/gust:application.yml")),
                           template_loader = str(Label("@gust//java/gust/backend:TemplateProvider")),
                           srcs = [],
                           controllers = [],
                           deps = None,
                           proto_deps = [],
                           data = [],
                           resources = [],
                           runtime_deps = [],
                           jvm_flags = [],
                           defs = {},
                           **kwargs):

    """ Wraps a regular JDK application with injected Micronaut dependencies and plugins. """

    computed_jvm_flags = _annotate_jvm_flags([i for i in jvm_flags], defs)

    if len(srcs) > 0:
        computed_deps = _dedupe_deps(deps + INJECTED_MICRONAUT_DEPS + controllers + template_loader)
        extra_runtime_deps = [template_loader]
    else:
        computed_deps = None
        extra_runtime_deps = (
            _dedupe_deps((deps or []) + INJECTED_MICRONAUT_DEPS + controllers + [template_loader]))

    _jdk_binary(
        name = name,
        srcs = srcs,
        deps = computed_deps,
        runtime_deps = _dedupe_deps(runtime_deps + INJECTED_MICRONAUT_RUNTIME_DEPS + [("%s-%s" % (
          p, JAVAPROTO_POSTFIX_
        )) for p in proto_deps] + controllers + extra_runtime_deps),
        data = data,
        resources = resources,
        classpath_resources = [config],
        main_class = main_class or "gust.backend.Application",
        jvm_flags = computed_jvm_flags,
        **kwargs
    )


## Meant for private use.
dedupe_deps_ = _dedupe_deps
ensure_types_ = _ensure_types

jdk_binary = _jdk_binary
jdk_library = _jdk_library
micronaut_library = _micronaut_library
micronaut_controller = _micronaut_controller
micronaut_application = _micronaut_application
