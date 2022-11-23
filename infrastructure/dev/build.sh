#!/usr/bin/env bash

set -e

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Set working directory
cd ${SCRIPT_DIR}
cd ../../

echo "--------------------------------------"
echo "Publishing libraries"
echo "--------------------------------------"

cd shared;sbt "clean;publishLocal";cd -
cd iris/client/scala-client;sbt "clean;publishLocal";cd -
cd castor/lib;sbt "clean;publishLocal";cd -
cd pollux/lib;sbt "clean;publishLocal";cd -
cd mercury/mercury-library;sbt "clean;publishLocal";cd -

echo "--------------------------------------"
echo "Building docker images"
echo "--------------------------------------"

cd mercury/mercury-mediator && sbt "project mediator; docker:publishLocal" && cd -
cd prism-agent/service && sbt docker:publishLocal && cd -
cd iris/service && sbt docker:publishLocal && cd -

cd ${SCRIPT_DIR}
