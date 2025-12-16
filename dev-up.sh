#!/bin/bash
# Dev startup script - builds jars then starts Docker Compose
set -e

echo "🔨 Building JARs..."
./gradlew bootJar --quiet

echo "🐳 Starting Docker Compose..."
cd infra
docker compose up --build "$@"
