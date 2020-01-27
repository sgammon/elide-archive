
load(
    "@rules_java//java:defs.bzl",
    _java_library = "java_library",
    _java_test = "java_test",
)

load(
    "@io_bazel_rules_webtesting//web:java.bzl",
    _java_web_test_suite = "java_web_test_suite",
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
        deps = (deps or DEFAULT_TEST_DEPS) + INJECTED_TEST_DEPS,
    )

    _java_web_test_suite(
        name = name,
        srcs = srcs,
        test_class = test_class,
        browsers = browsers or DEFAULT_BROWSERS,
        local = local,
        deps = (deps or DEFAULT_TEST_DEPS) + INJECTED_TEST_DEPS,
    )


java_test = _java_test
browser_test_java = _browser_test_java
