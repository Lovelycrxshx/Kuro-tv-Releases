# Kuro 5.82

**First cut of the 5.82 line · versionCode 85 · 2026-11-19**

Kuro 5.82 is a **version-code cut, not a feature cut**. The code inside this APK is exactly the
code that shipped as 5.81 rev 11.5 — the build behind the `v5.81` release — with one change:
`versionCode` moves from 84 to 85 and `versionName` moves from 5.81 to 5.82. No feature was
added, removed, or edited for this build.

The reason is the updater. It only offers a build whose `versionCode` is **strictly newer** than
the one you have installed, so everything built on 84 was invisible to anyone already on 5.81.
85 is the first build the in-app updater will actually see. From here on, cuts in the 5.82 line
keep 85 and are offered as ordinary updates once 5.83 opens at 86.

- APK: `Kuro-tv-5.82.apk` — 44,008,672 bytes (41.97 MiB)
- Source: `Kuro-tv-5.82-Source.zip` — 7,234,614 bytes (6.90 MiB)
- Package: `app.lovelycrxshx.kuro` · minSdk 26 (Android 8.0) · four ABIs in one APK

## What's inside

Everything below arrived in the 5.81 line and is unchanged here; it is repeated because this is
the first 5.82 build and for many people it is the first time the updater will hand it to them.

### Skip suggestions on every episode

The native player now reads skip markers for **every episode**, not just the current one.

- **Intro and outro ranges are suggested, never assumed.** A source marker wins, a marker you
  set yourself wins, and a suggestion only fills the gap when neither exists. If nothing has
  been marked for an episode, Kuro estimates from that episode's own duration and offers the
  range; it never moves the playhead on its own.
- **A suggestion is labelled as one.** The chip says suggested, so you can tell an estimate from
  a real marker at a glance, and one tap skips — the same as a hand-set marker.
- **Auto-skip does not fire on a suggested range.** Automatic skipping stays reserved for ranges
  that were explicitly marked, on this device or by the source.
- **The estimate is boring on purpose.** On a 24-minute episode it offers intro 8 s → 98 s and
  outro 1348 s → 1438 s, with no overlap. An episode under 120 s gets no suggestion at all,
  because there is no useful intro to find.

### Updater v2

- **Pinned signer.** An update is only accepted when the package name matches, the installed
  signer set equals the downloaded one, and that set contains the release certificate
  (`1D:37:C3:CA:…:03:E9:3E`, CN=Kuro Anime). A build signed by anyone else is refused before
  Android's installer is opened.
- **Resumable download.** An interrupted download continues from where it stopped instead of
  restarting, and the SHA-256 is verified over the finished file. A truncated or altered file
  fails the check and is deleted rather than installed.

## Unchanged

Playback engine, catalogue parsing, mirrors, Discord presence, downloads, PiP, profiles, the
gallery, search filters, the hero deck, casting, the settings surfaces and every theme behave
exactly as they did in 5.81. Nothing was refactored for this cut — that is the point of it.

## Install

Install over the top. **No uninstall.** The package name and signing key are unchanged, so
profiles, watch list, history, gallery, downloads and logins all stay. Uninstalling is the only
thing that removes them.

On a TV or Fire TV stick, sideload the APK with your usual tool, or let the in-app updater
download it and open Android's installer for you — you still confirm on Android's own screen,
as every Android app must.

## Verify

```
sha256sum Kuro-tv-5.82.apk
# 81f8b66ea6651b28cffdeca4f313f7e6e8e802a61f88ff91597e29b790ecb435

apksigner verify --print-certs Kuro-tv-5.82.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

The fingerprint above is the same one every Kuro release has carried since 5.80 and the same
one pinned inside the app. A build whose certificate differs is not a Kuro build.

## Notes

- `versionCode` 85. 5.81 was pinned at 84 for its whole line; 5.82 moves to 85, and the in-app
  updater will now offer this build to anyone on 5.81 or older.
- 5.82 feature work has not started. The next cuts in this line carry real changes and keep
  `versionCode` 85, exactly as the 5.81 cuts did.
- Built from the source archive linked on the release; the tree in this repository at tag
  `v5.82` is the same tree.
