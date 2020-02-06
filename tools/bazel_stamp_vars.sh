#!/usr/bin/env bash
echo BUILD_SCM_REVISION $(git rev-parse --short HEAD)
echo BUILD_SCM_VERSION $(git describe --abbrev=7 --always --tags HEAD)
echo BUILD_GUST_VERSION $(cat package.json | grep version | head -1 | awk -F: '{ print $2 }' | sed 's/[",]//g' | tr -d '[[:space:]]')
