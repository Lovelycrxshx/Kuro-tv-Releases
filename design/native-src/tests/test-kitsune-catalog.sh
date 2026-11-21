#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?Point JSON_JAR to an org.json jar}"
w=$(mktemp -d);trap 'rm -rf "$w"' EXIT
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$w" src/app/lovelycrxshx/kuro/KitsuneCatalog.java tests/KitsuneCatalogTest.java
java -cp "$w:$JSON_JAR" KitsuneCatalogTest "$@"
