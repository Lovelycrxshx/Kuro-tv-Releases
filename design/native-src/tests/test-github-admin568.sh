#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?Set JSON_JAR to org.json 20240303}"
: "${ANDROID_JAR:?Set ANDROID_JAR to Android platform 35 android.jar}"
t=$(mktemp -d);trap 'rm -rf "$t"' EXIT
javac --release 8 -Xlint:-options -encoding UTF-8 -cp "$JSON_JAR:$ANDROID_JAR" -d "$t" src/app/lovelycrxshx/kuro/AccountError.java src/app/lovelycrxshx/kuro/AccountOAuth.java src/app/lovelycrxshx/kuro/GitHubAdmin568.java tests/GitHubAdmin568HostTest.java
java -cp "$t:$JSON_JAR:$ANDROID_JAR" app.lovelycrxshx.kuro.GitHubAdmin568HostTest
