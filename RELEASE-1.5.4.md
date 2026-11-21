# Kuro 1.5.4

**versionCode 96 · 2026-11-19**

1.5.4 ships the **Cinematic Title Detail v2** screen, the **Streamlined
YouTube-Style Watch Page** (5-per-page server selector, 3-row scrollable
episode playlist, and YouTube Desktop horizontal recommended anime shelf),
**Smart Multi-Filter Browse & Voice Search**, **Desktop-Site Left-Rail &
Leanback DPAD UI**, **Encrypted DNS (DoH) + 8-lane catalog engine + Multi-Layer
Player Ad-Guard**, **40 real anime & manga Profile Art sets**, and **Two-Way
AniList / MyAnimeList Sync & Discord Hub**.

## What changed

- **Streamlined YouTube-Style Watch Page.**
  - **5 servers per page + "Other servers" pill:** shows 5 clean server buttons
    at a time under `Servers & audio` with an inline **Other servers** page
    switcher, eliminating tall server grids.
  - **3-row scrollable Episode Playlist card:** compact YouTube-playlist-style
    `Episodes` container (~230 dp max height) with smooth touch and remote
    scrolling, range chips, and active-episode auto-centering; removes the
    redundant header `Episodes` button and slide-over drawer.
  - **YouTube Desktop horizontal "More anime" shelf:** directly below `Anime
    details`, recommended anime cards scroll smoothly **left-to-right** in a
    horizontal `16:9` shelf (`224 dp` widescreen cards with ★ score and episode
    badges) with one-tap navigation into Title Detail v2.
- **Cinematic Title Detail v2.**
  - Full-bleed banner hero with ★ rating, studio, runtime and status pills, an
    interactive **Franchise & Watch-Order** relation strip, **Canon / Filler /
    Recap** badges, a **Skip Filler** switch (honoured automatically on
    Auto-Next), `16:9` episode rows with thumbnails, and **Batch Offline
    Download**.
- **Smart Multi-Filter Browse & Native Voice Search.**
  - Combinable OLED filter pills on Browse (**Dub Available**, **★ 8.0+**,
    **Airing / Completed**, **Genre**, **Format**, **Year**), a native Android
    `RecognizerIntent` **Voice Search** microphone pill, and compact **SUB |
    DUB** poster pills.
- **Desktop-Site Left-Rail & Android TV / Fire TV Leanback UI.**
  - Enabling *Settings → Site view → Desktop* (or launching on Android TV /
    Fire TV, now declared with `LEANBACK_LAUNCHER` and `@drawable/kuro_tv_banner`)
    activates the frosted-OLED left navigation rail, widescreen hero & shelves,
    glowing accent DPAD focus halo, spatial arrow-key/remote navigation, and
    foldable continuity.
- **Encrypted DNS (DoH), 8-Lane Catalog Engine & Multi-Layer Ad-Guard.**
  - Built-in **DNS-over-HTTPS** (`1.1.1.1` Cloudflare + `8.8.8.8` Google with
    in-memory TTL cache) bypasses ISP DNS blocks without a VPN, paired with an
    8-thread connection-pooled catalog client, automatic HLS software-decoder
    fallback (`Media3` `EXTENSION_RENDERER_MODE_PREFER`), `onRenderProcessGone`
    low-RAM WebView crash recovery, edge-to-edge `WindowInsets` safe areas, and
    a 3-layer **Player Ad-Guard** blocking ad domains, popups, and overlay click
    hijacks.
- **40 Anime & Manga Profile Art Sets + Tracker Sync & Discord Hub.**
  - **40 curated face avatars & matching `16:9` banners** (`art-11`–`art-50`)
    across shonen, seinen, romance, slice-of-life, and manga panels.
  - Upgraded **Account** hub with live **AniList** and **MyAnimeList (PKCE)**
    OAuth deep-link loopback, **Two-Way Auto-Sync** switches, and an interactive
    **Discord Rich Presence** preview with direct `v1.5.4/Kuro-tv-1.5.4.apk`
    Download button.
  - Restores **Signature Violet** (`violet`) as the default theme for fresh
    installs while keeping every user's chosen accent across all 20 themes.

## Carried forward from 1.5.3, 1.5.2, 1.5.1 & 1.5

- **Universal OLED Segmented Chapters & Pills player skin**, **fullscreen bottom
  swipe-up Up Next & Episodes sheet**, and **App-UI Auto-Next floating card**.
- **Pure black (OLED) mode** across all twenty accents, **Home & Browse shimmer
  skeletons**, **Hidden titles** manager, and complete local `.json` **Backup &
  restore** (including native episode resume checkpoints).
- **Monochrome status-bar icon**, inline Android 13+ **Allow notifications**
  prompt, **"You are on the latest version"** state, **Continue watching**,
  **long-press poster actions**, and **Downloads upkeep**.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same
signing key, so profiles, watch list, history, downloads and logins stay put.
`versionCode` 96 is above 95, so 1.0–1.5.3 offer this release automatically.
A device already on `1.5.4-beta` compares equal by version number; install this
APK over the top once if you want the non-beta `1.5.4` label.

## Verify

```
sha256sum Kuro-tv-1.5.4.apk
# 25ae918de0105f2120588842b0f9cc8b87b176d817e2efbca06159ec02881f46

apksigner verify --print-certs Kuro-tv-1.5.4.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK 53,085,266 bytes (50.6 MiB) · the source archive (`Kuro-tv-1.5.4-Source.zip`, 15.6 MiB) has its own `.sha256` companion.
