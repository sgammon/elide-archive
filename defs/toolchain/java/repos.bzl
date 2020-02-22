
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

FETCH_SOURCES = True
STRICT_DEPENDENCIES = True

ASM_VERSION = "7.0"
SLF4J_VERSION = "1.7.26"

PROTOBUF_VERSION = "3.11.4"

GRPC_JAVA_VERSION = "1.26.0"
OPENTRACING_VERSION = "0.2.1"

MICRONAUT_VERSION = "1.3.1"
MICRONAUT_GRPC_VERSION = "1.1.1"
MICRONAUT_TEST_VERSION = "1.1.2"
MICRONAUT_REDIS_VERSION = "1.2.0"
MICRONAUT_SECURITY_VERSION = "1.3.0"


REPOSITORIES = [
    "https://jcenter.bintray.com/",
    "https://maven.google.com",
    "https://repo1.maven.org/maven2",
    "https://dl.bintray.com/micronaut/core-releases-local",
]

BUILD_ARTIFACTS = [
    "org.ow2.asm:asm:%s" % ASM_VERSION,
    "org.slf4j:slf4j-api:%s" % SLF4J_VERSION,
]

MICRONAUT_BUILD_ARTIFACTS = [
    "io.grpc:grpc-core:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-auth:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-api:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-stub:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-context:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-protobuf:%s" % GRPC_JAVA_VERSION,
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
    "io.micronaut:micronaut-views-soy:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-router:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-session:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-tracing:%s" % MICRONAUT_VERSION,
    "io.micronaut:micronaut-security:%s" % MICRONAUT_SECURITY_VERSION,
    "io.micronaut:micronaut-multitenancy:%s" % MICRONAUT_VERSION,
    "io.micronaut.grpc:micronaut-grpc-runtime:%s" % MICRONAUT_GRPC_VERSION,
    "io.micronaut.grpc:micronaut-grpc-annotation:%s" % MICRONAUT_GRPC_VERSION,
    "io.micronaut.grpc:micronaut-protobuff-support:%s" % MICRONAUT_GRPC_VERSION,
    "io.micronaut.configuration:micronaut-redis-lettuce:%s" % MICRONAUT_REDIS_VERSION,

    maven.artifact("io.micronaut", "micronaut-views", MICRONAUT_VERSION, exclusions = [
        maven.exclusion(
           artifact = "types",
           group = "com.google.common.html.types",
       ),
       maven.exclusion(
          artifact = "soy",
          group = "com.google.template",
      ),
    ]),
]

RUNTIME_ARTIFACTS = [
    # No base runtime artifacts yet.
    "org.slf4j:slf4j-jdk14:%s" % SLF4J_VERSION,
]

MICRONAUT_RUNTIME_ARTIFACTS = [
    "io.micronaut:micronaut-runtime:%s" % MICRONAUT_VERSION,
    "io.opentracing.contrib:opentracing-grpc:%s" % OPENTRACING_VERSION,
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
        fetch_sources = FETCH_SOURCES,
        maven_install_json = "@gust//:maven_install.json",
        generate_compat_repositories = True,
        strict_visibility = STRICT_DEPENDENCIES,
        excluded_artifacts = [
            "com.google.template:soy",
            "com.google.common.html.types:types",
        ],
        override_targets = {
            "com.google.guava:guava": "@com_google_guava",
            "com.google.template:soy": "@com_google_template_soy",
            "com.google.common.html.types:types": "@com_google_template_soy",
            "com.google.code:gson": "@com_google_code_gson",
            "com.google.code.findbugs:jsr305": "@com_google_code_findbugs_jsr305",
            "com.google.closure:stylesheets": "@com_google_closure_stylesheets",
            "javax.inject:javax.inject": "@javax_inject",
            "javax.annotation:javax.annotation-api": "@javax_annotation_api",
        },
    )


gust_java_repositories = _gust_java_deps
