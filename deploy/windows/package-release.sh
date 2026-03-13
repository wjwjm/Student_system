#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
DIST_DIR="$ROOT_DIR/deploy/windows/dist"
PKG_DIR="$DIST_DIR/student-system-windows"

rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR/sql"

cp "$ROOT_DIR/backend/target/student-system-1.0.0.war" "$PKG_DIR/student-system-1.0.0.war"
cp "$ROOT_DIR/sql/schema.sql" "$PKG_DIR/sql/schema.sql"
cp "$ROOT_DIR/deploy/windows/docker-compose.yml" "$PKG_DIR/docker-compose.yml"
cp "$ROOT_DIR/deploy/windows/start.bat" "$PKG_DIR/start.bat"
cp "$ROOT_DIR/deploy/windows/stop.bat" "$PKG_DIR/stop.bat"
cp "$ROOT_DIR/deploy/windows/快速部署说明.md" "$PKG_DIR/快速部署说明.md"

(cd "$DIST_DIR" && zip -rq student-system-windows.zip student-system-windows)

echo "[INFO] Package created: $DIST_DIR/student-system-windows.zip"
