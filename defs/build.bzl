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
    "//defs:tools.bzl",
    "dependencies",
    "http_archive",
    "git_repository",
)

load(
    "//defs/toolchain:deps.bzl",
    "maven",
)

DEPS = {
    # Bazel: Skylib
    "bazel_skylib": {
        "type": "github",
        "repo": "bazelbuild/bazel-skylib",
        "target": "2ec2e6d715e993d96ad6222770805b5bd25399ae",
        "seal": "e388adedb28227eeccdb0168d90956c87f2f350938086e48706508b7691f343d"},

    # Common Tools for Bazel
    "bazel_common": {
        "type": "github",
        "repo": "sgammon/bazel-common",
        "target": "7aef387013f54dbdd28a3a2b10f234bb5a18a892",
        "local": "/workspace/GUST/vendor/bazel/common",
        "seal": "1ce4440462a39d35927fbac005b5eb67d49af53450b2d9c42dfded4df619ecc8"},

    # Bazel: Packaging
    "rules_pkg": {
        "type": "github",
        "repo": "bazelbuild/rules_pkg",
        "strip_prefix": "/pkg",
        "target": "36e8363031c761525a6a917cf7ecb84f6ccb32eb",
        "seal": "080e14e7faf579e673ac1ddfdc3091e757435f229e7ed9fba183efc419229aa4"},

    # Bazel: Stardoc
    "io_bazel_stardoc": {
        "type": "github",
        "repo": "bazelbuild/stardoc",
        "target": "87545335ef7fb248051a7e049e88177ac8168c03",
        "seal": "6266a305d6e7b794a39ca4563cc173af25719e7eeefa9a99c1d478f1f44db431"},

    # Rules: Closure
    "io_bazel_rules_closure": {
        "type": "github",
        "forceLocal": False,
        "repo": "sgammon/rules_closure",
        "target": "076e68d3e298669b57887ed3e8ce6fa03dd1285e",
        "local": "/Users/sam.g/Workspace/rules_closure",
        "seal": "3825b6b5c67c49bdc5c867293d34b410bfe7f08e233ce42e566db2b299e2f9ec"},

    # Rules: Protobuf
    "rules_proto": {
        "type": "github",
        "repo": "bazelbuild/rules_proto",
        "target": "7e4afce6fe62dbff0a4a03450143146f9f2d7488",
        "seal": "8e7d59a5b12b233be5652e3d29f42fba01c7cbab09f6b3a8d0a57ed6d1e9a0da"},

    # Rules: Python
    "rules_python": {
        "type": "github",
        "repo": "bazelbuild/rules_python",
        "target": "748aa53d7701e71101dfd15d800e100f6ff8e5d1",
        "seal": "64a3c26f95db470c32ad86c924b23a821cd16c3879eed732a7841779a32a60f8"},

    # Rules: Java
    "rules_java": {
        "type": "github",
        "repo": "bazelbuild/rules_java",
        "target": "9eb38ebffbaf4414fa3d2292b28e604a256dd5a5",
        "seal": "a0adff084a3e8ffac3b88582b208897cd615a29620aa5416337df93a3d3bfd15"},

    # Rules: GraalVM
    "rules_graal": {
        "type": "github",
        "repo": "sgammon/rules_graal",
        "local": "/Users/sam.g/Workspace/rules_graal",
        "target": "9e709f955b77e6a576acda7459a2cd9c43e9d9f3",
        "seal": "14faf2709ac9fe7baff509d35ee40b007676e72596cfc0a61704ba5634ded82a"},

    # Rules: Kotlin
    "io_bazel_rules_kotlin": {
        "type": "github",
        "repo": "bazelbuild/rules_kotlin",
        "forceLocal": False,
        "local": "/Users/sam.g/Workspace/rules_kotlin",
        "target": "495db022827bb498462b9ef486e2c97110dc1f00",
        "seal": "9fa74052e2ef421a4e83c74f40a1117457c241f4ee4f0b69a740cf763e3e4a8a"},

    # Rules: Apple (iOS/macOS/tvOS)
    "build_bazel_rules_apple": {
        "type": "github",
        "repo": "bazelbuild/rules_apple",
        "target": "c4868493c3e53e0ddb048c52701c3bd146db2ead",
        "seal": "131950895970fbccda8f10608827247b2306a043c2d2b5a2d6ba3389884e980c"},

    # Rules: Apple (Swift)
    "build_bazel_rules_swift": {
        "type": "github",
        "repo": "bazelbuild/rules_swift",
        "target": "d916037ac3a918ce7231c355945246d265cb78a3",
        "seal": "bb5e35d74b3f5dfd08614b0d845371ebf92a5f717a35945a2b7b35be5429b5ca"},

    # Deps: Apple Support
    "build_bazel_apple_support": {
        "type": "github",
        "repo": "bazelbuild/apple_support",
        "target": "f7f2b6d7c952f3cf6bdcedce6a0a2a40a27ff596",
        "seal": "45c848a9416ce042be0d9f213deeef0a05feb4ae262ca73cf622c14b7a7ab467"},

    # Rules: Go
    "io_bazel_rules_go": {
        "type": "github",
        "repo": "bazelbuild/rules_go",
        "target": "fb47d755261e501137acccbad4d195efeacbf001",
        "seal": "4cbce3f9909451b97bdc70ace57a07d7c7aac7723621a56e5f1e340e15fac3c8"},

    # Bazel: Gazelle
    "bazel_gazelle": {
        "type": "github",
        "repo": "bazelbuild/bazel-gazelle",
        "target": "d4eb4d207628c149044e3b5dc9e3c5aaaba79f11",
        "seal": "0d32479dbef7f5ae80ce7b0afc2456c14d84d6d7bbdaaf79bb021de41121313b"},

    # Rules: Web Testing
    "io_bazel_rules_webtesting": {
        "type": "github",
        "repo": "sgammon/rules_webtesting",
        "target": "6236473d758f9d88c3ecd2df7df104fb335429ba",
        "local": "/workspace/GUST/vendor/bazel/rules_webtesting",
        "seal": "591017fbe81b8487118f164f041b0e37331f3d8386277a90d6522b30e16b5609"},

    # Rules: SCSS/SASS
    "io_bazel_rules_sass": {
        "type": "github",
        "repo": "bazelbuild/rules_sass",
        "target": "6d723f01fc9bb19846d22ace42255a91cf0ebdfa",
        "repo_mapping": {"@build_bazel_rules_sass_deps" : "@npm"},
        "seal": "fc5525d9b2297f00a85d392629f02e86f7d5b2523028d39c52941cf9690a2200"},

    # Rules: Docker
    "io_bazel_rules_docker": {
        "type": "github",
        "repo": "bazelbuild/rules_docker",
        "target": "204e77bf954269d5e40c2850dd76dad4adbf2edf",
        "seal": "57e0bf27fa5606b39c5a6e30008968359c8d671cda2f0b65a23bf8e892b2d7a8"},

    # Rules: Kubernetes
    "io_bazel_rules_k8s": {
        "type": "github",
        "repo": "bazelbuild/rules_k8s",
        "target": "6e58661104f1d962bc9b03757618f3db647d9b6f",
        "seal": "7e4358fa276d5be0bf7e2ac5629f66dfe6eab4a779e7a5d189872dbb1209081b"},

    # Google: J2CL (Java-to-Closure)
    "com_google_j2cl": {
        "type": "github",
        "repo": "google/j2cl",
        "target": "4fda30b07470dfc87555847af14bdbba60a41774",
        "local": "/workspace/GUST/vendor/bazel/j2cl",
        "seal": "4518030355ae19e7ba991978ed32e061edda05ea1abd8b1a1fae930596ee29b0"},

    # Google: Elemental2
    "com_google_elemental2": {
        "type": "github",
        "repo": "google/elemental2",
        "target": "281ab6c570b4d000d6786b2d8ef54e9d0169391a",
        "seal": "bd67f90b779773a49baf2786063ad050a716dd31cd3381ab267c76f5a30d11b3"},

    # Google: JS Interop (Base)
    "com_google_jsinterop_base": {
        "type": "github",
        "repo": "google/jsinterop-base",
        "target": "fffe9c4de072e812fa6d4af385bda3969fb692fa",
        "seal": "cd807fb9457db9d07aaad73e7cb16595d3a14a96de1c29e4f185751e381a0712"},

    # Google: JS Interop (Generator)
    "com_google_jsinterop_generator": {
        "type": "github",
        "repo": "google/jsinterop-generator",
        "target": "e4248fa7c99ee65fc797ea20307d7edbd0dd715a",
        "seal": "c05d0978f487d5aa65986caece052d877b9a9576a15f915b463a53249e57b0a2"},

    # Google: JS Interop (Generator)
    "com_google_jsinterop_annotations": {
        "type": "github",
        "repo": "google/jsinterop-annotations",
        "target": "b5e8b46e46a68f030e63c53140072334c4e7ee9d",
        "seal": "0ac20baedf3590140fb9382a21ccd44ba1fbcea718e778639ce5e20d903c8629"},

    # Google: API (Core)
    "com_google_api": {
        "type": "github",
        "repo": "googleapis/googleapis",
        "target": "5359df5297982d551ac5dcc20c8a29de04c70090",
        "seal": "8326935ff048f6c4bed686d0f05713f572da520af278da0375d47255f1a59fdd"},

    # Google: API (Codegen)
    "com_google_api_codegen": {
        "type": "github",
        "repo": "googleapis/gapic-generator",
        "target": "857e2aab8017ecbb2abc9adc02d8571c02f94b3b",
        "seal": "bdaa14564a7eb408f98ba788018eb5d9a5330aaa5b5cfcdd36f3f7fe4477b489"},

    # BuildStack: Protobuf Rules
    "build_stack_rules_proto": {
        "type": "github",
        "repo": "stackb/rules_proto",
        "target": "b2913e6340bcbffb46793045ecac928dcf1b34a5",
        "seal": "d456a22a6a8d577499440e8408fc64396486291b570963f7b157f775be11823e"},

    # Normalize CSS
    "org_normalize_css": {
        "type": "github",
        "repo": "necolas/normalize.css",
        "target": "fc091cce1534909334c1911709a39c22d406977b",
        "overlay": "normalize.bzl",
        "seal": "53933ba214ab20b501a4ed1f08796ef716a5f1ccb8e0f9977a27f802cb09bcda"},

    # Common Protocol Buffers
    "proto_common": {
        "type": "github",
        "repo": "googleapis/api-common-protos",
        "target": "f37c0ecc55f19b1675448e4bad70fd45c93f5b8f",
        "overlay": "proto_common.bzl",
        "seal": "20f5e27c83f417c34bf36f38686435dd9e5ca454bef47c08c7d359c37ae26fcf"},

    # Safe HTML Types
    "safe_html_types": {
        "type": "github",
        "repo": "google/safe-html-types",
        "target": "8507735457ea41a37dfa027fb176d49d5783c4ba",
        "overlay": "safe_html_types.bzl",
        "seal": "2356090e7632f49ea581bb6f8808fa038a7433d433f3e8d7045a36f81fb39d65"},

    # gRPC: Core
    "com_github_grpc_grpc": {
        "type": "github",
        "repo": "sgammon/grpc",
        "target": "7e741fc12cc361906b1adc70bc3f7960eb3598d4",
        "seal": "c3b9395345828256b77a9472022966df590457e9ed8f351e104adeea6667754c"},

    # gRPC: Java
    "io_grpc_java": {
        "type": "github",
        "repo": "grpc/grpc-java",
        "target": "6cb4d90e6bbe0081d42d88bc83a8f4648087596e",
        "seal": "ba7b097d3e151cc0fcdd3e1c48d487eafa2acde258bfb044d8549f48c5c50940"},

    # gRPC: Web
    "com_github_grpc_grpc_web": {
        "type": "github",
        "repo": "grpc/grpc-web",
        "forceLocal": False,
        "local": "/workspace/GUST/vendor/grpc/web",
        "target": "3d921fffb7ce29f03329e411711d0751328c761a",
        "seal": "fc12f7cea2b3a5e52f231348d99709006c344652d4511117157ee7484ee42ae9"},

    # Compression: Brotli
    "org_brotli": {
        "type": "github",
        "repo": "sgammon/brotli",
        "target": "21378a50ab56ffe1989a7f119399d2616df519db",
        "seal": "2b1383b73512a9179e4333a03ef8bf3096fc06c2ac47afc4593f3b8ea02cbf65"},

    # Envoy
    "envoy": {
        "type": "github",
        "repo": "envoyproxy/envoy",
        "target": "c39a22e1ee744f4e6031c0b53f7ccd2b6165e29f",
        "seal": "57a1d595c0440e4f6e17559004b56e8a04e84eb96321702f0da7e065ab495a1d"},

    # Kubernetes: Build Tools
    "io_kubernetes_build": {
        "type": "github",
        "repo": "kubernetes/repo-infra",
        "target": "dccb5aa645ea455a1c8c4c97a2a72b640036efc9",
        "seal": "f4a99337b43b742d35f8592055af72aeb5ea0cdfa1acadd99d9e972d9aedd2b1"},

    # Firebase: JS SDK
    "com_google_firebase": {
        "type": "github",
        "repo": "firebase/firebase-js-sdk",
        "overlay": "firebase.bzl",
        "target": "c9560d3103032caa6dcd2fd6daed2b119bd7b713",
        "seal": "c9560d3103032caa6dcd2fd6daed2b119bd7b713"},

    # Firebase: Java SDK
    "com_google_firebase_java": {
        "type": "github",
        "repo": "firebase/firebase-admin-java",
        "target": "c40a69d04c73e158a0ea1e75b1ea7400c3544c92",
        "seal": "ece3fe87e25c2e09b1d58a6314ae12e52d48a51e4944ff0623f07e6c5f2ceffa"},

    # Firebase: UI (Web)
    "com_google_firebase_ui_web": {
        "type": "github",
        "repo": "bloombox/firebaseui-web",
        "overlay": "firebase-ui-web.bzl",
        "target": "4e01abe6793f83ee3ce46f721586bd77c7670eef",
        "seal": "96a159c26600fc520dfd5ffee1d3137099a28290a71b29516f42be11db107af8"},

    # Google: Incremental DOM
    "com_google_javascript_incremental_dom": {
        "type": "github",
        "repo": "bloombox/incremental-dom",
        "overlay": "idom.bzl",
        "target": "8866a9e57a216eaa6f3dac94240f437a573842ab",
        "local": "/workspace/Bloombox/Frontend/IncrementalDOM",
        "seal": "82c041a1a81368b6cac5ebab3cde4da212364674b2d74d4cb0931f7068f7636e"},

    # Google: Closure Stylesheets
    "com_google_closure_stylesheets": {
        "type": "java",
        "licenses": ["notice"],
        "targets": ["https://storage.googleapis.com/elide-software/closure-stylesheets-1.6.0-b16.jar"],
        "seal": "142123dc2c1c56c085380cd4d5b7c37965754a1805d0f30cfd5e8bf37c0a6ca0",
        "deps": [
            "@args4j",
            "@com_google_javascript_closure_compiler",
            "@com_google_code_gson",
            "@com_google_guava",
            "@com_google_code_findbugs_jsr305",
        ],
        "inject": "\n".join([
            "java_binary(",
            "    name = \"ClosureCommandLineCompiler\",",
            "    main_class = \"com.google.common.css.compiler.commandline.ClosureCommandLineCompiler\",",
            "    output_licenses = [\"unencumbered\"],",
            "    runtime_deps = [\":com_google_closure_stylesheets\"],",
            ")",
        ]),
    },

    # JavaX: Annotations API
    "javax_annotation_api": {
        "type": "java",
        "licenses": ["notice"],  # Apache 2.0
        "seal": "e04ba5195bcd555dc95650f7cc614d151e4bcd52d29a10b8aa2197f3ab89ab9b",
        "targets": [
          "https://repo1.maven.org/maven2/javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.jar",
        ],
    },

    # Google: Soy
    "com_google_template_soy": {
        "type": "java",
        "licenses": ["notice"],
        "targets": ["https://storage.googleapis.com/elide-software/frontend/soy/soy-lib-b29.jar"],
        "seal": "f2d8a4c079caa18135cddd05c379e5fa210a439986f960a871f3881ddd8455cc",
        "deps": [
            "@args4j",
            "@com_google_code_findbugs_jsr305",
            "@com_google_code_gson",
            "@safe_html_types//:java",
            "@safe_html_types//:java-proto",
            "@com_google_guava",
            "@com_google_inject_extensions_guice_assistedinject",
            "@com_google_inject_extensions_guice_multibindings",
            "@com_google_inject_guice",
            "@com_ibm_icu_icu4j",
            "@javax_inject",
            "@org_json",
            "@org_ow2_asm",
            "@org_ow2_asm_analysis",
            "@org_ow2_asm_commons",
            "@org_ow2_asm_util",
            "@com_google_protobuf//:protobuf_java",
        ],
        "inject": "\n".join([
            ("java_binary(\n" +
             "    name = \"%s\",\n" +
             "    main_class = \"com.google.template.soy.%s\",\n" +
             "    output_licenses = [\"unencumbered\"],\n" +
             "    runtime_deps = [\":com_google_template_soy\"],\n" +
             ")\n") % (name, name)
            for name in (
                "SoyParseInfoGenerator",
                "SoyToJbcSrcCompiler",
                "SoyToJsSrcCompiler",
                "SoyHeaderCompiler",
                "SoyToIncrementalDomSrcCompiler",
                "SoyToPySrcCompiler",
            )
        ]),
    },

    # Google: Soy (JS Sources)
    "com_google_template_soy_jssrc": {
        "type": "archive",
        "format": "zip",
        "overlay": "@gust//external:soy_jssrc.BUILD",
        "targets": ["https://storage.googleapis.com/elide-software/frontend/soy/soy-jssrc-b29.jar"],
        "seal": "32a1b96d30a3ec72d00a20ad9329046ce8960b473c2b94f1325dd969034fc81d"},

    # Micronaut: Views (Core)
    "io_micronaut_micronaut_views": {
        "type": "java",
        "licenses": ["notice"],
        "forceLocal": False,
        "overlay": "micronaut-views-core.bzl",
        "local": "/Users/sam.g/Workspace/micronaut-views/views-core",
        "targets": ["https://storage.googleapis.com/elide-software/micronaut/b26/views-core-1.3.4.BUILD-SNAPSHOT.jar"],
        "seal": "38c6bee8d6673119819df452921d5739f778646c81e2267658d01d56550d5e95",
        "deps": [
            maven("io.micronaut:micronaut-runtime"),
            maven("io.micronaut:micronaut-http-client"),
            maven("io.micronaut:micronaut-http-server-netty"),
            maven("io.micronaut.security:micronaut-security"),
        ],
    },

    # Micronaut: Views (Soy)
    "io_micronaut_micronaut_views_soy": {
        "type": "java",
        "licenses": ["notice"],
        "forceLocal": False,
        "overlay": "micronaut-views-soy.bzl",
        "local": "/Users/sam.g/Workspace/micronaut-views/views-soy",
        "targets": ["https://storage.googleapis.com/elide-software/micronaut/b26/views-soy-1.3.4.BUILD-SNAPSHOT.jar"],
        "seal": "781c153cd9a2c5f9d434b3cd7fb9c32b47f3a60cdb0d9a873bc1e2703052c717",
        "deps": [
            "@com_google_template_soy",
            maven("io.micronaut:micronaut-runtime"),
            maven("io.micronaut:micronaut-http"),
            maven("io.micronaut:micronaut-http-server"),
            maven("io.micronaut:micronaut-buffer-netty"),
        ],
    },

    # Tools: Android
    "tools_android": {
        "type": "github",
        "repo": "bazelbuild/tools_android",
        "target": "58d67fd54a3b7f5f1e6ddfa865442db23a60e1b6",
        "seal": "a192553d52a42df306437a8166fc6b5ec043282ac4f72e96999ae845ece6812f"},

    # Rules: Android
    "rules_android": {
        "type": "github",
        "repo": "bazelbuild/rules_android",
        "target": "e8fbc49f913101e846235b9c9a31b3aa9788364a",
        "seal": "6a3cfb7b7e54cf704bf2ff169bde03666ae3b49a536c27a5f43d013388a7c38d"},

    # Rules: C/C++
    "rules_cc": {
        "type": "github",
        "repo": "bazelbuild/rules_cc",
        "target": "5cbd3dfbd1613f71ef29bbb7b10310b81e272975",
        "seal": "ce19fea12ee666a0d399e6e15b5a77264f6da2b70f2759adea767c9a7f79b17c"},
}


def _install_dependencies(local = False):

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS, local)

install_dependencies = _install_dependencies
