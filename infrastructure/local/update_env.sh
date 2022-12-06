#!/usr/bin/env bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
ENV_FILE="${SCRIPT_DIR}/.env"

pip install ${SCRIPT_DIR}/../utils/python/github-helpers > /dev/null 2>&1

MERCURY_MEDIATOR_VERSION=$(github get-latest-package-version --package mercury-mediator --package-type container)
IRIS_SERVICE_VERSION=$(github get-latest-package-version --package iris-service --package-type container)
PRISM_AGENT_VERSION=$(github get-latest-package-version --package prism-agent --package-type container)

sed -i.bak "s/MERCURY_MEDIATOR_VERSION=.*/MERCURY_MEDIATOR_VERSION=${MERCURY_MEDIATOR_VERSION}/" ${ENV_FILE} && rm -f ${ENV_FILE}.bak
sed -i.bak "s/IRIS_SERVICE_VERSION=.*/IRIS_SERVICE_VERSION=${IRIS_SERVICE_VERSION}/" ${ENV_FILE} && rm -f ${ENV_FILE}.bak
sed -i.bak "s/PRISM_AGENT_VERSION=.*/PRISM_AGENT_VERSION=${PRISM_AGENT_VERSION}/" ${ENV_FILE} && rm -f ${ENV_FILE}.bak
