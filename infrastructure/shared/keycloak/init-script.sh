#!/usr/bin/env bash

set -e
set -u

KEYCLOAK_BASE_URL=$KEYCLOAK_BASE_URL
KEYCLOAK_ADMIN_USER=$KEYCLOAK_ADMIN_USER
KEYCLOAK_ADMIN_PASSWORD=$KEYCLOAK_ADMIN_PASSWORD
REALM_NAME=$REALM_NAME
PRISM_AGENT_CLIENT_SECRET=$PRISM_AGENT_CLIENT_SECRET

function get_admin_token() {
	local response=$(
		curl --request POST "$KEYCLOAK_BASE_URL/realms/master/protocol/openid-connect/token" \
			--fail -s \
			-d "grant_type=password" \
			-d "client_id=admin-cli" \
			-d "username=$KEYCLOAK_ADMIN_USER" \
			-d "password=$KEYCLOAK_ADMIN_PASSWORD"
	)
	local access_token=$(echo $response | jq -r '.access_token')
	echo $access_token
}

function create_realm() {
	local access_token=$1

	curl --request POST "$KEYCLOAK_BASE_URL/admin/realms" \
		--fail -s \
		-H "Authorization: Bearer $access_token" \
		-H "Content-Type: application/json" \
		--data-raw "{
			\"realm\": \"$REALM_NAME\",
			\"enabled\": true
		}"
}

function create_prism_agent_client() {
	local access_token=$1

	curl --request POST "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/clients" \
		--fail -s \
		-H "Authorization: Bearer $access_token" \
		-H "Content-Type: application/json" \
		--data-raw "{
			\"id\": \"prism-agent\",
			\"directAccessGrantsEnabled\": true,
			\"authorizationServicesEnabled\": true,
			\"serviceAccountsEnabled\": true,
			\"secret\": \"$PRISM_AGENT_CLIENT_SECRET\"
		}"
}

echo "Getting admin access token ..."
ADMIN_ACCESS_TOKEN=$(get_admin_token)

echo "Creating a new test realm ..."
create_realm $ADMIN_ACCESS_TOKEN

echo "Creating a new prism-agent client ..."
create_prism_agent_client $ADMIN_ACCESS_TOKEN
