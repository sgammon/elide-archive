
##
## GUST: Makefile
##

CI ?= no
CACHE ?= yes
REMOTE ?= no
VERBOSE ?= no
QUIET ?= yes
STRICT ?= no
COVERAGE ?= yes
FORCE_COVERAGE ?= no
PROJECT ?= bloom-sandbox
IMAGE_PROJECT ?= elide-tools
RBE_INSTANCE ?= default_instance
CACHE_KEY ?= GustBuild
REGISTRY ?= bloomworks
PROJECT_NAME ?= GUST
ENABLE_REPORTCI ?= yes

SAMPLES ?= //samples/rest_mvc/java:MicronautMVCSample //samples/soy_ssr/src:MicronautSSRSample

OUTPATH ?= dist/out
REVISION ?= $(shell git describe --abbrev=7 --always --tags HEAD)
BASE_VERSION ?= v1a
VERSION ?= $(shell (cat package.json | grep version | head -1 | awk -F: '{ print $2 }' | sed 's/[",]//g' | tr -d '[[:space:]]' | sed 's/version\://g'))
COVERAGE_DATA ?= $(OUTPATH)/_coverage/_coverage_report.dat
COVERAGE_REPORT ?= reports/coverage
COVERAGE_ARGS ?= --function-coverage \
                 --branch-coverage \
                 --highlight \
                 --demangle-cpp \
                 --show-details \
                 --title "$(PROJECT_NAME)" \
                 --precision 2 \
                 --legend \
                 --rc genhtml_med_limit=60 \
                 --rc genhtml_hi_limit=90

APP ?=
TARGETS ?= //java/... //proto/... //js/... //style/...
TESTS ?= //tests/...
COVERABLE ?= //javatests/... //jstests/...

TAG ?=
TEST_ARGS ?= --test_output=errors
TEST_ARGS_WITH_COVERAGE ?= --combined_report=lcov --nocache_test_results
BUILD_ARGS ?=

POSIX_FLAGS ?=
BAZELISK_ARGS ?=
BASE_ARGS ?= --google_default_credentials=true --define project=$(PROJECT)

ifneq (,$(findstring Darwin,$(shell uname -a)))
OUTPUT_BASE ?= darwin-dbg
else
OUTPUT_BASE ?= k8-fastbuild
endif


# Flag: `FORCE_COVERAGE`
ifeq ($(FORCE_COVERAGE),yes)
TEST_COMMAND ?= coverage
TEST_ARGS += $(TEST_ARGS_WITH_COVERAGE)
else
TEST_COMMAND ?= test
endif

# Flag: `STRICT`.
ifeq ($(STRICT),yes)
BAZELISK_ARGS += --strict
endif

# Flag: `CACHE`.
ifeq ($(CACHE),yes)
BASE_ARGS += --remote_cache=grpcs://remotebuildexecution.googleapis.com \
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
BAZELISK ?= $(shell which bazelisk)
GENHTML ?= $(shell which genhtml)
endif

# Flag: `VERBOSE`
ifeq ($(VERBOSE),yes)
BASE_ARGS += -s --verbose_failures
POSIX_FLAGS += -v
else
_RULE = @
endif

# Flag: `QUIET`
ifeq ($(QUIET),yes)
_RULE = @
endif


all: devtools build test

b build:  ## Build all framework targets.
	$(info Building $(PROJECT_NAME)...)
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(TAG) $(BASE_ARGS) $(BUILD_ARGS) -- $(TARGETS)

r run:  ## Run the specified target.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) -- $(APP)

c clean:  ## Clean ephemeral targets.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) clean

bases:  ## Build base images and push them.
	@echo "Building Alpine base ('$(BASE_VERSION)')..."
	$(_RULE)docker build -t us.gcr.io/$(IMAGE_PROJECT)/base/alpine:$(BASE_VERSION) ./base/alpine
	@echo "Building Node base ('$(BASE_VERSION)')..."
	$(_RULE)docker build -t us.gcr.io/$(IMAGE_PROJECT)/base/node:$(BASE_VERSION) ./base/node
	@echo "Pushing bases..."
	$(_RULE)docker push us.gcr.io/$(IMAGE_PROJECT)/base/alpine:$(BASE_VERSION)
	$(_RULE)docker push us.gcr.io/$(IMAGE_PROJECT)/base/node:$(BASE_VERSION)

samples:  ## Build and push sample app images.
	$(_RULE)for target in $(SAMPLES) ; do \
        $(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) $$(echo "$$target")-image-push && \
        $(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) $$(echo "$$target")-native-image-push; \
        done

distclean:  ## Clean targets, caches and dependencies.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) clean --expunge_async

forceclean: distclean  ## Clean everything, and sanitize the codebase (DANGEROUS).
	$(_RULE)git reset --hard && git clean -xdf

test:  ## Run all framework testsuites.
ifeq ($(COVERAGE),yes)
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) $(TEST_COMMAND) $(TAG) $(BASE_ARGS) $(TEST_ARGS) -- $(TESTS)
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) coverage $(TAG) $(BASE_ARGS) $(TEST_ARGS) $(TEST_ARGS_WITH_COVERAGE) -- $(COVERABLE)
	$(_RULE)$(GENHTML) $(COVERAGE_DATA) --output-directory $(COVERAGE_REPORT) $(COVERAGE_ARGS)
	$(_RULE)cp -f $(POSIX_FLAGS) $(COVERAGE_DATA) $(COVERAGE_REPORT)/lcov.dat
else
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) $(TEST_COMMAND) $(TAG) $(BASE_ARGS) $(TEST_ARGS) -- $(TESTS) $(COVERABLE)
endif

docs:  ## Build documentation for the framework.
	@echo "Building GUST docs..."
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(TAG) $(BASE_ARGS) //:docs

help:  ## Show this help text.
	$(info GUST Framework Tools:)
	@grep -E '^[a-z1-9A-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

devtools:  ## Install local development dependencies.
	@echo "Installing devtools..."
	$(_RULE)git submodule update --init --recursive

update-deps:  ## Re-seal and update all dependencies.
	@echo "Re-pinning Maven dependencies..."
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run @unpinned_maven//:pin

serve-coverage:  ## Serve the current coverage report (must generate first).
	@echo "Serving coverage report..."
	$(_RULE)cd $(COVERAGE_REPORT) && python -m SimpleHTTPServer

report-tests: ## Report test results to Report.CI.
	@echo "Scanning for test results..."
	$(_RULE)pip install -r tools/requirements.txt
	$(_RULE)find dist/out/$(OUTPUT_BASE) -name test.xml | xargs python tools/merge_test_results.py reports/tests.xml
	@echo "Generating HTML test report..."
	$(_RULE)cd reports && python -m junit2htmlreport tests.xml
ifeq ($(ENABLE_REPORTCI),yes)
	@echo "Reporting test results..."
	$(_RULE)-curl -s https://report.ci/upload.py | python - --include='reports/tests.xml' --framework=junit
endif

report-coverage:  ## Report coverage results to Codecov.
	@echo "Reporting Java coverage to Codecov..."
	$(_RULE)tools/report_java_coverage.sh $(COVERAGE_REPORT) backend,jvm javatests;

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


.PHONY: build test help samples release-images update-deps devtools
