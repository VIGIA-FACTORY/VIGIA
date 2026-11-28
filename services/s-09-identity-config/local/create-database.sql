-- Local only: one logical database and user per service, no shared schema (ADR-003, C8).
-- Runs once, when the PostgreSQL volume is created (docker compose down -v to recreate it).
CREATE USER vigia_identity_config WITH PASSWORD 'vigia_identity_config';
CREATE DATABASE vigia_identity_config OWNER vigia_identity_config;
