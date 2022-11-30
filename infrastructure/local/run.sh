#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Set working directory
cd ${SCRIPT_DIR}

echo "--------------------------------------"
echo "Bringing up stack using docker-compose"
echo "--------------------------------------"

docker-compose -p local -f ../shared/docker-compose.yml --env-file ${SCRIPT_DIR}/.env up
