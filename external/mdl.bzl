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

package(default_visibility = ["//visibility:public"])

load(
    "@io_bazel_rules_sass//:defs.bzl",
    "sass_library",
    "sass_binary",
)

load(
    "@io_bazel_rules_closure//closure:defs.bzl",
    "closure_js_library",
    "closure_css_library",
    "web_library",
)

sass_library(
    name = "mdl",
    srcs = ["src/material-design-lite.scss"],
    visibility = ["//visibility:public"])


## JS Sources
BASE_SUPPRESSIONS = [
    "JSC_REQUIRES_NOT_SORTED",
    "JSC_UNUSED_PRIVATE_PROPERTY",
    "JSC_DEBUGGER_STATEMENT_PRESENT"]

FULL_SUPPRESSIONS = [
    "JSC_UNKNOWN_EXPR_TYPE",
    "JSC_STRICT_INEXISTENT_PROPERTY",
    "JSC_IMPLICITLY_NULLABLE_JSDOC",
    "JSC_UNUSED_PRIVATE_PROPERTY"]


## Sources: Images
filegroup(
    name = "images-tick",
    srcs = glob(["src/images/tick.svg"]))

web_library(
    name = "images-web",
    srcs = [":images-tick"],
    path = "/images")


## Base Styles
sass_library(
    name = "color-definitions-scss",
    srcs = ["src/_color-definitions.scss"])

sass_library(
    name = "functions-scss",
    srcs = ["src/_functions.scss"])

sass_library(
    name = "variables-scss",
    srcs = ["src/_variables.scss"],
    deps = [":color-definitions-scss"])

sass_library(
    name = "mixins-scss",
    srcs = ["src/_mixins.scss"])


## Source Groups
filegroup(
    name = "component-handler-js",
    srcs = ["src/mdlComponentHandler.js"])

sass_library(
    name = "animation-scss",
    srcs = ["src/animation/_animation.scss"])

sass_binary(
    name = "animation-css",
    src = "src/animation/_animation.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":variables-scss"])

sass_library(
    name = "typography-scss",
    srcs = ["src/typography/_typography.scss"])

sass_binary(
    name = "typography-css",
    src = "src/typography/_typography.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "badge-scss",
    srcs = ["src/badge/_badge.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "badge-css",
    src = "src/badge/_badge.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "card-scss",
    srcs = ["src/card/_card.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "card-css",
    src = "src/card/_card.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "chip-scss",
    srcs = ["src/chip/_chip.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "chip-css",
    src = "src/chip/_chip.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "dialog-scss",
    srcs = ["src/dialog/_dialog.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "dialog-css",
    src = "src/dialog/_dialog.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "expansion-scss",
    srcs = ["src/expansion/_expansion.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "expansion-css",
    src = "src/expansion/_expansion.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "footer-scss",
    srcs = [
        "src/footer/_mini_footer.scss",
        "src/footer/_mega_footer.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "footer-mini-css",
    src = "src/footer/_mini_footer.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "footer-mega-css",
    src = "src/footer/_mega_footer.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "grid-scss",
    srcs = ["src/grid/_grid.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "grid-css",
    src = "src/grid/_grid.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "list-scss",
    srcs = ["src/list/_list.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "list-css",
    src = "src/list/_list.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "button-js",
    srcs = ["src/button/button.js"])

sass_library(
    name = "button-scss",
    srcs = ["src/button/_button.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "button-css",
    src = "src/button/_button.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "textfield-js",
    srcs = ["src/textfield/textfield.js"])

sass_library(
    name = "textfield-scss",
    srcs = ["src/textfield/_textfield.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "textfield-css",
    src = "src/textfield/_textfield.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "ripple-js",
    srcs = ["src/ripple/ripple.js"])

sass_library(
    name = "ripple-scss",
    srcs = ["src/ripple/_ripple.scss"],
    deps = [":variables-scss"])

sass_binary(
    name = "ripple-css",
    src = "src/ripple/_ripple.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":variables-scss"])

filegroup(
    name = "spinner-js",
    srcs = ["src/spinner/spinner.js"])

sass_library(
    name = "spinner-scss",
    srcs = ["src/spinner/_spinner.scss"],
    deps = [":variables-scss"])

sass_binary(
    name = "spinner-css",
    src = "src/spinner/_spinner.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":variables-scss"])

sass_library(
    name = "shadow-scss",
    srcs = ["src/shadow/_shadow.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "shadow-css",
    src = "src/shadow/_shadow.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "menu-js",
    srcs = ["src/menu/menu.js"])

sass_library(
    name = "menu-scss",
    srcs = ["src/menu/_menu.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "menu-css",
    src = "src/menu/_menu.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "checkbox-js",
    srcs = ["src/checkbox/checkbox.js"])

sass_library(
    name = "checkbox-scss",
    srcs = ["src/checkbox/_checkbox.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "checkbox-css",
    src = "src/checkbox/_checkbox.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "data-table-js",
    srcs = ["src/data-table/data-table.js"])

sass_library(
    name = "data-table-scss",
    srcs = ["src/data-table/_data-table.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "data-table-css",
    src = "src/data-table/_data-table.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "progress-js",
    srcs = ["src/progress/progress.js"])

sass_library(
    name = "progress-scss",
    srcs = ["src/progress/_progress.scss"],
    deps = [":variables-scss"])

sass_binary(
    name = "progress-css",
    src = "src/progress/_progress.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":variables-scss"])

filegroup(
    name = "icon-toggle-js",
    srcs = ["src/icon-toggle/icon-toggle.js"])

sass_library(
    name = "icon-toggle-scss",
    srcs = ["src/icon-toggle/_icon-toggle.scss"],
    deps = [":variables-scss"])

sass_binary(
    name = "icon-toggle-css",
    src = "src/icon-toggle/_icon-toggle.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":variables-scss"])

filegroup(
    name = "tooltip-js",
    srcs = ["src/tooltip/tooltip.js"])

sass_library(
    name = "tooltip-scss",
    srcs = ["src/tooltip/_tooltip.scss"],
    deps = [":variables-scss"])

sass_binary(
    name = "tooltip-css",
    src = "src/tooltip/_tooltip.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":variables-scss"])

filegroup(
    name = "tabs-js",
    srcs = ["src/tabs/tabs.js"])

sass_library(
    name = "tabs-scss",
    srcs = ["src/tabs/_tabs.scss"],
    deps = [":variables-scss"])

sass_binary(
    name = "tabs-css",
    src = "src/tabs/_tabs.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":variables-scss"])

filegroup(
    name = "snackbar-js",
    srcs = ["src/snackbar/snackbar.js"])

sass_library(
    name = "snackbar-scss",
    srcs = ["src/snackbar/_snackbar.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "snackbar-css",
    src = "src/snackbar/_snackbar.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "layout-js",
    srcs = ["src/layout/layout.js"])

sass_library(
    name = "layout-scss",
    srcs = ["src/layout/_layout.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "layout-css",
    src = "src/layout/_layout.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "switch-js",
    srcs = ["src/switch/switch.js"])

sass_library(
    name = "switch-scss",
    srcs = ["src/switch/_switch.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "switch-css",
    src = "src/switch/_switch.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

filegroup(
    name = "slider-js",
    srcs = ["src/slider/slider.js"])

sass_library(
    name = "slider-scss",
    srcs = ["src/slider/_slider.scss"],
    deps = [":variables-scss"])

sass_binary(
    name = "slider-css",
    src = "src/slider/_slider.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":variables-scss"])

filegroup(
    name = "radio-js",
    srcs = ["src/radio/radio.js"])

sass_library(
    name = "radio-scss",
    srcs = ["src/radio/_radio.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "radio-css",
    src = "src/radio/_radio.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_library(
    name = "palette-scss",
    srcs = ["src/palette/_palette.scss"],
    deps = [
        ":variables-scss",
        ":mixins-scss"])

sass_binary(
    name = "palette-css",
    src = "src/palette/_palette.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [
        ":variables-scss",
        ":mixins-scss"])


## Closure Libs
closure_js_library(
    name = "base",
    srcs = [":component-handler-js"],
    suppress = FULL_SUPPRESSIONS)

closure_css_library(
    name = "animation-styles",
    srcs = [":animation-css"])

closure_css_library(
    name = "typography-styles",
    srcs = [":animation-css"])

closure_css_library(
    name = "button-styles",
    srcs = [":button-css"])

closure_js_library(
    name = "button",
    srcs = [":button-js"],
    deps = [
        ":base",
        ":ripple",
        ":button-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/dom:tagname"],
    suppress = BASE_SUPPRESSIONS)

closure_css_library(
    name = "badge-styles",
    srcs = [":badge-css"])

closure_css_library(
    name = "card-styles",
    srcs = [":card-css"])

closure_css_library(
    name = "chip-styles",
    srcs = [":chip-css"])

closure_css_library(
    name = "dialog-styles",
    srcs = [":dialog-css"],
    deps = ["@com_google_dialog_polyfill//:dialog-polyfill-css"])

closure_js_library(
    name = "dialog",
    exports = [
        "@com_google_dialog_polyfill//:dialog-polyfill"])

closure_css_library(
    name = "expansion-styles",
    srcs = [":expansion-css"])

closure_css_library(
    name = "grid-styles",
    srcs = [":grid-css"])

closure_css_library(
    name = "footer-styles",
    srcs = [":footer-mini-css", ":footer-mega-css"])

closure_css_library(
    name = "textfield-styles",
    srcs = [":textfield-css"])

closure_js_library(
    name = "textfield",
    srcs = [":textfield-js"],
    deps = [
        ":base",
        ":textfield-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype"])

closure_css_library(
    name = "ripple-styles",
    srcs = [":ripple-css"])

closure_js_library(
    name = "ripple",
    srcs = [":ripple-js"],
    deps = [
        ":base",
        ":ripple-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype"])

closure_css_library(
    name = "spinner-styles",
    srcs = [":spinner-css"])

closure_js_library(
    name = "spinner",
    srcs = [":spinner-js"],
    deps = [
        ":base",
        ":spinner-styles",
        "@io_bazel_rules_closure//closure/library/dom:tagname"])

closure_css_library(
    name = "menu-styles",
    srcs = [":menu-css"])

closure_js_library(
    name = "menu",
    srcs = [":menu-js"],
    deps = [
        ":base",
        ":menu-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/dom:tagname"])

closure_css_library(
    name = "checkbox-styles",
    srcs = [":checkbox-css"])

closure_js_library(
    name = "checkbox",
    srcs = [":checkbox-js"],
    deps = [
        ":base",
        ":checkbox-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/dom:tagname"])

closure_css_library(
    name = "data-table-styles",
    srcs = [":data-table-css"])

closure_js_library(
    name = "data-table",
    srcs = [":data-table-js"],
    deps = [
        ":base",
        ":checkbox",
        ":data-table-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/dom:tagname"],
    suppress = ["JSC_UNKNOWN_EXPR_TYPE"])

closure_css_library(
    name = "icon-toggle-styles",
    srcs = [":icon-toggle-css"])

closure_js_library(
    name = "icon-toggle",
    srcs = [":icon-toggle-js"],
    deps = [
        ":base",
        ":icon-toggle-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/dom:tagname"])

closure_css_library(
    name = "progress-styles",
    srcs = [":progress-css"])

closure_js_library(
    name = "progress",
    srcs = [":progress-js"],
    deps = [
        ":base",
        ":progress-styles",
        "@io_bazel_rules_closure//closure/library/dom:tagname"])

closure_css_library(
    name = "tooltip-styles",
    srcs = [":tooltip-css"])

closure_js_library(
    name = "tooltip",
    srcs = [":tooltip-js"],
    deps = [
        ":base",
        ":tooltip-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype"])

closure_css_library(
    name = "list-styles",
    srcs = [":list-css"])

closure_css_library(
    name = "tabs-styles",
    srcs = [":tabs-css"])

closure_js_library(
    name = "tabs",
    srcs = [":tabs-js"],
    deps = [
        ":base",
        ":tabs-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/dom:tagname"])

closure_css_library(
    name = "shadow-styles",
    srcs = [":shadow-css"])

closure_css_library(
    name = "snackbar-styles",
    srcs = [":snackbar-css"])

closure_js_library(
    name = "snackbar",
    srcs = [":snackbar-js"],
    deps = [
        ":base",
        ":snackbar-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype"])

closure_css_library(
    name = "layout-styles",
    srcs = [":layout-css"])

closure_js_library(
    name = "layout",
    srcs = [":layout-js"],
    deps = [
        ":base",
        ":layout-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/dom:tagname"],
    suppress = [
        "JSC_UNKNOWN_EXPR_TYPE"])

closure_css_library(
    name = "switch-styles",
    srcs = [":switch-css"])

closure_js_library(
    name = "switch",
    srcs = [":switch-js"],
    deps = [
        ":base",
        ":switch-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/dom:tagname"])

closure_css_library(
    name = "slider-styles",
    srcs = [":slider-css"])

closure_js_library(
    name = "slider",
    srcs = [":slider-js"],
    deps = [
        ":base",
        ":slider-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype"])

closure_css_library(
    name = "radio-styles",
    srcs = [":radio-css"])

closure_js_library(
    name = "radio",
    srcs = [":radio-js"],
    deps = [
        ":base",
        ":radio-styles",
        "@io_bazel_rules_closure//closure/library/events:eventtype"])

closure_css_library(
    name = "palette-styles",
    srcs = [":palette-css"])


## Exports
closure_js_library(
    name = "mdl-js",
    exports = [
        ":base",
        ":ripple",
        ":button",
        ":textfield",
        ":spinner",
        ":menu",
        ":checkbox",
        ":data-table",
        ":tooltip",
        ":tabs",
        ":snackbar",
        ":progress",
        ":icon-toggle",
        ":layout",
        ":switch",
        ":slider",
        ":radio"])

closure_css_library(
    name = "mdl-css",
    exports = [
     ":animation-styles",
     ":typography-styles",
     ":ripple-styles",
     ":badge-styles",
     ":button-styles",
     ":card-styles",
     ":chip-styles",
     ":dialog-styles",
     ":expansion-styles",
     ":footer-styles",
     ":grid-styles",
     ":textfield-styles",
     ":spinner-styles",
     ":list-styles",
     ":menu-styles",
     ":checkbox-styles",
     ":data-table-styles",
     ":tooltip-styles",
     ":tabs-styles",
     ":shadow-styles",
     ":snackbar-styles",
     ":palette-styles",
     ":progress-styles",
     ":icon-toggle-styles",
     ":layout-styles",
     ":switch-styles",
     ":slider-styles",
     ":radio-styles"])
