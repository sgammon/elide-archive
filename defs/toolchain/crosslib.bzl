
load(
    "@rules_java//java:defs.bzl",
    "java_library",
)

load(
    "@io_bazel_rules_kotlin//kotlin:kotlin.bzl",
    "kt_jvm_library",
)

load(
    "@com_google_j2cl//build_defs:rules.bzl",
    "j2cl_import",
    "j2cl_library",
)


def _bridge_lib(name, srcs, deps = [], elemental = [], **kwargs):

    """ Prepare a library for use on the frontend, which bridges with Closure. """

    if len(elemental) > 0:
        computed_deps = [i for i in deps if "closure/library" not in i] + [
             "@com_google_elemental2//:elemental2-%s" % i for i in elemental]
        computed_j2cl_deps = (
            ["%s-j2cl" % i for i in deps if "closure/library" not in i] + [
             i for i in deps if "closure/library" in i] + [
             "@com_google_elemental2//:elemental2-%s-j2cl" % i for i in elemental])

    java_library(
        name = name,
        srcs = srcs,
        deps = [
            "@com_google_j2cl//:jsinterop-annotations",
        ] + [i for i in deps if "java" in i] + computed_deps,
    )

    j2cl_library(
        name = "%s-j2cl" % name,
        srcs = srcs,
        deps = [
            "@com_google_j2cl//:jsinterop-annotations-j2cl",
        ] + computed_j2cl_deps,
    )

def _cross_java_lib(name,
                    srcs,
                    deps = [],
                    jdeps = [],
                    jsdeps = [],
                    elemental = [],
                    **kwargs):

    """ Prepare a library for both Java (server-side) and J2CL (client-side). """

    if len(elemental) > 0:
        computed_deps = deps + [
            "@com_google_elemental2//:elemental2-%s" % i for i in elemental]
        computed_j2cl_deps = (["%s-j2cl" % i for i in deps] + [
             "@com_google_elemental2//:elemental2-%s-j2cl" % i for i in elemental])
    else:
        computed_deps = deps
        computed_j2cl_deps = ["%s-j2cl" % i for i in deps]

    if srcs[0].endswith(".js") or srcs[0].endswith(".java"):
        if len([s for s in srcs if s.endswith(".java")]) == 0:
            # handle without injected deps
            j2cl_library(
                name = name,
                srcs = srcs,
            )
        else:
            java_library(
                name = name,
                srcs = [s for s in srcs if not s.endswith(".js")],
                deps = [
                    "@com_google_j2cl//:jsinterop-annotations",
                ] + computed_deps + jdeps,
            )

            j2cl_library(
                name = "%s-j2cl" % name,
                srcs = srcs,
                deps = [
                    "@com_google_j2cl//:jsinterop-annotations-j2cl",
                ] + computed_j2cl_deps + jsdeps,
            )

    elif srcs[0].endswith(".kt"):
        ## TODO(sgammon): fix this when https://github.com/google/j2cl/issues/75 is solved
        fail("Kotlin is not yet supported for cross-builds.")

        kt_jvm_library(
            name = "%s-kt" % name,
            srcs = srcs,
            deps = [
                "@com_google_j2cl//:jsinterop-annotations",
            ] + deps + jdeps,
        )

        j2cl_library(
            name = "%s-j2cl" % name,
            srcs = ["%s-kt.jar" % name],
            deps = [
                "@com_google_j2cl//:jsinterop-annotations-j2cl",
            ] + ["%s-j2cl" % i for i in deps] + jsdeps,
        )

bridge_lib = _bridge_lib
cross_lib = _cross_java_lib
