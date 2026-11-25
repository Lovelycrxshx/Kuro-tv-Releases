#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
w=$(mktemp -d);trap 'rm -rf "$w"' EXIT
javac -encoding UTF-8 -d "$w" src/app/lovelycrxshx/kuro/MiruroCatalog.java tests/MiruroCatalogTest.java
java -cp "$w" app.lovelycrxshx.kuro.MiruroCatalogTest
