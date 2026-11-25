#!/usr/bin/env bash
# Robolectric tests use shadows and fakes; they do not test a real phone/provider.
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
: "${ANDROID_HOME:?Set ANDROID_HOME or ANDROID_SDK_ROOT to the Android SDK}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17+}"
export ANDROID_HOME JAVA_HOME
./gradlew --no-daemon testDebugUnitTest "$@"
echo 'PASS native Robolectric regression suite'
