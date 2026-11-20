# Kuro TV

An open-source media client designed for Android TV, Fire TV, and mobile devices.

[![Latest Release](https://img.shields.io/github/v/release/Lovelycrxshx/Kuro-tv-Releases?color=blue&label=Latest%20Version)](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/latest)
[![License](https://img.shields.io/badge/License-GPL--3.0-green.svg)](LICENSE)

**Current release: 1.0** (`versionCode` 87) — release notes in [RELEASE-1.0.md](RELEASE-1.0.md).
The site is at [lovelycrxshx.github.io/Kuro-tv-Releases](https://lovelycrxshx.github.io/Kuro-tv-Releases/).

---

## 📥 Download

Get the latest stable build from GitHub:

* **[Latest release](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/latest)** — APK,
  checksum and the matching source archive.
* The site mirrors the same file with its SHA-256 for comparison.

---

## 🔄 Updates

Kuro updates itself, in the app, from this repository:

* a newer release is found, downloaded, and verified against the published SHA-256 and the
  release certificate before Android's installer is offered;
* it installs **24 hours after publication** by default (48 if you prefer, or immediately) —
  *Settings → Kuro updates → Update delay*;
* if that window has already passed, the update happens on the next check. Android always
  shows its own confirmation; no app can install silently.

**One-time note for the 5.x line:** the numbers restarted at 1.0. A device still on 5.83 or
older compares version names, and by that arithmetic `1.0` is below `5.83`, so it will not
offer this release by itself — install 1.0 by hand once (same signing key, over the top) and
every update after that is automatic again.

---

## 🗂️ Sources & mirrors

Kuro is a player / front-end and hosts no media. The catalogue comes from these public mirrors.

## 📺 Features

* Optimized for stability and performance with integrated Discord Rich Presence (Social SDK 1.11.24259)
* Native Media3 / ExoPlayer playback, offline downloads, Google Cast and picture-in-picture
* Built against Android 15 (API 35) with 16 KB page-aligned native libraries
* Clean, ad-free navigation
* Lightweight playback engine with subtitle support
* Skip suggestions on every episode — a source marker or your own marker always wins, and a
  suggestion never skips by itself

---

## 🛠️ Installation Guide

### Android 8+ Devices
1. Install **Downloader** by AFTVnews from your device's app store.
2. In your device settings, enable **Install unknown apps** for the Downloader application.
3. Open Downloader and paste the direct APK URL from the [Releases](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/latest) section.
4. Select **Install** when prompted.

Installing over an existing Kuro keeps your profiles, watch list, history, gallery, downloads
and logins: same package `app.lovelycrxshx.kuro`, same signing key.

---

## ⚠️ Disclaimer

Kuro TV is purely a media player / front-end interface. It does not host, upload, or control any external media content or streams. All scrapers and metadata providers are run locally by the user.

---

## 🗺️ Roadmap

What's next for the 1.x line — the DPAD/focus overhaul, home skeletons, the airing calendar and
theming v2 — lives in **[ROADMAP.md](ROADMAP.md)**. The 5.x line is closed; its notes are in
[archive/](archive/).

---

## 📄 License

Distributed under the [GPL-3.0 License](LICENSE).
