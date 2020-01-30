
"Rule definitions to run PostCSS under Bazel."

load("@build_bazel_rules_nodejs//:providers.bzl", "run_node")

_DOC = """Run the PostCSS tool, in NodeJS, within Bazel.

Typical example:
```python
load("@gust//defs/toolchain/style:postcss.bzl", "postcss_optimize")

postcss_optimize(
    name = "final.min",
    src = "final.css",
    plugins = ["autoprefixer"],
)
```

Note that the `name` attribute determines what the resulting files will be called.
So the example above will output `final.min.css` and `final.min.css.map` (since
`sourcemap` defaults to `True`).
"""

DEFAULT_POSTCSS_PLUGINS = [
    "autoprefixer",
    "cssnano",
    "svgo",
]

_POSTCSS_ATTRS = {
    "src": attr.label(
        doc = """File to minify.

Expected to be a CSS file, typically compiled through SASS/SCSS and/or GSS.""",
        allow_files = [".css", ".map"],
        mandatory = True,
    ),
    "plugins": attr.string_list(
        doc = """Plugins to activate in PostCSS.

Each declared plugin will be `required()`, and placed into the PostCSS build pipeline.
"""
    ),
    "debug": attr.bool(
        doc = "Enable or disable debug output and messaging. Defaults to `false` in `COMPILATION_MODE=opt`.",
    ),
    "sourcemap": attr.bool(
        doc = "Enable sourcemaps alongside optimized CSS outputs.",
    ),
    "config": attr.label(
        doc = "Configuration file, with plugin options specified as keys in a single global object.",
        executable = False,
        allow_single_file = True,
        default = ":postcss_default_config.json",
    ),
    "postcss_bin": attr.label(
        doc = "An executable target that runs PostCSS.",
        default = Label("@gust//defs/toolchain/style:postcss"),
        executable = True,
        cfg = "host",
    ),
}


def _postcss_outs(sourcemap, name):

    """Supply some labelled outputs in the common case of a single entry point"""

    result = {}
    out = name + ".css"
    result[name] = out
    return result


def _postcss(ctx):

    """ Generate actions to run PostCSS. """

    args = []
    inputs = ctx.files.src[:]
    outputs = []
    sources = [f for f in inputs if f.extension == "css"]
    sourcemaps = [f for f in inputs if f.extension == "map"]
    if len(sources) > 1:
        fail("PostCSS compiler does not support multiple src inputs at this time. Got: (%s)."
                % str(sources))
    if len([f for f in inputs if f.is_directory]) > 0:
        fail("PostCSS compiler does not support directories at this time.")

    outputs.append(ctx.actions.declare_file("%s.css" % ctx.label.name))

    inputs.append(ctx.file.config)

    args.append("--source"); args.append(inputs[0].path)
    args.append("--target"); args.append(outputs[0].path)
    args.append("--config"); args.append(ctx.file.config.path)
    args.append("--plugins"); args.append(",".join(ctx.attr.plugins or DEFAULT_POSTCSS_PLUGINS))

    if ctx.attr.debug or ctx.var["COMPILATION_MODE"] == "dbg":
        args.append("--debug")
    elif ctx.var["COMPILATION_MODE"] == "opt":
        args.append("--opt")

    if ctx.attr.sourcemap:
        args.append("--sourcemap")

    sealed = ctx.actions.args()
    sealed.add_all(args)

    run_node(
        ctx,
        progress_message = "Optimizing stylesheet %s [postcss]" % (outputs[0].short_path),
        executable = "postcss_bin",
        inputs = inputs,
        outputs = outputs,
        arguments = [sealed],
        env = {"COMPILATION_MODE": ctx.var["COMPILATION_MODE"]},
    )

    return [
        DefaultInfo(files = depset(outputs))
    ]


postcss_optimize = rule(
    doc = _DOC,
    implementation = _postcss,
    attrs = _POSTCSS_ATTRS,
    outputs = _postcss_outs,
)
