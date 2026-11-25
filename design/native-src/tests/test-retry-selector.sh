#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?Set JSON_JAR to an org.json-compatible jar}"
w=$(mktemp -d);trap 'rm -rf "$w"' EXIT
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$w" src/app/lovelycrxshx/kuro/RetrySelector.java tests/RetrySelectorTest.java
java -cp "$w:$JSON_JAR" RetrySelectorTest
