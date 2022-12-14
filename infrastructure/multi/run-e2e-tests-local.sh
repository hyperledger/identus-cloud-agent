#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

echo "--------------------------------------"
echo "Starting issuer using local/run.sh"
echo "--------------------------------------"

${SCRIPT_DIR}/../local/run.sh -p 8080 -n issuer -b

echo "--------------------------------------"
echo "Starting holder using local/run.sh"
echo "--------------------------------------"

${SCRIPT_DIR}/../local/run.sh -p 8090 -n holder -b

echo "--------------------------------------"
echo "Starting verifier using local/run.sh"
echo "--------------------------------------"

${SCRIPT_DIR}/../local/run.sh -p 8100 -n verifier -b

echo "--------------------------------------"
echo "Run e2e tests"
echo "--------------------------------------"

(cd ${SCRIPT_DIR}/../../tests/e2e-tests/; ./gradlew test reports)
