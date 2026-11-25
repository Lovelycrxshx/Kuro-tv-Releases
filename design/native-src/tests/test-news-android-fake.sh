#!/usr/bin/env bash
# JSON_JAR may point to org.json:json:20240303. This is a host-side fake framework test, not an Android device test.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?Point JSON_JAR at the org.json JSON library jar}"
w=$(mktemp -d);trap 'rm -rf "$w"' EXIT
find tests/fake-android -name '*.java' -print > "$w/files"
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$w" @"$w/files" src/app/lovelycrxshx/kuro/{NewsParser,NewsLogic,NewsSync,NewsJobService}.java tests/NewsAndroidFakeTest.java
java -cp "$w:$JSON_JAR" NewsAndroidFakeTest
