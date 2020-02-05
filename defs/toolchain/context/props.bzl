
load(
    "//defs:config.bzl",
    _GUST_DEV = "DEV",
    _GUST_DEBUG = "DEBUG",
    _GUST_VERSION = "VERSION",
)

BASE_SYSTEM_PROPS = {
    "framework": "gust",
}

BASE_CLOSURE_FLAGS = [
    "--charset=UTF-8",
    "--inject_libraries",
    "--rewrite_polyfills",
    "--use_types_for_optimization",
    "--process_closure_primitives",
    "--process_common_js_modules",
    "--module_resolution=NODE",
    "--dependency_mode=PRUNE",
    "--isolation_mode=IIFE",
    "--generate_exports",
    "--export_local_property_definitions",
    "--assume_function_wrapper",
    "--env=BROWSER",
]


def _dedupe(list_of_strings):
    index = []
    built = []
    for item in list_of_strings:
        if item not in index:
            built.append(item)
    return built


def _annotate_defs_dict(props, override = {}, system = True):

    """ Annotate a defs dictionary with the provided properties, default state, and overrides to any default state. The
        result should be usable when defining Closure/J2CL `defs`, or usable via Java's `System.getProperty`. """

    config = dict()
    config.update(props)
    config.update(**{
        "gust.dev": _GUST_DEV,
        "gust.debug": _GUST_DEBUG,
        "gust.version": _GUST_VERSION,
    })
    config.update(**override)
    if system: config.update(**BASE_SYSTEM_PROPS)
    return config


def _symbolize_path(keypath):

    """ Take a key path like `gust.version`, and convert it to a global symbol like `GUST_VERSION`, which is usable as,
        say, an environment variable. """

    return keypath.replace(".", "_").upper()


def _annotate_defs_flags(props, override = {}):

    """ Annotate a defs dictionary with the provided properties, default state, and overrides to any default state (as
        above), but then format the return value as a list of definition flags, for frontend-targeted builds. """

    flags = []
    overlay = _annotate_defs_dict(props, override, False)
    for (key, value) in overlay.items():
        if type(value) == "bool":
            if value == True:
                flags.append("--define=%s" % key)
            else:
                flags.append("--define=%s=%s" % (key, str(value).lower()))
        elif type(value) == "string":
            flags.append("--define=%s=\"%s\"" % (key, value))
        else:
            flags.append("--define=%s=%s" % (key, value))
    return _dedupe(BASE_CLOSURE_FLAGS + flags)


def _annotate_jvm_flags(flags, defs = {}, override = {}):

    """ Annotate an existing set of flags, and a defs dictionary, with the provided properties, default state, and any
        overrides specified. The resulting set of flags should be suitable for use with invocation of a Java binary. """

    computed_jvm_flags = [i for i in flags]
    overlay_defs = _annotate_defs_dict(defs, override)
    for define in overlay_defs.keys():
        val = overlay_defs[define]
        if type(val) == "bool":
            computed_jvm_flags.append("-D%s=%s" % (define, (val and "true") or "false"))
        else:
            computed_jvm_flags.append("-D%s=%s" % (define, val))
    return computed_jvm_flags


annotate_jvm_flags = _annotate_jvm_flags
annotate_defs_dict = _annotate_defs_dict
annotate_defs_flags = _annotate_defs_flags
