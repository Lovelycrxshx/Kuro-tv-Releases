# Kuro 1.5.3

**versionCode 95 · 2026-11-19**

1.5.3 brings the **OLED Segmented Chapters & Pills** player UI across every
server player, a **fullscreen bottom swipe-up Up Next & Episodes sheet**, a
non-blocking **App-UI Auto-Next floating card**, and **direct GitHub Releases
APK downloads** across every Download button.

## What changed

- **Universal OLED Segmented Chapters & Pills player skin.**
  - Every playback path — embedded server iframes (JWPlayer, Video.js, Plyr),
    the built-in HLS `stream-player`, the native Media3 / ExoPlayer view
    (`KuroNativePlayer561`), and offline downloads (`KuroOfflineActivity561`) —
    now shares the same `#0A0A0EEF` frosted-OLED floating pill HUD, rounded
    accent scrubber with glowing thumb, segmented OP / ED chapter markers, and
    top/bottom gesture edge guards.
  - Adapts automatically to all twenty Kuro accents and **Pure black (OLED)**
    mode.
- **Fullscreen bottom swipe-up for Up Next & Episodes.**
  - Swiping up from the bottom edge during fullscreen playback slides up an
    accent-tinted OLED floating sheet featuring a one-tap **Up Next** card for
    Episode *N+1* plus a horizontal episode selector strip — switch episodes
    without leaving fullscreen, or tap **All episodes** to open the full
    season drawer.
- **App-UI Auto-Next floating card.**
  - Replaces the plain modal alert at the end of an episode with a sleek
    bottom-right OLED floating card (**Play Episode N+1**, live countdown pill,
    and **Stay**), keeping the video canvas unobstructed.
- **Direct GitHub Releases APK downloads everywhere.**
  - Tapping **Download** on the repository `README`, on the download page
    (`lovelycrxshx.github.io/Kuro-tv-Releases/`), or on the **Discord Rich
    Presence Download button** (`KuroPresence564` / `kuro_discord_rpc563.cpp`)
    serves `Kuro-tv-1.5.3.apk` straight from GitHub Releases with no Google
    Drive mirror or interstitial redirect.

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
# 67b31dccf24945d18d457efd9cbf2b063edd2f0879faaf6592db127d4870d3f7

apksigner verify --print-certs Kuro-tv-1.5.3.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK 48,737,329 bytes (46.5 MiB) · the source archive (`Kuro-tv-1.5.3-Source.zip`, 11.5 MiB) has its own `.sha256` companion.
