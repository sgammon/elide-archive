
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
        "target": "1c5ed0706a8e2f73116e128184be64a99e66e83d",
        "seal": "02a5ead53c54e77686a7ae678179cdd37780d5534e16176831328a8cb3646011"},

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
        "target": "b38f32ee6304d3fb1d98e83497ce238cbcc5e2e0",
        "seal": "4b5d8160a5ad0a1edf9dc84c4c205a4e4565b4022bf1b0a0fc00b90890e0e232"},

    # Bazel: Toolchains
    "bazel_toolchains": {
        "type": "github",
        "repo": "bazelbuild/bazel-toolchains",
        "target": "b7588fafd8d940b97e805153916c677e02a44716",
        "seal": "9831da5f71e95589dce35390b673aaf5cc7dd2800fa9b0b6ae4d2546a7e256ad"},

    # Rules: Closure
    "io_bazel_rules_closure": {
        "type": "github",
        "repo": "sgammon/rules_closure",
        "target": "4dc8434e964fa69d9c16d0936a706614c430ee1d",
        "local": "/workspace/GUST/vendor/bazel/rules_closure",
        "seal": "90fa364333565bd6ad26dd48a58285c54967f8e335471a388a2e046bfaec390d"},

    # Rules: SASS
    "io_bazel_rules_sass": {
        "type": "github",
        "repo": "bazelbuild/rules_sass",
        "target": "7e798ffb0f4a6147b766ee2a62d7f18b8155452e",
        "seal": "bf228b61d852fd38688ca4c1b9148b6bdc888053c4a86bffc451853124f5b2b1"},

    # Rules: Protobuf
    "rules_proto": {
        "type": "github",
        "repo": "bazelbuild/rules_proto",
        "target": "673e59d983eeef2e2ae0cfbb98ea87aad9de1795",
        "seal": "e2a3d5da2a8e316b5c3623e7d7047cdd942f2d0eb43f8c84ca0470f5413f128d"},

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
        "target": "32ddd6c4f0ad38a54169d049ec05febc393b58fc",
        "seal": "1969a89e8da396eb7754fd0247b7df39b6df433c3dcca0095b4ba30a5409cc9d"},

    # Rules: GraalVM
    "rules_graal": {
        "type": "github",
        "repo": "sgammon/rules_graal",
        "target": "bdf2909888c4cfd594a0e660977358bed21b739e",
        "local": "/workspace/GUST/vendor/bazel/rules_graal",
        "seal": "e3606de14da7dc57f82502b8f9a310741eff7ad9b29f0b91ff9218f88b027a7d"},

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
        "target": "030ea9ef8e4ea491fed13de1771e225eb5a52d18",
        "seal": "3f9456f7edb5644639cf362f15ae3bf8b3605c7a0400544c059f92786f551921"},

    # Rules: Go
    "io_bazel_rules_go": {
        "type": "github",
        "repo": "bazelbuild/rules_go",
        "target": "7d825a39a9c5fc49f4dfe7b09c8ce8ec59ea6be8",
        "seal": "62cf913701c81c0f5a3a2772fee652f44b994248608df155e4b61139b7127a88"},

    # Rules: Web Testing
    "io_bazel_rules_webtesting": {
        "type": "github",
        "repo": "sgammon/rules_webtesting",
        "target": "0a1cbf2c5bb878eb2ccbc304342b6a3619ba6e7d",
        "seal": "01a8fd568b26dff2c8afd68a673890c38d2ea5324d2259580106e7b8ad3f8e95",
        "local": "/workspace/GUST/vendor/bazel/rules_webtesting"},

    # Rules: SCSS/SASS
    "rules_sass": {
        "type": "github",
        "repo": "bazelbuild/rules_sass",
        "target": "7e798ffb0f4a6147b766ee2a62d7f18b8155452e",
        "seal": "bf228b61d852fd38688ca4c1b9148b6bdc888053c4a86bffc451853124f5b2b1"},

    # Rules: Docker
    "io_bazel_rules_docker": {
        "type": "github",
        "repo": "bazelbuild/rules_docker",
        "target": "d28594b57f4b14663efc0e667651a4ee12ab7246",
        "seal": "f95bb2c2c713fea13ace65898256435814aa006cd725985545379b3e8aaaee27"},

    # Google: J2CL (Java-to-Closure)
    "com_google_j2cl": {
        "type": "github",
        "repo": "sgammon/j2cl",
        "target": "3e3092f86b0cc0958a2dbc2c8098484acc1ddff0",
        "seal": "807d0c61bf966d70dbcfdf89f7fffd001ef9b66c472ef2b6e51031f70cc9c1c9",
        "local": "/workspace/GUST/vendor/bazel/j2cl"},

    # Google: Elemental2
    "com_google_elemental2": {
        "type": "github",
        "repo": "google/elemental2",
        "target": "6567f2ad00379f34f4a1e080faf90fc284615354",
        "seal": "94a978b5b4ced58b0ce95bfa6d3e08a3c55af4c89e49449aa6b61c2f34b931ab"},

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
        "target": "e2faab04f4cb7f9755072330866689b1943a16e9",
        "seal": "baf88715f8ea0e44f059565b7e7995fa8fd14eaeecc8266645c613a8c86bc2de"},

    # Google: API (Codegen)
    "com_google_api_codegen": {
        "type": "github",
        "repo": "googleapis/gapic-generator",
        "target": "a57f59b05d63e811cac6436af29378ed47ef50ed",
        "seal": "5dd6787e2e942b51b344ccce429c6eddeab54545c799c0067c4b02fc59a5d6f0"},

    # Google: GWT
    "org_gwtproject_gwt": {
        "type": "github",
        "repo": "gwtproject/gwt",
        "target": "2b2b39a124e30800f51a30c1e253fdb3ad521b2d",
        "seal": "af1a6fd3b20f2c5aebf6c8d3e35f9aec0faee20e9c5a2e6fdb14e588996cfaec"},

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

    # HTML5 Boilerplate
    "org_html5_boilerplate": {
        "type": "github",
        "repo": "h5bp/html5-boilerplate",
        "target": "36c8988392777cd3c2c6718bfa1114c79b344601",
        "overlay": "h5bp.bzl",
        "seal": None},

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
