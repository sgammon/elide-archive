
load(
    "//defs:tools.bzl",
    "dependencies",
    "http_archive",
    "git_repository",
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
        "seal": None},

    # Google: JS Interop (Generator)
    "com_google_jsinterop_generator": {
        "type": "github",
        "repo": "google/jsinterop-generator",
        "target": "4d566c621720f47d0b5adb1e03d617c1819d2e87",
        "seal": "df4aafb211742493b8bf6a0e176312d56d4a8d92aa8b50568ea2912be9bb615f"},

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

    # Google: JS Interop
    "com_google_jsinterop_annotations_head": {
        "type": "github",
        "repo": "google/jsinterop-annotations",
        "target": "8b6080dd1b0b9f60f4175243ea8038bf7fb8a557",
        "seal": "51aa1d36cbe736fc98fa53a825eff25c6e0811dc656d23979d837721f72e78cf"},

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

    "proto_common": {
        "type": "github",
        "repo": "googleapis/api-common-protos",
        "target": "a1049653796e24778de3073bd04760588494aecd",
        "overlay": "proto_common.bzl",
        "seal": "280bdadd0cc490ac601ba577694e290b2aa3bc5636dcb9f0d9eca27dc0f5791d"},

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

    # Google: Soy
    "com_google_template_soy": {
        "type": "java",
        "licenses": ["notice"],
        "targets": ["https://storage.googleapis.com/bloom-software/frontend/soy/soy-lib-b25.jar"],
        "seal": "3222dc45b2ec908d49b595c6476b8b98be354e5c73501db4393bc90aa49313ae",
        "deps": [
            "@args4j",
            "@com_google_code_findbugs_jsr305",
            "@com_google_code_gson",
            "@com_google_common_html_types",
            "@com_google_guava",
            "@com_google_inject_extensions_guice_assistedinject",
            "@com_google_inject_extensions_guice_multibindings",
            "@com_google_inject_guice",
            "@com_google_protobuf//:protobuf_java",
            "@com_ibm_icu_icu4j",
            "@javax_inject",
            "@org_json",
            "@org_ow2_asm",
            "@org_ow2_asm_analysis",
            "@org_ow2_asm_commons",
            "@org_ow2_asm_util",
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
        "targets": ["https://storage.googleapis.com/bloom-software/frontend/soy/soy-jssrc-b25.jar"],
        "seal": "f14f8428776fa3388d7c81258ca219f38e7d74d103e7b632f77d6f70ef5d1ed2"},
}


def _install_dependencies():

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS)

install_dependencies = _install_dependencies
