
load(
    "@npm_bazel_jasmine//:index.bzl",
    _jasmine_node_test = "jasmine_node_test",
)

load(
    "//defs/toolchain/ts:tsc.bzl",
    _ts_module = "ts_module",
)


INJECTED_NODE_TEST_DEPS = [
    "@npm//jasmine",
    "@npm//@types/jasmine",
]

INJECTED_NODE_RUNTIME_TEST_DEPS = [
    # this space left intentionally blank
]


def _node_test(name,
               srcs,
               data = [],
               deps = [],
               config = str(Label("//jstests:jasmine_config.json")),
               **kwargs):

    """ Define a Jasmine-based unit test for JS use (server-side). """

    _jasmine_node_test(
        name = name,
        srcs = srcs,
        deps = (deps or []) + INJECTED_NODE_TEST_DEPS,
        data = (data or []) + INJECTED_NODE_RUNTIME_TEST_DEPS,
        config_file = config,
        coverage = True,
        **kwargs,
    )


def _node_ts_test(name,
                  srcs,
                  data = [],
                  deps = [],
                  **kwargs):

    """ Define a Jasmine-based unit test for TS use (server-side). """

    _ts_module(
        name = "%s-ts" % name,
        srcs = srcs,
        deps = (deps or []) + INJECTED_NODE_TEST_DEPS,
    )

    native.filegroup(
        name = "%s-es5.js" % name,
        srcs = [":%s-ts" % name],
        output_group = "es5_sources",
    )

    _jasmine_node_test(
        name = name,
        srcs = [":%s-es5.js" % name],
        deps = (deps or []) + INJECTED_NODE_TEST_DEPS,
        data = data,
        coverage = True,
        **kwargs
    )


node_test = _node_test
node_ts_test = _node_ts_test
