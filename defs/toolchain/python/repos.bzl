
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
