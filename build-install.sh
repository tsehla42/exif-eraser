#!/bin/bash
set -e

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "Building app..."
./gradlew assembleDebug

echo "Checking connected devices..."
DEVICES=$(adb devices | grep -w "device" | awk '{print $1}' | grep -v "List")

if [ -z "$DEVICES" ]; then
    echo "No devices connected."
    exit 1
fi

echo "Installing on devices:"
for DEVICE in $DEVICES; do
    echo "  -> $DEVICE"
    adb -s "$DEVICE" install -r "$APK_PATH" && echo "    Installed!" || echo "    Failed to install on $DEVICE"
done

echo "Done."
