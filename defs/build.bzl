
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

load("//defs:tools.bzl", "dependencies")


DEPS = {
    # Bazel: Skylib
    "bazel_skylib": {
        "type": "github",
        "repo": "bazelbuild/bazel-skylib",
        "target": "327d61b5eaa15c11a868a1f7f3f97cdf07d31c58",
        "seal": "4542fed7aafdda95ee356dcb6bb5cc74cc6e5da1e54344ab206e550335804264"},

    # Bazel: Gazelle
    "bazel_gazelle": {
        "type": "github",
        "repo": "bazelbuild/bazel-gazelle",
        "target": "dd3ccdc3d36a76d76215f64f1dacb2d230504a27",
        "seal": "a1f89662ebd4c081d3e66c9560ada11d6a0c93660568a46e4a9063670d81f986"},

    # Rules: Closure
    "io_bazel_rules_closure": {
        "type": "github",
        "repo": "bazelbuild/rules_closure",
        "target": "9de67f151743d4d54963f3a8184259c8b5326677",
        "seal": "2746b2bc570104ce48f5fd58ac6101dd810d6ba3de8c7d6cd39382bd92ec12c4"},

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

    # Rules: External JVM
    "rules_jvm_external": {
        "type": "github",
        "repo": "bazelbuild/rules_jvm_external",
        "target": "0a58d828a5788c3f156435540fe24337ccee8616",
        "seal": "1ebe327ead0075a716f83f06266c2b22c629f3065327c84c547b87544958fe65"},

    # Rules: Go
    "io_bazel_rules_go": {
        "type": "github",
        "repo": "bazelbuild/rules_go",
        "target": "3762b89ad8b1d71007a4a07b194a48d505613c15",
        "seal": "ea0efefaab44e96ff04778fc291145096de91f619cade08b65dbdd4c46056baa"},

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
        "repo": "google/j2cl",
        "target": "333b142943bae6a115d537cd8dc2cc8b10604f98",
        "seal": "7cf86f05009f886c491c5d8e4c82ce2906b81efb30b71022fe0c6082924fa1e6"},

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

    # BuildStack: Protobuf Rules
    "build_stack_rules_proto": {
        "type": "github",
        "repo": "stackb/rules_proto",
        "target": "0a888dbeacebfe06acb7ba740e0723b1adb0dd52",
        "seal": "966316838b6454ca2f51718d6a801f8ebf7d1d41c82a51ac24af4d92115fa323"}
}


def _install_dependencies():

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS)


install_dependencies = _install_dependencies

