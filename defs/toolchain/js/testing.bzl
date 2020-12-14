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
    "@io_bazel_rules_closure//closure:defs.bzl",
    _closure_js_deps = "closure_js_deps",
    _closure_js_test = "closure_js_test",
    _closure_js_binary = "closure_js_binary",
    _closure_js_library = "closure_js_library",
)

load(
    "@npm//@bazel/karma:index.bzl",
    _karma_web_test = "karma_web_test",
    _karma_web_test_suite = "karma_web_test_suite",
)

load(
    "//defs/toolchain/context:props.bzl",
    _annotate_defs_dict = "annotate_defs_dict",
    _annotate_defs_flags = "annotate_defs_flags",
)

DEFAULT_PEER_DEPS = [
    "@npm//@bazel/karma",
    "@npm//jasmine-core",
    "@npm//karma",
    "@npm//karma-coverage",
    "@npm//karma-chrome-launcher",
    "@npm//karma-firefox-launcher",
    "@npm//karma-jasmine",
    "@npm//karma-jasmine-html-reporter",
    "@npm//karma-requirejs",
    "@npm//karma-sourcemap-loader",
    "@npm//requirejs",
    "@npm//tmp",
]

TEST_COMPILER_FLAGS = [
    "--env=BROWSER",
    "--isolation_mode=NONE",
    "--dependency_mode=PRUNE_LEGACY",
    "--process_common_js_modules",
    "--process_closure_primitives",
    "--formatting=PRETTY_PRINT",
    "--rewrite_polyfills",
    "--use_types_for_optimization",
    "--create_source_map=%outname%.map",
    "--source_map_include_content",
]


def _make_suffix(path):
    return "_" + path.replace("_test.js", "").replace("-", "_").replace("/", "_")


def _js_bin_test(name,
                 srcs = None,
                 data = None,
                 deps = None,
                 defs = None,
                 compilation_level = "BUNDLE",
                 css = None,
                 entry_points = None,
                 html = None,
                 language = None,
                 lenient = False,
                 suppress = None,
                 visibility = None,
                 karma_config = str(Label("@gust//tests:karma_config.js")),
                 tags = [],
                 extra_compiler_flags = TEST_COMPILER_FLAGS,
                 extra_peer_deps = [],
                 **kwargs):

    """ Build a closure JS test. """

    overlay_defs = _annotate_defs_flags(defs or {})

    if not srcs:
        fail("js_test rules cannot have an empty 'srcs' list")
    if language:
        print("js_test 'language' is removed and now always ES6 strict")
    for src in srcs:
        if not src.endswith("_test.js"):
            fail("js_test srcs must be files ending with _test.js")
    if len(srcs) == 1:
        work = [(name, srcs)]
    else:
        work = [(name + _make_suffix(src), [src]) for src in srcs]

    for shard, sauce in work:
        _closure_js_library(
            name = "%s_lib" % shard,
            srcs = sauce,
            data = data,
            deps = (deps or []) + [
                "@io_bazel_rules_closure//closure/library:testing",
                "@io_bazel_rules_closure//closure/testing/externs:jasmine",
            ],
            lenient = lenient,
            suppress = suppress,
            visibility = visibility,
            testonly = True,
            tags = tags,
        )

        if type(entry_points) == type({}):
            ep = entry_points.get(sauce[0])
        else:
            ep = entry_points

        _closure_js_binary(
            name = "%s_js" % shard,
            deps = [
                ":%s_lib" % shard,
            ],
            compilation_level = compilation_level,
            css = css,
            debug = True,
            defs = overlay_defs + (extra_compiler_flags or []),
            entry_points = ep,
            formatting = "PRETTY_PRINT",
            visibility = visibility,
            testonly = True,
            tags = tags,
        )

        _karma_web_test(
            name = shard,
            srcs = [
                ":%s_js.js" % shard,
            ],
            bootstrap = [str(Label("@io_bazel_rules_closure//closure/testing:karma_runner.js"))],
            config_file = karma_config,
            peer_deps = DEFAULT_PEER_DEPS + (extra_peer_deps or []),
        )


js_test = _js_bin_test
