#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

echo "--------------------------------------"
echo "Starting issuer using local/run.sh"
echo "--------------------------------------"

"${SCRIPT_DIR}"/../local/run.sh -p 8080 -n issuer -w

echo "--------------------------------------"
echo "Starting holder using local/run.sh"
echo "--------------------------------------"

"${SCRIPT_DIR}"/../local/run.sh -p 8090 -n holder -w

echo "--------------------------------------"
echo "Starting verifier using local/run.sh"
echo "--------------------------------------"

"${SCRIPT_DIR}"/../local/run.sh -p 8100 -n verifier -w

echo "--------------------------------------"
echo "Run e2e tests"
echo "--------------------------------------"

(
	cd "${SCRIPT_DIR}"/../../tests/integration-tests/
	./gradlew test reports
)
