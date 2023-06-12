#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

# Set working directory
cd ${SCRIPT_DIR}
cd ../../
sbt "clean;cleanFiles"
