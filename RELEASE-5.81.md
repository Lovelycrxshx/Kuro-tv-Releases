# Kuro 5.81

`app.lovelycrxshx.kuro` · versionCode **84** · minSdk 26 (Android 8.0+) · targetSdk 35 (Android 15)

Signed with the original Kuro release key (`CN=Kuro Anime`), so this installs straight over
5.72 – 5.80. **Do not uninstall or clear data.**

```
SHA-256  209b799d525450a59d36811c3c46238acd1fe92b6085bb544f87cdc0a60c9fe1
Size     55,021,231 bytes
```

## What's new

**Settings dialogs now speak the app's own visual language.** The update dialogs were the last
stock-Material surface in Kuro: grey panel, system teal accent, flat list rows, next to app
cards in dark violet. They are rebuilt on the same palette the player already uses.

- **Themed dialog toolkit** — rounded panel with the app's violet accent, ink and muted text,
  fade-and-rise entry, and footer actions with real focus states.
- **Kuro updates** is now a status panel: Installed, Automatic checks, Downloads, Reminder
  spacing, GitHub offers and Last check render as label/value rows (they were one grey
  paragraph of text), with Install / Check now / Turn off / Close as focusable actions.
- **Update reminder spacing** and **Update download network** are proper radio lists, each
  option carrying a short explanation of what it does.
- **Every other native alert** (cache clear, licences, Discord link/unlink, skip markers,
  Cast handoff, install prompts) inherits the same theme — no grey dialogs left in settings.
- **Remote-first:** every action and option is focusable with a visible ring, and initial
  focus lands on the primary action or the selected option, so DPAD users never hunt.

## Unchanged

Playback engine, catalogue parsing, mirrors, Discord presence, downloads, Cast, PiP and the
updater's behaviour are identical to 5.80 — this build changes presentation and the version
stamp, nothing else. No playback or network behaviour was altered.

## Install

1. Install **Downloader** by AFTVnews.
2. Enable *Install unknown apps* for Downloader.
3. Paste this release's APK URL into Downloader and install.

Or sideload:

```bash
adb install -r Kuro-tv-5.81.apk
```

## Verify

```bash
sha256sum Kuro-tv-5.81.apk
# 209b799d525450a59d36811c3c46238acd1fe92b6085bb544f87cdc0a60c9fe1

apksigner verify --print-certs Kuro-tv-5.81.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

Built against Android 15 (API 35) with 16 KB page alignment: all 8 native libraries pass the
`zipalign -P 16` check. Signature schemes v2 + v3.

## Notes

- Kuro is a player / front-end. It hosts no media; catalogue and stream access happens locally
  on your device through the public sources you point it at.
- Licensed under GPL-3.0. Corresponding source for this build is attached as
  `Kuro-tv-5.81-Source.zip`.
