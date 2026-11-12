#!/usr/bin/env bash
# BloodLink Desktop Client Launcher (Linux / macOS / Ubuntu)
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$SCRIPT_DIR/bin"

echo "========================================================"
echo "  BloodLink Desktop Client Launcher (Linux / Ubuntu)    "
echo "========================================================"

echo "Compiling BloodLink Desktop Client..."
find "$SCRIPT_DIR/src/com/bloodlink/client" -name "*.java" > /tmp/bloodlink_frontend_sources.txt
javac -encoding UTF-8 -d "$SCRIPT_DIR/bin" @/tmp/bloodlink_frontend_sources.txt

echo "Launching BloodLink Desktop Application..."
java -cp "$SCRIPT_DIR/bin" com.bloodlink.client.Main "$@"
