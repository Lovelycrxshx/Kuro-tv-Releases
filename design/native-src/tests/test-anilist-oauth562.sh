#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
w=$(mktemp -d); trap 'rm -rf "$w"' EXIT
javac -encoding UTF-8 -d "$w" src/app/lovelycrxshx/kuro/AccountError.java src/app/lovelycrxshx/kuro/AccountOAuth.java tests/AniListOAuth562HostTest.java
java -cp "$w" app.lovelycrxshx.kuro.AniListOAuth562HostTest
