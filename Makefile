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

##
## GUST: Makefile
##

CI ?= no
DEV ?= no
CACHE ?= no
REMOTE ?= no
DEBUG ?= no
VERBOSE ?= no
QUIET ?= yes
STRICT ?= no
COVERAGE ?= yes
FORCE_COVERAGE ?= no
PROJECT ?= elide-ai
IMAGE_PROJECT ?= elide-ai
RBE_INSTANCE ?= default_instance
CACHE_KEY ?= GustBuild
REGISTRY ?= bloomworks
PROJECT_NAME ?= GUST
ENABLE_REPORTCI ?= yes
JS_COVERAGE_REPORT ?= no
REPORTS ?= reports
CI_REPO ?= sgammon/GUST

SAMPLES ?= //samples/rest_mvc/java:MicronautMVCSample //samples/soy_ssr/src:MicronautSSRSample


ifneq (,$(findstring Darwin,$(shell uname -a)))
OUTPUT_BASE ?= darwin-dbg
else
OUTPUT_BASE ?= k8-fastbuild
endif

ARGS ?=
DOCS ?= docs
DISTPATH ?= dist
OUTPATH ?= $(DISTPATH)/out
BINPATH ?= $(DISTPATH)/bin
UNZIP ?= $(shell which unzip)
REVISION ?= $(shell git describe --abbrev=7 --always --tags HEAD)
BASE_VERSION ?= v1b
VERSION ?= $(shell (cat package.json | grep version | head -1 | awk -F: '{ print $2 }' | sed 's/[",]//g' | tr -d '[[:space:]]' | sed 's/version\://g'))
CHROME_COVERAGE ?= $(shell find dist/out/$(OUTPUT_BASE)/bin -name "coverage*.dat" | grep chrome | xargs)
COVERAGE_DATA ?= $(OUTPATH)/_coverage/_coverage_report.dat
COVERAGE_REPORT ?= $(REPORTS)/coverage
COVERAGE_ARGS ?= --function-coverage \
                 --branch-coverage \
                 --highlight \
                 --demangle-cpp \
                 --show-details \
                 --title "$(PROJECT_NAME)" \
                 --precision 2 \
                 --legend \
                 --no-source \
                 --rc genhtml_med_limit=60 \
                 --rc genhtml_hi_limit=90

APP ?=
TARGETS ?= //java/... //gust/... //js/... //style/...
TESTS ?= //javatests:suite //jstests/...
COVERABLE ?=

TAG ?=
ifeq ($(QUIET),yes)
TEST_ARGS ?=
else
TEST_ARGS ?= --test_output=errors
endif
TEST_ARGS_WITH_COVERAGE ?= --combined_report=lcov --nocache_test_results
BUILD_ARGS ?= --define project=$(PROJECT)

BUILDKEY_PLAINTEXT ?= $(shell pwd)/crypto/build-key.json
BUILDKEY_CIPHERTEXT ?= $(BUILDKEY_PLAINTEXT).enc
BUILDKEY_KMS_LOCATION ?= global
BUILDKEY_KMS_KEYRING ?= dev
BUILDKEY_KMS_KEY ?= key-material
BUILDKEY_BASE_ARGS ?= --location=$(BUILDKEY_KMS_LOCATION) --keyring=$(BUILDKEY_KMS_KEYRING) --key=$(BUILDKEY_KMS_KEY) \
			--plaintext-file=$(BUILDKEY_PLAINTEXT) --ciphertext-file=$(BUILDKEY_CIPHERTEXT) --project=$(IMAGE_PROJECT)

POSIX_FLAGS ?=
BAZELISK_ARGS ?=
BASE_ARGS ?=
BAZELISK_PREAMBLE ?=


# Flag: `FORCE_COVERAGE`
ifeq ($(FORCE_COVERAGE),yes)
TEST_COMMAND ?= coverage
TEST_ARGS += $(TEST_ARGS_WITH_COVERAGE)
else
TEST_COMMAND ?= test
endif

# Flag: `DEV`
ifeq ($(DEV),yes)
BASE_ARGS += --config=devkey --test_output=errors
BAZELISK_PREAMBLE = GOOGLE_APPLICATION_CREDENTIALS=$(BUILDKEY_PLAINTEXT)
endif

# Flag: `STRICT`.
ifeq ($(STRICT),yes)
BAZELISK_ARGS += --strict
endif

# Flag: `CACHE`.
ifeq ($(CACHE),yes)
BASE_ARGS += --remote_cache=grpcs://remotebuildexecution.googleapis.com --google_default_credentials=true \
             --remote_instance_name=projects/$(PROJECT)/instances/$(RBE_INSTANCE) \
	     --host_platform_remote_properties_override='properties:{name:"cache-silo-key" value:"$(CACHE_KEY)"}'
endif

# Flag: `REMOTE`
ifeq ($(REMOTE),yes)
TAG += --config=remote
ifeq ($(CACHE),no)
BASE_ARGS += --remote_instance_name=projects/$(PROJECT)/instances/$(RBE_INSTANCE)
endif
else
endif

# Flag: `CI`
ifeq ($(CI),yes)
TAG += --config=ci
_DEFAULT_JAVA_HOME = $(shell echo $$JAVA_HOME_12_X64)
BASE_ARGS += --define=ZULUBASE=$(_DEFAULT_JAVA_HOME) --define=jdk=zulu
BAZELISK ?= /bin/bazelisk
GENHTML ?= /bin/genhtml
else
TAG += --config=dev
IBAZEL ?= $(shell which ibazel)
BAZELISK ?= $(BAZELISK_PREAMBLE) $(shell which bazelisk)
GENHTML ?= $(shell which genhtml)
endif

# Flag: `DEBUG`
ifeq ($(DEBUG),yes)
VERBOSE = yes
QUIET = no
BASE_ARGS += --sandbox_debug
endif

# Flag: `VERBOSE`
ifeq ($(VERBOSE),yes)
BASE_ARGS += -s --verbose_failures
POSIX_FLAGS += -v
_RULE =
else

# Flag: `QUIET`
ifeq ($(QUIET),yes)
_RULE = @
endif
endif


all: devtools build test  ## Build and test all framework targets.

build:  ## Build all framework targets.
	$(info Building $(PROJECT_NAME)...)
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(TAG) $(BASE_ARGS) $(BUILD_ARGS) -- $(TARGETS)

run:  ## Run the specified target.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) -- $(APP) $(ARGS)

dev:  ## Develop against the specified target.
	$(_RULE)$(IBAZEL) run $(TAG) --define=LIVE_RELOAD=enabled --define=dev=enabled $(APP)

clean: clean-docs clean-reports  ## Clean ephemeral targets.
	$(_RULE)rm -f $(BUILDKEY_PLAINTEXT)
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) clean

clean-docs:  ## Clean built documentation.
	@echo "Cleaning docs..."
	$(_RULE)rm -fr $(POSIX_FLAGS) $(DOCS)

clean-reports:  ## Clean built reports.
	@echo "Cleaning reports..."
	$(_RULE) -fr $(POSIX_FLAGS) $(REPORTS)

bases:  ## Build base images and push them.
	@echo "Building Alpine base ('$(BASE_VERSION)')..."
	$(_RULE)docker build -t us.gcr.io/$(IMAGE_PROJECT)/base/alpine:$(BASE_VERSION) ./images/base/alpine
	@echo "Building Node base ('$(BASE_VERSION)')..."
	$(_RULE)docker build -t us.gcr.io/$(IMAGE_PROJECT)/base/node:$(BASE_VERSION) ./images/base/node
	@echo "Pushing bases..."
	$(_RULE)docker push us.gcr.io/$(IMAGE_PROJECT)/base/alpine:$(BASE_VERSION)
	$(_RULE)docker push us.gcr.io/$(IMAGE_PROJECT)/base/node:$(BASE_VERSION)

samples:  ## Build and push sample app images.
	$(_RULE)for target in $(SAMPLES) ; do \
        $(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) $$(echo "$$target")-image-push && \
        $(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) $$(echo "$$target")-native-image-push; \
        done

distclean: clean  ## Clean targets, caches and dependencies.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) clean --expunge

forceclean: distclean  ## Clean everything, and sanitize the codebase (DANGEROUS).
	$(_RULE)git reset --hard && git clean -xdf

test:  ## Run all framework testsuites.
ifeq ($(COVERAGE),yes)
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) coverage $(TAG) $(BASE_ARGS) $(TEST_ARGS) $(TEST_ARGS_WITH_COVERAGE) -- $(TESTS)
	$(_RULE)$(GENHTML) $(COVERAGE_DATA) --output-directory $(COVERAGE_REPORT) $(COVERAGE_ARGS)
	$(_RULE)cp -f $(POSIX_FLAGS) $(COVERAGE_DATA) $(COVERAGE_REPORT)/lcov.dat
else
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) $(TEST_COMMAND) $(TAG) $(BASE_ARGS) $(TEST_ARGS) -- $(TESTS)
endif

docs:  ## Build documentation for the framework.
	@echo "Building GUST framework docs (Java)..."
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(TAG) $(BASE_ARGS) -- //java:javadoc && \
		mkdir -p $(DOCS)/java && \
		$(UNZIP) -o -d $(DOCS)/java/ $(BINPATH)/java/gust/javadoc.jar;
	@#$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(TAG) $(BASE_ARGS) //:docs

help:  ## Show this help text.
	$(info GUST Framework Tools:)
	@grep -E '^[a-z1-9A-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

devtools:  ## Install local development dependencies.
	@echo "Installing devtools..."
	$(_RULE)git submodule update --init --recursive

builder-image:  ## Build a new version of the CI builder image for Gust.
	@echo "Building CI image..."
	$(_RULE)gcloud builds submit ./images/ci -t us.gcr.io/$(IMAGE_PROJECT)/tools/gcb

update-deps:  ## Re-seal and update all dependencies.
	@echo "Re-pinning Maven dependencies..."
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run @unpinned_maven//:pin

serve-docs: clean-docs docs  ## Serve the docs locally (must generate first).
	@echo "Serving framework docs..."
	$(_RULE)cd $(DOCS) && python -m SimpleHTTPServer

serve-coverage:  ## Serve the current coverage report (must generate first).
	@echo "Serving coverage report..."
	$(_RULE)cd $(COVERAGE_REPORT) && python -m SimpleHTTPServer

report-tests: ## Report test results to Report.CI.
	@echo "Scanning for test results..."
	$(_RULE)pip install -r tools/requirements.txt
	$(_RULE)find dist/out/$(OUTPUT_BASE) -name test.xml | xargs python3 tools/merge_test_results.py reports/tests.xml
	@echo "Generating HTML test report..."
	$(_RULE)cd reports && python3 -m junit2htmlreport tests.xml
ifeq ($(ENABLE_REPORTCI),yes)
	@echo "Reporting test results..."
	$(_RULE)-TRAVIS=true \
	    TRAVIS_COMMIT=$$BUILDKITE_COMMIT \
	    TRAVIS_BRANCH=$$BUILDKITE_BRANCH \
	    TRAVIS_COMMIT_MESSAGE=$$BUILDKITE_MESSAGE \
	    TRAVIS_PULL_REQUEST=$$BUILDKITE_PULL_REQUEST \
	    TRAVIS_PULL_REQUEST_BRANCH=$$BUILDKITE_PULL_REQUEST_BASE_BRANCH \
	    TRAVIS_REPO_SLUG=$(CI_REPO) \
	    curl -s https://raw.githubusercontent.com/report-ci/scripts/master/upload.py | python - --include='reports/tests.xml' --framework=junit
endif

report-coverage:  ## Report coverage results to Codecov.
	@echo "Building coverage tarball..."
	@cd reports/coverage && tar -czvf ../coverage.tar.gz ./*
	@echo "Reporting Java coverage to Codecov..."
	$(_RULE)tools/report_java_coverage.sh $(COVERAGE_REPORT) backend,jvm javatests;
ifeq ($(JS_COVERAGE_REPORT),yes)
	@echo "Reporting JS (frontend) coverage to Codecov..."
	$(_RULE)tools/report_js_coverage.sh frontend,js tests "$(addprefix -f ,$(CHROME_COVERAGE))";
endif

release-images:  ## Pull, tag, and release Docker images.
	@echo "Pulling images for revision $(REVISION)..."
	$(_RULE)docker pull us.gcr.io/$(IMAGE_PROJECT)/sample/basic/jvm:$(REVISION)
	$(_RULE)docker pull us.gcr.io/$(IMAGE_PROJECT)/sample/basic/native:$(REVISION)
	$(_RULE)docker pull us.gcr.io/$(IMAGE_PROJECT)/sample/ssr/jvm:$(REVISION)
	$(_RULE)docker pull us.gcr.io/$(IMAGE_PROJECT)/sample/ssr/native:$(REVISION)

	@echo "Tagging images for release ($(VERSION))..."
	$(_RULE)docker tag us.gcr.io/$(IMAGE_PROJECT)/sample/basic/native:$(REVISION) \
                       $(REGISTRY)/sample-basic:$(VERSION);
	$(_RULE)docker tag us.gcr.io/$(IMAGE_PROJECT)/sample/basic/jvm:$(REVISION) \
                       $(REGISTRY)/sample-basic-jvm:$(VERSION);
	$(_RULE)docker tag us.gcr.io/$(IMAGE_PROJECT)/sample/ssr/native:$(REVISION) \
                       $(REGISTRY)/sample-ssr:$(VERSION);
	$(_RULE)docker tag us.gcr.io/$(IMAGE_PROJECT)/sample/ssr/jvm:$(REVISION) \
                       $(REGISTRY)/sample-ssr-jvm:$(VERSION);

	@echo "Pushing images to repository ($(VERSION))..."
	$(_RULE)docker push $(REGISTRY)/sample-basic:$(VERSION);
	$(_RULE)docker push $(REGISTRY)/sample-basic-jvm:$(VERSION);
	$(_RULE)docker push $(REGISTRY)/sample-ssr:$(VERSION);
	$(_RULE)docker push $(REGISTRY)/sample-ssr-jvm:$(VERSION);


## Crypto
$(BUILDKEY_CIPHERTEXT):
	@echo "Encrypting build key..."
	$(_RULE)gcloud kms encrypt $(BUILDKEY_BASE_ARGS)

encrypt: $(BUILDKEY_CIPHERTEXT)  ## Encrypt private key material.
	@echo "Key material encrypted."

$(BUILDKEY_PLAINTEXT):
	@echo "Decrypting build key..."
	$(_RULE)gcloud kms decrypt $(BUILDKEY_BASE_ARGS)

decrypt: $(BUILDKEY_PLAINTEXT)  ## Decrypt private key material.
	@echo "Key material decrypted."


.PHONY: build test help samples release-images update-deps devtools

