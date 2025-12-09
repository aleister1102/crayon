#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Get version from build.gradle.kts
VERSION=$(grep 'version =' build.gradle.kts | cut -d '"' -f 2)

if [ -z "$VERSION" ]; then
  echo "Error: Could not find version in build.gradle.kts"
  exit 1
fi

TAG="v$VERSION"

echo "Releasing version $VERSION (Tag: $TAG)"

# Clean and build
echo "Building project..."
./gradlew clean build

# Check if JAR exists
JAR_FILE="build/libs/Crayon-$VERSION.jar"
if [ ! -f "$JAR_FILE" ]; then
  echo "Error: JAR file not found at $JAR_FILE"
  # Try to find any jar if naming is different (e.g. lowercase)
  JAR_FILE=$(find build/libs -name "*.jar" | head -n 1)
  if [ -z "$JAR_FILE" ]; then
      echo "Error: No JAR file found in build/libs/"
      exit 1
  fi
  echo "Found JAR file at $JAR_FILE"
fi

# Check if tag exists locally
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Tag $TAG already exists locally."
else
  echo "Creating git tag $TAG..."
  git tag "$TAG"
fi

# Push tag if not already on remote
if git ls-remote origin "refs/tags/$TAG" | grep -q "$TAG"; then
    echo "Tag $TAG already exists on remote."
else
    echo "Pushing tag $TAG..."
    git push origin "$TAG"
fi

# Create release using gh cli
echo "Creating release on GitHub..."
gh release create "$TAG" "$JAR_FILE" \
  --title "Release $TAG" \
  --notes "Release $TAG. See commit history for changes." \
  --generate-notes

echo "Release $TAG published successfully!"
