#!/usr/bin/env bash

set -e

CLOUD_AGENT_VERSION="$CLOUD_AGENT_VERSION" \
	docker compose \
	-f "./docker/docker-compose.yml" \
	down -v
