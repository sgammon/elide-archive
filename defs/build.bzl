
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
        "target": "add8e42934d1bf63ecf27f5439574383e74ef8fc",
        "seal": "088e451fbee1fadf328d69bd243f8de5b3a44f329946edc79bff7fca9382379c"},

    # Bazel: Stardoc
    "io_bazel_stardoc": {
        "type": "github",
        "repo": "bazelbuild/stardoc",
        "target": "7c18f436bcb2b9fa38d0a4353faacf0e2b214c4f",
        "seal": "6cceb8bdb4b75ba7c779ef2cbb3bd81c842fb7afeeb2f3895c041393e58ef025"},

    # Bazel: Gazelle
    "bazel_gazelle": {
        "type": "github",
        "repo": "bazelbuild/bazel-gazelle",
        "target": "57357431fc160b6877f632677d92657defd5ccbb",
        "seal": "f44b6750927a36ca616041ad8fd44878f008d5ad0c76e6ce8ddd89f30cf99595"},

    # Bazel: Toolchains
    "bazel_toolchains": {
        "type": "github",
        "repo": "bazelbuild/bazel-toolchains",
        "target": "9b20ae1817490a2adb3b38d7f2e2898b537cccb9",
        "seal": "d516d4bdc38b79ed752e37fa6821c51d35d7ba3c6ecc040b94b92e705349bdec"},

    # Rules: Closure
    "io_bazel_rules_closure": {
        "type": "github",
        "repo": "sgammon/rules_closure",
        "target": "5c6004c5378cb4339f9054acab3c07ada8c1c7ae",
        "local": "/workspace/GUST/vendor/bazel/rules_closure",
        "seal": "5b895c0885ed3e7fbee5d4bb28af21ce312ae934596e1ab018133ebcd320fd62"},

    # Rules: Protobuf
    "rules_proto": {
        "type": "github",
        "repo": "bazelbuild/rules_proto",
        "target": "f6b8d89b90a7956f6782a4a3609b2f0eee3ce965",
        "seal": "4d421d51f9ecfe9bf96ab23b55c6f2b809cbaf0eea24952683e397decfbd0dd0"},

    # Rules: Python
    "rules_python": {
        "type": "github",
        "repo": "bazelbuild/rules_python",
        "target": "38f86fb55b698c51e8510c807489c9f4e047480e",
        "seal": "c911dc70f62f507f3a361cbc21d6e0d502b91254382255309bc60b7a0f48de28"},

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
        "target": "36fad5adcb2ee3ce02ae5c66092b81eb416eb210",
        "local": "/workspace/GUST/vendor/bazel/rules_graal",
        "seal": "ec810cf18ead709c87c1736d70603ef40318f8d219ebee8c821f57e035bc4cfa"},

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
        "target": "d442b54d22ec60010053bb20c783e87558f3632e",
        "seal": "39bd9c3b485d85c37c351edc4930a458412b1178776e4c12f754dadc74a14b65"},

    # Rules: Apple (iOS/macOS/tvOS)
    "build_bazel_rules_apple": {
        "type": "github",
        "repo": "bazelbuild/rules_apple",
        "target": "5b47855a8be997c1463a1c0425b9aa08f2ba826f",
        "seal": "074ec4fee68b37a25e1935e53cf4cd2c9bfa103be39256fa7836473855615436"},

    # Rules: Apple (Swift)
    "build_bazel_rules_swift": {
        "type": "github",
        "repo": "bazelbuild/rules_swift",
        "target": "ebef63d4fd639785e995b9a2b20622ece100286a",
        "seal": "ce30e25bed943a9edae90770a5121618a7239d09f8e05bdc1aaa5643f730ad7b"},

    # Rules: Go
    "io_bazel_rules_go": {
        "type": "github",
        "repo": "bazelbuild/rules_go",
        "target": "b763c84aa4f124273bb6048136fe86a4de60a1ae",
        "seal": "d54e3c4ac3420f002c6d8940200ee86411635b472e044858d7f6b91241f89f1b"},

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
        "target": "7e798ffb0f4a6147b766ee2a62d7f18b8155452e",
        "seal": "bf228b61d852fd38688ca4c1b9148b6bdc888053c4a86bffc451853124f5b2b1"},

    # Rules: Docker
    "io_bazel_rules_docker": {
        "type": "github",
        "repo": "bazelbuild/rules_docker",
        "target": "e3ccf5006e17b83f2bab0f9764396dabb337172e",
        "seal": "3e9559277c0e990f9043aa7a6fb817fedf0c3fe77b582d386015bd9e4c52b506"},

    # Google: Protobuf
    "com_google_protobuf": {
        "type": "github",
        "repo": "google/protobuf",
        "target": "29cd005ce1fe1a8fabf11e325cb13006a6646d59",
        "seal": "51398b0b97b353c1c226d0ade0bae80c80380e691cba7c1a108918986784a1c7"},

    # Google: J2CL (Java-to-Closure)
    "com_google_j2cl": {
        "type": "github",
        "repo": "sgammon/j2cl",
        "target": "7fd9c21a6cbd9fa044ada83f772281cf87f2aa8a",
        "local": "/workspace/GUST/vendor/bazel/j2cl",
        "seal": "a45e6f1c94651c5babc461a78d04ca005a7720741aa4ad63612ad68715de3a70"},

    # Google: Elemental2
    "com_google_elemental2": {
        "type": "github",
        "repo": "google/elemental2",
        "target": "d328c1e688cc5e7e9eaec5c5d264423878827925",
        "seal": "ac6018af3274fd80d7d185305575474b4dfa1367e2c360e9f3c038f06affd7da"},

    # Google: JS Interop (Base)
    "com_google_jsinterop_base": {
        "type": "github",
        "repo": "google/jsinterop-base",
        "target": "172a78020d428e46cc86bcaebe946589c07452ae",
        "seal": "8edf4e4e51cff5c7346f6d43f2b822767ae16f1b65ca4d26b7040111d1f62c48"},

    # Google: JS Interop (Generator)
    "com_google_jsinterop_generator": {
        "type": "github",
        "repo": "google/jsinterop-generator",
        "target": "4d566c621720f47d0b5adb1e03d617c1819d2e87",
        "seal": "df4aafb211742493b8bf6a0e176312d56d4a8d92aa8b50568ea2912be9bb615f"},

    # Google: JS Interop (Annotations)
    "com_google_jsinterop_annotations_head": {
        "type": "github",
        "repo": "google/jsinterop-annotations",
        "target": "8b6080dd1b0b9f60f4175243ea8038bf7fb8a557",
        "seal": "51aa1d36cbe736fc98fa53a825eff25c6e0811dc656d23979d837721f72e78cf"},

    # Google: API (Core)
    "com_google_api": {
        "type": "github",
        "repo": "googleapis/googleapis",
        "target": "a9639a0a9854fd6e1be08bba1ac3897f4f16cb2f",
        "seal": "f8d381d8a5bc186e30f0cfb6c2ebdbe42c12bf30f0bf46a4a7d071be1c36b20f"},

    # Google: API (Codegen)
    "com_google_api_codegen": {
        "type": "github",
        "repo": "googleapis/gapic-generator",
        "target": "102ce460e9be8f42f7eb05442be6ce4e4dfb6a98",
        "seal": "e9801c52adf7b4d2bb4e07ce1584602cfb955f3c97e4016bd02640ac444150f6"},

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
        "target": "734b8d41d39a903c70132828616f26cb2c7f908c",
        "seal": "c89348b73f4bc59c0add4074cc0c620a5a2a08338eb4ef207d57eaa8453b82e8"},

    # Normalize CSS
    "org_normalize_css": {
        "type": "github",
        "repo": "necolas/normalize.css",
        "target": "fc091cce1534909334c1911709a39c22d406977b",
        "overlay": "normalize.bzl",
        "seal": None},

    # Material Design Lite
    "com_google_mdl": {
        "type": "github",
        "repo": "bloombox/material-design-lite",
        "target": "7e10595660b9c56ab67203f8ba966cb4883e1547",
        "overlay": "mdl.bzl",
        "seal": "f65b744aa0865bce2f9727b1b116fadf10639b63f4b511165a2ab65afa6d1046"},

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
        "target": "be1715d82f7b32e838565631ab7ad04850bfd4ff",
        "seal": "ba8f098924ac373c9a8f08a8179f073c4df1c45b9966ae6aaff87b53b7f0e009"},

    # gRPC: Java
    "io_grpc_java": {
        "type": "github",
        "repo": "grpc/grpc-java",
        "target": "0b4fa21d50578d917f15dad41ca7a7ab2612356b",
        "seal": "c47132e0dc0977a8434e3fb3b0e97b0f6269c31b3b77042a81d9e834f61cda1b"},

    # Security/TLS: BoringSSL
    "boringssl": {
        "type": "github",
        "repo": "google/boringssl",
        "target": "1c2769383f027befac5b75b6cedd25daf3bf4dcf",
        "seal": "a3d4de4f03cb321ef943678d72a045c9a19d26b23d6f4e313f97600c65201a27"},

    # Google: Closure Stylesheets
    "com_google_closure_stylesheets": {
        "type": "java",
        "licenses": ["notice"],
        "targets": ["https://storage.googleapis.com/bloom-software/closure-stylesheets-1.6.0-b4.jar",],
        "seal": "364f10a71163e56e86ee5233d9080d42fd45706345dafa3ffdeb333c1ba44e2c",
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
        "seal": None,
        "targets": [
          "https://repo1.maven.org/maven2/javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.jar",
        ],
    },

    # Google: Soy
    "com_google_template_soy": {
        "type": "java",
        "licenses": ["notice"],
        "targets": ["https://storage.googleapis.com/bloom-software/frontend/soy/soy-lib-b28.jar"],
        "seal": "4c95ff7fc4947273fab84958266098bebe4d991ea7e0c289211d81603d6a4ff6",
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
        "overlay": "@io_bazel_rules_closure//closure/templates:soy_jssrc.BUILD",
        "targets": ["https://storage.googleapis.com/bloom-software/frontend/soy/soy-jssrc-b28.jar"],
        "seal": "0e0506261139b7d008cad47c721d55210785f33fbd4beedd3cb36e6752d85320"},

    # Micronaut: Views (Core)
    "io_micronaut_micronaut_views": {
        "type": "java",
        "licenses": ["notice"],
        "targets": ["https://storage.googleapis.com/bloom-software/micronaut-views-core-1.3.2.BUILD.jar"],
        "seal": "e896ef7612ecfc9e62f400e4ab994a6868f9ec64206c3eaf16265801a3ca2300",
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
        "targets": ["https://storage.googleapis.com/bloom-software/micronaut-views-soy-1.3.2.BUILD.jar"],
        "seal": "b4f94328ab0416c395eab55d5f8743f49ba66b7b514c5ffb4b3837fb36edc2d1",
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


def _install_dependencies():

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS)

install_dependencies = _install_dependencies
