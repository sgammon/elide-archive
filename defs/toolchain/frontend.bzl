
load(
    "//defs/toolchain/js:closure.bzl",
    _js_app = "js_app",
    _js_test = "js_test",
    _js_module = "js_module",
)

load(
    "//defs/toolchain/ts:tsc.bzl",
    _ts_module = "ts_module",
)


js_app = _js_app
js_test = _js_test
js_module = _js_module
ts_module = _ts_module
