#!/usr/bin/env bash
# Build an update signed with the owner's existing, separately stored key.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}" "${KEYSTORE:?Set KEYSTORE to your existing private PKCS12 keystore}" "${KS_PASS:?Set KS_PASS privately}"
: "${ANDROID_HOME:?Set ANDROID_HOME or ANDROID_SDK_ROOT to an Android SDK containing platform 35 and build-tools 35.0.1}"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK 17 or newer}"
OUT=${OUT:-"$PWD/Kuro-tv-5.80.apk"}
export ANDROID_HOME JAVA_HOME KS_PASS
TOOLS="$ANDROID_HOME/build-tools/35.0.1"
W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
./scripts/build-discord-jni.sh
"${GRADLE_BIN:-./gradlew}" --no-daemon assembleRelease
"$TOOLS/zipalign" -f -P 16 4 build/outputs/apk/release/Kuro-tv-release-unsigned.apk "$W/aligned.apk"
"$JAVA_HOME/bin/java" -jar "$TOOLS/lib/apksigner.jar" sign --v4-signing-enabled false --ks "$KEYSTORE" --ks-pass env:KS_PASS --out "$OUT" "$W/aligned.apk"
"$JAVA_HOME/bin/java" -jar "$TOOLS/lib/apksigner.jar" verify --verbose --print-certs "$OUT"
"$TOOLS/zipalign" -c -P 16 4 "$OUT"
echo "Built $OUT"
