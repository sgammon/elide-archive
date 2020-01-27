

def _closure_path(*path):

    """ Computes a Closure Library dependency path, based on the
        `goog.provide`/`goog.module` path desired. """

    genpath = ""
    if len(path) == 1:
        genpath = "%s:%s" % (path[0], path[0])  # like `asserts` -> `asserts:asserts`
    elif len(path) > 1:
        genpath = "%s:%s" % ("/".join(path[0:-1]), path[-1].lower())  # like `some,deep,obj` -> `some/deep:obj`
    else:
        fail("Failed to figure out closure dependency path: '%s'." % str(path))

    return "@io_bazel_rules_closure//closure/library/%s" % genpath


closure = _closure_path
