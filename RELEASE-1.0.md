# Kuro 1.0

**versionCode 87 · 2026-11-19**

Kuro starts again at 1.0. This is not a repackaged 5.83: the version line restarts here, the
older releases are gone from the releases page, and this is the build everything updates
to next.

## What changed

- **The number.** `versionName` goes 5.83 → **1.0**. `versionCode` goes 86 → **87**, because
  Android — and the in-app updater — still need it to climb: 87 is newer than 86, whatever the
  name says.
- **One rule, so nobody is stranded.** Version names cannot express "1.0 comes after 5.83",
  so the updater says it once: a 1.x release is newer than any installed 5.x build. Every
  install on 5.80, 5.81, 5.82 or 5.83 will update to 1.0 the normal way — found, downloaded,
  verified, installed on the 24-hour window — with no sideloading. From 1.0 onward the
  comparison is the plain numeric one again: 1.1 over 1.0, 1.10 over 1.9, and so on.
- **Older releases removed.** v5.72 through v5.83 are deleted from this repo so the releases
  page has one build on it. (The `sdk-cache` release is infrastructure for the build runner,
  not a product release, and stays.)

## What it contains

Everything from 5.83, unchanged: the automatic update window (24 hours after publication by
default, 48 if you prefer, immediate on the next check once the window has passed), the
themed download screen with speed and time-left, the pinned signer and resumable download,
and skip suggestions on every episode that always let a real marker win.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same signing key
as every release since 5.62, so profiles, watch list, history, gallery, downloads and logins
stay exactly where they are.

## Verify

```
sha256sum Kuro-tv-1.0.apk
# 5f590b0e621ec65e548f2fb8ea641bd8229e8b02c90c0623b0e15aee271df565

apksigner verify --print-certs Kuro-tv-1.0.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK 44,008,672 bytes · source archive 7,256,556 bytes.
- The next release is 1.1 with `versionCode` 88; then 1.2 / 89, and so on. The code always
  moves forward by one, the name by whatever the release is.
- Android decides when background work runs, so a window that ends at 3 a.m. may install at
  7 a.m. Opening Kuro runs a check immediately.
