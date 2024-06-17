#!/bin/bash

set -e
set -u

function create_user_and_database() {
	local database=$1
	local app_user=${database}-application-user
	echo "Creating user and database '$database'"
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        DO \$\$
        BEGIN
            IF NOT EXISTS (
                SELECT FROM pg_catalog.pg_roles
                WHERE rolname = '$app_user') THEN
                CREATE USER "$app_user" WITH PASSWORD 'password';
            END IF;
        END
        \$\$;

        DO \$\$
        BEGIN
            IF NOT EXISTS (
                SELECT FROM pg_database
                WHERE datname = '$database') THEN
                CREATE DATABASE $database;
            END IF;
        END
        \$\$;

        \c $database

        DO \$\$
        BEGIN
            IF NOT EXISTS (
                SELECT FROM pg_catalog.pg_roles
                WHERE rolname = '$app_user') THEN
                ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "$app_user";
            END IF;
        END
        \$\$;
	EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
	echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
	for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
		create_user_and_database $db
	done
	echo "Multiple databases created"
fi
