
load(
    "@io_bazel_rules_webtesting//web:java_repositories.bzl",
    "RULES_WEBTESTING_ARTIFACTS",
)

load(
    "@rules_jvm_external//:defs.bzl",
    "maven_install",
)


REPOSITORIES = [
    "https://jcenter.bintray.com/",
    "https://maven.google.com",
    "https://repo1.maven.org/maven2",
]

TEST_ARTIFACTS = [
    # Add test artifacts here.
] + RULES_WEBTESTING_ARTIFACTS


def _gust_java_deps():

    """ Install Gust runtime Java dependencies. """

    maven_install(
        artifacts = TEST_ARTIFACTS,
        repositories = REPOSITORIES,
        maven_install_json = "@//:maven_install.json"
    )


gust_java_repositories = _gust_java_deps
