#!/usr/bin/env bash

set -e

echo "--------------------------------------"
echo "Bringing up stack using docker-compose"
echo "--------------------------------------"

docker-compose -f infrastructure/local/docker-compose.yml up

