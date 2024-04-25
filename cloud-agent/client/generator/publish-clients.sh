#!/bin/bash
set -e

AGENT_VERSION=${VERSION_TAG:13}

# install dependencies
yarn

# kotlin
gradle -p ../kotlin -Pversion=${AGENT_VERSION} publish

# typescript
yarn --cwd ../typescript
yarn --cwd ../typescript publish --new-version ${AGENT_VERSION} --no-git-tag-version

# python
