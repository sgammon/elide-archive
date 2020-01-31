
load(
    "@rules_java//java:defs.bzl",
    _java_test = "java_test",
    _java_library = "java_library",
)

load(
    "@io_bazel_rules_kotlin//kotlin:kotlin.bzl",
    _kt_jvm_test = "kt_jvm_test",
    _kt_jvm_library = "kt_jvm_library",
)

load(
    "@io_bazel_rules_webtesting//web:java.bzl",
    _java_web_test_suite = "java_web_test_suite",
)

load(
    "@io_bazel_rules_webtesting//web:kotlin.bzl",
    _kotlin_web_test_suite = "kotlin_web_test_suite",
)

load(
    "//defs/toolchain/java:rules.bzl",
    "dedupe_deps_",
    "ensure_types_",
    "INJECTED_MICRONAUT_DEPS",
    "INJECTED_MICRONAUT_RUNTIME_DEPS",
)

load(
    "//defs/toolchain:deps.bzl",
    "maven",
)

DEFAULT_BROWSERS = [
    "@io_bazel_rules_webtesting//browsers:chromium-local",
    "@io_bazel_rules_webtesting//browsers:firefox-local",
]

DEFAULT_TEST_DEPS = [
    # No test deps yet.
]

INJECTED_TEST_DEPS = [
    "@junit_junit",
    "@org_seleniumhq_selenium_selenium_api",
    "@io_bazel_rules_webtesting//java/com/google/testing/web",
    "//defs/toolchain/java/plugins:micronaut",
]

INJECTED_KOTLIN_TEST_DEPS = [
    "@com_github_jetbrains_kotlin//:kotlin-test",
    "//defs/toolchain/java/plugins:micronaut",
]

INJECTED_MICRONAUT_TEST_DEPS = [
    maven("io.micronaut:micronaut-http"),
    maven("io.micronaut:micronaut-http-client"),
    maven("io.micronaut.test:micronaut-test-core"),
    maven("io.micronaut.test:micronaut-test-kotlintest"),
]

INJECTED_MICRONAUT_TEST_RUNTIME_DEPS = [
    # No runtime deps yet.
]


def _browser_test_java(name,
                       srcs,
                       test_class,
                       deps = None,
                       browsers = None,
                       local = True):

    """ Run a full-stack test using `WebDriver`, and a Java test spec.
        Uses the default set of browsers if unspecified (Chromium and Gecko). """

    _java_library(
        name = "%s-java" % name,
        srcs = srcs,
        deps = dedupe_deps_((deps or DEFAULT_TEST_DEPS) + INJECTED_TEST_DEPS),
    )

    _java_web_test_suite(
        name = name,
        srcs = srcs,
        test_class = test_class,
        browsers = browsers or DEFAULT_BROWSERS,
        local = local,
        deps = dedupe_deps_((deps or DEFAULT_TEST_DEPS) + INJECTED_TEST_DEPS),
    )


def _jdk_test(name,
              srcs,
              test_class,
              deps = [],
              runtime_deps = [],
              **kwargs):

    """ Wrap a regular Java test so it can support Kotlin. """

    if srcs[0].endswith(".kt"):
        # process as kotlin
        ensure_types_(srcs, ".kt")
        _kt_jvm_test(
            name = name,
            srcs = srcs,
            test_class = test_class,
            deps = dedupe_deps_(deps),
            runtime_deps = dedupe_deps_(runtime_deps),
            **kwargs
        )

    else:
        ensure_types_(srcs, ".java")
        _java_test(
            name = name,
            srcs = srcs,
            test_class = test_class,
            deps = dedupe_deps_(deps),
            runtime_deps = dedupe_deps_(runtime_deps),
            **kwargs
        )


def _micronaut_test(name,
                    srcs,
                    test_class,
                    deps = [],
                    runtime_deps = [],
                    browser = False,
                    browsers = None,
                    local = True,
                    resources = [],
                    config = str(Label("@gust//java/gust:application.yml")),
                    template_loader = str(Label("@gust//java/gust/backend:TemplateProvider")),
                    **kwargs):

    """ Run a backend test on a Micronaut app. Basically wraps a regular JDK test,
        but with injected Micronaut dependencies and plugins. """

    if not browser:
        if srcs[0].endswith(".kt"):
            _kt_jvm_test(
                name = name,
                srcs = srcs,
                test_class = test_class,
                deps = dedupe_deps_((deps or DEFAULT_TEST_DEPS) + [template_loader]
                       + INJECTED_TEST_DEPS + INJECTED_MICRONAUT_TEST_DEPS + INJECTED_KOTLIN_TEST_DEPS),
               runtime_deps = dedupe_deps_(
                    INJECTED_TEST_DEPS + INJECTED_MICRONAUT_RUNTIME_DEPS + runtime_deps + [template_loader]),
                resources = resources,
                **kwargs
            )
        else:
            ensure_types_(srcs, ".java")
            _jdk_test(
                name = name,
                srcs = srcs,
                test_class = test_class,
                deps = dedupe_deps_((deps or DEFAULT_TEST_DEPS) + [template_loader]
                    + INJECTED_MICRONAUT_DEPS + INJECTED_MICRONAUT_TEST_DEPS),
                runtime_deps = dedupe_deps_(
                    INJECTED_TEST_DEPS + INJECTED_MICRONAUT_RUNTIME_DEPS + runtime_deps + [template_loader]),
                resources = resources,
                classpath_resources = [config],
                **kwargs
            )
    else:
        if srcs[0].endswith(".kt"):
            ensure_types_(srcs, ".kt")
            _kotlin_web_test_suite(
                name = name,
                srcs = srcs,
                test_class = test_class,
                browsers = browsers or DEFAULT_BROWSERS,
                local = local,
                deps = dedupe_deps_((deps or DEFAULT_TEST_DEPS) + [template_loader]
                        + INJECTED_TEST_DEPS + INJECTED_MICRONAUT_TEST_DEPS + INJECTED_KOTLIN_TEST_DEPS),
                runtime_deps = dedupe_deps_(
                    INJECTED_TEST_DEPS + INJECTED_MICRONAUT_RUNTIME_DEPS + runtime_deps + [template_loader]),
                resources = resources,
                **kwargs
            )
        else:
            ensure_types_(srcs, ".java")
            _java_web_test_suite(
                name = name,
                srcs = srcs,
                test_class = test_class,
                browsers = browsers or DEFAULT_BROWSERS,
                local = local,
                deps = dedupe_deps_((deps or DEFAULT_TEST_DEPS) + [template_loader]
                                        + INJECTED_TEST_DEPS + INJECTED_MICRONAUT_TEST_DEPS),
                runtime_deps = dedupe_deps_(
                    INJECTED_TEST_DEPS + INJECTED_MICRONAUT_RUNTIME_DEPS + runtime_deps + [template_loader]),
                resources = resources,
                classpath_resources = [config],
                **kwargs
            )


jdk_test = _java_test
micronaut_test = _micronaut_test
browser_test_java = _browser_test_java
