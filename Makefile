
##
## GUST: Makefile
##

CI ?= no
CACHE ?= yes
REMOTE ?= no
VERBOSE ?= no
QUIET ?= no
STRICT ?= no
COVERAGE ?= no
PROJECT ?= bloom-sandbox
RBE_INSTANCE ?= default_instance
CACHE_KEY ?= GustBuild

APP ?=
TARGETS ?= //java/... //proto/... //js/... //style/...
TESTS ?= //javatests/...

TAG ?=
TEST_ARGS ?= --test_output=errors
BUILD_ARGS ?=

BAZELISK ?= $(shell which bazelisk)
BAZELISK_ARGS ?=
BASE_ARGS ?= --google_default_credentials=true --define project=$(PROJECT)


# Flag: `COVERAGE`
ifeq ($(COVERAGE),yes)
TEST_COMMAND ?= coverage
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
else
TAG += --config=dev
endif

# Flag: `VERBOSE`
ifeq ($(VERBOSE),yes)
BASE_ARGS += -s
endif

# Flag: `QUIET`
ifeq ($(VERBOSE),yes)
_RULE = @
endif


all: devtools build test

b build:  ## Build all framework targets.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(TAG) $(BASE_ARGS) $(BUILD_ARGS) $(TARGETS)

r run:  ## Run the specified target.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) $(APP)

c clean:  ## Clean ephemeral targets.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) clean

samples:  ## Build and push sample app images.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) //javatests/server:BasicTestApplication-image-push
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) //javatests/server:BasicTestApplication-native-image-push
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) //javatests/ssr:SSRTestApplication-image-push
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(TAG) $(BASE_ARGS) $(BUILD_ARGS) //javatests/ssr:SSRTestApplication-native-image-push

distclean:  ## Clean targets, caches and dependencies.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) clean --expunge_async

forceclean: distclean  ## Clean everything, and sanitize the codebase (DANGEROUS).
	git reset --hard && git clean -xdf

test:  ## Run all framework testsuites.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) $(TEST_COMMAND) $(TAG) $(BASE_ARGS) $(TEST_ARGS) $(TESTS)

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
	$(BAZELISK) $(BAZELISK_ARGS) run @unpinned_maven//:pin

.PHONY: build test help samples
