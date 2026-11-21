#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JSON_JAR:?Set JSON_JAR to org.json 20240303}"
t=$(mktemp -d);trap 'rm -rf "$t"' EXIT
javac -encoding UTF-8 -cp "$JSON_JAR" -d "$t" src/app/lovelycrxshx/kuro/KuroPresence563.java src/app/lovelycrxshx/kuro/KuroArtwork567.java tests/KuroArtwork567HostTest.java
java -cp "$t:$JSON_JAR" app.lovelycrxshx.kuro.KuroArtwork567HostTest
