#!/usr/bin/env bash
# BloodLink Backend TCP Server Launcher (Linux / macOS / Ubuntu)
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$SCRIPT_DIR/bin"

echo "========================================================"
echo "  BloodLink Backend TCP Server Launcher (Linux / Ubuntu)"
echo "========================================================"

echo "Compiling BloodLink Server and Database..."
find "$SCRIPT_DIR/src" -name "*.java" > /tmp/bloodlink_server_sources.txt
javac -encoding UTF-8 -cp "$SCRIPT_DIR/lib/sqlite-jdbc-3.51.3.0.jar" -d "$SCRIPT_DIR/bin" @/tmp/bloodlink_server_sources.txt

# Default admin seeding (enabled by default for convenience)
export BLOODLINK_SEED_ADMIN="${BLOODLINK_SEED_ADMIN:-true}"
export BLOODLINK_ADMIN_USER="${BLOODLINK_ADMIN_USER:-admin@bloodlink.com}"
export BLOODLINK_ADMIN_PASS="${BLOODLINK_ADMIN_PASS:-Admin@BloodLink2026}"

echo "Starting BloodLink TCP Server on port 9090..."
java -cp "$SCRIPT_DIR/bin:$SCRIPT_DIR/lib/sqlite-jdbc-3.51.3.0.jar" com.bloodlink.server.ServerMain "$@"

