# Kuro 1.5.3

**versionCode 95 · 2026-11-19**

1.5.3 switches every **Download** entry point — the repository `README`, the
download page, and the in-app **Discord Rich Presence Download button** — to
serve the signed APK straight from **GitHub Releases** instead of redirecting
through a Google Drive mirror.

## What changed

- **Direct GitHub Releases APK downloads everywhere.**
  - Tapping the **Download** button on the GitHub repository `README` directly
    downloads `Kuro-tv-1.5.3.apk` from GitHub Releases (kept automatically in
    sync on future releases by `release-badge.yml`).
  - Tapping **Download Kuro.tv 1.5.3** on the download page
    (`lovelycrxshx.github.io/Kuro-tv-Releases/`) directly downloads
    `Kuro-tv-1.5.3.apk` from GitHub Releases with no Google Drive mirror or
    interstitial redirect.
  - The **Discord Rich Presence Download button** (`KuroPresence564` /
    `kuro_discord_rpc563.cpp`) now links directly to
    `https://github.com/Lovelycrxshx/Kuro-tv-Releases/releases/download/v1.5.3/Kuro-tv-1.5.3.apk`.

## Carried forward from 1.5.2, 1.5.1 & 1.5

- **Pure black (OLED) mode** across all twenty accents (`#000000` floor and
  `#0A0A0E` elevated surfaces across both WebView and native Android UI).
- **Home & Browse shimmer skeletons** (`2:3` poster placeholders and hero
  skeleton, `prefers-reduced-motion` aware).
- **Hidden titles manager** in Settings → Privacy and links.
- **Complete local `.json` Backup & restore** (lists, history, profile artwork,
  accent, OLED mode, hidden titles, download auto-clean rules, and native
  `KuroPlayback561` episode playhead checkpoints).
- **Monochrome Android status-bar glyph** (`@drawable/ic_stat_kuro`) and inline
  Android 13+ **Allow notifications** prompt in **Kuro updates**.
- **"You are on the latest version"** status in Kuro updates, the **3.6-second**
  cold-launch title sequence, **Continue watching**, **long-press poster
  actions**, and **Downloads upkeep**.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same
signing key, so profiles, watch list, history, downloads and logins stay put.
`versionCode` 95 is above 94, so 1.0–1.5.2 offer this release automatically.

## Verify

```
sha256sum Kuro-tv-1.5.3.apk
# ae3d9d203fb42a078d338076ce4f50b411e93622f57bb574c3a56619db3ecea6

apksigner verify --print-certs Kuro-tv-1.5.3.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK 48,714,273 bytes (46.5 MiB) · the source archive (`Kuro-tv-1.5.3-Source.zip`, 11.4 MiB) has its own `.sha256` companion.
