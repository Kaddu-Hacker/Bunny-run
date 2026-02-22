#!/bin/bash

# BunnyBot Release Script
set -e

VERSION=${1:-"1.0.0"}
TAG="v$VERSION"

echo "📝 Creating release $TAG..."

# Check if APK exists
if [ ! -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo "❌ APK not found! Run build-apk.sh first."
    exit 1
fi

# Create git tag
git tag -a "$TAG" -m "Release $VERSION"

# Push tag to GitHub
git push origin "$TAG"

# Create GitHub release with APK
gh release create "$TAG" \
    app/build/outputs/apk/release/app-release.apk \
    --title "BunnyBot v$VERSION" \
    --notes "Automated release of BunnyBot v$VERSION"

echo "✅ Release $TAG created successfully!"
echo "📦 Download: https://github.com/Kaddu-Hacker/Bunny-run/releases/tag/$TAG"
