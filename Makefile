
##
## GUST: Makefile
##

CI ?= no
CACHE ?= yes
REMOTE ?= no
VERBOSE ?= no
STRICT ?= no
COVERAGE ?= no
PROJECT ?= bloom-sandbox
RBE_INSTANCE ?= default_instance
CACHE_KEY ?= GustBuild

TARGETS ?= //java/... //proto/... //js/... //style/...
TESTS ?= //javatests/...

TAG ?= --config=dev
TEST_ARGS ?= --test_output=errors
BUILD_ARGS ?=

BAZELISK ?= $(shell which bazelisk)
BAZELISK_ARGS ?=
BASE_ARGS ?= --google_default_credentials=true


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

# Flag: `CI`
ifeq ($(CI),yes)
TAG += --config=ci
endif
endif

# Flag: `VERBOSE`
ifeq ($(VERBOSE),yes)
BASE_ARGS += -s
endif


all: devtools build test

build:  ## Build all framework targets.
	$(BAZELISK) $(BAZELISK_ARGS) build $(TAG) $(BASE_ARGS) $(BUILD_ARGS) $(TARGETS)

clean:  ## Clean ephemeral targets.
	$(BAZELISK) $(BAZELISK_ARGS) clean

distclean:  ## Clean targets, caches and dependencies.
	$(BAZELISK) $(BAZELISK_ARGS) clean --expunge_async

forceclean: distclean  ## Clean everything, and sanitize the codebase (DANGEROUS).
	git reset --hard && git clean -xdf

test:  ## Run all framework testsuites.
	$(BAZELISK) $(BAZELISK_ARGS) $(TEST_COMMAND) $(TAG) $(BASE_ARGS) $(TEST_ARGS) $(TESTS)

help:  ## Show this help text.
	$(info GUST Framework Tools:)
	@grep -E '^[a-z1-9A-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

devtools:  ## Install local development dependencies.
	@echo "Installing devtools..."
	@git submodule update --init --recursive

update-deps:  ## Re-seal and update all dependencies.
	@echo "Updating devtools..."
	git submodule update --remote --init
	@echo "Re-pinning Maven dependencies..."
	$(BAZELISK) $(BAZELISK_ARGS) run @unpinned_maven//:pin

.PHONY: build test help

