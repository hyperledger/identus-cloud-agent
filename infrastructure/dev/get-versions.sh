#!/usr/bin/env bash

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

# Set working directory
cd ${SCRIPT_DIR}

export PRISM_AGENT_VERSION=$(cd ../../prism-agent/service && sbt "project server" -Dsbt.supershell=false -error "print version")
echo "prism-agent server version: ${PRISM_AGENT_VERSION}"

export MERCURY_MEDIATOR_VERSION=$(cd ../../mercury/mercury-mediator && sbt "project mediator" -Dsbt.supershell=false -error "print version")
echo "mercury-mediator version: ${MERCURY_MEDIATOR_VERSION}"

export IRIS_SERVICE_VERSION=$(cd ../../iris/service && sbt "project server" -Dsbt.supershell=false -error "print version")
echo "iris server version: ${IRIS_SERVICE_VERSION}"
