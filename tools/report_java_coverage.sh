#!/bin/bash

set -o xtrace;

curl https://codecov.io/bash > ./.codecov.sh && chmod +x ./.codecov.sh && \
	./.codecov.sh -f $1/lcov.dat \
                -X gcov -X coveragepy -X search -X xcode -X gcovout -Z \
                -F $2 \
                -n $3; \
	rm -f ./.codecov.sh;

echo "Coverage reporting complete.";

