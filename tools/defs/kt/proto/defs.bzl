##
# Copyright © 2022, The Elide Framework Authors. All rights reserved.
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

"""
Provides kt_jvm_proto_library to generate Kotlin protos.
"""

load("@rules_proto//proto:defs.bzl", "ProtoInfo")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

KtProtoLibInfo = provider(
    "Information for a Kotlin JVM proto library.",
    fields = {"srcjars": "depset of .srcjar Files"},
)

def get_real_short_path(file):
    """Returns the correct short path for a `File`.
    Args:
        file: the `File` to return the short path for.
    Returns:
        The short path for `file`, handling any non-standard path segments if
        it's from external repositories.
    """

    # For some reason, files from other archives have short paths that look like:
    #   ../com_google_protobuf/google/protobuf/descriptor.proto
    short_path = file.short_path
    if short_path.startswith("../"):
        second_slash = short_path.index("/", 3)
        short_path = short_path[second_slash + 1:]

    # Sometimes it has another few prefixes like:
    #   _virtual_imports/any_proto/google/protobuf/any.proto
    #   benchmarks/_virtual_imports/100_msgs_proto/benchmarks/100_msgs.proto
    # We want just google/protobuf/any.proto.
    virtual_imports = "_virtual_imports/"
    if virtual_imports in short_path:
        short_path = short_path.split(virtual_imports)[1].split("/", 1)[1]
    return short_path

def _run_protoc(ctx, proto_lib, output_dir):
    """Executes protoc to generate Kotlin files."""
    proto_info = proto_lib[ProtoInfo]
    descriptor_sets = proto_info.transitive_descriptor_sets
    transitive_descriptor_set = depset(transitive = [descriptor_sets])
    proto_sources = [get_real_short_path(file) for file in proto_info.direct_sources]

    protoc_args = ctx.actions.args()
    protoc_args.set_param_file_format("multiline")
    protoc_args.use_param_file("@%s")
    protoc_args.add("--kotlin_out=" + output_dir.path)
    protoc_args.add_joined(
        transitive_descriptor_set,
        join_with = ctx.configuration.host_path_separator,
        format_joined = "--descriptor_set_in=%s",
    )
    protoc_args.add_all(proto_sources)
    ctx.actions.run(
        inputs = depset(transitive = [transitive_descriptor_set]),
        outputs = [output_dir],
        executable = ctx.executable._protoc,
        arguments = [protoc_args],
        mnemonic = "KtProtoc",
        progress_message = "Generating Kotlin protos for " + ctx.label.name,
    )

def _create_srcjar(ctx, input_dir, output_jar):
    """Bundles .kt files into a srcjar."""
    zipper_args = ctx.actions.args()
    zipper_args.add("c", output_jar)
    zipper_args.add_all([input_dir])

    ctx.actions.run(
        outputs = [output_jar],
        inputs = [input_dir],
        executable = ctx.executable._zipper,
        arguments = [zipper_args],
        mnemonic = "KtProtoSrcJar",
        progress_message = "Generating Kotlin proto srcjar for " + ctx.label.name,
    )

def _merge_srcjars(ctx, input_jars, output_jar):
    """Merges multiple srcjars into a single srcjar."""
    tmp_dir_name = ctx.label.name + "_srcjars"
    tmp_dir = ctx.actions.declare_directory(tmp_dir_name)

    merge_args = ctx.actions.args()
    merge_args.add(ctx.executable._zipper)
    merge_args.add(tmp_dir.path)
    merge_args.add_all(input_jars)

    ctx.actions.run(
        outputs = [tmp_dir],
        inputs = input_jars,
        executable = ctx.executable._extract_srcjars,
        tools = [ctx.executable._zipper],
        arguments = [merge_args],
        mnemonic = "KtProtoExtractSrcJars",
    )

    _create_srcjar(ctx, tmp_dir, output_jar)

def _kt_jvm_proto_aspect_impl(target, ctx):
    name = target.label.name

    gen_src_dir_name = name + "_kt_jvm_srcs"
    gen_src_dir = ctx.actions.declare_directory(gen_src_dir_name)
    _run_protoc(ctx, target, gen_src_dir)

    srcjar_name = name + "_kt_jvm.srcjar"
    srcjar = ctx.actions.declare_file(srcjar_name)
    _create_srcjar(ctx, gen_src_dir, srcjar)

    transitive = [
        dep[KtProtoLibInfo].srcjars
        for dep in ctx.rule.attr.deps
        if KtProtoLibInfo in dep
    ]
    return [KtProtoLibInfo(
        srcjars = depset(direct = [srcjar], transitive = transitive),
    )]

_kt_jvm_proto_aspect = aspect(
    attrs = {
        "_zipper": attr.label(
            default = Label("@bazel_tools//tools/zip:zipper"),
            cfg = "host",
            executable = True,
        ),
        "_protoc": attr.label(
            default = Label("@com_google_protobuf//:protoc"),
            cfg = "host",
            executable = True,
        ),
    },
    implementation = _kt_jvm_proto_aspect_impl,
    attr_aspects = ["deps"],
)

def _kt_jvm_proto_library_helper_impl(ctx):
    """Implementation of _kt_jvm_proto_library_helper rule."""
    proto_lib_info = ctx.attr.proto_dep[KtProtoLibInfo]
    _merge_srcjars(ctx, proto_lib_info.srcjars, ctx.outputs.srcjar)

_kt_jvm_proto_library_helper = rule(
    attrs = {
        "proto_dep": attr.label(
            providers = [ProtoInfo],
            aspects = [_kt_jvm_proto_aspect],
        ),
        "srcjar": attr.output(
            doc = "Generated Java source jar.",
            mandatory = True,
        ),
        "_zipper": attr.label(
            default = Label("@bazel_tools//tools/zip:zipper"),
            cfg = "host",
            executable = True,
        ),
        "_extract_srcjars": attr.label(
            default = Label("@wfa_common_jvm//build/kt_jvm_proto:extract_srcjars"),
            cfg = "host",
            executable = True,
        ),
    },
    implementation = _kt_jvm_proto_library_helper_impl,
)

def kt_jvm_proto_library(name, srcs = None, deps = None, **kwargs):
    """Generates Kotlin code for a protocol buffer library.
    For standard attributes, see:
      https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes
    Args:
      name: A name for the target
      srcs: Exactly one proto_library target to generate Kotlin APIs for
      deps: Exactly one java_proto_library target for srcs[0]
      **kwargs: other args to pass to the ultimate kt_jvm_library target
    """
    srcs = srcs or []
    deps = deps or []

    if len(srcs) != 1:
        fail("Expected exactly one src", "srcs")

    if len(deps) != 1:
        fail("Expected exactly one dep", "deps")

    generated_kt_name = name + "_DO_NOT_DEPEND_generated_kt"
    generated_srcjar = generated_kt_name + ".srcjar"
    _kt_jvm_proto_library_helper(
        name = generated_kt_name,
        proto_dep = srcs[0],
        srcjar = generated_srcjar,
        visibility = ["//visibility:private"],
    )

    kt_jvm_library(
        name = name,
        srcs = [generated_srcjar],
        # TODO: add Bazel rule in protobuf instead of relying on Maven
        deps = deps + ["@maven//:com_google_protobuf_protobuf_kotlin"],
        exports = deps,
        kotlinc_opts = "@elide//tools/defs/kt/proto:proto_gen_kt_options",
        **kwargs
    )
