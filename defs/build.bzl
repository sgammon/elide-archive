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
        "target": "bbce9a706563be844726a91e2f4136b6797e3a49",
        "local": "/Users/sam.g/Workspace/rules_closure",
        "seal": "a3817229b413ecaf9ab9173b9074e36e873d0e35dfd576ce6468b491438507bb"},

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
        "target": "0779709c477912c06de14d1fd9068b32d20574b8",
        "seal": "d60a25f1203a5f3df28e9e708c06ed0427cb063ba01698de0aac27e88b0ad25a"},

    # Rules: Apple (iOS/macOS/tvOS)
    "build_bazel_rules_apple": {
        "type": "github",
        "repo": "bazelbuild/rules_apple",
        "target": "6ba25082bc237be111c5867bfd4eba34ef96217c",
        "seal": "ce6e3b6a3764e9df1b247e147ec425ad8e7a97db821bebda5e9eed8a92ff2b3e"},

    # Rules: Apple (Swift)
    "build_bazel_rules_swift": {
        "type": "github",
        "repo": "bazelbuild/rules_swift",
        "target": "17126a9d4f049937a0ca03af0ebe61a0bf9c0492",
        "seal": "3d1d4b0e3ab60602220003062822eb7f984e99051d6a8a04b8729b653328b5c0"},

    # Deps: Apple Support
    "build_bazel_apple_support": {
        "type": "github",
        "repo": "bazelbuild/apple_support",
        "target": "8c585c66c29b9d528e5fcf78da8057a6f3a4f001",
        "seal": "0a8831032b06cabae582b604e734e10f32742311de8975d5182933e586760c5f"},

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
        "target": "b3e386da135f2ba1e65983a8666840548381c803",
        "seal": "50cc08209b9bad6454effdf50f8957a895f08ce584f31ce15fa9f35d53e76175"},

    # Rules: Web Testing
    "io_bazel_rules_webtesting": {
        "type": "github",
        "repo": "sgammon/rules_webtesting",
        "target": "b861fa9748410131d9eab2318163006efd02e96a",
        "local": "/workspace/GUST/vendor/bazel/rules_webtesting",
        "seal": "887ebbdc0815c7e61376d8b74f03a5692dcbe857bc61b8b4bb4d94322b276d4e"},

    # Rules: SCSS/SASS
    "io_bazel_rules_sass": {
        "type": "github",
        "repo": "bazelbuild/rules_sass",
        "target": "a8ee85cd28518944c0de381cffa0444305bd0bf5",
        "seal": "73d22c8981d4abf4304aca63a5d33d7aac02c78ca475935cfb9ac8645098d757"},

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

    # Google: Protobuf
    "com_google_protobuf": {
        "type": "github",
        "repo": "google/protobuf",
        "target": "2514f0bd7da7e2af1bed4c5d1b84f031c4d12c10",
        "seal": "04c5e84492093469a7e6342114df3cfd1281e4a90b0d18428ac6c297b87c03e3"},

    # Google: J2CL (Java-to-Closure)
    "com_google_j2cl": {
        "type": "github",
        "repo": "sgammon/j2cl",
        "target": "5084fe6863ded1eb72fbec4ff654af48bc8abae8",
        "local": "/workspace/GUST/vendor/bazel/j2cl",
        "seal": "7874cc5e560067e2fa6788a75596af7a7fbd690194b5e7286b2b9d11ea931db7"},

    # Google: Elemental2
    "com_google_elemental2": {
        "type": "github",
        "repo": "sgammon/elemental2",
        "target": "35295fcb16770fa6b424be14e2dbbcc01b964af5",
        "seal": "47fa516661b3b6fe1ebaa35210726642b0a4bfbf0a7fa22eba9a94c9715bd826"},

    # Google: JS Interop (Base)
    "com_google_jsinterop_base": {
        "type": "github",
        "repo": "google/jsinterop-base",
        "target": "d63376592856ef4dfd3ef68500df9745cd8c6919",
        "seal": "b22b91d64f963fa7cc1fcf99430139de74306fd25ff4ef2da5f0100dae67efba"},

    # Google: JS Interop (Generator)
    "com_google_jsinterop_generator": {
        "type": "github",
        "repo": "google/jsinterop-generator",
        "target": "4881aa1b210d3a44db9789e1a7079679b6a4d0af",
        "seal": "87cf682f565d92f3e1f20b7d0e7b7d2b2b6593deec8ab1ded4d796c1dc72fc34"},

    # Google: JS Interop (Generator)
    "com_google_jsinterop_annotations": {
        "type": "github",
        "repo": "google/jsinterop-annotations",
        "target": "04bda45586e2a7e0ef5a02f908b828f5da6747af",
        "seal": "483d3d18ace60a8e62796abef374d472cfeecf233ef1b6f384a47fb99e4bb23f"},

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
        "target": "9da693477658964878d4dd5a3f4d5e0d197950db",
        "seal": "8a6c58021565dd7d02d07beb0f5851f0679ac99d540034a132a759959d3a8269"},

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
        "target": "0fcae75a2c20a140137e3a9c48a87d15ceffabd7",
        "overlay": "proto_common.bzl",
        "seal": "3cc5d56cf02dbf2e41022d84135cff48d0720d229b13806a6fc799b2ad3693c5"},

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
        "target": "c40e2dcb0b0ee0f1bba73e59ac812ee3625a9fce",
        "seal": "97e2b57f13a08180fcf5504c851744e21f0434003179c761a98c81ea6d253361"},

    # gRPC: Web
    "com_github_grpc_grpc_web": {
        "type": "github",
        "repo": "sgammon/grpc-web",
        "forceLocal": False,
        "local": "/workspace/GUST/vendor/grpc/web",
        "target": "6aa18295f2f6dd6e9a608e2362f5ddcbe6e69ee1",
        "seal": "42460855313b61b1b4f9bfef0723e6735cd3735234e03d74f5199e8b88ec10ed"},

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
        "target": "9d593bc72fcc6f695ed3666525d0638dfdf50b62",
        "seal": "f298860e52321aef52d62d4e6df6c8f55b522f25eac1fc3e73b89632966b4f83"},

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
        "targets": ["https://storage.googleapis.com/elide-software/closure-stylesheets-1.6.0-b10.jar"],
        "seal": "11b4341aa1d4a76cf90c2f0ee90ef85f584688bf49757c79040ecbef06dfa9c3",
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
        "targets": ["https://storage.googleapis.com/elide-software/micronaut/b17/views-core-1.3.4.BUILD-SNAPSHOT.jar"],
        "seal": "07e397a705d449fb1e5496ec888117d4198faae878f0f7724b47b8da74fb5841",
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
        "targets": ["https://storage.googleapis.com/elide-software/micronaut/b17/views-soy-1.3.4.BUILD-SNAPSHOT.jar"],
        "seal": "c751a3040f7dc3a671fae250ee7b5e3f58bb738f9812d862dd3c74a742431160",
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
