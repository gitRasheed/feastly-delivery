# Feastly Delivery

A food delivery platform built with **Kotlin**, **Spring Boot 3**, and **microservices architecture**. Demonstrates event-driven design, saga orchestration, and modern observability patterns.

## Tech Stack

- **Language**: Kotlin 2.0, Java 21
- **Framework**: Spring Boot 3.4
- **Messaging**: Apache Kafka (KRaft mode)
- **Database**: PostgreSQL 15

- **Observability**: Prometheus, Grafana, Loki, Tempo
- **Build**: Gradle with Kotlin DSL


## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local development)

### 1. Clone and configure

```bash
cp infra/.env.example infra/.env
```

### 2. Start all services

```bash
docker compose -f infra/docker-compose.yml up --build
```

### 3. Run locally (development)

```bash
docker compose -f infra/docker-compose.yml up postgres kafka redis -d

./gradlew :order-service:bootRun
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| order-service | 8080 | Order management, saga orchestration |
| dispatch-service | 8081 | Driver assignment, delivery tracking |
| driver-tracking-service | 8082 | Real-time driver location |
| user-service | 8083 | User profiles, addresses |
| restaurant-service | 8084 | Restaurant & menu management |

## Infrastructure

| Component | Port | URL |
|-----------|------|-----|
| PostgreSQL | 5432 | `localhost:5432` |

| Kafka | 9092 | `localhost:9092` |
| Prometheus | 9090 | http://localhost:9090 |
| Grafana | 3000 | http://localhost:3000 |
| Loki | 3100 | http://localhost:3100 |
| Tempo | 4317/3200 | OTLP: `localhost:4317` |

## API Documentation

Each service exposes OpenAPI documentation:

- Order Service: http://localhost:8080/swagger-ui.html
- Dispatch Service: http://localhost:8081/swagger-ui.html
- Driver Tracking: http://localhost:8082/swagger-ui.html

## Build & Test

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Run with test reports
./gradlew build --scan
```
