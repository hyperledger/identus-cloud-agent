#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

echo "--------------------------------------"
echo "Starting multitenant using local/run.sh"
echo "--------------------------------------"

${SCRIPT_DIR}/run.sh -p 8080 -n multitenant -w

export AGENT_AUTH_REQUIRED=true
export AGENT_AUTH_HEADER=api-key

export ACME_AUTH_KEY=acme
export ACME_AGENT_URL=http://localhost:8080/prism-agent

export BOB_AUTH_KEY=bob
export BOB_AGENT_URL=http://localhost:8080/prism-agent

export FABER_AUTH_KEY=faber
export FABER_AGENT_URL=http://localhost:8080/prism-agent

curl --location 'http://localhost:8080/prism-agent/events/webhooks' \
	--header "api-key: $ACME_AUTH_KEY" \
	--header 'Content-Type: application/json' \
	--header 'Accept: application/json' \
	--data '{
    "url": "http://host.docker.internal:9955"
  }'

curl --location 'http://localhost:8080/prism-agent/events/webhooks' \
	--header "api-key: $BOB_AUTH_KEY" \
	--header 'Content-Type: application/json' \
	--header 'Accept: application/json' \
	--data '{
    "url": "http://host.docker.internal:9956"
  }'

curl --location 'http://localhost:8080/prism-agent/events/webhooks' \
	--header "api-key: $FABER_AUTH_KEY" \
	--header 'Content-Type: application/json' \
	--header 'Accept: application/json' \
	--data '{
    "url": "http://host.docker.internal:9957"
  }'

# (
# 	cd ${SCRIPT_DIR}/../../tests/e2e-tests/
# 	./gradlew test reports
# )
