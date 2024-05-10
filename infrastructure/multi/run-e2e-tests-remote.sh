#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

echo "--------------------------------------"
echo "Run e2e tests"
echo "--------------------------------------"

export ACME_AGENT_URL=https://agent-df56h.atalaprism.io/cloud-agent
export BOB_AGENT_URL=https://agent-kj46b.atalaprism.io/cloud-agent
export MALLORY_AGENT_URL=https://agent-sd98k.atalaprism.io/cloud-agent

(
	cd "${SCRIPT_DIR}"/../../tests/e2e-tests/
	AGENT_AUTH_REQUIRED=true ./gradlew test reports
)
