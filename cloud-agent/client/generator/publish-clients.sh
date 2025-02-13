#!/bin/bash
set -e

AGENT_VERSION=${VERSION_TAG:13}
echo version=${AGENT_VERSION}

# install dependencies
yarn

gradle -p ../kotlin -Pversion=${AGENT_VERSION} build
gradle -p ../kotlin -Pversion=${AGENT_VERSION} publish --debug

# typescript
yarn --cwd ../typescript
yarn --cwd ../typescript publish --new-version ${AGENT_VERSION} --no-git-tag-version --non-interactive

# python
