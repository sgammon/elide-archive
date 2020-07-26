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
    "@build_bazel_rules_apple//apple:apple.bzl",
    "apple_static_framework_import",
    "apple_dynamic_framework_import",
)


def _labelize(target):

    """ Turn an array of target paths into Labels. """

    return [Label(i) for i in target]


def _apple_framework(name, framework_imports, dynamic = False, **kwargs):

    """ Consume an Apple XCFramework. """

    if dynamic:
        apple_dynamic_framework_import(
            name = name,
            framework_imports = framework_imports,
            **kwargs,
        )
    else:
        apple_static_framework_import(
            name = name,
            framework_imports = framework_imports,
            **kwargs,
        )


def _apple_xcframework(name, path, exclude = None, **kwargs):

    """ Consume an Apple XCFramework. """

    apple_static_framework_import(
        name = name,
        framework_imports = select({
            # x86: simulator or target
            "@gust//defs/conditions:dev": native.glob([
                 "%s/ios-i386_x86_64-simulator/**" % path,
            ], exclude = exclude or []),

            # ARM: prod or debug
            "@gust//defs/conditions:release": native.glob([
                 "%s/ios-armv7_arm64/**" % path,
            ], exclude = exclude or []),
        }),
        **kwargs,
    )


apple_framework = _apple_framework
apple_xcframework = _apple_xcframework
