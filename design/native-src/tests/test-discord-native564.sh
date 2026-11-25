#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to a JDK}"
t=$(mktemp -d);trap 'rm -rf "$t"' EXIT
cp tests/fake-discord-sdk/FakeDiscordSdk564.h "$t/discordpp.h"
c++ -std=c++17 -Wall -Wextra -Wno-misleading-indentation -I"$t" -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" tests/KuroDiscordNative564Test.cpp -o "$t/test"
"$t/test"
