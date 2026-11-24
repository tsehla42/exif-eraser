#!/bin/bash
# Build the ExifEraser release APK inside Debian so it matches F-Droid's buildserver.
#
# WHY THIS EXISTS
# F-Droid's `Binaries:` verification copies our signature onto their rebuild and
# compares byte-for-byte. Fedora ships zlib-ng-compat, which is API-compatible but
# emits *different deflate output* than Debian's real zlib1g — so an APK built here
# on Fedora has identical content yet compresses ~74 bytes differently, and
# apksigcopier refuses with:
#     "APK Signing Block offset < central directory offset"
# Building on Debian makes content *and* compression match (verified: 0 differing
# entries, only the 4096-byte signing block differs from their unsigned build).
#
# USAGE
#   ./scripts/build-release-in-debian.sh
# Output: /tmp/opencode/debian-out/exif-eraser-1.0.0.apk  (then upload to the release)
#
# Requirements: podman (or docker), the host Android SDK with the same
# build-tools/platform the wrapper asks for, and a clean, *tagged* working tree —
# vcsInfo embeds HEAD, so the APK must be built at the release tag.
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${OUT:-/tmp/exif-eraser-debian-build}"

if ! git -C "$REPO" diff --quiet || ! git -C "$REPO" diff --cached --quiet; then
  echo "working tree is dirty — vcsInfo would not match the tag. Commit or stash first." >&2
  exit 1
fi
echo "building at: $(git -C "$REPO" rev-parse HEAD) (tag: $(git -C "$REPO" describe --tags --exact-match 2>/dev/null || echo 'NOT TAGGED'))"

rm -rf "$OUT" && mkdir -p "$OUT"

run_podman() {
  if command -v podman >/dev/null 2>&1; then PODMAN=podman; else PODMAN=docker; fi
  "$PODMAN" run --rm \
    --security-opt label=disable \
    -v "$REPO":/src:ro \
    -v "$OUT":/out \
    -v "$HOME/android-sdk":"$HOME/android-sdk":ro \
    -v "$HOME/projects/android/certificates":"$HOME/projects/android/certificates":ro \
    -v fdroid-gradle-home:/root/.gradle \
    debian:trixie bash -c "$1"
}

run_podman '
  set -e
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y -qq openjdk-21-jdk-headless wget unzip >/dev/null
  echo "JDK : $(java -version 2>&1 | head -1)"
  echo "zlib: $(dpkg -l zlib1g | tail -1)"
  rm -rf /work && cp -a /src /work && cd /work
  cp /src/keystore.properties .
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  export ANDROID_HOME='"$HOME"'/android-sdk
  export PATH="$JAVA_HOME/bin:$PATH"
  ./gradlew assembleRelease --console=plain --no-daemon
  cp app/build/outputs/apk/release/app-release.apk /out/exif-eraser-1.0.0.apk
  echo "BUILT OK"
'

echo
echo "Output: $OUT/exif-eraser-1.0.0.apk"
sha256sum "$OUT/exif-eraser-1.0.0.apk"
