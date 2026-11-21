# Kuro TV

An open-source media client designed for Android TV, Fire TV, and mobile devices.

[![Latest Release](https://img.shields.io/badge/Latest_Version-1.5.4-blue)](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/latest)
[![License](https://img.shields.io/badge/License-GPL--3.0-green.svg)](LICENSE)

**Current release: 1.5.4** (`versionCode` 96) — release notes in [RELEASE-1.5.4.md](RELEASE-1.5.4.md).
Notes for every earlier cut are in [archive/1.x/](archive/1.x/), and the same text is on the
[releases page](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases).

---

## 📥 Download

[![Download](design/download-button.svg)](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/download/v1.5.4/Kuro-tv-1.5.4.apk)

* **[Direct APK download](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/download/v1.5.4/Kuro-tv-1.5.4.apk)** — signed APK (`Kuro-tv-1.5.4.apk`, 50.6 MiB) straight from GitHub Releases.
* **[Download page](https://lovelycrxshx.github.io/Kuro-tv-Releases/)** — direct GitHub APK download,
  SHA-256 checksum and install steps.
* **[Latest release](https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/latest)** — APK,
  checksum and the matching source archive on GitHub Releases.

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

* **Cinematic Title Detail v2** — full-bleed banner header, ★ score & studio pills, interactive
  **Franchise & Watch-Order** strip, Canon / Filler / Recap badges, **Skip Filler** switch, and
  batch offline downloads
* **Streamlined YouTube-style Watch page** — 5 servers per page with an **Other servers** switcher,
  3-row scrollable episode playlist card, and a **YouTube Desktop horizontal "More anime" shelf**
  right below Anime details
* **Smart Multi-Filter Browse & Voice Search** — combinable OLED filter pills (**Dub Available**,
  **★ 8.0+**, Airing / Completed, Genre, Format, Year), native Android Voice Search mic pill, and
  SUB | DUB poster pills
* **Desktop-site Left-Rail & Android TV / Fire TV Leanback UI** — frosted-OLED left navigation rail,
  widescreen hero & shelves, and glowing accent DPAD spatial focus
* **Encrypted DNS (DoH) + 8-lane catalog engine + Multi-layer Ad-Guard** — built-in `1.1.1.1` /
  `8.8.8.8` DoH resolver to unblock carrier-restricted mirrors without a VPN, HLS codec fallback,
  low-RAM WebView crash recovery, and zero-popup player ad blocking
* **40 Anime & Manga Profile Art sets + Two-way AniList / MAL Sync** — 40 curated face avatars and
  matching `16:9` banners (`art-11`–`art-50`), live AniList & MyAnimeList sync cards, and integrated
  Discord Rich Presence (Social SDK 1.11.24259) with a direct GitHub Releases **Download** button
* Native Media3 / ExoPlayer playback, offline downloads and picture-in-picture
* **OLED Segmented Chapters & Pills player skin** across all server embeds, the built-in HLS player,
  native Media3 playback, and offline downloads — with segmented OP / ED chapter markers and accent-lit pills
* **Fullscreen swipe-up Up Next & Episodes sheet** plus a non-blocking **Auto-Next floating card** at episode end
* Built against Android 15 (API 35) with 16 KB page-aligned native libraries
* Clean, ad-free navigation
* Lightweight playback engine with subtitle support
* Skip suggestions on every episode — a source marker or your own marker always wins, and a
  suggestion never skips by itself
* **Continue watching** — Home opens with the episodes you have started, the minutes you are in,
  and one tap back to the right episode
* **Long press a poster** — Open, add or remove from My List, or hide it from Home (with a
  **Hidden titles** manager in Settings → Privacy and links)
* **Twenty accents + Pure black (OLED)** chosen in Settings (defaulting to **Signature Violet**), with the native dialogs, bars and
  player following
* **Shimmer loading skeletons** on Home and Browse so catalogues never jump when artwork arrives
* **Local `.json` Backup & restore** — lists, history, episode resume checkpoints, profile art,
  accent, OLED mode and settings
* **Downloads upkeep** — count and size, delete watched, keep the latest episode per series,
  delete all, and an optional quiet sweep every 7 or 30 days
* **What's new** — one card per build on Home, and the same list in Settings → Developer

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

What's next for the 1.x line — an update channel with a real beta ring, the DPAD/focus
overhaul, the airing calendar and custom themes — lives in
**[ROADMAP.md](ROADMAP.md)**. The 5.x line is closed, and 1.0–1.5.3 are history: both are in
**[archive/](archive/)**.

---

## 📄 License

Distributed under the [GPL-3.0 License](LICENSE).
