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
    "//defs:config.bzl",
    _RENAMING = "RENAMING",
    _JVM_DEBUG_PORT = "JVM_DEBUG_PORT",
)

load(
    "@rules_pkg//pkg:pkg.bzl",
    _pkg_tar = "pkg_tar",
)

load(
    "@rules_java//java:defs.bzl",
    _java_binary = "java_binary",
    _java_library = "java_library",
)

load(
    "@io_bazel_rules_kotlin//kotlin:kotlin.bzl",
    _kt_jvm_binary = "kt_jvm_binary",
    _kt_jvm_library = "kt_jvm_library",
)

load(
    "@io_bazel_rules_docker//java:image.bzl",
    _java_image = "java_image",
)

load(
    "@io_bazel_rules_docker//container:image.bzl",
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
    "//defs/toolchain:deps.bzl",
    "javaproto",
)

load(
    "//defs/toolchain:schema.bzl",
    "GRPCJAVA_POSTFIX_",
    "JAVAPROTO_POSTFIX_",
    "CLOSUREPROTO_POSTFIX_",
)

load(
    "//defs/toolchain/context:props.bzl",
    _annotate_defs_dict = "annotate_defs_dict",
    _annotate_jvm_flags = "annotate_jvm_flags",
)

load(
    "//defs/toolchain:deps.bzl",
    "maven",
)


INJECTED_MICRONAUT_DEPS = [
    "@javax_inject",
    "@javax_annotation_api",
    "@gust//java:framework",
    "@gust//defs/toolchain/java/plugins:picocli",
    "@gust//defs/toolchain/java/plugins:micronaut",
    "@gust//java/gust/backend/runtime:logging",
    "@com_google_guava",
    "@com_google_template_soy",
    "@com_google_common_html_types",
    "@com_google_code_findbugs_jsr305",
    "@io_micronaut_micronaut_views",
    "@io_micronaut_micronaut_views_soy",
    "@org_brotli//java/org/brotli/wrapper/enc:enc",
    maven("org.slf4j:slf4j-api"),
    maven("com.google.protobuf:protobuf-java"),
    maven("io.micronaut:micronaut-aop"),
    maven("io.micronaut:micronaut-core"),
    maven("io.micronaut:micronaut-http"),
    maven("io.micronaut:micronaut-http-client"),
    maven("io.micronaut:micronaut-security-annotations"),
    maven("io.micronaut:micronaut-inject"),
    maven("io.micronaut:micronaut-inject-java"),
    maven("io.micronaut:micronaut-validation"),
    maven("io.micronaut:micronaut-http-server"),
    maven("io.micronaut:micronaut-http-server-netty"),
    maven("io.micronaut:micronaut-graal"),
    maven("io.micronaut:micronaut-router"),
    maven("io.micronaut:micronaut-tracing"),
    maven("io.micronaut:micronaut-session"),
    maven("io.micronaut:micronaut-security"),
    maven("io.micronaut:micronaut-messaging"),
    maven("io.micronaut:micronaut-websocket"),
    maven("io.micronaut:micronaut-multitenancy"),
    maven("io.micronaut:micronaut-runtime"),
    maven("io.reactivex.rxjava2:rxjava"),
]

INJECTED_MICRONAUT_GRPC_DEPS = [
    maven("io.grpc:grpc-core"),
    maven("io.grpc:grpc-auth"),
    maven("io.grpc:grpc-api"),
    maven("io.grpc:grpc-stub"),
    maven("io.grpc:grpc-context"),
    maven("io.grpc:grpc-protobuf"),
    maven("io.micronaut.grpc:micronaut-grpc-runtime"),
    maven("io.micronaut.grpc:micronaut-grpc-annotation"),
    maven("io.micronaut.grpc:micronaut-protobuff-support"),
]

INJECTED_GAPI_DEPS = [
    maven("com.google.cloud:libraries-bom"),
    maven("com.google.cloud:google-cloud-firestore"),
    maven("com.google.cloud:google-cloud-storage"),
    maven("com.google.cloud:google-cloud-pubsub"),
]

INJECTED_MICRONAUT_RUNTIME_DEPS = [
    maven("ch.qos.logback:logback-classic"),
    maven("io.micronaut:micronaut-runtime"),
]

INJECTED_CONTROLLERS = [
    "@gust//java/gust/backend:AssetController",
    "@gust//java/gust/backend/builtin:BuiltinsController",
]

INJECTED_CONTROLLER_DEPS = [
    "@gust//java/gust/backend/annotations:annotations",
    "@gust//java/gust/backend:PageRender",
    "@gust//java/gust/backend:PageContext",
    "@gust//java/gust/backend:PageContextManager",
    "@gust//java/gust/backend:BaseController",
    "@gust//java/gust/backend:AppController",
    javaproto("@gust//java/gust/backend/builtin:sitemap_proto"),
]


_JVM_APP_DEBUG_FLAGS = [
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:%s" % _JVM_DEBUG_PORT,
]

_JVM_APP_RELEASE_FLAGS = [
    # None yet.
]

INJECTED_LIBRARIES = [
    # None yet.
]

INJECTED_RESOURCE_JARS = [
    # None yet.
]

INJECTED_JVM_FLAGS = [
    # None yet.
]

ASSET_DIGEST_CHARS = 8
ASSET_DIGEST_ROUNDS = 3
ASSET_MANIFEST_PATH = "assets.pb"
ASSET_MANIFEST_FORMAT = "BINARY"
ASSET_DIGEST_ALGORITHM = "SHA512"
ASSETS_ENABLE_GZIP = True
ASSETS_ENABLE_BROTLI = True


def join_cmd(strings):

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


def _dedupe_deps(deps):

    """ Deduplicate a set of string labels in a deps argument. """

    if deps == None:
        return deps
    depindex = []
    for i in deps:
        if i not in depindex:
            depindex.append(i)
    return depindex


def _ensure_types(srcs, ext):

    """ Ensure that all srcs are of a certain file extension, or fail. """

    if any([(not s.endswith(ext)) for s in srcs]):
        fail("All sources must either be Kotlin or Java. " +
             "Expected file extension was '%s'." % ext)


def _jdk_binary(name,
                srcs = [],
                deps = None,
                data = [],
                jvm_flags = [],
                resource_jars = [],
                tags = [],
                **kwargs):

    """ Generate a JDK binary, with built-in support for Kotlin. """

    if len(srcs) > 0 and srcs[0].endswith(".kt"):
        # process as kotlin
        _ensure_types(srcs, ".kt")
        _kt_jvm_binary(
            name = name,
            srcs = srcs,
            deps = _dedupe_deps(deps),
            data = data + INJECTED_LIBRARIES,
            resource_jars = resource_jars + INJECTED_RESOURCE_JARS,
            jvm_flags = select({
               "@gust//defs/config:live_reload": ["-DLIVE_RELOAD=enabled"] + INJECTED_JVM_FLAGS + jvm_flags,
               "//conditions:default": INJECTED_JVM_FLAGS + jvm_flags,
            }) + select({
               "@gust//defs/config:release": _JVM_APP_RELEASE_FLAGS,
               "@gust//defs/config:debug": _JVM_APP_DEBUG_FLAGS,
            }),
            tags = [
                "ibazel_live_reload",
                "iblaze_notify_changes",
            ] + tags,
            **kwargs
        )

    else:
        # process as java
        _ensure_types(srcs, ".java")
        _java_binary(
            name = name,
            srcs = srcs,
            deps = _dedupe_deps(deps),
            data = data + INJECTED_LIBRARIES,
            resource_jars = resource_jars + INJECTED_RESOURCE_JARS,
            jvm_flags = select({
               "@gust//defs/config:live_reload": ["-DLIVE_RELOAD=enabled"] + INJECTED_JVM_FLAGS + jvm_flags,
               "//conditions:default": INJECTED_JVM_FLAGS + jvm_flags,
            }) + select({
               "@gust//defs/config:release": _JVM_APP_RELEASE_FLAGS,
               "@gust//defs/config:debug": _JVM_APP_DEBUG_FLAGS,
               "//conditions:default": _JVM_APP_DEBUG_FLAGS,
            }),
            tags = [
                "ibazel_live_reload",
                "iblaze_notify_changes",
            ] + tags,
            **kwargs
        )


def _jdk_library(name,
                 srcs = [],
                 deps = None,
                 data = [],
                 **kwargs):

    """ Generate a JDK binary, with support for both Java and Kotlin. """

    if len(srcs) > 0 and srcs[0].endswith(".kt"):
        # process as kotlin
        _ensure_types(srcs, ".kt")
        _kt_jvm_library(
            name = name,
            srcs = srcs,
            deps = _dedupe_deps(deps),
            data = data,
            **kwargs
        )

    else:
        # process as java
        _ensure_types(srcs, ".java")
        _java_library(
            name = name,
            srcs = srcs,
            deps = _dedupe_deps(deps),
            data = data,
            **kwargs
        )


def _micronaut_library(name,
                       srcs = [],
                       deps = [],
                       proto_deps = [],
                       runtime_deps = [],
                       data = [],
                       templates = [],
                       exports = [],
                       **kwargs):

    """ Wraps a regular JDK library with injected Micronaut dependencies and plugins. """

    _jdk_library(
        name = name,
        srcs = srcs,
        deps = _dedupe_deps(deps + INJECTED_MICRONAUT_DEPS + [("%s-%s" % (
           p, JAVAPROTO_POSTFIX_
        )) for p in proto_deps] + [
           ("%s-java" % t) for t in templates
        ] + [
           ("%s-java_jcompiled" % t) for t in templates
        ]),
        runtime_deps = _dedupe_deps(runtime_deps + INJECTED_MICRONAUT_RUNTIME_DEPS + [
          ("%s-java" % t) for t in templates
        ] + [
          ("%s-java_jcompiled" % t) for t in templates
        ]),
        exports = _dedupe_deps([("%s-%s" % (
          p, JAVAPROTO_POSTFIX_
        )) for p in proto_deps] + [
          ("%s-java" % t) for t in templates
        ] + [
          ("%s-java_jcompiled" % t) for t in templates
        ] + exports),
        data = data,
        **kwargs
    )


def _micronaut_controller(name,
                          srcs,
                          deps = [],
                          proto_deps = [],
                          templates = [],
                          runtime_deps = [],
                          data = [],
                          **kwargs):

    """ Wraps a regular Micronaut JDK library which is intended to be used as a
        controller, potentially with templates to inject. """

    _micronaut_library(
        name = name,
        srcs = srcs,
        proto_deps = proto_deps,
        templates = templates,
        deps = _dedupe_deps((deps or []) + INJECTED_CONTROLLER_DEPS),
        runtime_deps = runtime_deps,
        data = data,
        **kwargs
    )



def _micronaut_service(name,
                       srcs,
                       deps = [],
                       proto_deps = [],
                       services = [],
                       templates = [],
                       runtime_deps = [],
                       data = [],
                       **kwargs):

    """ Wraps a Micronaut library with dependencies for services via gRPC. """

    _micronaut_library(
        name = name,
        srcs = srcs,
        proto_deps = proto_deps + services,
        templates = templates,
        deps = (deps or []) + [
            ("%s-%s" % (svc, GRPCJAVA_POSTFIX_))
            for svc in services
        ] + INJECTED_MICRONAUT_GRPC_DEPS,
        runtime_deps = runtime_deps,
        data = data,
        **kwargs
    )


def _micronaut_native_configset(name, srcs, **kwargs):

    """ Generate a configuration file set which governs reflection or JNI access
        in a native image. """

    native.filegroup(
        name = "%s-files" % name,
        srcs = srcs,
    )

    _java_library(
        name = "%s-lib" % name,
        resources = srcs,
        **kwargs
    )


native_tools = native


def _micronaut_application(name,
                           native = False,
                           main_class = "gust.backend.Application",
                           config = str(Label("@gust//java/gust:application.yml")),
                           template_loader = str(Label("@gust//java/gust/backend:TemplateProvider")),
                           logging_config = str(Label("@gust//java/gust:logback.xml")),
                           base = str(Label("@gust//java/gust/backend:base")),
                           native_base = str(Label("@gust//java/gust/backend:native")),
                           native_templates = [],
                           repository = None,
                           native_repository = None,
                           native_configsets = [],
                           registry = "us.gcr.io",
                           image_format = "OCI",
                           srcs = [],
                           controllers = [],
                           services = [],
                           tag = None,
                           deps = None,
                           proto_deps = [],
                           data = [],
                           resources = [],
                           resource_jars = [],
                           runtime_deps = [],
                           jvm_flags = [],
                           defs = {},
                           ports = [],
                           tags = [],
                           inject_main = True,
                           reflection_configuration = None,
                           js_modules = {},
                           css_modules = {},
                           classpath_resources = [],
                           **kwargs):

    """ Wraps a regular JDK application with injected Micronaut dependencies and plugins. """

    computed_jvm_flags = _annotate_jvm_flags([i for i in jvm_flags], defs)

    bundle_inputs = []
    injected_resources = []
    if len(js_modules) > 0:
        for module in js_modules:
            injected_resources.append(js_modules[module])
#            injected_resources.append("%s.map" % js_modules[module])
            bundle_inputs.append("--js=\"%s:$(locations %s)\"" % (module, js_modules[module]))
    if len(js_modules) > 0:
        for module in css_modules:
            injected_resources.append("%s.css" % css_modules[module])
            injected_resources.append("%s.css.json" % css_modules[module])

            # should we reference the rewrite maps?
            if _RENAMING:
                bundle_inputs.append("--css=\"%s:$(locations %s) $(locations %s.css.json)\""
                                    % (module, css_modules[module], css_modules[module]))
            else:
                bundle_inputs.append("--css=\"%s:$(locations %s)\""
                                     % (module, css_modules[module]))

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
        (_RENAMING and "--rewrite-maps") or ("--no-rewrite-maps"),
    ]

    native_tools.genrule(
        name = "%s-assets-manifest" % name,
        outs = [ASSET_MANIFEST_PATH],
        srcs = [
            target for (entry, target) in js_modules.items()
        ] + [
            target for (entry, target) in css_modules.items()
        ] + [
            ("%s.css.json" % target) for (entry, target) in css_modules.items()
        ],
        # ends up as `./asset_bundler.sh --output='-' (...) --css="module.here:some/file.css some/file.css" --`
        cmd = join_cmd([
          "./$(location @gust//tools:AssetBundler)", " ",
          "--", select({
              "@gust//defs/config:debug": "dbg",
              "@gust//defs/config:release": "opt",
              "//conditions:default": "dbg",
          }),
          " ",
          " ".join(bundler_args + bundle_inputs),
        ]),
        message = "Generating asset manifest",
        output_to_bindir = True,
        tools = ["@gust//tools:AssetBundler"],
    )

    _java_library(
        name = "%s-assets" % name,
        resources = injected_resources + [
            ":%s-assets-manifest" % name
        ],
    )

    if len(srcs) > 0:
        computed_deps = _dedupe_deps((deps or []) + INJECTED_MICRONAUT_DEPS + controllers + services)
        computed_image_deps = _dedupe_deps((deps or []) + INJECTED_MICRONAUT_DEPS)
        computed_image_layers = _dedupe_deps((
            INJECTED_MICRONAUT_RUNTIME_DEPS + [template_loader] + [":%s-assets" % name] + controllers + services))
        computed_runtime_deps = [template_loader]

        if inject_main:
            computed_deps.append("@gust//java/gust/backend:backend")
    else:
        computed_deps = None
        computed_image_deps = []
        computed_image_layers = []
        computed_runtime_deps = _dedupe_deps(
            (deps or []) +
            INJECTED_MICRONAUT_DEPS +
            INJECTED_CONTROLLERS +
            services +
            controllers + [
                maven("io.micronaut:micronaut-runtime"),
            ] + [template_loader] + [("%s-%s" % (
               p, JAVAPROTO_POSTFIX_
            )) for p in proto_deps] + INJECTED_MICRONAUT_RUNTIME_DEPS + [
               ("%s-java" % t) for t in native_templates
            ] + [
               ("%s-java_jcompiled" % t) for t in native_templates
            ])

        if inject_main:
            computed_runtime_deps.append("@gust//java/gust/backend:backend")

    _java_image(
        name = "%s-image" % name,
        srcs = srcs,
        main_class = main_class,
        deps = computed_image_deps,
        runtime_deps = computed_runtime_deps,
        jvm_flags = computed_jvm_flags + ["-Dgust.engine=jvm"],
        base = base,
        layers = computed_image_layers,
        resource_jars = resource_jars + [
            ":%s-assets" % name
        ],
        classpath_resources = classpath_resources + [
            config,
            logging_config,
        ],
    )

    _java_library(
        name = "%s-lib" % name,
        srcs = srcs,
        deps = computed_deps,
        runtime_deps = computed_runtime_deps,
        resources = [
            config,
            logging_config,
        ],
        resource_jars = [
            ("%s-lib" % r) for r in native_configsets
        ] + [":%s-assets" % name],
        resource_strip_prefix = "java/gust" in config and "java/gust/" or None,
    )

    if native:
        _graal_binary(
            name = "%s-native" % name,
            deps = _dedupe_deps(["%s-lib" % name] + computed_runtime_deps),
            main_class = main_class,
            c_compiler_path = "/usr/bin/clang",
            configsets = [
                ("%s-files" % c) for c in native_configsets
            ],
            graal_extra_args = [
                # General build flags
                "--no-fallback",

                # Memory usage/runtime flags
                "--no-server",
                "-J-Xms1g",
                "-J-Xmx12g",

                # Extra native-image flags
                "-H:+ParseRuntimeOptions",
                "-H:+TraceClassInitialization",
                "-H:IncludeResources=application.yml|logback.xml|assets.pb",
                "-H:-UseServiceLoaderFeature",

                # Build-time init @TODO(sgammon): investigate why these are needed
                "--initialize-at-build-time=com.google.template.soy.jbcsrc.api.RenderResult$Type",
                "--initialize-at-build-time=org.conscrypt.OpenSSLProvider",
                "--initialize-at-build-time=org.conscrypt.Conscrypt",
                "--initialize-at-run-time=io.netty.buffer.PooledByteBufAllocator,io.netty.buffer.ByteBufUtil,io.netty.buffer.ByteBufAllocator,com.sun.jndi.dns.DnsClient,io.netty.handler.ssl.ConscryptAlpnSslEngine,io.netty.handler.ssl.JettyNpnSslEngine,io.netty.handler.ssl.ReferenceCountedOpenSslEngine,io.netty.handler.ssl.JdkNpnApplicationProtocolNegotiator,io.netty.handler.ssl.ReferenceCountedOpenSslServerContext,io.netty.handler.ssl.ReferenceCountedOpenSslClientContext,io.netty.handler.ssl.util.BouncyCastleSelfSignedCertGenerator,io.netty.handler.ssl.ReferenceCountedOpenSslContext,io.micronaut.buffer.netty.NettyByteBufferFactory,io.netty.handler.ssl.JettyAlpnSslEngine$ClientEngine,io.netty.handler.ssl.JettyAlpnSslEngine$ServerEngine,io.netty.handler.codec.http2.Http2CodecUtil,io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler,io.netty.handler.codec.http2.Http2ServerUpgradeCodec,io.micronaut.http.netty.channel.converters.EpollChannelOptionFactory,io.micronaut.http.netty.channel.converters.KQueueChannelOptionFactory,io.micronaut.http.bind.binders.ContinuationArgumentBinder$Companion,io.micronaut.http.bind.binders.ContinuationArgumentBinder",
            ] + computed_jvm_flags + ["-Dgust.engine=native"],
            reflection_configuration = reflection_configuration,
        )

        _pkg_tar(
            name = "%s-native-pkg" % name,
            extension = "tar",
            srcs = ["%s-native" % name],
        )

        _container_image(
            name = "%s-native-image" % name,
            base = native_base,
            directory = "/app",
            files = ["%s-native" % name],
            workdir = "/app",
            ports = ports,
            cmd = None,
            env = {
                "PORT": "8080",
                "ENGINE": "native",
            },
            entrypoint = [
                "/app/entrypoint",
            ] + computed_jvm_flags + ["-Dgust.engine=native"],
            symlinks = {
                "/app/entrypoint": "/app/%s-native" % name
            },
        )

        if native_repository != None:
            _container_push(
                name = "%s-native-image-push" % name,
                image = ":%s-native-image" % name,
                tag = tag or "{BUILD_SCM_VERSION}",
                format = image_format,
                repository = native_repository,
                registry = registry,
            )

    if repository != None:
        _container_push(
            name = "%s-image-push" % name,
            image = ":%s-image" % name,
            tag = tag or "{BUILD_SCM_VERSION}",
            format = image_format,
            repository = repository,
            registry = registry,
        )

    _jdk_binary(
        name = name,
        srcs = srcs,
        deps = computed_deps,
        runtime_deps = computed_runtime_deps,
        resource_jars = [":%s-assets" % name],
        data = data,
        resources = resources,
        classpath_resources = classpath_resources + [config, logging_config],
        main_class = main_class or "gust.backend.Application",
        jvm_flags = computed_jvm_flags,
        tags = (tags or []),
        **kwargs
    )


## Meant for private use.
dedupe_deps_ = _dedupe_deps
ensure_types_ = _ensure_types

jdk_binary = _jdk_binary
jdk_library = _jdk_library
micronaut_library = _micronaut_library
micronaut_service = _micronaut_service
micronaut_controller = _micronaut_controller
micronaut_interceptor = _micronaut_service
micronaut_application = _micronaut_application
micronaut_native_configset = _micronaut_native_configset
