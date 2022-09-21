#!/usr/bin/env bash

echo "--------------------------------------"
echo "Building docker images"
echo "--------------------------------------"

cd castor/service; sbt docker:publishLocal; cd -
cd mercury/prism-mediator; sbt "project mediator; docker:publishLocal"; cd -

echo "--------------------------------------"
echo "Bringing up stack using docker-compose"
echo "--------------------------------------"

docker-compose -f infrastructure/local/docker-compose.yml up

