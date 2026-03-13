@echo off
setlocal

cd /d %~dp0

echo [INFO] Stopping student-system stack...
docker compose -f docker-compose.yml down
if errorlevel 1 (
  echo [ERROR] Failed to stop containers.
  exit /b 1
)

echo [INFO] Done.
