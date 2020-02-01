
load(
    "@io_bazel_rules_closure//closure:repositories.bzl",
    "rules_closure_dependencies",
    "rules_closure_toolchains",
)

load(
    "@com_google_j2cl//build_defs:rules.bzl",
    "setup_j2cl_workspace",
)

_RULES_CLOSURE_OMIT = [
    "com_google_auto_common",
    "com_google_template_soy",
    "com_google_template_soy_jssrc",
    "com_google_closure_stylesheets",
    "rules_java",
    "rules_proto",
    "rules_python",
]

_J2CL_CLOSURE_OMIT = [i for i in _RULES_CLOSURE_OMIT if "auto_common" not in i] + [
    "com_google_jsinterop_annotations_head",
    "org_gwtproject_gwt",
]


def _setup_workspace():

    """ Setup workspace hooks, toolchains, and repos. """

    ## Closure
    rules_closure_dependencies(**dict([("omit_%s" % label, True) for label in _RULES_CLOSURE_OMIT]))
    rules_closure_toolchains()

    ## J2CL
    setup_j2cl_workspace(**dict([("omit_%s" % label, True) for label in _J2CL_CLOSURE_OMIT]))


setup_workspace = _setup_workspace
