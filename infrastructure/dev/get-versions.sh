#!/usr/bin/env bash

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

# Set working directory
cd ${SCRIPT_DIR}

export PRISM_AGENT_VERSION=$(cd ../../ && sbt "project agent" -Dsbt.supershell=false -error "print version")
echo "prism-agent server version: ${PRISM_AGENT_VERSION}"

export MERCURY_MEDIATOR_VERSION=$(cd ../../ && sbt "project mediator" -Dsbt.supershell=false -error "print version")
echo "mercury-mediator version: ${MERCURY_MEDIATOR_VERSION}"

export PRISM_NODE_VERSION=v2.1.1
echo "prism node version: ${PRISM_NODE_VERSION}"
