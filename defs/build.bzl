
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
        "target": "4b25373d12887f5add565197c4a163e9f1d9b716",
        "seal": "67bd836ea57bc80e70473c1f840048a4f7cffc9b1530b05c81eb3e85de60f06f"},

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
        "target": "0d378ccadef1b527e3b927aabdeaae38f5d46156",
        "seal": "95dd34badf22cd5ba2b8ccfe457774f2cfcccfe550d02eec8875f0bc479f90e7"},

    # Bazel: Toolchains
    "bazel_toolchains": {
        "type": "github",
        "repo": "bazelbuild/bazel-toolchains",
        "target": "b7e3d4ca4f1d34ad394d8ed53872b827d178f9a9",
        "seal": "a3d702305458418ed63479c75f0b376bad4c01da06eb9d9e35778ad8a49241aa"},

    # Rules: Closure
    "io_bazel_rules_closure": {
        "type": "github",
        "repo": "bazelbuild/rules_closure",
        "target": "3fd839565aa5920a1420169daa3b2a230dc04395",
        "local": "/workspace/GUST/vendor/bazel/rules_closure",
        "seal": "a57a9fc9abdc953477e6f6617444866f3e25bb202273e3155075051cb4341054"},

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
        "target": "d7666ec475c1f8d4a6803cbc0a0b6b4374360868",
        "seal": "3cd625058dc989f6fac0bf8cf7c3cac6d654052500bd8ffea15de1b47bd3d20d"},

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
        "target": "c3aef368424448593a0a72e61582855515f85961",
        "seal": "2181d13a59cf0c25ed88f538d384773c09e6093a78ce6cc72b071f66c5e71e0b",
        "local": "/workspace/GUST/vendor/bazel/rules_graal"},

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
        "target": "62e0a4c34a43867988bbe351fc9ede1389eb2291",
        "seal": "b983fd7e06b751729e4cc7321b334b03c93ccfc74bf0c5ba931b9a0b7a528dd3"},

    # Rules: Go
    "io_bazel_rules_go": {
        "type": "github",
        "repo": "bazelbuild/rules_go",
        "target": "5474789768ede024a294137d8f249d5cb5b60627",
        "seal": "ba01f366cc3bfd4fc1f0b822461af1b3e46aa9cf0952e833c1459a563b30693f"},

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
        "target": "b1aaf646b4e835649d8af1110bf7ed840da83fd6",
        "seal": "f044cea5ccf37d78120d1224cfde6dff1c4b6e6cf8b73b4d0d96488ffa46312a"},

    # Rules: Docker
    "io_bazel_rules_docker": {
        "type": "github",
        "repo": "bazelbuild/rules_docker",
        "target": "77eee135e16f50d72c3197512d224a1820915b6a",
        "seal": "a52094a81b4d413b75b1529af71ca43e4ebdc787bc40e58f3848c2630875ab14"},

    # Google: J2CL (Java-to-Closure)
    "com_google_j2cl": {
        "type": "github",
        "repo": "sgammon/j2cl",
        "target": "75643f82d45499aba7d48c7e091f335ff5811813",
        "seal": "f0d2e18882f38e25252b5e52a128d0443f326ea00e697bf7648056917be0a0a5",
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
        "target": "3ca2c014e24eb5111c8e7248b1e1eb833977c83d",
        "seal": "705e55c145a1e40c61816af04d1b49ed19ecb1fa182c062d46379d367d00db66"},

    # Google: API (Codegen)
    "com_google_api_codegen": {
        "type": "github",
        "repo": "googleapis/gapic-generator",
        "target": "b7033a61dbfc7f1ad856b17299cf8aeb688a94f4",
        "seal": "311e0a01fc222902de084dfb7b03a2402b64f62e139dc451c8f4b0c60fcab207"},

    # Google: GWT
    "org_gwtproject_gwt": {
        "type": "github",
        "repo": "gwtproject/gwt",
        "target": "cb9b374f2da7611f80db8fcfe36b113057ff35ab",
        "seal": "fc4a5ee51baa4a691f060a171d6aa03a67d0d31b3b4a9e020f2efdeb360f7eb1"},

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
        "target": "6b334ece48828fb8e45052976d3516f808819ac7",
        "seal": "2c62ecc133ee0400d969750a5591909a9b3839af402f9c9d148cffb0ce9b374b"},

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
