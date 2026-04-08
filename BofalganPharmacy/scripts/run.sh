#!/bin/bash
# ======================================================
# Bofalgan Pharmaceuticals - Linux/macOS Launch Script
# ======================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(dirname "$SCRIPT_DIR")"
LIB_DIR="$APP_DIR/lib"
BUILD_DIR="$APP_DIR/build"

# Build classpath
CP="$BUILD_DIR"
for jar in "$LIB_DIR"/*.jar; do
    CP="$CP:$jar"
done

java --module-path "$LIB_DIR" \
     --add-modules javafx.controls,javafx.graphics,javafx.base \
     -cp "$CP" \
     -Xmx512m \
     com.bofalgan.pharmacy.Main
