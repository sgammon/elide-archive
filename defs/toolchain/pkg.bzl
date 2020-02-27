
load(
    "@bazel_tools//tools/build_defs/pkg:pkg.bzl",
    _pkg_tar = "pkg_tar",
)


load(
    "@rules_pkg//pkg:pkg.bzl",
    _pkg_zip = "pkg_zip",
)


def _archive(name,
             srcs,
             deps = None,
             format = "tar",
             **kwargs):

    """ Create a generic tarball or zip archive. """

    if format == "tar":
        _pkg_tar(
            name = name,
            srcs = srcs,
            deps = deps,
            **kwargs
        )
    elif format == "zip":
        _pkg_zip(
            name = name,
            srcs = srcs,
            deps = deps,
            **kwargs
        )
    else:
        fail("Unrecognized package format: '%s'." % format)


zipball = _pkg_zip
tarball = _pkg_tar
archive = _archive
