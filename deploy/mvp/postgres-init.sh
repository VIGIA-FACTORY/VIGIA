#!/bin/sh
# One database and one user per owner (ADR-003, D-54). Runs once, on the first start of an empty volume.
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<SQL
CREATE USER identity WITH PASSWORD '${IDENTITY_DB_PASSWORD}';
CREATE DATABASE identity OWNER identity;
CREATE USER community WITH PASSWORD '${COMMUNITY_DB_PASSWORD}';
CREATE DATABASE community OWNER community;
CREATE USER keycloak WITH PASSWORD '${KEYCLOAK_DB_PASSWORD}';
CREATE DATABASE keycloak OWNER keycloak;
CREATE USER n8n WITH PASSWORD '${N8N_DB_PASSWORD}';
CREATE DATABASE n8n OWNER n8n;
REVOKE ALL ON DATABASE identity, community, keycloak, n8n FROM PUBLIC;
SQL
