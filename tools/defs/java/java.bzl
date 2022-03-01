MAVEN_REPO = "@maven"

def _transform_dep(target):
    """Transform a Maven dependency target."""
    return target.replace(".", "_").replace("-", "_").replace(":", "_")

def maven(target):
    """Calculate a Maven target."""
    return "%s//:%s" % (
        MAVEN_REPO,
        _transform_dep(target),
    )
