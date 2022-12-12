#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Set working directory
cd ${SCRIPT_DIR}

echo "--------------------------------------"
echo "Starting issuer using local/run.sh"
echo "--------------------------------------"

../local/run.sh -p 8080 -n issuer -b

echo "--------------------------------------"
echo "Starting holder using local/run.sh"
echo "--------------------------------------"

../local/run.sh -p 8090 -n holder -b

echo "--------------------------------------"
echo "Starting verifier using local/run.sh"
echo "--------------------------------------"

../local/run.sh -p 9000 -n verifier -b
