#!/bin/bash

# BunnyBot APK Build Script
set -e

echo "🔨 Building BunnyBot APK..."

# Clean previous builds
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Check if build was successful
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo "✅ APK built successfully!"
    echo "📦 APK location: app/build/outputs/apk/release/app-release.apk"
    ls -lh app/build/outputs/apk/release/app-release.apk
else
    echo "❌ APK build failed!"
    exit 1
fi
