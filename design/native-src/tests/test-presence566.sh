#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
t=$(mktemp -d);trap 'rm -rf "$t"' EXIT
javac -encoding UTF-8 -d "$t" src/app/lovelycrxshx/kuro/KuroPresence563.java src/app/lovelycrxshx/kuro/KuroPresence564.java tests/KuroPresence566HostTest.java
java -cp "$t" app.lovelycrxshx.kuro.KuroPresence566HostTest
