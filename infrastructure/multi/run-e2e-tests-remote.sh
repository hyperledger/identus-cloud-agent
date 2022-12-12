#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

echo "--------------------------------------"
echo "Run e2e tests"
echo "--------------------------------------"

export ACME_AGENT_URL=https://agent-df56h.atalaprism.io/prism-agent
export BOB_AGENT_URL=https://agent-kj46b.atalaprism.io/prism-agent
export MALLORY_AGENT_URL=https://agent-sd98k.ataaprism.io/prism-agent

(cd ${SCRIPT_DIR}/../../tests/e2e-tests/; AGENT_AUTH_REQUIRED=true gradle test --info)
