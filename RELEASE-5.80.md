# Kuro 5.80

`app.lovelycrxshx.kuro` · versionCode **83** · minSdk 26 (Android 8.0+) · targetSdk 35 (Android 15)

Signed with the original Kuro release key (`CN=Kuro Anime`), so this installs straight
over 5.72–5.75. **Do not uninstall or clear data.**

```
SHA-256  f03e4d9f1b5c3b7fb74d26d31007b42c4ffa306de2bb6bc5c75fd492dec23724
Size     55,021,175 bytes
```

## Features

- **Discord Rich Presence** through the Discord Social SDK 1.11.24259, with a native
  JNI bridge built from `kuro_discord_rpc563.cpp`; OAuth returns via
  `app.lovelycrxshx.kuro://oauth`. Sharing defaults to off.
- **Native playback** on Media3 / ExoPlayer 1.7.1 — HLS, OkHttp data source,
  download database, and the packaged Anime4K upscaling shader.
- **Offline downloads** — foreground `dataSync` download service plus a dedicated
  offline player activity.
- **Google Cast** using Google's default receiver, so no separate Kuro Cast
  developer account is needed.
- **Picture-in-Picture** with on-screen controls; resizable and multi-window aware.
- **In-app updates** — background job, download service, FileProvider
  (`…updates572`) and `REQUEST_INSTALL_PACKAGES`, so updates install without
  leaving the app.
- **In-app WebKit catalogue** with a background news-sync job.

## Improvements

- Built against **Android 15 / API 35** with **16 KB page-aligned** native libraries —
  correct on 16 KB-page devices and ready for Play's alignment requirement.
- **Source mirrors**: `hianime.fm`, `hianime.al`, `hianimes.es` — a fallback chain
  (same library, first host that answers wins), not a merge.
- Clean, ad-free navigation unchanged from the 5.7x line; no tracking changes.

## Install

**Android 8+ / Fire TV / Android TV**

1. Install **Downloader** by AFTVnews.
2. Enable *Install unknown apps* for Downloader.
3. Paste this release's APK URL into Downloader and install.

Or sideload directly:

```bash
adb install -r Kuro-tv-5.80.apk
```

## Verify the download

```bash
sha256sum Kuro-tv-5.80.apk
# f03e4d9f1b5c3b7fb74d26d31007b42c4ffa306de2bb6bc5c75fd492dec23724
```

Verify the signer (should print `CN=Kuro Anime`, SHA-256
`1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E`):

```bash
apksigner verify --print-certs Kuro-tv-5.80.apk
```

## Notes

- Kuro is a player / front-end. It hosts no media; all catalogue and stream access
  happens locally on your device through the public sources you point it at.
- Licensed under GPL-3.0. Corresponding source for this build is attached as
  `Kuro-tv-5.80-Source.zip` / `Kuro-tv-5.80-Source.txt`.
