#!/bin/bash

set -e

# Variables
ENV_FILE=".env"
PERF_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
AGENT_DIR="$PERF_DIR/../../.."
DOCKERFILE_NODE="$AGENT_DIR/infrastructure/shared/docker-compose-node.yml"
DOCKERFILE_AGENT="$AGENT_DIR/infrastructure/shared/docker-compose-agent.yml"
K6_URL="https://github.com/grafana/k6/releases/download/v0.45.0/k6-v0.45.0-macos-arm64.zip"
K6_ZIP_FILE="$(basename ${K6_URL})"

# Functions
function startAgent() {
	echo "Starting [$NAME] agent"
	PORT="${PORT}" \
		ADMIN_TOKEN="${ADMIN_TOKEN}" \
		DEFAULT_WALLET_ENABLED="${DEFAULT_WALLET_ENABLED}" \
		DEFAULT_WALLET_AUTH_API_KEY="${DEFAULT_WALLET_AUTH_API_KEY}" \
		API_KEY_AUTO_PROVISIONING="${API_KEY_AUTO_PROVISIONING}" \
		API_KEY_ENABLED="${API_KEY_ENABLED}" \
		DOCKERHOST="${DOCKERHOST}" \
		PG_PORT="${PG_PORT}" \
		NODE_REFRESH_AND_SUBMIT_PERIOD="${NODE_REFRESH_AND_SUBMIT_PERIOD}" \
		NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD="${NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD}" \
		NODE_WALLET_MAX_TPS="${NODE_WALLET_MAX_TPS}" \
		docker compose -p "${NAME}" -f "${DOCKERFILE_AGENT}" \
		--env-file "${ENV_FILE}" up -d --wait 2>/dev/null
	echo "Agent [$NAME] healthy"
}

function startPrismNode() {
	echo "Starting [$NAME]"
		NODE_REFRESH_AND_SUBMIT_PERIOD="${NODE_REFRESH_AND_SUBMIT_PERIOD}" \
		NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD="${NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD}" \
		NODE_WALLET_MAX_TPS="${NODE_WALLET_MAX_TPS}" \
		docker compose -p "${NAME}" -f "${DOCKERFILE_NODE}" \
		--env-file "${ENV_FILE}" up -d --wait 2>/dev/null
	echo "[$NAME] healthy"
}

function stopAgent() {
	echo "Stopping [${NAME}] agent"
	PORT="${PORT}" \
		DOCKERHOST="${DOCKERHOST}" \
		docker compose \
		-p "${NAME}" \
		-f "${DOCKERFILE}" \
		--env-file "${ENV_FILE}" down -v 2>/dev/null
	echo "Agent [${NAME}] stopped"
}

function stopPrismNode() {
	echo "Stopping [${NAME}] agent"
		docker compose \
		-p "${NAME}" \
		-f "${DOCKERFILE}" \
		--env-file "${ENV_FILE}" down -v 2>/dev/null
	echo "Agent [${NAME}] stopped"
}

function createPrismNode1() {
    local NAME="prism-node"
    local PG_PORT=5432
    local NODE_REFRESH_AND_SUBMIT_PERIOD="1s"
    local NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD="1s"
    local NODE_WALLET_MAX_TPS="1000"

    echo "Creating PrismNode with Name: $NAME"
    docker network create agent-network || echo "Network agent-network already exists"
    echo "Starting Docker compose for PrismNode"
    NODE_REFRESH_AND_SUBMIT_PERIOD="${NODE_REFRESH_AND_SUBMIT_PERIOD}" \
    NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD="${NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD}" \
    NODE_WALLET_MAX_TPS="${NODE_WALLET_MAX_TPS}" \
    docker compose -p "${NAME}" -f "${DOCKERFILE_NODE}" \
    --env-file "${ENV_FILE}" up -d --wait 2>/dev/null || {
        echo "Failed to start PrismNode Docker containers"
        exit 1
    }

    echo "PrismNode [$NAME] is now healthy"
}

function createPrismNode() {
	local NAME="prism-node"
	local PG_PORT=5432
	local NODE_REFRESH_AND_SUBMIT_PERIOD="1s"
	local NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD="1s"
	local NODE_WALLET_MAX_TPS="1000"
	startPrismNode
}

function createIssuer() {
	local NAME="issuer"
	local PORT=8080
	local ADMIN_TOKEN=admin
	local DEFAULT_WALLET_ENABLED=true
	local DEFAULT_WALLET_AUTH_API_KEY=default
	local API_KEY_AUTO_PROVISIONING=false
	local API_KEY_ENABLED=true
	local DOCKERHOST="host.docker.internal"
	local PG_PORT=5432
	local NODE_REFRESH_AND_SUBMIT_PERIOD="1s"
	local NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD="1s"
	local NODE_WALLET_MAX_TPS="1000"

	startAgent
}

function createHolder() {
	local NAME="holder"
	local PORT=8200
	local ADMIN_TOKEN=admin
	local DEFAULT_WALLET_ENABLED=true
	local DEFAULT_WALLET_AUTH_API_KEY=default
	local API_KEY_AUTO_PROVISIONING=false
	local API_KEY_ENABLED=true
	local DOCKERHOST="host.docker.internal"
	local PG_PORT=5432
	local NODE_REFRESH_AND_SUBMIT_PERIOD="1s"
	local NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD="1s"
	local NODE_WALLET_MAX_TPS="1000"

	startAgent
}

function createVerifier() {
	local NAME="verifier"
	local PORT=8100
	local ADMIN_TOKEN=admin
	local DEFAULT_WALLET_ENABLED=true
	local DEFAULT_WALLET_AUTH_API_KEY=default
	local API_KEY_AUTO_PROVISIONING=false
	local API_KEY_ENABLED=true
	local DOCKERHOST="host.docker.internal"
	local PG_PORT=5432
	local NODE_REFRESH_AND_SUBMIT_PERIOD="1s"
	local NODE_MOVE_SCHEDULED_TO_PENDING_PERIOD="1s"
	local NODE_WALLET_MAX_TPS="1000"

	startAgent
}

function removeIssuer() {
	local NAME="issuer"
	local PORT=8080
	local DOCKERHOST="host.docker.internal"

	stopAgent
}

function removeVerifier() {
	local NAME="verifier"
	local PORT=8100
	local DOCKERHOST="host.docker.internal"

	stopAgent
}

function removeHolder() {
	local NAME="holder"
	local PORT=8200
	local DOCKERHOST="host.docker.internal"

	stopAgent
}

function removePrismNode() {
	local NAME="prism-node"
	stopPrismNode
}

# clean up on finish
function cleanup() {
  local exit_code=$?
  if [[ $exit_code -eq 0 ]]; then
      echo "Script exited normally with code $exit_code."
  else
      echo "Script exited with error code $exit_code."
  fi
	echo "Removing K6 binaries"
	rm k6
	rm "$K6_ZIP_FILE"

  removePrismNode &
	removeIssuer &
	removeVerifier &
	removeHolder &
	wait
  # Ensure no containers are left connected to the network
  echo "Forcefully stopping all containers on agent-network"
  docker ps -q --filter network=agent-network | xargs -r docker stop || echo "No containers to stop"
  docker ps -a -q --filter network=agent-network | xargs -r docker rm || echo "No containers to remove"

  echo "Removing agent-network"
  docker network rm agent-network || echo "Failed to remove agent-network"
}

trap 'cleanup' EXIT

# download and unzip k6
echo "Downloading K6"
curl -LO -s "${K6_URL}"
unzip -j "${K6_ZIP_FILE}" >/dev/null
echo "K6 downloaded"

## navigate to main project
cd "$AGENT_DIR"

##sbt docker:publishLocal
AGENT_VERSION=$(cut -d '"' -f 2 version.sbt)

## back to performance folder
cd "$PERF_DIR"

# set version to env file
sed -i.bak "s/AGENT_VERSION=.*/AGENT_VERSION=${AGENT_VERSION}/" "${ENV_FILE}" && rm -f "${ENV_FILE}.bak"

# create docker  agent-network
docker network create agent-network
# create Prism Node
createPrismNode

createIssuer
createHolder
createVerifier

# yarn install
echo "Installing dependencies"
yarn -s >/dev/null
echo "Building performance tests"
yarn webpack >/dev/null

# start perf test
echo "Starting performance testing"

export ISSUER_AGENT_API_KEY=default
export HOLDER_AGENT_API_KEY=default
export VERIFIER_AGENT_API_KEY=default

./k6 run -e SCENARIO_LABEL=create-prism-did-smoke ./dist/create-prism-did-test.js
./k6 run -e SCENARIO_LABEL=credential-offer-smoke ./dist/credential-offer-test.js
./k6 run -e SCENARIO_LABEL=credential-definition-smoke ./dist/credential-definition-test.js
./k6 run -e SCENARIO_LABEL=credential-schema-smoke ./dist/credential-schema-test.js
./k6 run -e SCENARIO_LABEL=did-publishing-smoke ./dist/did-publishing-test.js
./k6 run -e SCENARIO_LABEL=connection-flow-smoke ./dist/connection-flow-test.js
./k6 run -e SCENARIO_LABEL=issuance-flow-smoke ./dist/issuance-flow-test.js
./k6 run -e SCENARIO_LABEL=present-proof-flow-smoke ./dist/present-proof-flow-test.js

