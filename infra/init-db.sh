#!/bin/bash
set -e

# Create multiple databases for microservices
# Note: POSTGRES_DB defaults to POSTGRES_USER (admin) if not set
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE feastly;
    CREATE DATABASE dispatch;
    CREATE DATABASE driver_tracking;
    GRANT ALL PRIVILEGES ON DATABASE feastly TO admin;
    GRANT ALL PRIVILEGES ON DATABASE dispatch TO admin;
    GRANT ALL PRIVILEGES ON DATABASE driver_tracking TO admin;
EOSQL
