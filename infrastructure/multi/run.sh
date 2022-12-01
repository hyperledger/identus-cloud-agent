#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Set working directory
cd ${SCRIPT_DIR}

echo "--------------------------------------"
echo "Bringing up stack using docker-compose"
echo "--------------------------------------"

../local/run.sh -p 8080 -n issuer -b
../local/run.sh -p 8090 -n holder -b
../local/run.sh -p 9000 -n verifier -b
