
load(
    "@rules_pkg//pkg:pkg.bzl",
    _pkg_tar = "pkg_tar",
    _pkg_zip = "pkg_zip",
)

load(
    "@io_bazel_rules_k8s//k8s:object.bzl",
    _k8s_object = "k8s_object",
)

load(
    "@io_bazel_rules_k8s//k8s:objects.bzl",
    _k8s_objects = "k8s_objects",
)



def _k8s_config(name,
                kind = None,
                template = None,
                deps = None,
                **kwargs):

    """ Generate targets for a generic Kubernetes config file. """

    if deps != None and template != None:
        fail("Cannot specify both `deps` and `template` for k8s_config. Please use `deps=` for groupings of " +
             "Kubernetes objects.")

    native.filegroup(
        name = "%s-files" % name,
        srcs = (template and [template] or []) + (deps or []),
    )

    if deps != None:
        _pkg_tar(
            name = "%s-tar" % name,
            srcs = [":%s-files" % name],
            deps = [
                ("%s-tar" % n) for n in (deps or [])
            ],
        )

        _pkg_tar(
            name = "%s-zip" % name,
            srcs = [":%s-files" % name],
            deps = [
                ("%s-zip" % n) for n in (deps or [])
            ],
        )

        _k8s_objects(
            name = name,
            objects = deps,
            **kwargs
        )

    else:
        _k8s_object(
            name = name,
            kind = kind,
            template = template,
            **kwargs
        )


k8s_config = _k8s_config
