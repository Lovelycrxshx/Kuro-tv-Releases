#!/usr/bin/env bash
# Build Kuro's own JNI glue against the developer-supplied Discord Social SDK.
# The Discord SDK is not included in Kuro's source backup: see README-5.64.txt.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${KURO_DISCORD_SDK_AAR:?Set KURO_DISCORD_SDK_AAR to the Discord discord_partner_sdk.aar}"
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
NDK=${ANDROID_NDK_HOME:-"$ANDROID_HOME/ndk/27.2.12479018"}
TOOL="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
[ -f "$KURO_DISCORD_SDK_AAR" ] || { echo 'Discord SDK AAR not found' >&2; exit 1; }
[ -x "$TOOL/clang++" ] || { echo 'Android NDK r27c not found; set ANDROID_NDK_HOME' >&2; exit 1; }
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
unzip -q "$KURO_DISCORD_SDK_AAR" 'prefab/modules/discord_partner_sdk/include/*' 'jni/*/libdiscord_partner_sdk.so' -d "$TMP"
INC="$TMP/prefab/modules/discord_partner_sdk/include"
for pair in 'arm64-v8a:aarch64-linux-android26' 'armeabi-v7a:armv7a-linux-androideabi26' 'x86_64:x86_64-linux-android26' 'x86:i686-linux-android26'; do
 abi=${pair%%:*}; target=${pair#*:}; out="build/kuro-discord-jni/$abi"
 mkdir -p "$out"
 "$TOOL/$target-clang++" -std=c++20 -Os -DNDEBUG -fPIC -fvisibility=hidden -shared -static-libstdc++ \
  -Wl,--no-undefined -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 \
  -I"$INC" native/kuro_discord_rpc563.cpp "$TMP/jni/$abi/libdiscord_partner_sdk.so" -llog \
  -o "$out/libkuro_discord_rpc563.so"
 "$TOOL/llvm-strip" --strip-unneeded "$out/libkuro_discord_rpc563.so"
 echo "Built Kuro RPC bridge for $abi"
done
