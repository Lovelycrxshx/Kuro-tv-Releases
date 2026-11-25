#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
w=$(mktemp -d);trap 'rm -rf "$w"' EXIT
javac -encoding UTF-8 -d "$w" src/app/lovelycrxshx/kuro/{NewsParser,NewsLogic}.java tests/NewsSources541Test.java
java -cp "$w" NewsSources541Test "$@"
