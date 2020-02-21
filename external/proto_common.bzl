
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
