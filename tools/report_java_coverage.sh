#!/bin/bash
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

set -o xtrace;

echo "Uploading to Codecov...";
curl -L https://codecov.io/bash > ./.codecov.sh && chmod +x ./.codecov.sh && \
	./.codecov.sh -f "$1/lcov.dat" \
                -X gcov -X coveragepy -X search -X xcode -X gcovout -Z \
                -F $2 \
                -n $3; \
	rm -f ./.codecov.sh;

echo "Uploading to CodeClimate...";
curl -L https://codeclimate.com/downloads/test-reporter/test-reporter-latest-linux-amd64 > ./codeclimate.sh && \
  chmod +x ./codeclimate.sh && \
  ./codeclimate.sh format-coverage --input-type lcov --output reports/codeclimate.json "$1/lcov.dat" && \
  ./codeclimate.sh upload-coverage --input reports/codeclimate.json;

echo "Coverage reporting complete.";
