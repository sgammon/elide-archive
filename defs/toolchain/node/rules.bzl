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
    "@com_google_j2cl//build_defs:rules.bzl",
    _j2cl_application = "j2cl_application",
)

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    _closure_js_library = "closure_js_library",
    _closure_js_binary = "closure_js_binary",
)

load(
    "@build_bazel_rules_nodejs//:index.bzl",
    _pkg_web = "pkg_web",
    _nodejs_binary = "nodejs_binary",
)

load(
    "@npm//@bazel/rollup:index.bzl",
    _rollup_bundle = "rollup_bundle",
)

load(
    "//defs/toolchain/ts:tsc.bzl",
    _ts_module = "ts_module",
)

load(
    "//defs/toolchain/java:rules.bzl",
    "dedupe_deps_",
)

load(
    "@io_bazel_rules_docker//nodejs:image.bzl",
    _nodejs_image = "nodejs_image",
)

load(
    "@io_bazel_rules_docker//container:push.bzl",
    _container_push = "container_push",
)


INJECTED_NODE_DEPS = [
    "@npm//express",
    "@npm//winston",
    "@npm//@types/node",
    "@npm//@types/express",
    "@npm//@types/winston",
    "@npm//@feathersjs/express",
    "@npm//@feathersjs/feathers",
    "@gust//js/backend:init",
    "@gust//js/backend:app",
]

INJECTED_NODE_RUNTIME_DEPS = [
    "@gust//js/backend:init",
    "@gust//js/backend:main",
]


def _feathers_library(name,
                      srcs = None,
                      deps = None,
                      **kwargs):

    """ Build a closure JS library, used for metadata for a FeathersJS/ExpressJS target. """

    _ts_module(
        name = name,
        srcs = srcs,
        deps = (deps or []) + INJECTED_NODE_DEPS,
    )

    native.filegroup(
        name = "%s-es5" % name,
        srcs = [":%s" % name],
        output_group = "es5_sources",
    )


def _feathers_app(name,
                  controllers = [],
                  entry_point = None,
                  main = str(Label("@gust//js/backend:main.js")),
                  deps = None,
                  env = [],
                  base = None,
                  repository = None,
                  registry = "us.gcr.io",
                  tag = None,
                  image_format = "OCI",
                  **kwargs):

    """ Build a combined backend JS application target, for FeathersJS/ExpressJS. """

    if entry_point == None:
        _rollup_bundle(
            name = "%s-bundle" % name,
            srcs = [("%s-es5" % i) for i in controllers] + [main],
            deps = (deps or []) + [main],
            entry_point = main,
            format = "cjs",
            sourcemap = "inline",
        )

        _nodejs_binary(
            name = name,
            entry_point = main,
            data = (deps or []) + INJECTED_NODE_RUNTIME_DEPS,
            install_source_map_support = True,
            **kwargs
        )

    else:
        if entry_point.endswith(".ts"):
            _ts_module(
                name = "%s-ts" % name,
                srcs = [entry_point],
                deps = dedupe_deps_((deps or []) + INJECTED_NODE_DEPS),
            )

            native.filegroup(
                name = "%s-es5.js" % name,
                srcs = [":%s-ts" % name],
                output_group = "es5_sources",
            )

            _rollup_bundle(
                name = "%s-bundle" % name,
                srcs = ["%s-es5.js" % name],
                entry_point = "%s-es5.js" % name,
            )
        else:
            _rollup_bundle(
                name = "%s-bundle" % name,
                srcs = [entry_point],
                entry_point = entry_point,
            )

        _nodejs_binary(
            name = name,
            entry_point = "%s-bundle.js" % name,
            data = dedupe_deps_((deps or []) + INJECTED_NODE_RUNTIME_DEPS + [

            ]),
            install_source_map_support = True,
            **kwargs
        )

        if base == None:
            _nodejs_image(
                name = "%s-image" % name,
                entry_point = "%s-bundle.js" % name,
                data = dedupe_deps_((deps or []) + INJECTED_NODE_DEPS + INJECTED_NODE_RUNTIME_DEPS),
                install_source_map_support = True,
            )

        else:
            fail("NodeJS does not support custom base images yet.")

        if repository != None:
            _container_push(
                name = "%s-image-push" % name,
                image = ":%s-image" % name,
                format = image_format,
                registry = registry,
                repository = repository,
                tag = tag or "{BUILD_SCM_VERSION}",
            )


feathers_app = _feathers_app
feathers_library = _feathers_library
