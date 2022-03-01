def _target_name(name, postfix):
    if "/" in name and ":" in name:
        # it's an absolute reference with full spec.
        return "%s:%s_%s" % (
            name.split(":")[0],
            name.split(":")[1],
            postfix,
        )
    elif "/" in name and ":" not in name:
        # it's an absolute reference with shorthand. resolve the last path portion and apply it as a target name.
        alias = (name.startswith("//") and name[2:] or name).split("/")[-1]
        return "%s:%s_%s" % (
            name,
            alias,
            postfix,
        )
    else:
        # it's a relative reference. safe to string-doctor and return.
        return "%s_%s" % (
            name,
            postfix,
        )

target_name = _target_name
