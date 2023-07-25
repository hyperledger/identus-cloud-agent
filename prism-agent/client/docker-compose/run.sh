#!/usr/bin/env bash

PRISM_AGENT_VERSION=${VERSION_TAG:13}

PORT="${PRISM_AGENT_PORT:-8080}" \
PRISM_AGENT_VERSION="$PRISM_AGENT_VERSION" \
docker compose \
  -f "docker-compose.yml" \
  up -d --wait
