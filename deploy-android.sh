#!/usr/bin/env bash
# Pegasus Video Player — Build + Deploy su Odin2 (Git Bash / WSL / macOS)
set -e

DEVICE=69bdb979
BUILD_TYPE=${1:-debug}

if [ "$BUILD_TYPE" = "release" ]; then
    APK=app/build/outputs/apk/release/app-release.apk
    ./gradlew assembleRelease
else
    APK=app/build/outputs/apk/debug/app-debug.apk
    ./gradlew assembleDebug
fi

adb -s "$DEVICE" install -r -g "$APK"
adb -s "$DEVICE" shell dumpsys package com.pegasus.videoplayer | grep versionName
echo "Deploy OK ($BUILD_TYPE)"
