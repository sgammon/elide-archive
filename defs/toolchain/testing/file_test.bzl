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

_LICENSE =r"""/*
 Copyright The Closure Library Authors.
 SPDX-License-Identifier: Apache-2.0
*/
""".replace("\n", "\\n").replace("*", "\\*")


def _impl(ctx):
    exe = ctx.outputs.executable
    file_ = ctx.file.file
    content = ctx.attr.content
    regexp = ctx.attr.regexp
    matches = ctx.attr.matches
    if content and regexp:
        fail("Must specify one and only one of content or regexp")
    if content and matches != -1:
        fail("matches only makes sense with regexp")
    if not regexp:
        # Make sure content ends with new file since sed will always add one in Mac
        if content and not content.endswith("\n"):
            content += "\n"

        expected = ctx.actions.declare_file(exe.basename + ".expected")
        ctx.actions.write(
            output = expected,
            content = content,
        )

        actual = ctx.actions.declare_file(exe.basename + ".actual")
        ctx.actions.run_shell(
            inputs = [file_],
            outputs = [actual],
            arguments = [file_.path, actual.path],
            command = "sed -e ':a' -e '$!N' -e '$!ba' -e 's|%s||' -e '$a\\' $1>$2" % _LICENSE,
        )

        ctx.actions.write(
            output = exe,
            content = "diff -u %s %s" % (expected.short_path, actual.short_path),
            is_executable = True,
        )
        return struct(runfiles = ctx.runfiles([exe, expected, actual]))
    if matches != -1:
        script = "[ %s == $(grep -c %s %s) ]" % (
            matches,
            repr(regexp),
            file_.short_path,
        )
    else:
        script = "grep %s %s" % (repr(regexp), file_.short_path)
    if ctx.attr.invert:
        script = "! " + script
    ctx.actions.write(
        output = exe,
        content = script,
        is_executable = True,
    )
    return struct(runfiles = ctx.runfiles([exe, file_]))

_file_test = rule(
    attrs = {
        "file": attr.label(
            mandatory = True,
            allow_single_file = True,
        ),
        "content": attr.string(default = ""),
        "regexp": attr.string(default = ""),
        "matches": attr.int(default = -1),
        "invert": attr.bool(),
    },
    executable = True,
    implementation = _impl,
    test = True,
)

def file_test(size = "small", **kwargs):

    """ Test generated file content. """

    _file_test(size = size, **kwargs)
