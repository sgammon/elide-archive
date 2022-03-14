load(
    "@io_bazel_rules_docker//container:container.bzl",
    _container_image = "container_image",
)
load(
    "@io_bazel_rules_docker//container:push.bzl",
    _container_push = "container_push",
)
load(
    "@rules_graal//graal:graal.bzl",
    _graal_binary = "graal_binary",
)
load(
    "@rules_pkg//pkg:pkg.bzl",
    _pkg_tar = "pkg_tar",
)
load(
    "@rules_pkg//pkg:mappings.bzl",
    _strip_prefix = "strip_prefix",
)
load(
    "//tools/defs/java:java.bzl",
    _java_binary = "java_binary",
    _java_library = "java_library",
    _maven = "maven",
)
load(
    "//tools/defs/kt:defs.bzl",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_library = "kt_jvm_library",
)
load(
    "//tools/defs/micronaut:micronaut.bzl",
    _micronaut_library = "micronaut_library",
)
load(
    "//tools/defs/model:model.bzl",
    _javaproto = "javaproto",
)
load(
    "//tools/defs/model:service.bzl",
    _javagrpc = "javagrpc",
)

ENABLE_KT_BINARY = False
DEFAULT_STRIP_PREFIX = None

GRAAL_IMAGE_ARGS = [
    "-O1",
    "--auto-fallback",
    "--enable-http",
    "--enable-https",
    "--language:nfi",
    "--language:regex",
    "--language:llvm",
    "-Duser.country=US",
    "-Duser.language=en",
    "-H:IncludeLocales=en",
    "-H:-EnableSecurityServicesFeature",
]

NATIVE_IMAGE_COMPILE_DEPS = [
    _maven("com.google.cloud:native-image-support"),
]

ASSET_DIGEST_CHARS = 8
ASSET_DIGEST_ROUNDS = 3
ASSET_MANIFEST_PATH = "assets.pb"
ASSET_MANIFEST_FORMAT = "BINARY"
ASSET_DIGEST_ALGORITHM = "SHA512"
ASSETS_ENABLE_GZIP = True
ASSETS_ENABLE_BROTLI = True


def server_binary(
        name,
        srcs = [],
        deps = [],
        runtime_deps = [],
        resources = [],
        assets = [],
        main_class = None,
        micronaut = True,
        optimized = False,
        resource_strip_prefix = DEFAULT_STRIP_PREFIX,
        native_image_features = [],
        graal_extra_args = GRAAL_IMAGE_ARGS,
        image_name = None,
        gcloud_project = None,
        docker_registry = "us-docker.pkg.dev",
        docker_repository = None,
        docker_format = "Docker",
        jvm_image_base = None,
        jvm_image_repository = None,
        native_image_base = None,
        native_image_repository = None,
        **kwargs):
    """Wraps a Kotlin-enabled JVM server binary target."""
    ## Wiring: Assets
    libdeps = []
    if len(assets) > 0:
        _java_library(
            name = "%s.assets" % name,
            resource_strip_prefix = resource_strip_prefix or native.package_name(),
            resources = [
                "%s.manifest" % (n) for n in assets
            ],
        )
        libdeps.append(
            ":%s.assets" % name,
        )

    ## Wiring: Resources
    if len(resources) > 0:
        _java_library(
            name = "%s.resources" % name,
            resource_strip_prefix = resource_strip_prefix or _strip_prefix.from_pkg(),
            resources = resources,
        )
        libdeps.append(
            ":%s.resources" % name,
        )

    ## Wiring: Entrypoint
    libdef = _kt_jvm_library
    if micronaut:
        libdef = _micronaut_library
    if len(srcs) > 0:
        libdef(
            name = "%s.entry" % name,
            srcs = srcs,
            deps = deps,
            runtime_deps = runtime_deps,
        )
        libdeps.append(
            ":%s.entry" % name,
        )

    binargs = {
        "name": "%s.jvm" % name,
        "main_class": main_class,
        "runtime_deps": runtime_deps + libdeps,
    }
    binargs.update(kwargs)

    ## Targets: JVM
    if ENABLE_KT_BINARY:
        _kt_jvm_binary(
            srcs = srcs,
            deps = deps,
            **binargs
        )
    else:
        _java_binary(
            **binargs
        )

    ## Targets: GraalVM
    _graal_binary(
        name = "%s.native" % name,
        main_class = main_class,
        include_resources = ".*",
        native_image_features = native_image_features,
        graal_extra_args = graal_extra_args,
        deps = [":%s.jvm" % name] + libdeps + NATIVE_IMAGE_COMPILE_DEPS,
    )
    if optimized:
        native.alias(
            name = name,
            actual = "%s.native" % name,
        )
    else:
        native.alias(
            name = name,
            actual = "%s.jvm" % name,
        )

    ## Targets: Docker
    resolved_jvm_image_repository = jvm_image_repository or ("%s/jvm" % docker_repository)
    resolved_native_image_repository = native_image_repository or ("%s/native" % docker_repository)

    _container_image(
        name = "%s.jvm.image" % name,
        base = jvm_image_base or "@graalvm_base//image",
        files = [":%s.jvm_deploy.jar" % name],
        entrypoint = ["java", "-jar"],
        cmd = ["%s.jvm_deploy.jar" % name],
        tags = ["no-ide"],
    )
    _container_push(
        name = "%s.jvm.image.push" % name,
        image = ":%s.jvm.image" % name,
        registry = docker_registry,
        format = docker_format,
        repository = resolved_jvm_image_repository,
        tags = ["no-ide"],
    )
    _container_image(
        name = "%s.native.image" % name,
        base = native_image_base or "@runtime_base//image",
        files = [":%s.native" % name],
        cmd = ["/%s.native-bin" % name],
        tags = ["no-ide"],
    )
    _container_push(
        name = "%s.native.image.push" % name,
        image = ":%s.native.image" % name,
        registry = docker_registry,
        format = docker_format,
        repository = resolved_native_image_repository,
        tags = ["no-ide"],
    )

def _join_cmd(strings):
    """Joins a sequence of objects as strings, with select statements that return
       strings handled correctly. This has O(N^2) performance, so don't use it for
       building up large results.
       This is mostly equivalent to " ".join(strings), except for handling select
       statements correctly."""
    result = ''
    first = True
    for string in strings:
        if type(string) == 'select':
            result += string
        else:
            result += str(string)
    return result

def server_assets(
        name,
        srcs = [],
        js_modules = {},
        css_modules = {},
        enable_renaming = False,
        resource_strip_prefix = None,
        compress = False,
        **kwargs):
    """Wraps server-side assets in a `java_library` target."""
    bundle_inputs = []
    injected_resources = []
    if len(js_modules) > 0:
        for module in js_modules:
            injected_resources.append(js_modules[module])
            injected_resources.append("%s.map" % js_modules[module])
            bundle_inputs.append("--js=\"%s:$(locations %s)\"" % (module, js_modules[module]))
    if len(css_modules) > 0:
        for module in css_modules:
            injected_resources.append("%s.css" % css_modules[module])
            if enable_renaming:
                injected_resources.append("%s.css.json" % css_modules[module])

            # should we reference the rewrite maps?
            if enable_renaming:
                bundle_inputs.append("--css=\"%s:$(locations %s) $(locations %s.css.json)\""
                                    % (module, css_modules[module], css_modules[module]))
            else:
                bundle_inputs.append("--css=\"%s:$(locations %s)\""
                                     % (module, css_modules[module]))

    extension = "tar"
    if compress:
        extension = "tar.gz"
    _pkg_tar(
        name = name,
        srcs = srcs,
        strip_prefix = resource_strip_prefix or _strip_prefix.from_pkg(),
        **kwargs
    )

    bundler_args = [
        "--output=\"$@\"",
        "--format=" + ASSET_MANIFEST_FORMAT,
        "--digest=" + ASSET_DIGEST_ALGORITHM,
        "--digest-length=" + str(ASSET_DIGEST_CHARS),
        "--digest-rounds=" + str(ASSET_DIGEST_ROUNDS),
        "--embed",
        "--precompress",
        "--variants=IDENTITY" + (
            (ASSETS_ENABLE_GZIP and ",GZIP" or "") +
            (ASSETS_ENABLE_BROTLI and ",BROTLI" or "")),
        (enable_renaming and "--rewrite-maps") or ("--no-rewrite-maps"),
    ]

    native.genrule(
        name = "%s.manifest" % name,
        outs = [ASSET_MANIFEST_PATH],
        srcs = [
            target for (entry, target) in js_modules.items()
        ] + [
            target for (entry, target) in css_modules.items()
        ] + (enable_renaming and [
            ("%s.css.json" % target) for (entry, target) in css_modules.items()
        ] or []),
        # ends up as `./asset_bundler.sh --output='-' (...) --css="module.here:some/file.css some/file.css" --`
        cmd = _join_cmd([
          "./$(location @elide//tools/bundler)", " ",
          "--", select({
              "@elide//tools/defs/conditions:debug": "dbg",
              "@elide//tools/defs/conditions:release": "opt",
              "//conditions:default": "dbg",
          }),
          " ",
          " ".join(bundler_args + bundle_inputs),
        ]),
        message = "Generating asset manifest",
        output_to_bindir = True,
        tools = ["@elide//tools/bundler"],
    )

maven = _maven
javagrpc = _javagrpc
javaproto = _javaproto
java_library = _java_library
java_binary = _java_binary
kt_jvm_library = _kt_jvm_library
kt_jvm_binary = _kt_jvm_binary
micronaut_library = _micronaut_library
