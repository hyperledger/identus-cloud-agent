#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

ENV_FILE="${ENV_FILE:=${SCRIPT_DIR}/.env}"
PORT="${PORT:=9500}"

echo "ENV_FILE               = ${ENV_FILE}"

echo "--------------------------------------"
echo "Starting stack using docker compose"
echo "--------------------------------------"

PORT=${PORT} docker compose -f "${SCRIPT_DIR}"/docker-compose.yml \
	--env-file "${ENV_FILE}" up -d --wait

export AGENT_AUTH_REQUIRED=true
export ACME_AGENT_URL=http://localhost:${PORT}/issuer/cloud-agent
export ACME_AUTH_KEY=default
export BOB_AGENT_URL=http://localhost:${PORT}/verifier/cloud-agent
export BOB_AUTH_KEY=default
export MALLORY_AGENT_URL=http://localhost:${PORT}/holder/cloud-agent
export MALLORY_AUTH_KEY=default
export FABER_AGENT_URL=http://localhost:${PORT}/holder/cloud-agent
export FABER_AUTH_KEY=default

(
	cd "${SCRIPT_DIR}"/../../tests/e2e-tests/
	./gradlew test reports
)
