
##
## GUST: Makefile
##

BAZELISK ?= $(shell which bazelisk)


all: build test

build:  ## Build all framework targets.
	$(BAZELISK) build //...

test:  ## Run all framework testsuites.
	$(BAZELISK) test //...

help:  ## Show this help text.
	$(info GUST Framework Tools:)
	@grep -E '^[a-z1-9A-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


.PHONY: build test help

