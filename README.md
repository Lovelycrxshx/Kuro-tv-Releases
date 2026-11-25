# Kuro TV

An open-source media client for Android TV, Fire TV, and mobile devices.

[![Latest Release](https://img.shields.io/badge/Latest_Version-1.5.6-blue)](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/latest)
[![License](https://img.shields.io/badge/License-GPL--3.0-green.svg)](LICENSE)

**Current release: 1.5.6** (`versionCode` 133) — release notes in [RELEASE-1.5.6.md](RELEASE-1.5.6.md),
attached to the [latest release](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/latest).

---

## 📥 Download

The APK and its source archive are attached to the GitHub release as assets:

* **[Direct APK download](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/download/v1.5.6/Kuro-tv-1.5.6.apk)** —
  signed APK (`Kuro-tv-1.5.6.apk`, ~49.5 MiB) straight from GitHub Releases.
* **[Latest release](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/latest)** — the APK, its
  `.sha256` checksum, and the matching source archive (`Kuro-tv-1.5.6-Source.zip` + `.sha256`).

Verify the download before installing:

```
sha256sum Kuro-tv-1.5.6.apk
# compare against Kuro-tv-1.5.6.apk.sha256 on the release page

apksigner verify --print-certs Kuro-tv-1.5.6.apk
# CN=Kuro Anime
```

---

## 🔄 Updates

Kuro updates itself, in the app, from this repository:

* a newer release is found, downloaded, and verified against the published SHA-256 and the
  release certificate before Android's installer is offered;
* it installs **24 hours after publication** by default (48 if you prefer, or immediately) —
  *Settings → Kuro updates → Update delay*;
* if that window has already passed, the update happens on the next check. Android always
  shows its own confirmation; no app can install silently.

**One-time note for the 5.x line:** the version numbers restarted at 1.0. A device still on 5.83
or older compares version names, and by that arithmetic `1.0` is below `5.83`, so it will not
offer this release by itself — install 1.0 by hand once (same signing key, over the top) and
every update after that is automatic again.

---

## 📺 Features

* **Cinematic Title Detail v2** — full-bleed banner header, ★ score & studio pills, interactive
  Franchise & Watch-Order strip, Canon / Filler / Recap badges, Skip Filler switch, batch offline downloads
* **Streamlined YouTube-style Watch page** — 5 servers per page with an Other servers switcher,
  3-row scrollable episode playlist card, and a YouTube Desktop "More anime" shelf
* **Smart Multi-Filter Browse & Voice Search** — combinable OLED filter pills (Dub Available,
  ★ 8.0+, Airing / Completed, Genre, Format, Year), native Android Voice Search mic pill, SUB | DUB poster pills
* **Desktop-site Left-Rail & Android TV / Fire TV Leanback UI** — frosted-OLED left navigation rail,
  widescreen hero & shelves, glowing accent DPAD spatial focus
* **Encrypted DNS (DoH) + 8-lane catalog engine + Multi-layer Ad-Guard** — built-in `1.1.1.1` /
  `8.8.8.8` DoH resolver, HLS codec fallback, low-RAM WebView crash recovery, zero-popup player ad blocking
* **Anime & Manga Profile Art + Two-way AniList / MAL Sync** — curated avatars and matching `16:9`
  banners, live AniList & MyAnimeList sync cards, and Discord Rich Presence with a direct GitHub
  Releases Download button
* Native Media3 / ExoPlayer playback, offline downloads and picture-in-picture
* **OLED Segmented Chapters & Pills player skin** across all server embeds, the built-in HLS player,
  native Media3 playback, and offline downloads
* **Fullscreen swipe-up Up Next & Episodes sheet** plus a non-blocking Auto-Next floating card
* Built against Android 15 (API 35) with 16 KB page-aligned native libraries
* **Continue watching**, **Long-press poster** actions, **Twenty accents + Pure black (OLED)**,
  **Shimmer loading skeletons**, **Local `.json` Backup & restore**, and **Downloads upkeep**
* **What's new** — one card per build on Home, and developer contact & notices in
  Settings → Contact developer & notice for bugs and error

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

Kuro TV is purely a media player / front-end interface. It does not host, upload, or control any
external media content or streams. All scrapers and metadata providers are run locally by the user.

---

## 📄 License

Distributed under the [GPL-3.0 License](LICENSE).
