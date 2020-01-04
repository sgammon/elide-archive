
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

load("//defs:config.bzl", "LOCAL")


def _process_install_deps(deps):

    """ Process dependencies and install them. """

    for key in deps.keys():
        repo = deps[key]
        if repo["type"] != "github":
            fail("Dependencies only supports github repos, for now.")
        else:
            org = repo["repo"].split("/")[0]
            repoName = repo["repo"].split("/")[1]

            if LOCAL == True and repo.get("local") != None:
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
                    git_repository(
                      name = key,
                      remote = "git@github.com:" + repo["repo"] + ".git",
                      commit = repo["target"],
                      shallow_since = repo.get("seal"))
                else:
                    http_archive(
                        name = key,
                        strip_prefix = "%s-%s" % (repoName, repo["target"]),
                        sha256 = repo.get("seal"),
                        build_file = repo.get("overlay"),
                        url = "https://github.com/%s/archive/%s.tar.gz" % (repo["repo"], repo["target"]))


def _github_repo(name, repo, tag, sha256 = None):
    if native.existing_rule(name):
        return

    _, project_name = repo.split("/")
    http_archive(
        name = name,
        strip_prefix = "%s-%s" % (project_name, tag),
        url = "https://github.com/%s/archive/%s.zip" % (repo, tag),
        sha256 = sha256)


dependencies = _process_install_deps
github_repo = _github_repo


