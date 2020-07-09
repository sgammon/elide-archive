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
        "target": "6970e21d290ceaa36502d0c94533b26e5ec18c0b",
        "seal": "534ffdf8beffdbf37b101accb959e7c076cacbc1a704bfeda0f74b9009a65e31"},

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
        "target": "3349f129a4f3c475ca32949c26dc90822ffebaac",
        "seal": "9b2f6a6cf61b075ae9b6dbbe675d4e5432aab0d62da680d554baf114c1a42844"},

    # Bazel: Stardoc
    "io_bazel_stardoc": {
        "type": "github",
        "repo": "bazelbuild/stardoc",
        "target": "87545335ef7fb248051a7e049e88177ac8168c03",
        "seal": "6266a305d6e7b794a39ca4563cc173af25719e7eeefa9a99c1d478f1f44db431"},

    # Bazel: Gazelle
    "bazel_gazelle": {
        "type": "github",
        "repo": "bazelbuild/bazel-gazelle",
        "target": "83b2dc564469ef341d2e84f358ebf24ffa003a71",
        "seal": "191ebb38376bb8de3ddc5097c98fe49690bb8cc4ed7b9673bab38ffc87305b3f"},

    # Bazel: Toolchains
    "bazel_toolchains": {
        "type": "github",
        "repo": "bazelbuild/bazel-toolchains",
        "target": "82f6f165f44b8e699c878059dbe4bc9da4aef190",
        "seal": "45b7149f20e64e8cc5d7d4e8fe6f7bab94b305555edaac6b8c645f687aebaf9c"},

    # Rules: Closure
    "io_bazel_rules_closure": {
        "type": "github",
        "forceLocal": False,
        "repo": "sgammon/rules_closure",
        "target": "efbc39ab0860e983150df70270d9972b9e4320b8",
        "local": "/workspace/GUST/vendor/bazel/rules_closure",
        "seal": "c6b2ca692c78bc3aec7a73b9b3ba2870a91ac45a45faccfa8684a62429dcb2f9"},

    # Rules: Protobuf
    "rules_proto": {
        "type": "github",
        "repo": "bazelbuild/rules_proto",
        "target": "218ffa7dfa5408492dc86c01ee637614f8695c45",
        "seal": "2490dca4f249b8a9a3ab07bd1ba6eca085aaf8e45a734af92aad0c42d9dc7aaf"},

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
        "target": "a397eab217c65046b0f918dbb2a4e7b401c57a99",
        "seal": "5bdbe2e6dd065ed25e1b18192a2790009edd06f055bffdf80248e4c6709083dc"},

    # Rules: Kotlin
    "io_bazel_rules_kotlin": {
        "type": "github",
        "repo": "bazelbuild/rules_kotlin",
        "target": "686518ffd8e58609a21f258616d154ba2934a8e8",
        "seal": "110df9613694dab1cc15f1b6f181ce67e743ce2b1316e82f0f546c8fed9711a3"},

    # Rules: External JVM
    "rules_jvm_external": {
        "type": "github",
        "repo": "bazelbuild/rules_jvm_external",
        "target": "4489ffeaaa9adcad4fa2657c1ca2eeb7b1ad4b84",
        "seal": "6be1e2c4ad81cb00851df5ec7dcc2547cdd5cb555d829c5b665470f3d4d3229b"},

    # Rules: Apple (iOS/macOS/tvOS)
    "build_bazel_rules_apple": {
        "type": "github",
        "repo": "bazelbuild/rules_apple",
        "target": "19f031f09185e0fcd722c22e596d09bd6fff7944",
        "seal": "95c4a23bf252dfdcf435488d7bcbeb9cb3511a0cefabd8b5c5145b339af8714b"},

    # Rules: Apple (Swift)
    "build_bazel_rules_swift": {
        "type": "github",
        "repo": "bazelbuild/rules_swift",
        "target": "ebef63d4fd639785e995b9a2b20622ece100286a",
        "seal": "ce30e25bed943a9edae90770a5121618a7239d09f8e05bdc1aaa5643f730ad7b"},

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
        "target": "87cc49af2c6d52eb3445860a15fcd264c86c5e80",
        "seal": "91c34d38f1740dcd9d321928cdf28bd9ba28e58a0717662ef850d42bb694531b"},

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
        "target": "2c07f6ddc6ea1228519007e6548f2df915b13b0c",
        "seal": "abe16bfc95dd203be64afa7e1cbfed3bbe7c30a1e6274e1a69a31a1ab4514705"},

    # Rules: Docker
    "io_bazel_rules_docker": {
        "type": "github",
        "repo": "bazelbuild/rules_docker",
        "target": "9e5b065ed0699bd8491ba1f6bef6eb428f2d230c",
        "seal": "5b5941ba81a8f92d6cf1af5176ccfd3b5f2f7e5672741c3fa64f4023c22bed01"},

    # Rules: Kubernetes
    "io_bazel_rules_k8s": {
        "type": "github",
        "repo": "bazelbuild/rules_k8s",
        "target": "3dcc327551590e7bdc16e40c9ead13a26b7c3c10",
        "seal": "0f19a0a10a2e756959d4084c5f18e46b8ea3e2c62ee795340d7d8f32aff7f2cd"},

    # Google: Protobuf
    "com_google_protobuf": {
        "type": "github",
        "repo": "google/protobuf",
        "target": "d0bfd5221182da1a7cc280f3337b5e41a89539cf",
        "seal": "2435b7fb83b8a608c24ca677907aa9a35e482a7f018e65ca69481b3c8c9f7caf"},

    # Google: J2CL (Java-to-Closure)
    "com_google_j2cl": {
        "type": "github",
        "repo": "sgammon/j2cl",
        "target": "9f499e7973afda4c485d9c68659fb7b1f1d8b023",
        "local": "/workspace/GUST/vendor/bazel/j2cl",
        "seal": "e776e27b538e7b09f17e83df5926a2199b86f020bc6734ca4a8664478572e014"},

    # Google: Elemental2
    "com_google_elemental2": {
        "type": "github",
        "repo": "sgammon/elemental2",
        "target": "aab248e2170e9250e992aee97d476d3d768fe583",
        "seal": "71caae6d7886515f347cef6c605186a6f2329b618af67fcd85b03bd7c7529363"},

    # Google: JS Interop (Base)
    "com_google_jsinterop_base": {
        "type": "github",
        "repo": "google/jsinterop-base",
        "target": "1e7fc7ad4088882f9633d4e34d86f6797d06b623",
        "seal": "36b9b5990102805d52f04e546233c324d200d89dc50a885d060f3981824fd6fd"},

    # Google: JS Interop (Generator)
    "com_google_jsinterop_generator": {
        "type": "github",
        "repo": "google/jsinterop-generator",
        "target": "3fb3be7cf0e9b58900ea9c1f17f0b64f2a3c77ac",
        "seal": "aca596250300a92555197a2254e0c76493c10ee7e06e6de4454482a3c13a37ed"},

    # Google: JS Interop (Annotations)
    "com_google_jsinterop_annotations_head": {
        "type": "github",
        "repo": "google/jsinterop-annotations",
        "target": "d9ed0742444dfbb06a37d01df01e4bd3fc5b3c05",
        "seal": "7f8d5788187840d02f1d80c100147a41dc02686e769ec704652680c82419ccbc"},

    # Google: API (Core)
    "com_google_api": {
        "type": "github",
        "repo": "googleapis/googleapis",
        "target": "4ba9aa8a4a1413b88dca5a8fa931824ee9c284e6",
        "seal": "d9cb67c305d39fbd7a1a0faff27a14b32ec28399d523c8b78be94f4cfdb4fa94"},

    # Google: API (Codegen)
    "com_google_api_codegen": {
        "type": "github",
        "repo": "googleapis/gapic-generator",
        "target": "65fd292290d5e389ffa555f6f8cd71582a9607b3",
        "seal": "71d5cefc80aa7b085e2c64ba3489039826d65477085d01fc3f63a4e0ccaa1d0c"},

    # Google: GWT
    "org_gwtproject_gwt": {
        "type": "github",
        "repo": "gwtproject/gwt",
        "target": "f236a6964669a70253ad16fdb9f627834997519e",
        "seal": "6256fb60b6749789a40203d32daf217638b0ae5daeca93056b9c1fb048672042"},

    # BuildStack: Protobuf Rules
    "build_stack_rules_proto": {
        "type": "github",
        "repo": "stackb/rules_proto",
        "target": "1d6b84118399828511faeecc145d399c1e7bdee2",
        "seal": "7e421578cba10736b6411d991514771996c7d21b4575d7f33e1d606a6a2cfe4d"},

    # Normalize CSS
    "org_normalize_css": {
        "type": "github",
        "repo": "necolas/normalize.css",
        "target": "fc091cce1534909334c1911709a39c22d406977b",
        "overlay": "normalize.bzl",
        "seal": "53933ba214ab20b501a4ed1f08796ef716a5f1ccb8e0f9977a27f802cb09bcda"},

    # Material Design Lite
    "mdl": {
        "type": "github",
        "repo": "sgammon/material-design-lite",
        "overlay": "mdl.bzl",
        "target": "a963d185479d06c495bcf9c44966036d089813bf",
        "seal": "7abe53c1f56f3f2317386e8d1aaf2dce853238e03bcdc3a985b17bf089fbe56c"},

    # Common Protocol Buffers
    "proto_common": {
        "type": "github",
        "repo": "googleapis/api-common-protos",
        "target": "fd62e4d97ca6829b9166ae86bc6429574ff4e5db",
        "overlay": "proto_common.bzl",
        "seal": "4a84c293b3758d2cd5b6da27ffb0166f6ce23b99f70ea14ef28cb77099744889"},

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
        "repo": "grpc/grpc",
        "target": "e73882dc0fcedab1ffe789e44ed6254819639ce3",
        "seal": "8fe4e5aa436037a08634eb29bde99c02cd804df9f4da383ec738165fd6cdcadf"},

    # gRPC: Java
    "io_grpc_java": {
        "type": "github",
        "repo": "grpc/grpc-java",
        "target": "47d545ad70dbf33b3f00052c1b519394ba52e222",
        "seal": "5685ed4875fae4a5d11e31c4431bffa7672ce5451c598ebe1c2e0bd1d96c0286"},

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

    # Security/TLS: BoringSSL
    "boringssl": {
        "type": "github",
        "repo": "google/boringssl",
        "target": "0b710a305b42b67522003a314dea3e3868485665",
        "seal": "10a93b319f9f0a396d7c3d6c05b6195b2f002ae9002e492b5457298dac160993"},

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
        "targets": ["https://storage.googleapis.com/elide-software/closure-stylesheets-1.6.0-b9.jar"],
        "seal": "ef0d4e8b658ff37f99f7702ef82a80f4e9f8b3a8731a0277981267c09567e61a",
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
            "@com_google_common_html_types",
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
        "local": "/workspace/GUST/vendor/micronaut/views/views-core",
        "targets": ["https://storage.googleapis.com/elide-software/micronaut/b12/views-core-1.3.4.BUILD-SNAPSHOT.jar"],
        "seal": "d3e5a36cbb802238dcecc442e0c48c2bee8e2706505a8d7a995642d123bbabb2",
        "deps": [
            maven("io.micronaut:micronaut-runtime"),
            maven("io.micronaut:micronaut-http-client"),
            maven("io.micronaut:micronaut-http-server-netty"),
            maven("io.micronaut:micronaut-security"),
        ],
    },

    # Micronaut: Views (Soy)
    "io_micronaut_micronaut_views_soy": {
        "type": "java",
        "licenses": ["notice"],
        "forceLocal": False,
        "overlay": "micronaut-views-soy.bzl",
        "local": "/workspace/GUST/vendor/micronaut/views/views-soy",
        "targets": ["https://storage.googleapis.com/elide-software/micronaut/b12/views-soy-1.3.4.BUILD-SNAPSHOT.jar"],
        "seal": "57c6844328ae59a0ae942ab23d22b79c8db7359a395c0669fcf3a1b2b13a9f06",
        "deps": [
            "@com_google_template_soy",
            "@com_google_common_html_types",
            maven("io.micronaut:micronaut-runtime"),
            maven("io.micronaut:micronaut-http"),
            maven("io.micronaut:micronaut-http-server"),
            maven("io.micronaut:micronaut-buffer-netty"),
        ],
    },
}


def _install_dependencies(local = False):

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS, local)

install_dependencies = _install_dependencies
