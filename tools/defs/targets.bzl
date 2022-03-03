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

"""Provides generic macros and utilities."""

load(
    "//tools/defs/java:java.bzl",
    _java_library = "java_library",
    _maven = "maven",
)
load(
    "//tools/defs/model:internals.bzl",
    _proto_library = "proto_library",
)
load(
    "//tools/defs/model:model.bzl",
    _model = "model",
)

def _replace(format, name):
    """Replace a string-formatted name during target fanout."""
    return format.replace("%s", name)

def _replace_all(items, name):
    """Replace every string in a list of string-formatted names during target fanout."""
    return [_replace(i, name) for i in items]

def _dep_lookup(map, name):
    """Lookup extra dependencies in the provided `map`, at key `name`, returning an empty array if none are found."""
    return map.get(name, [])

def fanout(
        entries,
        rule,
        name = None,
        srcs = [],
        deps = [],
        runtime_deps = [],
        extra_deps = {},
        extra_runtime_deps = {},
        package = None,
        package_info = None,
        package_srcs = [],
        package_kwargs = {},
        **kwargs):
    """Given a list of names, produce a corresponding fanned-out set of targets via `rule`. Provide the entry value to
       each member of `srcs`, `deps`, and `runtime_deps`, and apply any `kwargs` to the resulting targets.

       If `extra_deps` are provided, a lookup will be made using the value from `entries` as a key during each
       iteration, with an array expected in response (if any); default values result in an empty array. Any resulting
       deps from the lookup are appended as extra `deps`. The same goes for `extra_runtime_deps`, but resulting deps are
       appended to `runtime_deps`.

       All string formatting occurs via regular Python string interpolation, with the single and only variable being a
       string which replaces with the entry value in each iteration (i.e. `%s`).

       :param entries: Entries to generate targets from.
       :param rule: Rule to invoke for each target.
       :param name: Name template to use for each target.
       :param srcs: Sources to wire in for each target, with string formatting applied.
       :param deps: Dependencies to wire in for each target, with string formatting applied.
       :param runtime_deps: Runtime-only dependencies to wire in for each target, with string formatting applied.
       :param extra_deps: Lookup-based dependencies to add to matching targets as `deps`.
       :param extra_runtime_deps: Lookup-based dependencies to add to matching targets as `runtime_deps`.
       :param package: Special case: Rollup all sources into an exports-only `rule` at this name.
       :param package_info: Include the specified `package-info.java` source in the rolled-up `rule`.
       :param package_srcs: All package sources which should roll-up for Javadoc purposes.
       :param package_kwargs: Additional keyword arguments to pass to the rolled-up `rule`.
       :param kwargs: Keyword arguments to pass along to each invocation of `rule`.
       """

    [
        rule(
            name = _replace(name or "%s", t),
            srcs = _replace_all(srcs, t),
            deps = _replace_all(deps, t) + _dep_lookup(extra_deps, t),
            runtime_deps = _replace_all(runtime_deps, t) + _dep_lookup(extra_runtime_deps, t),
            **kwargs
        ) for t in entries
    ]

    if package:
        rule(
            name = package.split(".")[-1],  ## use last portion of package path
            srcs = package_info and [package_info] or [],
            exports = [":%s" % n for n in entries],
            **package_kwargs
        )
    if package_srcs:
        native.filegroup(
            name = "sources",
            srcs = package_srcs,
        )

def protos(
        protos,
        rule = _model,
        name = None,
        srcs = ["%s.proto"],
        deps = [],
        extra_deps = {},
        package = None,
        package_srcs = [],
        package_kwargs = {},
        **kwargs):
    """Given a list of names, produce a corresponding fanned-out set of Protocol Buffer model targets via `rule`.
       Provide the entry value to each member of `srcs`, `deps`, and `runtime_deps`, and apply any `kwargs` to the
       resulting targets.

       If `extra_deps` are provided, a lookup will be made using the value from `protos` as a key during each
       iteration, with an array expected in response (if any); default values result in an empty array. Any resulting
       deps from the lookup are appended as extra `deps`.

       All string formatting occurs via regular Python string interpolation, with the single and only variable being a
       string which replaces with the entry value in each iteration (i.e. `%s`).

       :param protos: Entries to generate targets from.
       :param rule: Rule to invoke for each target.
       :param name: Name template to use for each target.
       :param srcs: Sources to wire in for each target, with string formatting applied.
       :param deps: Dependencies to wire in for each target, with string formatting applied.
       :param extra_deps: Lookup-based dependencies to add to matching targets as `deps`.
       :param package: Special case: Rollup all sources into an exports-only `rule` at this name.
       :param package_srcs: All package sources which should roll-up for proto-doc purposes.
       :param package_kwargs: Additional keyword arguments to pass to the rolled-up `rule`.
       :param kwargs: Keyword arguments to pass along to each invocation of `rule`.
       """

    [
        rule(
            name = _replace(name or "%s", t),
            srcs = _replace_all(srcs, t),
            deps = _replace_all(deps, t) + _dep_lookup(extra_deps, t),
            **kwargs
        ) for t in protos
    ]

    if package:
        _proto_library(
            name = package.split(".")[-1],  ## use last portion of package path
            exports = [":%s" % n for n in protos],
            **package_kwargs
        )
    if package_srcs:
        native.filegroup(
            name = "sources",
            srcs = package_srcs,
        )

def java_package(
        entries,
        rule = _java_library,
        srcs = ["%s.java"],
        deps = [],
        maven = [],
        package_info = "package-info.java",
        **kwargs):
    """Syntactic sugar to fan-out a set of Java libraries. For use documentation, see `fanout` in this same module.

       Defaults set by this method include:
        rule: `java_library`
        srcs: `["%s.java"]`
        package_info: `"package-info.java"`"""
    fanout(
        entries,
        rule = rule,
        srcs = srcs,
        deps = deps + [_maven(dep) for dep in maven],
        package_info = package_info,
        **kwargs
    )


## Re-export common tools for server-side development
maven = _maven
