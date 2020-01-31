
load(
    "@rules_python//python:defs.bzl",
    _py_test = "py_test",
)


def _werkzeug_test():

    """ Defines a Python test, which injects Werkzeug dependencies and facilitates easy service
        mocking. """

    pass


py_test = _py_test
werkzeug_test = _werkzeug_test
