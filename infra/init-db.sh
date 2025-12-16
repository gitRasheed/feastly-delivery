#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE feastly;
    CREATE DATABASE dispatch;
    CREATE DATABASE driver_tracking;
    CREATE DATABASE users;
    CREATE DATABASE restaurants;
    GRANT ALL PRIVILEGES ON DATABASE feastly TO admin;
    GRANT ALL PRIVILEGES ON DATABASE dispatch TO admin;
    GRANT ALL PRIVILEGES ON DATABASE driver_tracking TO admin;
    GRANT ALL PRIVILEGES ON DATABASE users TO admin;
    GRANT ALL PRIVILEGES ON DATABASE restaurants TO admin;
EOSQL
