#!/bin/bash
# ======================================================
# Full build + run script using ANT
# Requires Apache ANT installed
# ======================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(dirname "$SCRIPT_DIR")"

cd "$APP_DIR"

echo "=== Building Bofalgan Pharmaceuticals ==="
ant compile

if [ $? -eq 0 ]; then
    echo "=== Build successful. Launching... ==="
    ant run
else
    echo "=== BUILD FAILED. Check errors above. ==="
    exit 1
fi
