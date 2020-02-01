
load(
    "//defs/toolchain/js:rules.bzl",
    _js_app = "js_app",
    _js_test = "js_test",
    _js_module = "js_module",
)

load(
    "//defs/toolchain/ts:tsc.bzl",
    _ts_module = "ts_module",
)

load(
    "//defs/toolchain/style:rules.bzl",
    _style_binary = "style_binary",
    _style_library = "style_library",
)


js_app = _js_app
js_test = _js_test
js_module = _js_module
ts_module = _ts_module
style_binary = _style_binary
style_library = _style_library
