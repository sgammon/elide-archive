
load(
    "@io_bazel_rules_docker//container:image.bzl",
    _docker_container_image = "container_image",
)

load(
    "@io_bazel_rules_docker//container:push.bzl",
    _docker_container_push = "container_push",
)


def _container_image(name,
                     repository,
                     tag = None,
                     registry = "us.gcr.io",
                     image_format = "OCI",
                     **kwargs):

    """ Generate a regular Docker container image. """

    _docker_container_image(
        name = name,
        **kwargs
    )

    _docker_container_push(
        name = "%s-push" % name,
        image = ":%s" % name,
        tag = tag or "{BUILD_SCM_VERSION}",
        registry = registry,
        repository = repository,
        format = image_format,
    )


container_image = _container_image
