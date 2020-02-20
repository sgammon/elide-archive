#!/bin/bash

set -o xtrace;

curl https://codecov.io/bash > ./.codecov.sh && chmod +x ./.codecov.sh && \
	./.codecov.sh \
                -X gcov -X coveragepy -X search -X xcode -X gcovout -Z \
                -F $1 \
                -n $2 \
                $3;
	rm -f ./.codecov.sh;

echo "Coverage reporting complete.";

