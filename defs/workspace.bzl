
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
    "com_google_closure_stylesheets",
    "com_google_template_soy_jssrc",
    "com_google_template_soy",
]

_J2CL_CLOSURE_OMIT = [i for i in _RULES_CLOSURE_OMIT if "auto_common" not in i] + [
    "org_gwtproject_gwt",
    "com_google_jsinterop_annotations_head",
]


def _setup_workspace():

    """ Setup workspace hooks, toolchains, and repos. """

    ## Closure
    rules_closure_dependencies(**dict([("omit_%s" % label, True) for label in _RULES_CLOSURE_OMIT]))
    rules_closure_toolchains()

    ## J2CL
    setup_j2cl_workspace(**dict([("omit_%s" % label, True) for label in _J2CL_CLOSURE_OMIT]))


setup_workspace = _setup_workspace
