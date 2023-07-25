#!/bin/bash
set -e

PRISM_AGENT_VERSION=${VERSION_TAG:13}

# install dependencies
yarn

# kotlin
gradle -p ../kotlin -Pversion=${PRISM_AGENT_VERSION} publish

# typescript
yarn --cwd ../typescript
yarn --cwd ../typescript publish --new-version ${PRISM_AGENT_VERSION} --no-git-tag-version

# python