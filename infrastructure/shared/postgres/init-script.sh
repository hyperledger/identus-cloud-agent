#!/bin/bash

set -e
set -u

function create_user_and_database() {
	local database=$1
	local app_user=${database}-application-user
	echo "  Creating user and database '$database'"

	# Check if user exists
	user_exists=$(psql -U "$POSTGRES_USER" -tAc "SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = '$app_user'")
	if [ "$user_exists" != "1" ]; then
		psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
			      CREATE USER "$app_user" WITH PASSWORD 'password';
		EOSQL
	else
		echo "  User '$app_user' already exists, skipping creation."
	fi

	# Check if database exists
	db_exists=$(psql -U "$POSTGRES_USER" -tAc "SELECT 1 FROM pg_database WHERE datname = '$database'")
	if [ "$db_exists" != "1" ]; then
		psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
			        CREATE DATABASE $database;
		EOSQL
	else
		echo "  Database '$database' already exists, skipping creation."
	fi

	# Grant privileges
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
		      \c $database
		      ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "$app_user";
	EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
	echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
	for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
		create_user_and_database $db
	done
	echo "Multiple databases created"
fi
