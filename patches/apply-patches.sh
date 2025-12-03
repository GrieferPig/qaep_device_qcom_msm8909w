#!/bin/bash
#
# Apply device-specific patches to Android source tree
# Run this script from the root of the Android source tree
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_ROOT="$(pwd)"

echo "Applying msm8909w device patches..."

# Apply framework patches
if [ -d "$ANDROID_ROOT/frameworks/base" ]; then
    echo "Applying framework patches..."
    cd "$ANDROID_ROOT/frameworks/base"
    
    for patch in "$SCRIPT_DIR"/*.patch; do
        if [ -f "$patch" ]; then
            echo "  Applying $(basename $patch)..."
            git apply --check "$patch" 
            if [ $? -eq 0 ]; then
                git apply "$patch"
                echo "    Success!"
            else
                echo "    Patch already applied or conflicts detected, skipping."
            fi
        fi
    done
    
    cd "$ANDROID_ROOT"
else
    echo "ERROR: frameworks/base not found!"
    exit 1
fi

echo "Done applying patches."
