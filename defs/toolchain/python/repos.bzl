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
    "@py//:requirements.bzl",
    _base_pip_install = "pip_install",
)

load(
    "@werkzeug//:requirements.bzl",
    _werkzeug_pip_install = "pip_install",
)


def _gust_py_deps(werkzeug = True):

    """ Install Gust runtime Java dependencies. """

    _base_pip_install()
    if werkzeug:
        _werkzeug_pip_install()


gust_python_repositories = _gust_py_deps
