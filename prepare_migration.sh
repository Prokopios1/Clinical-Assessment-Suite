#!/bin/bash

# Project Migration Packager for Clinical-Assessment-Android
# This script creates a clean archive of the project for transfer to another machine.

PROJECT_DIR="/home/fupa/Projects/Clinical-Assessment-Android"
ARCHIVE_NAME="clinical_assessment_migration_$(date +%Y%m%d).tar.gz"

echo "📦 Preparing migration archive: $ARCHIVE_NAME..."

# Navigate to project directory
cd "$PROJECT_DIR" || exit 1

# Check for essential files
ESSENTIAL_FILES=("app/google-services.json" "app/release.jks" "gradlew" "build.gradle" "settings.gradle")
for file in "${ESSENTIAL_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo "⚠️ Warning: $file not found. Migration might be incomplete."
    fi
done

# Create the archive
# Excluding build artifacts and IDE configurations to keep it small.
tar -czvf "$ARCHIVE_NAME" \
    --exclude='app/build' \
    --exclude='build' \
    --exclude='.gradle' \
    --exclude='.kotlin' \
    --exclude='.idea' \
    --exclude='.vscode' \
    --exclude='*.apk' \
    --exclude='build_log.txt' \
    .

echo "✅ Migration archive created successfully at: $PROJECT_DIR/$ARCHIVE_NAME"
echo "🚀 You can now transfer this file to your new machine."
