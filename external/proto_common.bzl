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

package(
    default_visibility = ["//visibility:public"],
)

load("@rules_proto//proto:defs.bzl", "proto_library")


# Sources Export
exports_files(glob([
    "google/**/*.proto",
]))

# Well-Known/Common Types
proto_library(
    name = "type_calendar_period",
    srcs = ["google/type/calendar_period.proto"],
)

proto_library(
    name = "type_color",
    srcs = ["google/type/color.proto"],
)

proto_library(
    name = "type_date",
    srcs = ["google/type/date.proto"],
)

proto_library(
    name = "type_dayofweek",
    srcs = ["google/type/dayofweek.proto"],
)

proto_library(
    name = "type_expr",
    srcs = ["google/type/expr.proto"],
)

proto_library(
    name = "type_fraction",
    srcs = ["google/type/fraction.proto"],
)

proto_library(
    name = "type_latlng",
    srcs = ["google/type/latlng.proto"],
)

proto_library(
    name = "type_money",
    srcs = ["google/type/money.proto"],
)

proto_library(
    name = "type_month",
    srcs = ["google/type/month.proto"],
)

proto_library(
    name = "type_postal_address",
    srcs = ["google/type/postal_address.proto"],
)

proto_library(
    name = "type_quaternion",
    srcs = ["google/type/quaternion.proto"],
)

proto_library(
    name = "type_timeofday",
    srcs = ["google/type/timeofday.proto"],
)

# RPC Types
proto_library(
    name = "rpc_code",
    srcs = ["google/rpc/code.proto"],
)

proto_library(
    name = "rpc_error_details",
    srcs = ["google/rpc/error_details.proto"],
    deps = ["@com_google_protobuf//:duration_proto"],
)

proto_library(
    name = "rpc_status",
    srcs = ["google/rpc/status.proto"],
    deps = ["@com_google_protobuf//:any_proto"],
)
