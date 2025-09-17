# ---- Build stage ----
FROM gradle:8.10.0-jdk21 AS builder
WORKDIR /app
COPY .. .
RUN gradle clean bootJar --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
