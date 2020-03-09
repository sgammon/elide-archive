#!/bin/bash

set -o xtrace;

echo "Uploading to Codecov...";
curl https://codecov.io/bash > ./.codecov.sh && chmod +x ./.codecov.sh && \
	./.codecov.sh -f "$1/lcov.dat" \
                -X gcov -X coveragepy -X search -X xcode -X gcovout -Z \
                -F $2 \
                -n $3; \
	rm -f ./.codecov.sh;

echo "Uploading to CodeClimate...";
curl https://codeclimate.com/downloads/test-reporter/test-reporter-latest-linux-amd64 > ./codeclimate.sh && \
  chmod +x ./codeclimate.sh && \
  ./codeclimate.sh format-coverage --input-type lcov --output reports/codeclimate.json "$1/lcov.dat" && \
  ./codeclimate.sh upload-coverage --input reports/codeclimate.json;

echo "Coverage reporting complete.";
