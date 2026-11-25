# Kuro 1.5.6

**versionCode 133 · 2026-11-25**

1.5.6 builds on the **1.5.5** feature set — the interactive Profile Art Studio,
unified multi-source search, domain health sweeper, position-lock on server
switch, brute-force quality render, and live theme accent preview — and adds
**Additional settings in Account (no login required)**, a **Discord custom RPC
login gate**, **fully separate profile-art avatars and banners**, and an
**update button position fix**.

## What changed

- **Additional settings, no login required.**
  - A new *Additional settings* row in the Account section opens themes,
    playback, privacy and updates for every visitor, signed in or not.
- **Discord custom RPC requires a Discord login.**
  - The Rich Presence and Presence customization panels now show a lock notice
    and disable their controls until a Discord account is connected in
    Profile → Account.
- **Profile art: avatars and banners fully separate.**
  - The combined Paired Sets view and Surprise pair action are removed. Every
    avatar (PFP) and every 16:9 banner is chosen independently from the
    Avatars and Banners tabs, with dynamic counts (43 entries).
- **Update button position fixed.**
  - The header update button is repositioned for a clean fit beside the other
    header controls.
- **Bug fixes and polish** across the app.

## Carried forward from 1.5.5, 1.5.4, 1.5.3, 1.5.2, 1.5.1 & 1.5

- **Profile Art Studio & Live Image Preview** — live 16:9 banner + avatar
  stage, Split / Avatars / Banners tabs, instant character search, themed
  action buttons, and custom gallery uploads.
- **Full PFP & Banner Image Rendering (CSP-safe)** — native `<img>` rendering
  across the Account hero, top header chip, profile drawer, and Watch screen
  header.
- **Streamlined YouTube-Style Watch Page** — 5 servers per page, 3-row
  scrollable Episode Playlist card, YouTube Desktop "More anime" shelf.
- **Cinematic Title Detail v2** — full-bleed banner hero, Franchise &
  Watch-Order strip, Canon / Filler / Recap badges, Skip Filler switch, 16:9
  episode rows, Batch Offline Download.
- **Smart Multi-Filter Browse & Native Voice Search**, **Desktop-Site Left-Rail
  & Leanback DPAD UI**, **Encrypted DNS (DoH) + 8-lane catalog engine +
  Multi-Layer Player Ad-Guard**, and **43 Anime & Manga Profile Art sets +
  Tracker Sync & Discord Hub**.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`,
same signing key, so profiles, watch list, history, downloads and logins stay
put. `versionCode` 133 is above 132, so 1.0–1.5.5 offer this release
automatically.

## Verify

```
sha256sum Kuro-tv-1.5.6.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.5.6.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · the source archive
  (`Kuro-tv-1.5.6-Source.zip`) has its own `.sha256` companion.
