##
# Copyright © 2022, The Elide Framework Authors. All rights reserved.
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

package(
    default_visibility = ["//visibility:public"],
)

load("@rules_proto//proto:defs.bzl", "proto_library")
load("@elide//tools/defs/java:java.bzl", "maven")


# Safe HTML Types
proto_library(
    name = "proto",
    srcs = ["proto/src/main/protobuf/webutil/html/types/html.proto"],
    strip_import_prefix = "proto/src/main/protobuf",
)

# Java HTML Types
java_proto_library(
    name = "java-proto",
    deps = [":proto"],
)

java_library(
    name = "java",
    srcs = glob(["types/src/main/java/com/google/common/html/types/*.java"]),
    deps = [
        ":java-proto",
        "@com_google_guava",
        "@javax_annotation_api",
        "@com_google_jsinterop_annotations//:jsinterop-annotations",
        maven("com.google.errorprone:error_prone_annotations"),
    ],
    exports = [
        ":java-proto",
    ]
)
