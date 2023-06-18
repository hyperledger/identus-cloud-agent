#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

REPO_HOME="${SCRIPT_DIR}/../.."

echo "--------------------------------------"
echo "Building service docker images"
echo "--------------------------------------"

cd ${REPO_HOME}
sbt "clean; docker:publishLocal"
