# Kuro 1.5.5

**versionCode 107 · 2026-11-19**

1.5.5 ships the interactive **Profile Art Studio** with a live 16:9 banner +
avatar preview stage, **fixed strict-CSP PFP & banner rendering** using native
`<img>` elements, and the full **1.5.4** feature set — the Streamlined
YouTube-Style Watch Page, Cinematic Title Detail v2, Smart Multi-Filter Browse &
Voice Search, Desktop-Site Left-Rail & Leanback DPAD UI, Encrypted DNS (DoH) +
8-lane catalog engine + Multi-Layer Player Ad-Guard, 40 real anime & manga
Profile Art sets, and Two-Way AniList / MyAnimeList Sync & Discord Hub.

## What changed

- **Profile Art Studio & Live Image Preview.**
  - Redesigned Profile art with a live **16:9 banner + avatar stage**, Split /
    Paired Sets / Avatars / Banners tabs, instant character search, themed
    action buttons, and custom gallery uploads.
- **Full PFP & Banner Image Rendering (CSP-safe).**
  - Fixed strict-CSP banner and avatar rendering — the previous
    `style.backgroundImage` approach was blocked by the page CSP
    (`style-src 'self'`). Avatars and banners now render with native `<img>`
    elements across the Account hero, top header chip, profile drawer, and
    Watch screen header.
- **Clean Iconography & Native Touch.**
  - Zero 4-point sparkle badges, disabled browser-style hold-to-select popups,
    a Kuro-themed Up Next card, and controllable App DPI & UI Size.
- **Streamlined YouTube-Style Watch Page** (carried from 1.5.4).
  - **5 servers per page + "Other servers" pill**, a **3-row scrollable
    Episode Playlist card**, and a **YouTube Desktop horizontal "More anime"
    shelf** directly below Anime details.
- **Cinematic Title Detail v2** (carried from 1.5.4).
  - Full-bleed banner hero, Franchise & Watch-Order relation strip,
    Canon / Filler / Recap badges, Skip Filler switch, 16:9 episode rows with
    thumbnails, and Batch Offline Download.
- **Smart Multi-Filter Browse & Native Voice Search** (carried from 1.5.4).
  - Combinable OLED filter pills, a native `RecognizerIntent` Voice Search
    microphone pill, and compact SUB | DUB poster pills.
- **Desktop-Site Left-Rail & Android TV / Fire TV Leanback UI** (carried from 1.5.4).
  - Frosted-OLED left navigation rail, widescreen hero & shelves, glowing
    DPAD focus halo, spatial arrow-key/remote navigation, and foldable
    continuity.
- **Encrypted DNS (DoH), 8-Lane Catalog Engine & Multi-Layer Ad-Guard** (carried from 1.5.4).
  - DNS-over-HTTPS (`1.1.1.1` Cloudflare + `8.8.8.8` Google), 8-thread
    connection-pooled catalog client, HLS software-decoder fallback,
    low-RAM WebView crash recovery, edge-to-edge safe areas, and a 3-layer
    Player Ad-Guard.
- **40 Anime & Manga Profile Art Sets + Tracker Sync & Discord Hub** (carried from 1.5.4).
  - 40 curated face avatars & matching 16:9 banners (`art-11`–`art-50`), an
    upgraded Account hub with live AniList / MyAnimeList (PKCE) OAuth,
    Two-Way Auto-Sync switches, and an interactive Discord Rich Presence
    preview with a direct `v1.5.5/Kuro-tv-1.5.5.apk` Download button.
  - Restores **Signature Violet** (`violet`) as the default theme for fresh
    installs while keeping every user's chosen accent across all 20 themes.

## Carried forward from 1.5.4, 1.5.3, 1.5.2, 1.5.1 & 1.5

- **Universal OLED Segmented Chapters & Pills player skin**, fullscreen bottom
  swipe-up Up Next & Episodes sheet, and App-UI Auto-Next floating card.
- **Pure black (OLED) mode** across all twenty accents, Home & Browse shimmer
  skeletons, Hidden titles manager, and complete local `.json` Backup &
  restore (including native episode resume checkpoints).
- **Monochrome status-bar icon**, inline Android 13+ Allow notifications prompt,
  **"You are on the latest version"** state, Continue watching, long-press
  poster actions, and Downloads upkeep.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`,
same signing key, so profiles, watch list, history, downloads and logins stay
put. `versionCode` 107 is above 106, so 1.0–1.5.5-beta offer this release
automatically. A device already on `1.5.5-beta` compares equal by version
number; install this APK over the top once if you want the non-beta `1.5.5`
label.

## Verify

```
sha256sum Kuro-tv-1.5.5.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.5.5.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · the source archive
  (`Kuro-tv-1.5.5-Source.zip`) has its own `.sha256` companion.
