#!/usr/bin/env bash

set -e

echo "--------------------------------------"
echo "Publishing libraries"
echo "--------------------------------------"

cd shared && sbt publishLocal && cd -
cd iris/client/scala-client && sbt publishLocal && cd -
cd castor/lib && sbt publishLocal && cd -

echo "--------------------------------------"
echo "Building docker images"
echo "--------------------------------------"

cd mercury/prism-mediator && sbt "project mediator; docker:publishLocal" && cd -
cd pollux/service && sbt docker:publishLocal && cd -
cd prism-agent/service && sbt docker:publishLocal && cd -

echo "--------------------------------------"
echo "Bringing up stack using docker-compose"
echo "--------------------------------------"

docker-compose -f infrastructure/local/docker-compose.yml up

