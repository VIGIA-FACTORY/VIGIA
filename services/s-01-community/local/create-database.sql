-- Local only: one logical database and user per service, no shared schema (ADR-003, C8).
-- Runs once, when the PostgreSQL volume is created (docker compose down -v to recreate it).
CREATE USER vigia_community WITH PASSWORD 'vigia_community';
CREATE DATABASE vigia_community OWNER vigia_community;
