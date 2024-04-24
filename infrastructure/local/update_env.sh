#!/usr/bin/env bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
ENV_FILE="${SCRIPT_DIR}/.env"

pip install ${SCRIPT_DIR}/../utils/python/github-helpers > /dev/null 2>&1

AGENT_VERSION=$(github get-latest-package-version --package prism-agent --package-type container)

sed -i.bak "s/AGENT_VERSION=.*/AGENT_VERSION=${AGENT_VERSION}/" ${ENV_FILE} && rm -f ${ENV_FILE}.bak
