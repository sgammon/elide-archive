
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
        "target": "f13e3cac7022bd8bdd32233d1dae95a56df01a51",
        "seal": "2dc75b384cf5cf5306aa1f6530b6e629f4547182d2203cfe8196d01ee4c6c23c"},

    # Bazel: Gazelle
    "bazel_gazelle": {
        "type": "github",
        "repo": "bazelbuild/bazel-gazelle",
        "target": "dd3ccdc3d36a76d76215f64f1dacb2d230504a27",
        "seal": "a1f89662ebd4c081d3e66c9560ada11d6a0c93660568a46e4a9063670d81f986"},

    # Bazel: Toolchains
    "bazel_toolchains": {
        "type": "github",
        "repo": "bazelbuild/bazel-toolchains",
        "target": "b7e3d4ca4f1d34ad394d8ed53872b827d178f9a9",
        "seal": "a3d702305458418ed63479c75f0b376bad4c01da06eb9d9e35778ad8a49241aa"},

    # Rules: NodeJS
    "build_bazel_rules_nodejs": {
        "type": "github",
        "repo": "bazelbuild/rules_nodejs",
        "target": "1.2.0",
        "seal": "a74ed5df2c2437bc8a4f5b29cda3d9ad4f36f2f325cc61769b4c7653a6b76e8c"},

    # Rules: Closure
    "io_bazel_rules_closure": {
        "type": "github",
        "repo": "sgammon/rules_closure",
        "target": "06af65abad5f1351b46c93578e21147486c0b03b",
        "seal": "80975218961db517448551bccd951e4a6d3fb8c100a8eaa134d366fc7ab832a6",
        "local": "/workspace/GUST/vendor/bazel/rules_closure"},

    # Rules: SASS
    "io_bazel_rules_sass": {
        "type": "github",
        "repo": "bazelbuild/rules_sass",
        "target": "7e798ffb0f4a6147b766ee2a62d7f18b8155452e",
        "seal": None},

    # Rules: Protobuf
    "rules_proto": {
        "type": "github",
        "repo": "bazelbuild/rules_proto",
        "target": "2c0468366367d7ed97a1f702f9cd7155ab3f73c5",
        "seal": "73ebe9d15ba42401c785f9d0aeebccd73bd80bf6b8ac78f74996d31f2c0ad7a6"},

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
        "target": "0a7cc6a0b6764232a0ddd31ad87b489e1d47b166",
        "seal": "3d0e809a5a14cfe7e1071103e9e53528f2fa93e72b175fb43a8bdea74156382d"},

    # Rules: Go
    "io_bazel_rules_go": {
        "type": "github",
        "repo": "bazelbuild/rules_go",
        "target": "3762b89ad8b1d71007a4a07b194a48d505613c15",
        "seal": "ea0efefaab44e96ff04778fc291145096de91f619cade08b65dbdd4c46056baa"},

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
        "target": "50ab10c32f43724f2647b894ec13c644ef727455",
        "seal": "c2510cf3e15340f24d8c6c57ae776ef899569bf7ef16952eeede4e9aea67cb12"},

    # Google: J2CL (Java-to-Closure)
    "com_google_j2cl": {
        "type": "github",
        "repo": "sgammon/j2cl",
        "target": "7e23248cc36ed25f8c8866083999e62d73f767b6",
        "seal": "e59f94fc887de6e02ed7919e6f1459a1993310964632ff87d3b53d421ab68139",
        "local": "/workspace/GUST/vendor/bazel/j2cl"},

    # Google: Clutz
    "io_angular_clutz": {
        "type": "github",
        "repo": "angular/clutz",
        "target": "c11cbb41595d8274dea6dbb983e2fba07580535e",
        "seal": None},

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
        "target": "91ef2d9dd69807b0b79555f22566fb2d81e49ff9",
        "seal": "b6cebc82e6c37b5cb01b4bcff131d325aafef854cd4336a716e5778d91a254dc"},

    # Google: API (Codegen)
    "com_google_api_codegen": {
        "type": "github",
        "repo": "googleapis/gapic-generator",
        "target": "cd919793e439d2d21505900aa08ef9f99519d8fa",
        "seal": "117af9da41fe3a35d7576b192ff8ac309222309ad8e356bd667113e0e07e5c54"},

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
        "target": "0a888dbeacebfe06acb7ba740e0723b1adb0dd52",
        "seal": "966316838b6454ca2f51718d6a801f8ebf7d1d41c82a51ac24af4d92115fa323"},
}


def _install_dependencies():

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS)


install_dependencies = _install_dependencies

