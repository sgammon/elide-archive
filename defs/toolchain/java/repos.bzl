
load(
    "@io_bazel_rules_webtesting//web:java_repositories.bzl",
    "RULES_WEBTESTING_ARTIFACTS",
)

load(
    "@rules_jvm_external//:defs.bzl",
    "maven_install",
)

load(
    "@rules_jvm_external//:specs.bzl",
    "maven",
)

load(
    "//defs:config.bzl",
    "GRAALVM_VERSION",
)


ASM_VERSION = "7.0"
SLF4J_VERSION = "1.7.26"
ANNOTATIONS_VERSION = "1.3.2"

SOY_VERSION = "2019-10-08"
GUAVA_VERSION = "25.1-jre"
FINDBUGS_VERSION = "3.0.2"
PROTOBUF_VERSION = "3.11.3"

SVM_VERSION = "19.2.1"

MICRONAUT_VERSION = "1.3.0"
MICRONAUT_TEST_VERSION = "1.1.2"
MICRONAUT_REDIS_VERSION = "1.2.0"


REPOSITORIES = [
    "https://jcenter.bintray.com/",
    "https://maven.google.com",
    "https://repo1.maven.org/maven2",
    "https://dl.bintray.com/micronaut/core-releases-local",
]

BUILD_ARTIFACTS = [
    "org.ow2.asm:asm:%s" % ASM_VERSION,
    "org.slf4j:slf4j-api:%s" % SLF4J_VERSION,
    "javax.annotation:javax.annotation-api:%s" % ANNOTATIONS_VERSION,
]

MICRONAUT_BUILD_ARTIFACTS = [
    "com.google.guava:guava:%s" % GUAVA_VERSION,
    "com.google.code.findbugs:jsr305:%s" % FINDBUGS_VERSION,
    "com.google.protobuf:protobuf-java:%s" % PROTOBUF_VERSION,
    "io.micronaut:micronaut-aop:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-core:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-http:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-http-client:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-inject:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-inject-java:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-runtime:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-validation:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-http-server:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-http-server-netty:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-graal:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-views:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-router:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-session:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-tracing:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-security:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-multitenancy:%s" % MICRONAUT_VERSION,
    "io.micronaut.configuration:micronaut-redis-lettuce:%s" % MICRONAUT_REDIS_VERSION,
    maven.artifact("com.google.template", "soy", SOY_VERSION, neverlink = True),
]

RUNTIME_ARTIFACTS = [
    # No base runtime artifacts yet.
    "org.slf4j:slf4j-jdk14:%s" % SLF4J_VERSION,
]

MICRONAUT_RUNTIME_ARTIFACTS = [
    "io.micronaut:micronaut-runtime:%s" % MICRONAUT_VERSION,
]

TEST_ARTIFACTS = [
    # No base testing artifacts yet.
] + RULES_WEBTESTING_ARTIFACTS

MICRONAUT_TEST_ARTIFACTS = [
    maven.artifact("io.micronaut.test", "micronaut-test-core", MICRONAUT_TEST_VERSION, testonly = True),
    maven.artifact("io.micronaut.test", "micronaut-test-kotlintest", MICRONAUT_TEST_VERSION, testonly = True),
]


def _gust_java_deps(micronaut = True):

    """ Install Gust runtime Java dependencies. """

    artifacts = BUILD_ARTIFACTS + RUNTIME_ARTIFACTS + TEST_ARTIFACTS
    if micronaut:
        artifacts += [i for i in (
            MICRONAUT_BUILD_ARTIFACTS +
            MICRONAUT_RUNTIME_ARTIFACTS +
            MICRONAUT_TEST_ARTIFACTS) if i not in artifacts]

    maven_install(
        artifacts = artifacts,
        repositories = REPOSITORIES,
        maven_install_json = "@gust//:maven_install.json",
        generate_compat_repositories = True,
    )


gust_java_repositories = _gust_java_deps
