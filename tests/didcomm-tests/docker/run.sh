#!/usr/bin/env bash

set -e

CLOUD_AGENT_VERSION=$1

PORT="$PRISM_PORT" \
	CLOUD_AGENT_VERSION="$CLOUD_AGENT_VERSION" \
	docker compose \
	-f "docker/docker-compose.yml" \
	up -d --wait
