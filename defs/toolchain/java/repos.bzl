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
    "//defs/toolchain:deps.bzl",
    _maven = "maven",
)

load(
    "//defs:config.bzl",
    _GRAALVM_VERSION = "GRAALVM_VERSION",
    _PROTOBUF_VERSION = "PROTOBUF_VERSION",
)

FETCH_SOURCES = True
STRICT_DEPENDENCIES = True

ASM_VERSION = "7.0"
SLF4J_VERSION = "1.7.30"
GSON_VERSION = "2.8.6"

GRAALVM_VERSION = _GRAALVM_VERSION

LOGBACK_VERSION = "1.2.3"
PROTOBUF_VERSION = _PROTOBUF_VERSION

VALIDATION_VERSION = "2.0.0.Final"

JUNIT_JUPITER_VERSION = "5.6.0"
JUNIT_PLATFORM_VERSION = "1.6.0"

GAX_VERSION = "1.60.0"
NETTY_VERSION = "4.1.51.Final"
RXJAVA_VERSION = "2.2.20"
PICOCLI_VERSION = "4.2.0"
REACTIVE_VERSION = "1.0.3"
THREETEN_VERSION = "1.5.0"
GAPI_COMMON_VERSION = "1.10.1"
GCLOUD_API_VERSION = "0.142.0"
GCLOUD_GRPC_VERSION = "1.93.10"
GCLOUD_TASKS_VERSION = "1.30.7"
GCLOUD_PUBSUB_VERSION = "1.108.7"
GCLOUD_STORAGE_VERSION = "1.113.2"
GCLOUD_FIRESTORE_VERSION = "2.1.0"
GCLOUD_MONITORING_VERSION = "2.0.7"
COMMON_PROTOS_VERSION = "2.0.1"
KOTLIN_TEST_VERSION = "3.4.2"
J2OBJC_ANNOTATIONS_VERSION = "1.3"
GAUTH_CREDENTIALS_VERSION = "0.22.0"
GUAVA_FAILUREACCESS_VERSION = "1.0.1"
ANIMAL_SNIFFER_VERSION = "1.19"
ANDROID_ANNOTATIONS_VERSION = "4.1.1.4"
PERFMARK_VERSION = "0.23.0"

GRPC_JAVA_VERSION = "1.33.1"
TOMCAT_ANNOTATIONS_VERSION = "6.0.53"
OPENTRACING_VERSION = "0.2.3"

MICRONAUT_VERSION = "2.1.3"
MICRONAUT_DATA_VERSION = "2.1.1"
MICRONAUT_GRPC_VERSION = "2.1.0"
MICRONAUT_TEST_VERSION = "2.2.1"
MICRONAUT_REDIS_VERSION = "3.0.0"
MICRONAUT_CACHE_VERSION = "2.1.0"
MICRONAUT_SECURITY_VERSION = "2.1.2"

GRPC_EXCLUSIONS = [
      maven.exclusion(
         artifact = "grpc-api",
         group = "io.grpc",
     ),
      maven.exclusion(
         artifact = "grpc-auth",
         group = "io.grpc",
     ),
     maven.exclusion(
         artifact = "grpc-core",
         group = "io.grpc",
     ),
     maven.exclusion(
         artifact = "grpc-netty",
         group = "io.grpc",
     ),
     maven.exclusion(
         artifact = "grpc-okhttp",
         group = "io.grpc",
     ),
     maven.exclusion(
          artifact = "grpc-services",
          group = "io.grpc",
      ),
     maven.exclusion(
         artifact = "grpc-auth",
         group = "io.grpc",
     ),
     maven.exclusion(
         artifact = "grpc-stub",
         group = "io.grpc",
     ),
     maven.exclusion(
         artifact = "grpc-context",
         group = "io.grpc",
     ),
     maven.exclusion(
         artifact = "grpc-protobuf",
         group = "io.grpc",
     ),
     maven.exclusion(
         artifact = "grpc-protobuf-lite",
         group = "io.grpc",
     ),
]

SOY_EXCLUSIONS = [
    maven.exclusion(
       artifact = "types",
       group = "com.google.common.html.types",
   ),
   maven.exclusion(
      artifact = "soy",
      group = "com.google.template",
  ),
]

GCLOUD_EXCLUSIONS = GRPC_EXCLUSIONS + SOY_EXCLUSIONS + [
   maven.exclusion(
      artifact = "guava",
      group = "com.google.guava",
  ),
]


REPOSITORIES = [
    "https://repo1.maven.org/maven2",
    "https://dl.bintray.com/micronaut/core-releases-local",
    "https://jcenter.bintray.com/",
    "https://maven.google.com",
]

BUILD_ARTIFACTS = [
    "org.ow2.asm:asm:%s" % ASM_VERSION,
    "org.slf4j:slf4j-api:%s" % SLF4J_VERSION,
    "javax.validation:validation-api:%s" % VALIDATION_VERSION,
    "org.apache.tomcat:annotations-api:%s" % TOMCAT_ANNOTATIONS_VERSION,
    "com.google.code.gson:gson:%s" % GSON_VERSION,
]

# These are not auto-injected.
EXTRA_BUILD_ARTIFACTS = [
    "info.picocli:picocli:%s" % PICOCLI_VERSION,
    "info.picocli:picocli-codegen:%s" % PICOCLI_VERSION,
]

GRPC_BUILD_ARTIFACTS = [
    "io.grpc:grpc-core:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-auth:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-api:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-services:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-stub:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-context:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-protobuf:%s" % GRPC_JAVA_VERSION,
    "io.grpc:grpc-netty:%s" % GRPC_JAVA_VERSION,
    "com.google.api.grpc:proto-google-common-protos:%s" % COMMON_PROTOS_VERSION,
    "com.google.api.grpc:grpc-google-common-protos:%s" % COMMON_PROTOS_VERSION,
    "com.google.j2objc:j2objc-annotations:%s" % J2OBJC_ANNOTATIONS_VERSION,
    "com.google.auth:google-auth-library-credentials:%s" % GAUTH_CREDENTIALS_VERSION,
    "com.google.guava:failureaccess:%s" % GUAVA_FAILUREACCESS_VERSION,
    "org.codehaus.mojo:animal-sniffer-annotations:%s" % ANIMAL_SNIFFER_VERSION,
    "io.perfmark:perfmark-api:%s" % PERFMARK_VERSION,
    "io.perfmark:perfmark-java9:%s" % PERFMARK_VERSION,
]

ANDROID_ARTIFACTS = [
    "com.google.android:annotations:%s" % ANDROID_ANNOTATIONS_VERSION,
]

JUNIT_JUPITER_GROUP_ID = "org.junit.jupiter"
JUNIT_JUPITER_ARTIFACT_ID_LIST = [
    "junit-jupiter-api",
    "junit-jupiter-engine",
    "junit-jupiter-params",
]

JUNIT_PLATFORM_GROUP_ID = "org.junit.platform"
JUNIT_PLATFORM_ARTIFACT_ID_LIST = [
    "junit-platform-commons",
    "junit-platform-console",
    "junit-platform-engine",
    "junit-platform-launcher",
    "junit-platform-reporting",
    "junit-platform-suite-api",
]

JUNIT_EXTRA_DEPENDENCIES = [
    ("org.apiguardian", "apiguardian-api", "1.0.0"),
    ("org.opentest4j", "opentest4j", "1.1.1"),
]

def _micronaut_artifact(coordinates):

    """ Inject exclusions for Micronaut. """

    split = coordinates.split(":")
    return maven.artifact(split[0], split[1], split[2], exclusions = (
        SOY_EXCLUSIONS +
        GRPC_EXCLUSIONS))

MICRONAUT_COORDINATES = [
    "io.micronaut:micronaut-aop",
    "io.micronaut:micronaut-core",
    "io.micronaut:micronaut-http",
    "io.micronaut:micronaut-http-client",
    "io.micronaut:micronaut-inject",
    "io.micronaut:micronaut-inject-java",
    "io.micronaut:micronaut-runtime",
    "io.micronaut:micronaut-validation",
    "io.micronaut:micronaut-http-server",
    "io.micronaut:micronaut-http-server-netty",
    "io.micronaut:micronaut-buffer-netty",
    "io.micronaut:micronaut-graal",
    "io.micronaut:micronaut-router",
    "io.micronaut:micronaut-tracing",
    "io.micronaut:micronaut-messaging",
    "io.micronaut:micronaut-multitenancy",
    "io.micronaut:micronaut-websocket",
    "io.micronaut:micronaut-session",
]

MICRONAUT_EXTRAS = [
    ("io.netty:netty-buffer", NETTY_VERSION),
    ("io.netty:netty-transport", NETTY_VERSION),
    ("io.netty:netty-resolver", NETTY_VERSION),
    ("io.netty:netty-common", NETTY_VERSION),
    ("io.netty:netty-codec", NETTY_VERSION),
    ("io.netty:netty-codec-http", NETTY_VERSION),
    ("io.netty:netty-codec-http2", NETTY_VERSION),
    ("io.netty:netty-codec-socks", NETTY_VERSION),
    ("io.netty:netty-handler", NETTY_VERSION),
    ("io.netty:netty-handler-proxy", NETTY_VERSION),
    ("io.micronaut.security:micronaut-security", MICRONAUT_SECURITY_VERSION),
    ("io.micronaut.security:micronaut-security-session", MICRONAUT_SECURITY_VERSION),
    ("io.micronaut.security:micronaut-security-annotations", MICRONAUT_SECURITY_VERSION),
    ("io.micronaut.data:micronaut-data-processor", MICRONAUT_DATA_VERSION),
    ("io.micronaut.grpc:micronaut-grpc-runtime", MICRONAUT_GRPC_VERSION),
    ("io.micronaut.grpc:micronaut-grpc-annotation", MICRONAUT_GRPC_VERSION),
    ("io.micronaut.grpc:micronaut-protobuff-support", MICRONAUT_GRPC_VERSION),
    ("io.micronaut.redis:micronaut-redis-lettuce", MICRONAUT_REDIS_VERSION),
    ("io.micronaut.cache:micronaut-cache-caffeine", MICRONAUT_CACHE_VERSION)
]

MICRONAUT_BUILD_ARTIFACTS = ["%s:%s" % (i, MICRONAUT_VERSION) for i in MICRONAUT_COORDINATES] + [
    _micronaut_artifact("%s:%s" % x) for x in MICRONAUT_EXTRAS
] + [
    ## Reactive Java
    _micronaut_artifact("org.reactivestreams:reactive-streams:%s" % REACTIVE_VERSION),
    _micronaut_artifact("io.reactivex.rxjava2:rxjava:%s" % RXJAVA_VERSION),
]

RUNTIME_ARTIFACTS = [
    # No base runtime artifacts yet.
    "ch.qos.logback:logback-classic:%s" % LOGBACK_VERSION,
]

MICRONAUT_RUNTIME_ARTIFACTS = [
    "io.micronaut:micronaut-runtime:%s" % MICRONAUT_VERSION,
    "io.opentracing.contrib:opentracing-grpc:%s" % OPENTRACING_VERSION,
]

GOOGLE_COORDINATES = [
    ("com.google.api:gax", GAX_VERSION),
    ("com.google.api:gax-grpc", GAX_VERSION),
    ("com.google.api:api-common", GAPI_COMMON_VERSION),
    ("org.threeten:threetenbp", THREETEN_VERSION),
]

GOOGLE_CLOUD_COORDINATES = [
    ("com.google.cloud:google-cloud-core", GCLOUD_GRPC_VERSION),
    ("com.google.cloud:google-cloud-core-grpc", GCLOUD_GRPC_VERSION),
    ("com.google.cloud:google-cloud-tasks", GCLOUD_TASKS_VERSION),
    ("com.google.cloud:google-cloud-pubsub", GCLOUD_PUBSUB_VERSION),
    ("com.google.cloud:google-cloud-storage", GCLOUD_STORAGE_VERSION),
    ("com.google.cloud:google-cloud-firestore", GCLOUD_FIRESTORE_VERSION),
    ("com.google.cloud:google-cloud-monitoring", GCLOUD_MONITORING_VERSION),
]

GOOGLE_ARTIFACTS = [
    # Google API Extensions (GAX)
    maven.artifact(i[0].split(":")[0], i[0].split(":")[1], i[1], exclusions = GCLOUD_EXCLUSIONS)
    for i in GOOGLE_COORDINATES] + [

    # Google Cloud
    maven.artifact(i[0].split(":")[0], i[0].split(":")[1], i[1], exclusions = GCLOUD_EXCLUSIONS)
    for i in ([("com.google.cloud:google-cloud-bom", GCLOUD_API_VERSION)] + GOOGLE_CLOUD_COORDINATES)
]

TEST_ARTIFACTS = [
    # None yet.
] + RULES_WEBTESTING_ARTIFACTS

MICRONAUT_TEST_ARTIFACTS = [
#    maven.artifact("io.kotlintest", "kotlintest-core", KOTLIN_TEST_VERSION, testonly = True),
#    maven.artifact("io.kotlintest", "kotlintest-extensions", KOTLIN_TEST_VERSION, testonly = True),
#    maven.artifact("io.kotlintest", "kotlintest-runner-jvm", KOTLIN_TEST_VERSION, testonly = True),
#    maven.artifact("io.kotlintest", "kotlintest-runner-junit5", KOTLIN_TEST_VERSION, testonly = True),
#    maven.artifact("io.kotlintest", "kotlintest-runner-console", KOTLIN_TEST_VERSION, testonly = True),
    maven.artifact("io.micronaut.test", "micronaut-test-core", MICRONAUT_TEST_VERSION, testonly = True),
    maven.artifact("io.micronaut.test", "micronaut-test-kotlintest", MICRONAUT_TEST_VERSION, testonly = True),
    maven.artifact("io.micronaut.test", "micronaut-test-junit5", MICRONAUT_TEST_VERSION, testonly = True),
]

def junit_jupiter_java_repositories(version = JUNIT_JUPITER_VERSION):

    """Imports dependencies for JUnit Jupiter"""

    artifactset = []
    for artifact_id in JUNIT_JUPITER_ARTIFACT_ID_LIST:
        artifactset.append(maven.artifact(
            JUNIT_JUPITER_GROUP_ID,
            artifact_id,
            version,
            testonly = True,
        ))

    for t in JUNIT_EXTRA_DEPENDENCIES:
        artifactset.append(maven.artifact(
            testonly = True,
            *t,
        ))
    return artifactset

def junit_platform_java_repositories(version = JUNIT_PLATFORM_VERSION):

    """Imports dependencies for JUnit Platform"""

    artifactset = []
    for artifact_id in JUNIT_PLATFORM_ARTIFACT_ID_LIST:
        artifactset.append(maven.artifact(
            JUNIT_PLATFORM_GROUP_ID,
            artifact_id,
            version,
            testonly = True,
        ))
    return artifactset

def _format_maven_jar_name(group_id, artifact_id):
    return ("%s_%s" % (group_id, artifact_id)).replace(".", "_").replace("-", "_")

def _format_maven_jar_dep_name(group_id, artifact_id):
    return "@%s//jar" % _format_maven_jar_name(group_id, artifact_id)


def _gust_java_deps(
        app_artifacts = [],
        app_repositories = [],
        app_fetch_sources = True,
        app_excludes = [],
        app_overrides = [],
        micronaut = True,
        android = True,
        junit5 = True):

    """ Install Gust runtime Java dependencies. """

    artifacts = BUILD_ARTIFACTS + RUNTIME_ARTIFACTS + TEST_ARTIFACTS
    if micronaut:
        artifacts += [i for i in (
            GOOGLE_ARTIFACTS +
            GRPC_BUILD_ARTIFACTS +
            MICRONAUT_BUILD_ARTIFACTS +
            MICRONAUT_RUNTIME_ARTIFACTS +
            EXTRA_BUILD_ARTIFACTS +
            MICRONAUT_TEST_ARTIFACTS) if i not in artifacts]
    if android:
        artifacts += [i for i in (
            ANDROID_ARTIFACTS
        ) if i not in artifacts]

    artifacts += (app_artifacts or [])

    if junit5:
        artifacts += (
            junit_platform_java_repositories() +
            junit_jupiter_java_repositories())

    maven_install(
        artifacts = artifacts,
        repositories = REPOSITORIES + (app_repositories or []),
        fetch_sources = app_fetch_sources,
        maven_install_json = "//:maven_install.json",
        generate_compat_repositories = True,
        strict_visibility = STRICT_DEPENDENCIES,
        excluded_artifacts = [
            "com.google.template:soy",
            "com.google.common.html.types:types",
        ] + (app_excludes or []),
        override_targets = dict(([
            ("io.micronaut:micronaut-views", "@io_micronaut_micronaut_views"),
            ("io.micronaut:micronaut-views-soy", "@io_micronaut_micronaut_views_soy"),
            ("com.google.guava:guava", "@com_google_guava"),
            ("com.google.guava", "@com_google_guava"),
            ("com.google.protobuf:protobuf-java", "@com_google_protobuf//:protobuf_java"),
            ("com.google.protobuf:protobuf-javalite", "@com_google_protobuf//:protobuf_javalite"),
            ("com.google.protobuf:protobuf-java-util", "@com_google_protobuf//:protobuf_java_util"),
            ("com.google.grpc:grpc-auth", "@io_grpc_java//auth:auth"),
            ("com.google.grpc:grpc-api", "@io_grpc_java//api:api"),
            ("com.google.grpc:grpc-core", "@io_grpc_java//core:core"),
            ("com.google.grpc:grpc-stub", "@io_grpc_java//stub:stub"),
            ("com.google.grpc:grpc-protobuf", "@io_grpc_java//protobuf:protobuf"),
            ("com.google.grpc:grpc-context", "@io_grpc_java//context:context"),
            ("com.google.grpc:grpc-netty", "@io_grpc_java//netty:netty"),
            ("com.google.grpc:grpc-netty-shaded", "@io_grpc_java//netty-shaded:netty-shaded"),
            ("com.google.template:soy", "@com_google_template_soy"),
            ("com.google.common.html.types:types", "@com_google_template_soy"),
            ("com.google.code.findbugs:jsr305", "@com_google_code_findbugs_jsr305"),
            ("com.google.closure:stylesheets", "@com_google_closure_stylesheets"),
            ("javax.inject:javax.inject", "@javax_inject"),
            ("javax.annotation:javax.annotation-api", "@javax_annotation_api"),
        ] + (app_overrides or []))),
    )


OVERRIDE_DEPS = [
    "@io_micronaut_micronaut_views",
    "@io_micronaut_micronaut_views_soy",
    "@com_google_guava",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_javalite",
    "@io_grpc_java//auth:auth",
    "@io_grpc_java//api:api",
    "@io_grpc_java//core:core",
    "@io_grpc_java//stub:stub",
    "@io_grpc_java//protobuf:protobuf",
    "@io_grpc_java//context:context",
    "@io_grpc_java//netty:netty",
    "@com_google_template_soy",
    "@com_google_code_findbugs_jsr305",
    "@com_google_closure_stylesheets",
    "@javax_inject",
    "@javax_annotation_api",
]


def _clean_versions(deps):

    """ Clean version specifications from passed-in dependencies. """

    return [_maven(":".join(dep.split(":")[0:2])) for dep in deps]


ALL_DEPENDENCIES = (_clean_versions(
    BUILD_ARTIFACTS +
    GRPC_BUILD_ARTIFACTS +
    MICRONAUT_COORDINATES +
    RUNTIME_ARTIFACTS +
    [x[0] for x in GOOGLE_COORDINATES] +
    [x[0] for x in GOOGLE_CLOUD_COORDINATES] +
    [x[0] for x in MICRONAUT_EXTRAS]) +

    OVERRIDE_DEPS)


gust_java_repositories = _gust_java_deps
format_maven_jar_name = _format_maven_jar_name
format_maven_jar_dep_name = _format_maven_jar_dep_name
