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

export ISSUER_AGENT_URL=http://localhost:${PORT}/issuer/cloud-agent
export ISSUER_AGENT_API_KEY=default
export HOLDER_AGENT_URL=http://localhost:${PORT}/holder/cloud-agent
export HOLDER_AGENT_API_KEY=default
export VERIFIER_AGENT_URL=http://localhost:${PORT}/verifier/cloud-agent
export VERIFIER_AGENT_API_KEY=default

echo "--------------------------------------"
echo "Run perf tests"
echo "--------------------------------------"

(
	export K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write
	export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true
	cd "${SCRIPT_DIR}"/../../tests/performance-tests/agent-performance-tests-k6
	yarn install
	yarn webpack
	k6 run -e SCENARIO_LABEL=st-create-prism-did-smoke dist/create-prism-did-test.js -o experimental-prometheus-rw
	k6 run -e SCENARIO_LABEL=st-credential-offer-smoke dist/credential-offer-test.js -o experimental-prometheus-rw
	k6 run -e SCENARIO_LABEL=st-credential-definition-smoke dist/credential-definition-test.js -o experimental-prometheus-rw
	k6 run -e SCENARIO_LABEL=st-credential-schema-smoke dist/credential-schema-test.js -o experimental-prometheus-rw
	k6 run -e SCENARIO_LABEL=st-did-publishing-smoke dist/did-publishing-test.js -o experimental-prometheus-rw
	k6 run -e SCENARIO_LABEL=st-connection-flow-smoke dist/connection-flow-test.js -o experimental-prometheus-rw
	k6 run -e SCENARIO_LABEL=st-issuance-flow-smoke dist/issuance-flow-test.js -o experimental-prometheus-rw
	k6 run -e SCENARIO_LABEL=st-present-proof-flow-smoke dist/present-proof-flow-test.js -o experimental-prometheus-rw
)
