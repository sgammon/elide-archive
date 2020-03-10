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
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    _http_archive="http_archive",
)

load(
    "@bazel_tools//tools/build_defs/repo:git.bzl",
    _git_repository="git_repository",
)

load(
    "@bazel_tools//tools/build_defs/repo:java.bzl",
    _java_import_external = "java_import_external",
)

load(
    "//defs:config.bzl",
    _LOCAL = "LOCAL",
)


def _process_install_deps(deps):

    """ Process dependencies and install them. """

    for key in deps.keys():
        repo = deps[key]
        if repo["type"] == "github":
            _process_github_dep(key, repo)
        elif repo["type"] == "java":
            _process_java_dep(key, repo)
        elif repo["type"] == "archive":
            _process_archive_dep(key, repo)
        else:
            fail(("Unrecognized dependency type: '%s' for package '%s'."
                    % (repo["type"], key)))


def _process_archive_dep(key, repo):

    """ Process an external archive dependency. """

    _http_archive(
        name = key,
        url = repo.get("target"),
        urls = repo.get("targets"),
        strip_prefix = repo.get("strip"),
        sha256 = repo.get("seal"),
        build_file = repo.get("overlay"),
    )


def _process_java_dep(key, repo):

    """ Process an external Java dependency. """

    _java_import_external(
        name = key,
        licenses = repo.get("licenses"),
        jar_urls = repo["targets"],
        jar_sha256 = repo.get("seal"),
        deps = repo.get("deps", []),
        extra_build_file_content = repo.get("inject"),
    )


def _process_github_dep(key, repo):

    """ Process a dependency declaration from Github. """

    org = repo["repo"].split("/")[0]
    repoName = repo["repo"].split("/")[1]

    if (_LOCAL == True or repo.get("forceLocal") == True) and repo.get("local") != None:
        # local override
        if repo.get("overlay") != None:
            # local new
            native.new_local_repository(
                name = key,
                build_file = "//external:%s" % repo["overlay"],
                path = repo["local"])
        else:
            # regular local
            native.local_repository(
                name = key,
                path = repo["local"])

    else:
        if repo.get("private") == True:
            _git_repository(
              name = key,
              remote = "git@github.com:" + repo["repo"] + ".git",
              commit = repo["target"],
              shallow_since = repo.get("seal"))
        else:
            if repo.get("directory") != None:
                renderedTarget = "%s/%s" % (repo["target"], repo["directory"])
            else:
                renderedTarget = repo["target"]
            _http_archive(
                name = key,
                strip_prefix = "%s-%s" % (repoName, renderedTarget),
                sha256 = repo.get("seal"),
                build_file = repo.get("overlay"),
                url = "https://github.com/%s/archive/%s.tar.gz" % (repo["repo"], repo["target"]))


def _github_repo(name, repo, tag, sha256 = None):

    """ Declare a Github repository. """

    if native.existing_rule(name):
        return

    if repo.get("directory") != None:
        renderedTarget = "%s/%s" % (tag, repo["directory"])
    else:
        renderedTarget = tag

    _, project_name = repo.split("/")
    _http_archive(
        name = name,
        strip_prefix = "%s-%s" % (project_name, renderedTarget),
        url = "https://github.com/%s/archive/%s.zip" % (repo, tag),
        sha256 = sha256)


dependencies = _process_install_deps
github_repo = _github_repo
git_repository = _git_repository
http_archive = _http_archive
