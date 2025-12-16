@echo off
REM Dev startup script - builds jars then starts Docker Compose

echo Building JARs...
call .\gradlew.bat bootJar --quiet
if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

echo Starting Docker Compose...
cd infra
docker compose up --build %*
