#!/bin/bash
set -e

KEYSTORE="keystore/hwmdm-release.jks"
KEYSTORE_PASS="HwMdm!Release2026"
KEYALIAS="hwmdm-release"
OUT_DIR="/opt/projetos/hwmdm/repo-mdm/source/volumes/work/files"

# Set version to 1.9 (combined build with all features)
sed -i 's/versionCode .*/versionCode 15399/' app/build.gradle
sed -i 's/versionName .*/versionName "1.9"/' app/build.gradle

# Clean and build
./gradlew clean assembleRelease --stacktrace 2>&1 | grep -i "BUILD\|ERROR" || true

APK="app/build/outputs/apk/release/app-release-unsigned.apk"
if [ ! -f "$APK" ]; then
  APK="app/build/outputs/apk/release/app-release.apk"
fi

if [ -f "$APK" ]; then
  # Sign with jarsigner
  jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
    -keystore "$KEYSTORE" -storepass "$KEYSTORE_PASS" \
    -keypass "$KEYSTORE_PASS" \
    "$APK" "$KEYALIAS"
  
  cp "$APK" "$OUT_DIR/hmdm-1.9-olimpia-all.apk"
  echo "✓ Signed APK saved: hmdm-1.9-olimpia-all.apk"
else
  echo "✗ APK not found"
  ls -la app/build/outputs/apk/release/ 2>/dev/null || echo "No build outputs"
fi
