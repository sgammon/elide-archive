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
    "@gust//defs/toolchain:deps.bzl",
    "maven",
)

load(
    "@gust//defs/toolchain/java:rules.bzl",
    java_library = "jdk_library",
)


java_library(
    name = "io_micronaut_micronaut_views",
    srcs = glob([
        "src/main/java/io/micronaut/views/*.java",
        "src/main/java/io/micronaut/views/csp/*.java",
        "src/main/java/io/micronaut/views/model/*.java",
        "src/main/java/io/micronaut/views/exceptions/*.java",
    ]),
    deps = [
        "@gust//defs/toolchain/java/plugins:micronaut",
        "@javax_inject//:javax_inject",
        "@com_google_code_findbugs_jsr305",
        maven("io.micronaut:micronaut-core"),
        maven("io.micronaut:micronaut-inject"),
        maven("io.micronaut:micronaut-runtime"),
        maven("io.micronaut:micronaut-router"),
        maven("io.micronaut:micronaut-http"),
        maven("io.micronaut:micronaut-http-server"),
        maven("io.micronaut:micronaut-security"),
        maven("io.reactivex.rxjava2:rxjava"),
        maven("org.reactivestreams:reactive-streams"),
        maven("org.slf4j:slf4j-api"),
    ],
)
