##
# Copyright © 2020, The Gust Framework Authors. All rights reserved.
#
# The Gust/Elide framework and tools, and all associated source or object computer code, except where otherwise noted,
# are licensed under the Zero Prosperity license, which is enclosed in this repository, in the file LICENSE.txt. Use of
# this code in object or source form requires and implies consent and agreement to that license in principle and
# practice. Source or object code not listing this header, or unless specified otherwise, remain the property of
# Elide LLC and its suppliers, if any. The intellectual and technical concepts contained herein are proprietary to
# Elide LLC and its suppliers and may be covered by U.S. and Foreign Patents, or patents in process, and are protected
# by trade secret and copyright law. Dissemination of this information, or reproduction of this material, in any form,
# is strictly forbidden except in adherence with assigned license requirements.
##

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
    "com_google_code_gson",
    "com_google_template_soy",
    "com_google_template_soy_jssrc",
    "rules_java",
    "rules_proto",
    "rules_python",
]

_J2CL_CLOSURE_OMIT = [i for i in _RULES_CLOSURE_OMIT if "auto_common" not in i]


def _setup_workspace():

    """ Setup workspace hooks, toolchains, and repos. """

    ## Closure
    rules_closure_dependencies(**dict([("omit_%s" % label, True) for label in _RULES_CLOSURE_OMIT]))
    rules_closure_toolchains()

    ## J2CL
    setup_j2cl_workspace(**dict([("omit_%s" % label, True) for label in _J2CL_CLOSURE_OMIT]))


setup_workspace = _setup_workspace
