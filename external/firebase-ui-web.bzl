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
    "@io_bazel_rules_closure//closure:defs.bzl",
    "closure_js_library",
    "closure_js_template_library",
    "closure_js_binary",
    "closure_js_proto_library",
    "closure_css_library",
    "closure_css_binary",
    "web_library",
)

load(
    "@io_bazel_rules_closure//closure/private/rules:soy_library.bzl",
    closure_template_library = "soy_library",
)

load(
    "@io_bazel_rules_sass//:defs.bzl",
    "sass_library",
    "sass_binary",
)


## Configuration
WIDGET_SUPPRESSIONS = [
    "JSC_UNKNOWN_EXPR_TYPE",
    "JSC_HIDDEN_INTERFACE_PROPERTY",
]


## Sources: Images
filegroup(name = "image-anonymous", srcs = ["image/anonymous.png"])
filegroup(name = "image-facebook", srcs = ["image/facebook.svg"])
filegroup(name = "image-flags_sprite", srcs = ["image/flags_sprite.png"])
filegroup(name = "image-flags_sprite_2x", srcs = ["image/flags_sprite_2x.png"])
filegroup(name = "image-github", srcs = ["image/github.svg"])
filegroup(name = "image-google", srcs = ["image/google.svg"])
filegroup(name = "image-mail", srcs = ["image/mail.svg"])
filegroup(name = "image-phone", srcs = ["image/phone.svg"])
filegroup(name = "image-twitter", srcs = ["image/twitter.svg"])
filegroup(name = "image-success_status", srcs = ["image/success_status.png"])

filegroup(name = "images",
          srcs = [":image-anonymous",
                  ":image-facebook",
                  ":image-flags_sprite",
                  ":image-flags_sprite_2x",
                  ":image-github",
                  ":image-google",
                  ":image-mail",
                  ":image-phone",
                  ":image-success_status",
                  ":image-twitter"])


## Sources: Styles (SASS)
sass_library(
    name = "sass-vars",
    srcs = ["stylesheet/vars.scss"],
)

sass_library(
    name = "sass-flags",
    srcs = ["stylesheet/flags.scss"])

sass_binary(
    name = "sass-firebase-ui",
    src = "stylesheet/firebase-ui.scss",
    output_style = "expanded",
    sourcemap = False,
    deps = [":sass-vars", ":sass-flags"])

closure_css_library(
    name = "css-firebase-ui",
    srcs = [":sass-firebase-ui"])


## Sources: Templates
closure_js_template_library(
    name = "strings-tpl",
    srcs = ["soy/strings.soy"])

closure_js_template_library(
    name = "elements-tpl",
    srcs = ["soy/elements.soy"],
    deps = [":css-firebase-ui"])

closure_js_template_library(
    name = "pages-tpl",
    srcs = ["soy/pages.soy"],
    deps = [
        ":elements-tpl",
        ":strings-tpl",
        ":css-firebase-ui"])


## Sources: Externs
filegroup(
    name = "externs-accountchooser",
    srcs = ["javascript/externs/accountchooser.js"])

filegroup(
    name = "externs-dialogpolyfill",
    srcs = ["javascript/externs/dialog_polyfill.js"])

filegroup(
    name = "externs-googleyolo",
    srcs = ["javascript/externs/googleyolo.js"])

filegroup(
    name = "externs-recaptcha",
    srcs = ["javascript/externs/recaptcha.js"])


## Sources: JavaScript
closure_js_library(
    name = "data-country-js",
    srcs = ["javascript/data/country.js"],
    deps = ["@io_bazel_rules_closure//closure/library/structs:trie"])


## Sources: Utils
closure_js_library(
    name = "utils-account-js",
    srcs = ["javascript/utils/account.js"])

closure_js_library(
    name = "utils-acclient-js",
    srcs = [
        "javascript/utils/acclient.js",
        ":externs-accountchooser"],
    deps = [
        ":utils-account-js",
        "@io_bazel_rules_closure//closure/library/uri:uri",
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/asserts:asserts"])

closure_js_library(
    name = "utils-actioncodeurlbuilder-js",
    srcs = ["javascript/utils/actioncodeurlbuilder.js"],
    deps = ["@io_bazel_rules_closure//closure/library/uri:uri"])

closure_js_library(
    name = "utils-config-js",
    srcs = ["javascript/utils/config.js"])

closure_js_library(
    name = "utils-cookiemechanism-js",
    srcs = ["javascript/utils/cookiemechanism.js"],
    deps = [
        "@io_bazel_rules_closure//closure/library/net:cookies",
        "@io_bazel_rules_closure//closure/library/storage/mechanism:mechanism"])

closure_js_library(
    name = "utils-crypt-js",
    srcs = ["javascript/utils/crypt.js"],
    deps = [
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/crypt:crypt",
        "@io_bazel_rules_closure//closure/library/crypt:aes"])

closure_js_library(
    name = "utils-eventregister-js",
    srcs = ["javascript/utils/eventregister.js"],
    deps = [
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/events:eventtarget"])

closure_js_library(
    name = "utils-googleyolo-js",
    srcs = [
        "javascript/utils/googleyolo.js",
        ":externs-googleyolo"],
    deps = [
        ":utils-util-js",
        "@io_bazel_rules_closure//closure/library/promise:promise",
        "@io_bazel_rules_closure//closure/library/html:trustedresourceurl",
        "@io_bazel_rules_closure//closure/library/net:jsloader",
        "@io_bazel_rules_closure//closure/library/string:const"])

closure_js_library(
    name = "utils-idp-js",
    srcs = ["javascript/utils/idp.js"],
    deps = [
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/object:object"])

closure_js_library(
    name = "utils-log-js",
    srcs = ["javascript/utils/log.js"],
    deps = [
        "@io_bazel_rules_closure//closure/library/debug:console",
        "@io_bazel_rules_closure//closure/library/log:log"])

closure_js_library(
    name = "utils-pendingemailcredential-js",
    srcs = ["javascript/utils/pendingemailcredential.js"])

closure_js_library(
    name = "utils-phoneauthresult-js",
    srcs = ["javascript/utils/phoneauthresult.js"],
    deps = ["@io_bazel_rules_closure//closure/library/promise:promise"])

closure_js_library(
    name = "utils-phonenumber-js",
    srcs = ["javascript/utils/phonenumber.js"],
    deps = [
        ":data-country-js",
        "@io_bazel_rules_closure//closure/library/string:string"])

closure_js_library(
    name = "utils-sni-js",
    srcs = ["javascript/utils/sni.js"])

closure_js_library(
    name = "utils-redirectstatus-js",
    srcs = ["javascript/utils/redirectstatus.js"])

closure_js_library(
    name = "utils-storage-js",
    srcs = ["javascript/utils/storage.js"],
    deps = [
        ":utils-account-js",
        ":utils-redirectstatus-js",
        ":utils-cookiemechanism-js",
        ":utils-pendingemailcredential-js",
        ":utils-crypt-js",
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/storage:storage",
        "@io_bazel_rules_closure//closure/library/storage/mechanism:html5localstorage",
        "@io_bazel_rules_closure//closure/library/storage/mechanism:html5sessionstorage",
        "@io_bazel_rules_closure//closure/library/storage/mechanism:mechanismfactory"])

closure_js_library(
    name = "utils-util-js",
    srcs = ["javascript/utils/util.js"],
    deps = [
        ":data-country-js",
        "@io_bazel_rules_closure//closure/library/promise:promise",
        "@io_bazel_rules_closure//closure/library/dom:dom",
        "@io_bazel_rules_closure//closure/library/events:events",
        "@io_bazel_rules_closure//closure/library/events:eventtype",
        "@io_bazel_rules_closure//closure/library/html:safeurl",
        "@io_bazel_rules_closure//closure/library/useragent:useragent",
        "@io_bazel_rules_closure//closure/library/window:window"])


## Sources: JavaScript (UI Commons)
closure_js_library(
    name = "ui-mdl",
    srcs = ["javascript/ui/mdl.js"],
    deps = [
        "@com_google_mdl//:base",
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/dom:dom",
        "@io_bazel_rules_closure//closure/library/dom:classlist"])


## Sources: JavaScript (Elements)
closure_js_proto_library(
    name = "soy_html_proto",
    srcs = ["@com_google_common_html_types_html_proto//file"],
)

closure_js_library(
    name = "soy_jssrc",
    srcs = ["@com_google_template_soy_jssrc"],
    lenient = True,
    deps = [
        ":soy_html_proto",
        "@gust//js:ij",
        "@gust//js:xid",
        "@io_bazel_rules_closure//closure/protobuf:jspb",
        "@io_bazel_rules_closure//closure/library/array",
        "@io_bazel_rules_closure//closure/library/asserts",
        "@io_bazel_rules_closure//closure/library/debug",
        "@io_bazel_rules_closure//closure/library/dom",
        "@io_bazel_rules_closure//closure/library/format",
        "@io_bazel_rules_closure//closure/library/html:safehtml",
        "@io_bazel_rules_closure//closure/library/html:safescript",
        "@io_bazel_rules_closure//closure/library/html:safestyle",
        "@io_bazel_rules_closure//closure/library/html:safestylesheet",
        "@io_bazel_rules_closure//closure/library/html:safeurl",
        "@io_bazel_rules_closure//closure/library/html:trustedresourceurl",
        "@io_bazel_rules_closure//closure/library/html:uncheckedconversions",
        "@io_bazel_rules_closure//closure/library/i18n:bidi",
        "@io_bazel_rules_closure//closure/library/i18n:bidiformatter",
        "@io_bazel_rules_closure//closure/library/i18n:numberformat",
        "@io_bazel_rules_closure//closure/library/object",
        "@io_bazel_rules_closure//closure/library/soy:all_js",
        "@io_bazel_rules_closure//closure/library/string",
        "@io_bazel_rules_closure//closure/library/string:const",
        "@io_bazel_rules_closure//closure/library/uri",
    ],
)

closure_js_library(
    name = "soy_jssrc_idom",
    srcs = [
        "@com_google_template_soy_jssrc//:idom"],
    suppress = [
        "reportUnknownTypes",
        "strictCheckTypes",
        "lintChecks",
        "JSC_NULLABLE_RETURN_WITH_NAME",
    ],
    deps = [
        ":soy_jssrc",
        "@io_bazel_rules_closure//closure/library/asserts",
        "@io_bazel_rules_closure//closure/library/soy",
        "@io_bazel_rules_closure//closure/library/soy:data",
        "@io_bazel_rules_closure//closure/library/string",
        "@com_google_javascript_incremental_dom//:idom-js"
    ],
)

closure_js_library(
    name = "element-common-js",
    srcs = ["javascript/ui/element/common.js"],
    deps = [
        ":soy_jssrc_idom",
        "@io_bazel_rules_closure//closure/library/dom:dom",
        "@io_bazel_rules_closure//closure/library/dom:classlist",
        "@io_bazel_rules_closure//closure/library/dom:forms",
        "@io_bazel_rules_closure//closure/library/ui:component",
        "@io_bazel_rules_closure//closure/library/events:keycodes",
        "@io_bazel_rules_closure//closure/library/events:keyhandler",
        "@io_bazel_rules_closure//closure/library/events:actionhandler",
        "@io_bazel_rules_closure//closure/library/events:focushandler",
        "@io_bazel_rules_closure//closure/library/events:inputhandler",
])

closure_js_library(
    name = "element-dialog-js",
    srcs = [
        "javascript/ui/element/dialog.js",
        ":externs-dialogpolyfill"],
    deps = [
        ":ui-mdl",
        ":element-common-js",
        "@io_bazel_rules_closure//closure/library/dom:dom"])

closure_js_library(
    name = "element-email-js",
    srcs = ["javascript/ui/element/email.js"],
    deps = [
        ":strings-tpl",
        ":element-common-js",
        "@com_google_mdl//:textfield",
        "@io_bazel_rules_closure//closure/library/asserts:asserts",
        "@io_bazel_rules_closure//closure/library/string:string",
        "@io_bazel_rules_closure//closure/library/format:emailaddress",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-form-js",
    srcs = ["javascript/ui/element/form.js"],
    deps = [
        ":element-common-js",
        "@com_google_mdl//:button",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-idps-js",
    srcs = ["javascript/ui/element/idps.js"],
    deps = [
        ":element-common-js",
        "@io_bazel_rules_closure//closure/library/asserts:asserts",
        "@io_bazel_rules_closure//closure/library/dom:dataset"])

closure_js_library(
    name = "element-infobar-js",
    srcs = ["javascript/ui/element/infobar.js"],
    deps = [
        ":element-common-js",
        ":elements-tpl",
        "@com_google_javascript_incremental_dom//:idom-js",
        "@io_bazel_rules_closure//closure/library/dom:dom",
        "@io_bazel_rules_closure//closure/library/soy:soy",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-listboxdialog-js",
    srcs = ["javascript/ui/element/listboxdialog.js"],
    deps = [
        ":element-common-js",
        ":elements-tpl",
        ":element-dialog-js",
        "@com_google_javascript_incremental_dom//:idom-js",
        "@io_bazel_rules_closure//closure/library/dom:dom",
        "@io_bazel_rules_closure//closure/library/dom:tagname",
        "@io_bazel_rules_closure//closure/library/dom:dataset",
        "@io_bazel_rules_closure//closure/library/soy:soy",
        "@io_bazel_rules_closure//closure/library/style:style",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-name-js",
    srcs = ["javascript/ui/element/name.js"],
    deps = [
        ":strings-tpl",
        ":element-common-js",
        "@com_google_mdl//:textfield",
        "@io_bazel_rules_closure//closure/library/asserts:asserts",
        "@io_bazel_rules_closure//closure/library/string:string",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-newpassword-js",
    srcs = ["javascript/ui/element/newpassword.js"],
    deps = [
        ":strings-tpl",
        ":element-common-js",
        "@com_google_mdl//:textfield",
        "@io_bazel_rules_closure//closure/library/dom:classlist",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-password-js",
    srcs = ["javascript/ui/element/password.js"],
    deps = [
        ":strings-tpl",
        ":element-common-js",
        "@com_google_mdl//:textfield",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-phoneconfirmationcode-js",
    srcs = ["javascript/ui/element/phoneconfirmationcode.js"],
    deps = [
        ":element-common-js",
        "@io_bazel_rules_closure//closure/library/string:string",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-phonenumber-js",
    srcs = ["javascript/ui/element/phonenumber.js"],
    deps = [
        ":strings-tpl",
        ":element-common-js",
        ":element-listboxdialog-js",
        ":data-country-js",
        ":utils-phonenumber-js",
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/dom:dom",
        "@io_bazel_rules_closure//closure/library/dom:forms",
        "@io_bazel_rules_closure//closure/library/dom:classlist",
        "@io_bazel_rules_closure//closure/library/string:string",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-progressdialog-js",
    srcs = ["javascript/ui/element/progressdialog.js"],
    deps = [
        ":elements-tpl",
        ":element-common-js",
        ":element-dialog-js",
        "@io_bazel_rules_closure//closure/library/soy:soy",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-recaptcha-js",
    srcs = ["javascript/ui/element/recaptcha.js"],
    deps = [
        ":element-common-js"])

closure_js_library(
    name = "element-resend-js",
    srcs = ["javascript/ui/element/resend.js"],
    deps = [
        ":strings-tpl",
        ":element-common-js",
        "@io_bazel_rules_closure//closure/library/dom:dom",
        "@io_bazel_rules_closure//closure/library/ui:component"])

closure_js_library(
    name = "element-tospp-js",
    srcs = ["javascript/ui/element/tospp.js"],
    deps = [
        ":element-common-js",
        "@io_bazel_rules_closure//closure/library/ui:component"])


## Sources: JavaScript (Pages)
closure_js_library(
    name = "page-base-js",
    srcs = ["javascript/ui/page/base.js"],
    deps = [
        ":elements-tpl",
        ":ui-mdl",
        ":element-common-js",
        ":element-dialog-js",
        ":element-infobar-js",
        ":element-progressdialog-js",
        ":element-tospp-js",
        ":utils-eventregister-js",
        "@com_google_javascript_incremental_dom//:idom-js",
        "@io_bazel_rules_closure//closure/library/dom:dom",
        "@io_bazel_rules_closure//closure/library/dom:classlist",
        "@io_bazel_rules_closure//closure/library/events:event",
        "@io_bazel_rules_closure//closure/library/object:object",
        "@io_bazel_rules_closure//closure/library/soy:soy",
        "@io_bazel_rules_closure//closure/library/ui:component"],
    suppress = ["JSC_UNKNOWN_EXPR_TYPE"])

closure_js_library(
    name = "page-anonymoususermismatch-js",
    srcs = ["javascript/ui/page/anonymoususermismatch.js"],
    deps = [
        ":pages-tpl",
        ":element-common-js",
        ":element-form-js",
        ":page-base-js"])

closure_js_library(
    name = "page-blank-js",
    srcs = ["javascript/ui/page/blank.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js"])

closure_js_library(
    name = "page-callback-js",
    srcs = ["javascript/ui/page/callback.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js"])

closure_js_library(
    name = "page-differentdeviceerror-js",
    srcs = ["javascript/ui/page/differentdeviceerror.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-common-js",
        ":element-form-js"])

closure_js_library(
    name = "page-emailchangerevoke-js",
    srcs = ["javascript/ui/page/emailchangerevoke.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-common-js",
        ":element-form-js"])

closure_js_library(
    name = "page-emaillinksigninconfirmation-js",
    srcs = ["javascript/ui/page/emaillinksigninconfirmation.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-common-js",
        ":element-form-js",
        ":element-email-js",
        "@io_bazel_rules_closure//closure/library/dom:selection"])

closure_js_library(
    name = "page-emaillinksigninlinkingdifferentdevice-js",
    srcs = ["javascript/ui/page/emaillinksigninlinkingdifferentdevice.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-form-js"])

closure_js_library(
    name = "page-emaillinksigninlinking-js",
    srcs = ["javascript/ui/page/emaillinksigninlinking.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-form-js"])

closure_js_library(
    name = "page-emaillinksigninsent-js",
    srcs = ["javascript/ui/page/emaillinksigninsent.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-common-js",
        ":element-form-js"])

closure_js_library(
    name = "page-emailmismatch-js",
    srcs = ["javascript/ui/page/emailmismatch.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-form-js"])

closure_js_library(
    name = "page-emailnotreceived-js",
    srcs = ["javascript/ui/page/emailnotreceived.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-common-js",
        ":element-form-js"])

closure_js_library(
    name = "page-federatedlinking-js",
    srcs = ["javascript/ui/page/federatedlinking.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-common-js",
        ":element-form-js"])

closure_js_library(
    name = "page-notice-js",
    srcs = ["javascript/ui/page/notice.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-form-js",
        ":element-common-js"],
    suppress = ["JSC_UNKNOWN_EXPR_TYPE"])

closure_js_library(
    name = "page-passwordlinking-js",
    srcs = ["javascript/ui/page/passwordlinking.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-common-js",
        ":element-form-js",
        ":element-password-js",
        "@io_bazel_rules_closure//closure/library/asserts:asserts"])

closure_js_library(
    name = "page-passwordrecovery-js",
    srcs = ["javascript/ui/page/passwordrecovery.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-common-js",
        ":element-form-js",
        ":element-email-js"])

closure_js_library(
    name = "page-passwordreset-js",
    srcs = ["javascript/ui/page/passwordreset.js"],
    deps = [
        ":pages-tpl",
        ":page-base-js",
        ":element-form-js",
        ":element-newpassword-js"])

closure_js_library(
    name = "page-passwordsignin-js",
    srcs = ["javascript/ui/page/passwordsignin.js"],
    deps = [
        ":pages-tpl",
        ":element-common-js",
        ":page-base-js",
        ":element-form-js",
        ":element-email-js",
        ":element-password-js"])

closure_js_library(
    name = "page-passwordsignup-js",
    srcs = ["javascript/ui/page/passwordsignup.js"],
    deps = [
        ":pages-tpl",
        ":element-common-js",
        ":page-base-js",
        ":element-form-js",
        ":element-email-js",
        ":element-name-js",
        ":element-newpassword-js"])

closure_js_library(
    name = "page-phonesigninfinish-js",
    srcs = ["javascript/ui/page/phonesigninfinish.js"],
    deps = [
        ":pages-tpl",
        ":element-common-js",
        ":page-base-js",
        ":element-form-js",
        ":element-phoneconfirmationcode-js",
        ":element-resend-js"])

closure_js_library(
    name = "page-phonesigninstart-js",
    srcs = ["javascript/ui/page/phonesigninstart.js"],
    deps = [
        ":pages-tpl",
        ":element-common-js",
        ":page-base-js",
        ":element-form-js",
        ":element-phonenumber-js",
        ":element-recaptcha-js",
        "@io_bazel_rules_closure//closure/library/dom:selection"])

closure_js_library(
    name = "page-providersignin-js",
    srcs = ["javascript/ui/page/providersignin.js"],
    deps = [
        ":pages-tpl",
        ":element-idps-js",
        ":page-base-js"])

closure_js_library(
    name = "page-signin-js",
    srcs = ["javascript/ui/page/signin.js"],
    deps = [
        ":pages-tpl",
        ":element-common-js",
        ":element-idps-js",
        ":element-email-js",
        ":element-form-js",
        ":page-base-js",
        "@io_bazel_rules_closure//closure/library/dom:selection"])

closure_js_library(
    name = "page-unsupportedprovider-js",
    srcs = ["javascript/ui/page/unsupportedprovider.js"],
    deps = [
        ":pages-tpl",
        ":element-common-js",
        ":element-form-js",
        ":page-base-js"])


## Sources: JavaScript (Handlers)
closure_js_library(
    name = "handlers-handler-js",
    srcs = ["javascript/widgets/handler/handler.js"],
    deps = [
        "@io_bazel_rules_closure//closure/library/asserts:asserts"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-common-js",
    srcs = ["javascript/widgets/handler/common.js"],
    deps = [
        ":strings-tpl",
        ":utils-account-js",
        ":utils-pendingemailcredential-js",
        ":utils-acclient-js",
        ":utils-idp-js",
        ":utils-log-js",
        ":utils-sni-js",
        ":utils-util-js",
        ":utils-storage-js",
        ":utils-redirectstatus-js",
        ":element-common-js",
        ":page-base-js",
        ":page-passwordlinking-js",
        ":page-passwordsignin-js",
        ":page-notice-js",
        ":widget-config-js",
        ":handlers-handler-js",
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/html:trustedresourceurl",
        "@io_bazel_rules_closure//closure/library/net:jsloader",
        "@io_bazel_rules_closure//closure/library/string:const",
        "@io_bazel_rules_closure//closure/library/promise:promise"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-callback-js",
    srcs = ["javascript/widgets/handler/callback.js"],
    deps = [
        ":strings-tpl",
        ":utils-pendingemailcredential-js",
        ":utils-idp-js",
        ":utils-storage-js",
        ":page-callback-js",
        ":handlers-common-js",
        ":handlers-handler-js",
        "@io_bazel_rules_closure//closure/library/array:array"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-actioncode-js",
    srcs = ["javascript/widgets/handler/actioncode.js"],
    deps = [
        ":strings-tpl",
        ":element-common-js",
        ":page-emailchangerevoke-js",
        ":page-notice-js",
        ":page-passwordrecovery-js",
        ":page-passwordreset-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-anonymoususermismatch-js",
    srcs = ["javascript/widgets/handler/anonymoususermismatch.js"],
    deps = [
        ":page-anonymoususermismatch-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-differentdeviceerror-js",
    srcs = ["javascript/widgets/handler/differentdeviceerror.js"],
    deps = [
        ":page-differentdeviceerror-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-emaillinkconfirmation-js",
    srcs = ["javascript/widgets/handler/emaillinkconfirmation.js"],
    deps = [
        ":page-emaillinksigninconfirmation-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-emaillinknewdevicelinking-js",
    srcs = ["javascript/widgets/handler/emaillinknewdevicelinking.js"],
    deps = [
        ":handlers-common-js",
        ":handlers-handler-js",
        ":utils-actioncodeurlbuilder-js",
        ":page-emaillinksigninlinkingdifferentdevice-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-emaillinksignincallback-js",
    srcs = ["javascript/widgets/handler/emaillinksignincallback.js"],
    deps = [
        ":strings-tpl",
        ":page-blank-js",
        ":element-common-js",
        ":element-progressdialog-js",
        ":handlers-common-js",
        ":handlers-handler-js",
        ":handlers-anonymoususermismatch-js",
        ":handlers-differentdeviceerror-js",
        ":handlers-emaillinkconfirmation-js",
        ":handlers-emaillinknewdevicelinking-js",
        ":utils-actioncodeurlbuilder-js",
        ":utils-storage-js",
        "@io_bazel_rules_closure//closure/library/promise:promise"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-emaillinksigninlinking-js",
    srcs = ["javascript/widgets/handler/emaillinksigninlinking.js"],
    deps = [
        ":utils-storage-js",
        ":page-emaillinksigninlinking-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-emaillinksigninsent-js",
    srcs = ["javascript/widgets/handler/emaillinksigninsent.js"],
    deps = [
        ":page-emaillinksigninsent-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-emailmismatch-js",
    srcs = ["javascript/widgets/handler/emailmismatch.js"],
    deps = [
        ":utils-storage-js",
        ":page-emailmismatch-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-emailnotreceived-js",
    srcs = ["javascript/widgets/handler/emailnotreceived.js"],
    deps = [
        ":page-emailnotreceived-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-federatedlinking-js",
    srcs = ["javascript/widgets/handler/federatedlinking.js"],
    deps = [
        ":page-federatedlinking-js",
        ":handlers-common-js",
        ":handlers-handler-js",
        ":utils-storage-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-federatedredirect-js",
    srcs = ["javascript/widgets/handler/federatedredirect.js"],
    deps = [
        ":page-blank-js",
        ":handlers-handler-js",
        ":handlers-common-js",
        "@io_bazel_rules_closure//closure/library/asserts:asserts"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-federatedsignin-js",
    srcs = ["javascript/widgets/handler/federatedsignin.js"],
    deps = [
        ":page-federatedlinking-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-passwordlinking-js",
    srcs = ["javascript/widgets/handler/passwordlinking.js"],
    deps = [
        ":utils-log-js",
        ":utils-storage-js",
        ":element-common-js",
        ":page-passwordlinking-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-passwordrecovery-js",
    srcs = ["javascript/widgets/handler/passwordrecovery.js"],
    deps = [
        ":element-common-js",
        ":page-notice-js",
        ":page-passwordrecovery-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-passwordsignin-js",
    srcs = ["javascript/widgets/handler/passwordsignin.js"],
    deps = [
        ":element-common-js",
        ":page-passwordsignin-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-passwordsignup-js",
    srcs = ["javascript/widgets/handler/passwordsignup.js"],
    deps = [
        ":strings-tpl",
        ":utils-log-js",
        ":element-common-js",
        ":page-passwordsignup-js",
        ":handlers-common-js",
        ":handlers-handler-js",
        "@io_bazel_rules_closure//closure/library/json:json",
        "@io_bazel_rules_closure//closure/library/string:string"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-phonesigninfinish-js",
    srcs = ["javascript/widgets/handler/phonesigninfinish.js"],
    deps = [
        ":strings-tpl",
        ":utils-phonenumber-js",
        ":element-common-js",
        ":element-progressdialog-js",
        ":page-phonesigninfinish-js",
        ":handlers-handler-js",
        ":handlers-common-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-phonesigninstart-js",
    srcs = [
        "javascript/widgets/handler/phonesigninstart.js",
        ":externs-recaptcha"],
    deps = [
        ":strings-tpl",
        ":data-country-js",
        ":utils-phonenumber-js",
        ":element-common-js",
        ":element-progressdialog-js",
        ":page-phonesigninstart-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-providersignin-js",
    srcs = ["javascript/widgets/handler/providersignin.js"],
    deps = [
        ":widget-config-js",
        ":page-providersignin-js",
        ":handlers-handler-js",
        ":handlers-common-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-sendemaillinkforsignin-js",
    srcs = ["javascript/widgets/handler/sendemaillinkforsignin.js"],
    deps = [
        ":page-callback-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-starter-js",
    srcs = ["javascript/widgets/handler/starter.js"],
    deps = [
        ":utils-util-js",
        ":widget-config-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-unsupportedprovider-js",
    srcs = ["javascript/widgets/handler/unsupportedprovider.js"],
    deps = [
        ":page-unsupportedprovider-js",
        ":handlers-handler-js",
        ":handlers-common-js"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "handlers-signin-js",
    srcs = ["javascript/widgets/handler/signin.js"],
    deps = [
        ":page-signin-js",
        ":handlers-common-js",
        ":handlers-handler-js"],
    suppress = WIDGET_SUPPRESSIONS)


## Sources: JavaScript (Widgets)
closure_js_library(
    name = "widget-authuierror-js",
    srcs = ["javascript/widgets/authuierror.js"],
    deps = [":strings-tpl"])

closure_js_library(
    name = "widget-config-js",
    srcs = ["javascript/widgets/config.js"],
    deps = [
        ":data-country-js",
        ":widget-authuierror-js",
        ":utils-config-js",
        ":utils-phonenumber-js",
        ":utils-idp-js",
        ":utils-log-js",
        ":utils-util-js",
        "@io_bazel_rules_closure//closure/library/uri:uri",
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/object:object",
        "@io_bazel_rules_closure//closure/library/uri:utils"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "widget-dispatcher-js",
    srcs = ["javascript/widgets/dispatcher.js"],
    deps = [
        ":strings-tpl",
        ":utils-acclient-js",
        ":utils-storage-js",
        ":utils-util-js",
        ":widget-config-js",
        ":handlers-handler-js",
        ":handlers-common-js",
        "@io_bazel_rules_closure//closure/library/asserts:asserts",
        "@io_bazel_rules_closure//closure/library/uri:utils"],
    suppress = WIDGET_SUPPRESSIONS)

closure_js_library(
    name = "widget-authui-js",
    srcs = [
        "javascript/widgets/authui.js",
        "@com_google_firebase//:firebase-externs-js"],
    deps = [
        ":widget-authuierror-js",
        ":widget-config-js",
        ":widget-dispatcher-js",
        ":utils-actioncodeurlbuilder-js",
        ":utils-eventregister-js",
        ":utils-googleyolo-js",
        ":utils-phoneauthresult-js",
        ":utils-log-js",
        ":utils-storage-js",
        ":utils-util-js",
        ":firebaseui-handlers-js",
        "@io_bazel_rules_closure//closure/library/promise:promise",
        "@io_bazel_rules_closure//closure/library/array:array",
        "@io_bazel_rules_closure//closure/library/events:events",
        "@io_bazel_rules_closure//closure/library/events:eventtype"],
    suppress = WIDGET_SUPPRESSIONS)


## Exports
closure_js_library(
    name = "firebaseui-widgets-js",
    exports = [
        ":widget-authuierror-js",
        ":widget-config-js",
        ":widget-dispatcher-js",
        ":widget-authui-js"])

closure_js_library(
    name = "firebaseui-handlers-js",
    exports = [
        ":handlers-actioncode-js",
        ":handlers-anonymoususermismatch-js",
        ":handlers-handler-js",
        ":handlers-common-js",
        ":handlers-callback-js",
        ":handlers-differentdeviceerror-js",
        ":handlers-emaillinkconfirmation-js",
        ":handlers-emaillinknewdevicelinking-js",
        ":handlers-emaillinksignincallback-js",
        ":handlers-emaillinksigninlinking-js",
        ":handlers-emaillinksigninsent-js",
        ":handlers-emailmismatch-js",
        ":handlers-emailnotreceived-js",
        ":handlers-federatedlinking-js",
        ":handlers-federatedredirect-js",
        ":handlers-federatedsignin-js",
        ":handlers-passwordlinking-js",
        ":handlers-passwordrecovery-js",
        ":handlers-passwordsignin-js",
        ":handlers-passwordsignup-js",
        ":handlers-phonesigninfinish-js",
        ":handlers-phonesigninstart-js",
        ":handlers-providersignin-js",
        ":handlers-sendemaillinkforsignin-js",
        ":handlers-starter-js",
        ":handlers-unsupportedprovider-js",
        ":handlers-signin-js"])

closure_js_library(
    name = "firebaseui-utils-js",
    exports = [
        ":utils-account-js",
        ":utils-acclient-js",
        ":utils-actioncodeurlbuilder-js",
        ":utils-config-js",
        ":utils-cookiemechanism-js",
        ":utils-crypt-js",
        ":utils-eventregister-js",
        ":utils-googleyolo-js",
        ":utils-idp-js",
        ":utils-log-js",
        ":utils-pendingemailcredential-js",
        ":utils-phoneauthresult-js",
        ":utils-phonenumber-js",
        ":utils-sni-js",
        ":utils-storage-js",
        ":utils-util-js"])

closure_js_library(
    name = "firebaseui-element-js",
    exports = [
        ":element-common-js",
        ":element-dialog-js",
        ":element-email-js",
        ":element-form-js",
        ":element-idps-js",
        ":element-infobar-js",
        ":element-listboxdialog-js",
        ":element-name-js",
        ":element-newpassword-js",
        ":element-password-js",
        ":element-phoneconfirmationcode-js",
        ":element-phonenumber-js",
        ":element-progressdialog-js",
        ":element-recaptcha-js",
        ":element-resend-js",
        ":element-tospp-js"])

closure_js_library(
    name = "firebaseui-page-js",
    exports = [
        ":page-base-js",
        ":page-anonymoususermismatch-js",
        ":page-blank-js",
        ":page-callback-js",
        ":page-differentdeviceerror-js",
        ":page-emailchangerevoke-js",
        ":page-emaillinksigninconfirmation-js",
        ":page-emaillinksigninlinkingdifferentdevice-js",
        ":page-emaillinksigninlinking-js",
        ":page-emaillinksigninsent-js",
        ":page-emailmismatch-js",
        ":page-emailnotreceived-js",
        ":page-federatedlinking-js",
        ":page-notice-js",
        ":page-passwordlinking-js",
        ":page-passwordrecovery-js",
        ":page-passwordreset-js",
        ":page-passwordsignin-js",
        ":page-passwordsignup-js",
        ":page-phonesigninfinish-js",
        ":page-phonesigninstart-js",
        ":page-providersignin-js",
        ":page-signin-js",
        ":page-unsupportedprovider-js"])

closure_js_library(
    name = "firebaseui-js",
    exports = [
        # - Templates
        ":strings-tpl",
        ":elements-tpl",
        ":pages-tpl",

        # - JavaScript
        ":ui-mdl",
        ":firebaseui-utils-js",
        ":firebaseui-element-js",
        ":firebaseui-page-js",
        ":firebaseui-handlers-js",
        ":firebaseui-widgets-js",
        ":data-country-js"])

closure_js_library(
    name = "firebaseui",
    exports = [":firebaseui-js"])


## Translations
filegroup(name = "translations-ar-XB", srcs = ["translations/ar-XB.xtb"])
filegroup(name = "translations-cs", srcs = ["translations/cs.xtb"])
filegroup(name = "translations-en-GB", srcs = ["translations/en-GB.xtb"])
filegroup(name = "translations-fa", srcs = ["translations/fa.xtb"])
filegroup(name = "translations-hi", srcs = ["translations/hi.xtb"])
filegroup(name = "translations-it", srcs = ["translations/it.xtb"])
filegroup(name = "translations-lt", srcs = ["translations/lt.xtb"])
filegroup(name = "translations-pl", srcs = ["translations/pl.xtb"])
filegroup(name = "translations-ro", srcs = ["translations/ro.xtb"])
filegroup(name = "translations-sr", srcs = ["translations/sr.xtb"])
filegroup(name = "translations-uk", srcs = ["translations/uk.xtb"])
filegroup(name = "translations-ar", srcs = ["translations/ar.xtb"])
filegroup(name = "translations-da", srcs = ["translations/da.xtb"])
filegroup(name = "translations-en-XA", srcs = ["translations/en-XA.xtb"])
filegroup(name = "translations-fi", srcs = ["translations/fi.xtb"])
filegroup(name = "translations-hr", srcs = ["translations/hr.xtb"])
filegroup(name = "translations-iw", srcs = ["translations/iw.xtb"])
filegroup(name = "translations-lv", srcs = ["translations/lv.xtb"])
filegroup(name = "translations-pt-BR", srcs = ["translations/pt-BR.xtb"])
filegroup(name = "translations-ru", srcs = ["translations/ru.xtb"])
filegroup(name = "translations-sv", srcs = ["translations/sv.xtb"])
filegroup(name = "translations-vi", srcs = ["translations/vi.xtb"])
filegroup(name = "translations-bg", srcs = ["translations/bg.xtb"])
filegroup(name = "translations-de", srcs = ["translations/de.xtb"])
filegroup(name = "translations-es-419", srcs = ["translations/es-419.xtb"])
filegroup(name = "translations-fil", srcs = ["translations/fil.xtb"])
filegroup(name = "translations-hu", srcs = ["translations/hu.xtb"])
filegroup(name = "translations-ja", srcs = ["translations/ja.xtb"])
filegroup(name = "translations-nl", srcs = ["translations/nl.xtb"])
filegroup(name = "translations-pt-PT", srcs = ["translations/pt-PT.xtb"])
filegroup(name = "translations-sk", srcs = ["translations/sk.xtb"])
filegroup(name = "translations-th", srcs = ["translations/th.xtb"])
filegroup(name = "translations-zh-CN", srcs = ["translations/zh-CN.xtb"])
filegroup(name = "translations-ca", srcs = ["translations/ca.xtb"])
filegroup(name = "translations-el", srcs = ["translations/el.xtb"])
filegroup(name = "translations-es", srcs = ["translations/es.xtb"])
filegroup(name = "translations-fr", srcs = ["translations/fr.xtb"])
filegroup(name = "translations-id", srcs = ["translations/id.xtb"])
filegroup(name = "translations-ko", srcs = ["translations/ko.xtb"])
filegroup(name = "translations-no", srcs = ["translations/no.xtb"])
filegroup(name = "translations-pt", srcs = ["translations/pt.xtb"])
filegroup(name = "translations-sl", srcs = ["translations/sl.xtb"])
filegroup(name = "translations-tr", srcs = ["translations/tr.xtb"])
filegroup(name = "translations-zh-TW", srcs = ["translations/zh-TW.xtb"])

filegroup(
    name = "translations",
    srcs = [
      ":translations-ar-XB",
      ":translations-cs",
      ":translations-en-GB",
      ":translations-fa",
      ":translations-hi",
      ":translations-it",
      ":translations-lt",
      ":translations-pl",
      ":translations-ro",
      ":translations-sr",
      ":translations-uk",
      ":translations-ar",
      ":translations-da",
      ":translations-en-XA",
      ":translations-fi",
      ":translations-hr",
      ":translations-iw",
      ":translations-lv",
      ":translations-pt-BR",
      ":translations-ru",
      ":translations-sv",
      ":translations-vi",
      ":translations-bg",
      ":translations-de",
      ":translations-es-419",
      ":translations-fil",
      ":translations-hu",
      ":translations-ja",
      ":translations-nl",
      ":translations-pt-PT",
      ":translations-sk",
      ":translations-th",
      ":translations-zh-CN",
      ":translations-ca",
      ":translations-el",
      ":translations-es",
      ":translations-fr",
      ":translations-id",
      ":translations-ko",
      ":translations-no",
      ":translations-pt",
      ":translations-sl",
      ":translations-tr",
      ":translations-zh-TW"])
