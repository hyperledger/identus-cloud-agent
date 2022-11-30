#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

REPO_HOME="${SCRIPT_DIR}/../.."

LIBS="shared iris/client/scala-client castor/lib pollux/lib mercury/mercury-library"
SERVICES="mercury/mercury-mediator prism-agent/service iris/service"

echo "--------------------------------------"
echo "Publishing libraries"
echo "--------------------------------------"

for LIB in ${LIBS}; do
  (cd ${REPO_HOME}/${LIB}; sbt "clean;publishLocal")
done

echo "--------------------------------------"
echo "Building service docker images"
echo "--------------------------------------"

for SERVICE in ${SERVICES}; do
  (cd ${REPO_HOME}/${SERVICE}; sbt docker:publishLocal)
done
