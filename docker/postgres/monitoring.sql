-- Monitoring user for postgres-exporter.
-- This file runs only when the PostgreSQL Docker volume is initialized.

CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'prometheus') THEN
        CREATE USER prometheus WITH PASSWORD 'prometheus';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE ecommerce TO prometheus;
GRANT pg_monitor TO prometheus;
