#!/bin/bash
set -e

AGENT_VERSION=${VERSION_TAG:13}
echo version=${AGENT_VERSION}

# install dependencies
yarn

# kotlin
# Determine if the version is a snapshot or a release
if [[ "$AGENT_VERSION" == *-* ]]; then
  echo "Publishing snapshot version"
  # kotlin
  gradle -p ../kotlin -Pversion=${AGENT_VERSION} publishToSonatype
else
  echo "Publishing release version"
  # kotlin
  gradle -p ../kotlin -Pversion=${AGENT_VERSION} publishToSonatype closeAndReleaseSonatypeStagingRepository
fi

# typescript
yarn --cwd ../typescript
yarn --cwd ../typescript publish --new-version ${AGENT_VERSION} --no-git-tag-version

# python
