@echo off
setlocal

cd /d %~dp0

echo [INFO] Starting student-system stack (MySQL + Tomcat)...
docker compose -f docker-compose.yml up -d
if errorlevel 1 (
  echo [ERROR] Failed to start containers.
  exit /b 1
)

echo [INFO] Done.
echo [INFO] Visit: http://localhost:8080/student/
echo [INFO] Default account: admin / admin123
