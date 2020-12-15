##
# Copyright © 2020, The Gust Framework Authors. All rights reserved.
#
# The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
# are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
# this code in object or source form requires and implies consent and agreement to that license in principle and
# practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
# Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
# Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
# by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
# is strictly forbidden except in adherence with assigned license requirements.
##

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
    "//defs/toolchain/java:repos.bzl",
    "JUNIT_JUPITER_GROUP_ID",
    "JUNIT_PLATFORM_GROUP_ID",
    "JUNIT_JUPITER_ARTIFACT_ID_LIST",
    "JUNIT_PLATFORM_ARTIFACT_ID_LIST",
    "JUNIT_EXTRA_DEPENDENCIES",
    "format_maven_jar_dep_name",
)

load(
    "//defs/toolchain/context:props.bzl",
    _annotate_jvm_flags = "annotate_jvm_flags",
)

load(
    "//defs/toolchain:deps.bzl",
    "maven",
)


ENABLE_JUNIT5 = True

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
    "@gust//defs/toolchain/java/plugins:micronaut",
    "@com_google_template_soy",
]

INJECTED_KOTLIN_TEST_DEPS = [
#    "@com_github_jetbrains_kotlin//:kotlin-test",
#    maven("io.micronaut.test:micronaut-test-kotlintest"),
    "@gust//defs/toolchain/java/plugins:micronaut",
]

INJECTED_MICRONAUT_TEST_DEPS = [
    maven("io.micronaut:micronaut-runtime"),
    maven("io.micronaut:micronaut-http"),
    maven("io.micronaut:micronaut-http-client"),
    maven("io.micronaut.test:micronaut-test-core"),
    maven("io.micronaut.test:micronaut-test-junit5"),
]

INJECTED_MICRONAUT_TEST_RUNTIME_DEPS = [
    # No runtime deps yet.
]


def _browser_test_java(name,
                       srcs,
                       test_class,
                       deps = None,
                       browsers = None,
                       local = True,
                       jvm_flags = [],
                       lib_args = {},
                       **kwargs):

    """ Run a full-stack test using `WebDriver`, and a Java test spec.
        Uses the default set of browsers if unspecified (Chromium and Gecko). """

    computed_jvm_flags = _annotate_jvm_flags(jvm_flags)

    _java_library(
        name = "%s-java" % name,
        srcs = srcs,
        deps = dedupe_deps_((deps or DEFAULT_TEST_DEPS) + INJECTED_TEST_DEPS),
        testonly = True,
        exported_plugins = [
          "@gust//defs/toolchain/java/plugins:micronaut",
        ],
        **lib_args,
    )

    _java_web_test_suite(
        name = name,
        srcs = srcs,
        test_class = test_class,
        browsers = browsers or DEFAULT_BROWSERS,
        local = local,
        deps = dedupe_deps_((deps or DEFAULT_TEST_DEPS) + INJECTED_TEST_DEPS),
        jvm_flags = computed_jvm_flags,
        testonly = True,
        **kwargs,
    )


def _jdk_test(name,
              srcs,
              test_class = None,
              deps = [],
              runtime_deps = [],
              jvm_flags = [],
              enable_junit5 = ENABLE_JUNIT5,
              testonly = True,
              **kwargs):

    """ Wrap a regular Java test so it can support Kotlin. """

    computed_jvm_flags = _annotate_jvm_flags(jvm_flags)

    if srcs[0].endswith(".kt"):
        # process as kotlin
        ensure_types_(srcs, ".kt")
        _kt_jvm_test(
            name = name,
            srcs = srcs,
            test_class = test_class,
            deps = dedupe_deps_(deps),
            runtime_deps = dedupe_deps_(runtime_deps),
            jvm_flags = computed_jvm_flags,
            testonly = testonly,
            **kwargs
        )

    else:
        ensure_types_(srcs, ".java")
        if enable_junit5:
            _java_junit5_test(
                name = name,
                srcs = srcs,
                test_class = test_class,
                deps = deps,
                runtime_deps = runtime_deps,
                jvm_flags = computed_jvm_flags,
                testonly = testonly,
                **kwargs
            )
        else:
            _java_test(
                name = name,
                srcs = srcs,
                test_class = test_class,
                deps = dedupe_deps_(deps),
                runtime_deps = dedupe_deps_(runtime_deps),
                jvm_flags = computed_jvm_flags,
                testonly = testonly,
                **kwargs
            )


def _java_junit5_test(name,
                      srcs,
                      test_package = None,
                      deps = [],
                      runtime_deps = [],
                      testonly = True,
                      classpath_resources = None,
                      **kwargs):

    """ Establish Bazel targets and configuration for a Junit5 test case. """

    FILTER_KWARGS = [
        "main_class",
        "use_testrunner",
        "args",
    ]

    for arg in FILTER_KWARGS:
        if arg in kwargs.keys():
            kwargs.pop(arg)

    junit_console_args = []
    if test_package:
        junit_console_args += ["--select-package", test_package]
    else:
        fail("must specify 'test_package'")

    _java_test(
        name = name,
        srcs = srcs,
        use_testrunner = False,
        main_class = "org.junit.platform.console.ConsoleLauncher",
        args = junit_console_args,
        testonly = testonly,
        deps = dedupe_deps_(deps + [
            maven("org.junit.jupiter:junit-jupiter-api"),
            maven("org.junit.jupiter:junit-jupiter-engine"),
            maven("org.junit.jupiter:junit-jupiter-params"),
            maven("org.junit.platform:junit-platform-suite-api"),
            maven("org.apiguardian:apiguardian-api"),
            maven("org.opentest4j:opentest4j"),
            maven("com.google.guava:guava"),
        ]),
        runtime_deps = dedupe_deps_(runtime_deps + [
            maven("org.junit.platform:junit-platform-commons"),
            maven("org.junit.platform:junit-platform-console"),
            maven("org.junit.platform:junit-platform-engine"),
            maven("org.junit.platform:junit-platform-launcher"),
            maven("org.junit.platform:junit-platform-suite-api"),
            maven("ch.qos.logback:logback-classic"),
        ]),
        classpath_resources = (classpath_resources or [
            "@gust//javatests:logback.xml",
        ]),
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
                    jvm_flags = [],
                    config = str(Label("@gust//java/gust:application.yml")),
                    template_loader = str(Label("@gust//java/gust/backend:TemplateProvider")),
                    **kwargs):

    """ Run a backend test on a Micronaut app. Basically wraps a regular JDK test,
        but with injected Micronaut dependencies and plugins. """

    if not browser:
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
            jvm_flags = jvm_flags,
            **kwargs
        )
    else:
        computed_jvm_flags = _annotate_jvm_flags(jvm_flags)
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
                jvm_flags = computed_jvm_flags,
                testonly = True,
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
                jvm_flags = computed_jvm_flags,
                testonly = True,
                **kwargs
            )


jdk_test = _jdk_test
micronaut_test = _micronaut_test
java_junit5_test = _java_junit5_test
browser_test_java = _browser_test_java
