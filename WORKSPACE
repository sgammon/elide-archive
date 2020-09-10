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

workspace(
  name = "gust",
  managed_directories = {"@npm": ["node_modules"]})

load("//defs:build.bzl", "install_dependencies")
load("//defs:config.bzl", "CHROMIUM", "FIREFOX", "SAUCE", "GRAALVM_VERSION", "GRAALVM_JDK_VERSION", "K8S_VERSION")
install_dependencies()

load("//defs:workspace.bzl", "setup_workspace")
setup_workspace()

#
# Apple Platforms
#

load(
    "@build_bazel_rules_swift//swift:repositories.bzl",
    "swift_rules_dependencies",
)

swift_rules_dependencies()

load(
    "@build_bazel_apple_support//lib:repositories.bzl",
    "apple_support_dependencies",
)

apple_support_dependencies()


#
# Protobuf
#

load(
    "@com_google_protobuf//:protobuf_deps.bzl",
    "protobuf_deps",
)

protobuf_deps()


#
# Extensions
#

## NodeJS
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "build_bazel_rules_nodejs",
    sha256 = "6a67a8a1bf6fddc9113f73471029b819eef4575c3a936a4a01d57e411894d692",
    urls = ["https://github.com/bazelbuild/rules_nodejs/releases/download/2.0.2/rules_nodejs-2.0.2.tar.gz"],
)

load("@build_bazel_rules_nodejs//:index.bzl",
     "node_repositories",
     "yarn_install")

node_repositories(
    package_json = ["//:package.json"],
    node_version = "10.13.0",
    yarn_version = "1.12.1")

yarn_install(
    name = "npm",
    package_json = "//:package.json",
    yarn_lock = "//:yarn.lock")

load("@npm//:install_bazel_dependencies.bzl",
     "install_bazel_dependencies")

install_bazel_dependencies(suppress_warning=True)

## SASS
load("@io_bazel_rules_sass//:package.bzl", "rules_sass_dependencies")
rules_sass_dependencies()

load("@io_bazel_rules_sass//:defs.bzl", "sass_repositories")
sass_repositories()

## Kotlin
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
register_toolchains("//defs/toolchain/kt:kotlin_toolchain")

## GraalVM
load("@rules_graal//graal:graal_bindist.bzl", "graal_bindist_repository")

graal_bindist_repository(
  name = "graal",
  version = GRAALVM_VERSION,
  java_version = GRAALVM_JDK_VERSION,
)

## Java Repos/Deps
load("//defs/toolchain/java:repos.bzl", "gust_java_repositories")
gust_java_repositories()

load("@maven//:defs.bzl", "pinned_maven_install")
pinned_maven_install()

## Go
load("@io_bazel_rules_go//go:deps.bzl", "go_register_toolchains", "go_rules_dependencies")
load("@bazel_gazelle//:deps.bzl", "gazelle_dependencies")

go_rules_dependencies()
go_register_toolchains()
gazelle_dependencies()

## Web Testing
load("@io_bazel_rules_webtesting//web:repositories.bzl", "web_test_repositories")
web_test_repositories()

load("@io_bazel_rules_webtesting//web:go_repositories.bzl", "go_repositories", "go_internal_repositories")
go_repositories()
go_internal_repositories()

load("@io_bazel_rules_webtesting//web:java_repositories.bzl", "java_repositories")
java_repositories()

load("@io_bazel_rules_webtesting//web:py_repositories.bzl", "py_repositories")
py_repositories()

load("@io_bazel_rules_webtesting//web/versioned:browsers-0.3.2.bzl", "browser_repositories")
browser_repositories(chromium=CHROMIUM, firefox=FIREFOX, sauce=SAUCE)

## Docker
load("@io_bazel_rules_docker//repositories:repositories.bzl", container_repositories = "repositories")
load("@io_bazel_rules_docker//java:image.bzl", _java_image_repos = "repositories")
load("@io_bazel_rules_docker//go:image.bzl", _go_image_repos = "repositories")
load("@io_bazel_rules_docker//nodejs:image.bzl", _nodejs_image_repos = "repositories")
load("@io_bazel_rules_docker//python:image.bzl", _py_image_repos = "repositories")

load("@io_bazel_rules_docker//repositories:deps.bzl",
     container_deps = "deps")

container_repositories()
container_deps()

load("@io_bazel_rules_docker//repositories:pip_repositories.bzl", "pip_deps")
pip_deps()

_go_image_repos()
_py_image_repos()
_java_image_repos()
_nodejs_image_repos()

## JS Interop
load("@com_google_jsinterop_generator//build_defs:rules.bzl", "setup_jsinterop_generator_workspace")
load("@com_google_jsinterop_generator//build_defs:repository.bzl", "load_jsinterop_generator_repo_deps")
load_jsinterop_generator_repo_deps()
setup_jsinterop_generator_workspace()

## API Codegen
load("@com_google_api_codegen//rules_gapic/java:java_gapic_repositories.bzl", "java_gapic_repositories")
java_gapic_repositories()

load("@com_google_api//:repository_rules.bzl", "switched_rules_by_language")
switched_rules_by_language(
    name = "com_google_googleapis_imports",
    grpc = True)

## Karma Setup
load("@npm//@bazel/karma:package.bzl", "npm_bazel_karma_dependencies")
npm_bazel_karma_dependencies()

## Web Testing Setup
load("@io_bazel_rules_webtesting//web:repositories.bzl", "web_test_repositories")
 
web_test_repositories()
 
load("@io_bazel_rules_webtesting//web/versioned:browsers-0.3.2.bzl", "browser_repositories")
 
browser_repositories(
    chromium = True,
    firefox = True,
)

## Toolchains (RBE)
load("@bazel_toolchains//rules:rbe_repo.bzl", "rbe_autoconfig")
rbe_autoconfig(name = "rbe_default")

## Stardoc
load("@io_bazel_stardoc//:setup.bzl", "stardoc_repositories")
stardoc_repositories()

## Python
load("@rules_python//python:repositories.bzl", "py_repositories")
py_repositories()

load("@rules_python//python:pip.bzl", "pip_repositories")
pip_repositories()

load("@rules_python//python:pip.bzl", pip_import = "pip3_import")

pip_import(
    name = "protobuf_py",
    requirements = "@build_stack_rules_proto//python/requirements:protobuf.txt")

load("@protobuf_py//:requirements.bzl", proto_pip_install = "pip_install")
proto_pip_install()

pip_import(
    name = "py",
    requirements = "//defs/toolchain/python:requirements_base.txt")

pip_import(
    name = "werkzeug",
    requirements = "//defs/toolchain/python:requirements_werkzeug.txt")

pip_import(
    name = "grpc_python_dependencies",
    requirements = "@com_github_grpc_grpc//:requirements.bazel.txt")

load("@grpc_python_dependencies//:requirements.bzl", grpc_pip_install="pip_install")
grpc_pip_install()

load("//defs/toolchain/python:repos.bzl", "gust_python_repositories")
gust_python_repositories()

load("@build_stack_rules_proto//python:deps.bzl", "python_proto_compile")
python_proto_compile()

## Swift
load("@build_stack_rules_proto//swift:deps.bzl", "swift_proto_library")
swift_proto_library()

## gRPC Core
load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", "grpc_deps", "grpc_test_only_deps")
grpc_deps()
grpc_test_only_deps()

load("@com_github_grpc_grpc//bazel:grpc_extra_deps.bzl", "grpc_extra_deps")
grpc_extra_deps()

## gRPC Java
load("@io_grpc_java//:repositories.bzl", "grpc_java_repositories")
grpc_java_repositories()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()

load("@io_bazel_rules_k8s//k8s:k8s_go_deps.bzl", k8s_go_deps = "deps")
k8s_go_deps()

## Java Containers
load("@io_bazel_rules_docker//container:container.bzl", "container_pull")

container_pull(
    name = "java_base",
    registry = "gcr.io",
    repository = "distroless/java",
    digest = "sha256:0ce06c40e99e0dce26bdbcec30afe7a890a57bbd250777bd31ff2d1b798c7809",
)

container_pull(
    name = "native_base",
    registry = "us.gcr.io",
    repository = "elide-ai/base/alpine",
    digest = "sha256:ce4ee2c86f73fb0ed996c23a7ff60f33edd3ba760b1dc411a174240424524de4",
)

container_pull(
    name = "node_base",
    registry = "us.gcr.io",
    repository = "elide-ai/base/node",
    digest = "sha256:de50770615dcf1112f3116166ec72bf6fcf4c163d90c1a060503d5f5c5b73dcd",
)

container_pull(
    name = "envoy_base",
    registry = "index.docker.io",
    repository = "envoyproxy/envoy-alpine",
    digest = "sha256:19f3b361450e31f68b46f891b0c8726041739f44ab9b90aecbca5f426c0d2eaf",
)

## K8S Setup
load("@io_bazel_rules_k8s//toolchains/kubectl:kubectl_configure.bzl", "kubectl_configure")
kubectl_configure(
    name="k8s_config",
    build_srcs = True,
    k8s_commit = "v%s" % K8S_VERSION,
    k8s_prefix = "kubernetes-%s" % K8S_VERSION,
    k8s_sha256 = "e091944229641c5b2b2a6ac57767802548b50830cf6710bc676e851bd8233f74",
    k8s_repo_tools_commit = "df02ded38f9506e5bbcbf21702034b4fef815f2f",
    k8s_repo_tools_prefix = "repo-infra-df02ded38f9506e5bbcbf21702034b4fef815f2f",
    k8s_repo_tools_sha = "4a8384320fba401cbf21fef177aa113ed8fe35952ace98e00b796cac87ae7868",
)

load("@io_bazel_rules_k8s//k8s:k8s.bzl", "k8s_repositories")
k8s_repositories()

load("@io_bazel_rules_k8s//k8s:k8s.bzl", "k8s_defaults")
k8s_defaults(
  name = "k9",
  kind = "deployment",
  cluster = "$(cluster)",
)

## Brotli Setup
load("@org_brotli//java:repositories.bzl", "load_brotli_repositories")
load_brotli_repositories()

