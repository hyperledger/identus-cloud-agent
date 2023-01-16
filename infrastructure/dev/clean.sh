#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

# Set working directory
cd ${SCRIPT_DIR}
cd ../../

echo "--------------------------------------"
echo "Cleaning libraries"
echo "--------------------------------------"

cd shared
sbt "clean;cleanFiles"
cd -
cd iris/client/scala-client
sbt "clean;cleanFiles"
cd -
cd castor/lib
sbt "clean;cleanFiles"
cd -
cd pollux/vc-jwt
sbt "clean;cleanFiles"
cd -
cd pollux/lib
sbt "clean;cleanFiles"
cd -
cd mercury/mercury-library
sbt "clean;cleanFiles"
cd -
