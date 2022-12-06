#!/usr/bin/env bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
ENV_FILE="${SCRIPT_DIR}/.env"

pip install ${SCRIPT_DIR}/../utils/python/github-helpers

MERCURY_MEDIATOR_VERSION=$(github get-latest-package-version --package mercury-mediator --package-type container)
IRIS_SERVICE_VERSION=$(github get-latest-package-version --package iris-service --package-type container)
PRISM_AGENT_VERSION=$(github get-latest-package-version --package prism-agent --package-type container)

ls -la ${ENV_FILE}

echo MERCURY_MEDIATOR_VERSION=${MERCURY_MEDIATOR_VERSION}
echo IRIS_SERVICE_VERSION=${IRIS_SERVICE_VERSION}
echo PRISM_AGENT_VERSION=${PRISM_AGENT_VERSION}

sed -i '' -e "s/MERCURY_MEDIATOR_VERSION=.*/MERCURY_MEDIATOR_VERSION=${MERCURY_MEDIATOR_VERSION}/" ${ENV_FILE}
sed -i '' -e "s/IRIS_SERVICE_VERSION=.*/IRIS_SERVICE_VERSION=${IRIS_SERVICE_VERSION}/" ${ENV_FILE}
sed -i '' -e "s/PRISM_AGENT_VERSION=.*/PRISM_AGENT_VERSION=${PRISM_AGENT_VERSION}/" ${ENV_FILE}
