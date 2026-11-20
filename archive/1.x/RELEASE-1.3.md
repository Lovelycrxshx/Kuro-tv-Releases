# Kuro 1.3

**versionCode 90 · 2026-11-20**

1.3 is a repair cut. **1.1 and 1.2 shipped the theme files but the app could never load
them**, so on a phone both looked exactly like 1.0: violet, with no Theme colour card in
Account. This build fixes that.

## What was broken

The app is a WebView that answers every `https://kuro.local/…` request from the APK, but
only for the paths it lists one by one in `MainActivity`. 1.1 added the accent
stylesheets (`theme-teal-1.1.css` … `theme-violet-1.1.css`), the picker stylesheet and
`theme-accent-1.1.js` to `assets/` and linked them all from `index.html` — and never added
their paths to that list. The WebView dropped every one of them:

- the accent stylesheet never loaded, so the page kept the 1.0 violet from `style.css`;
- `theme-accent-1.1.js` never ran, so the Account picker was never built;
- 1.2's native palette waited for a choice that could never arrive.

The files were inside the APK the whole time, which is why the release gates — they check
the APK's contents, not what the shell serves — stayed green.

## What changed

- **The served list is generated from the overlay.** `tools/webview_assets_1.3.py` reads
  the overlay directory and writes `KuroWebAssets13`, which `shouldInterceptRequest`
  consults before its hand-written list. Whatever an overlay ships is served — a future
  asset cannot be shadowed again.
- **A gate for it.** The build now fails unless every theme file is in that list, and the
  publish gate fails unless the file names are visible in the compiled `classes.dex`
  (i.e. actually reachable from the shell), not merely present in the archive.
- **Everything 1.1 and 1.2 promised is now live:** teal by default, Account → **Theme
  colour** with Teal / Crimson / Indigo / Amber / Violet, and the same accent on the
  status bar, the navigation bar, the stock dialogs, the player options panel, the watch
  header, the episode drawer, the diagnostics sheet, the offline screen and the update
  dialog.

## Honest notes

- **Nothing about the look changed in this cut beyond making it work.** The palettes,
  the art pairs and the native theme table are the 1.1/1.2 code, now actually loaded.
- **If you installed 1.1 or 1.2 and saw no change, that was this bug, not your phone.**
  This build fixes it; you do not need to clear data or reinstall.
- **A dialog already on screen** keeps its colours until it is opened again; everything
  new follows the accent.
- **The features still queued** (audio-track memory, auto-next countdown, level readout,
  artwork cache controls, in-app What's new) are not in this build either — this cut is
  the repair only.
- **Source archive.** The published `Kuro-tv-1.3-Source.zip` is the 1.0 tree plus the 1.1
  assets, the 1.2 native palette rewrite and this fix — the same program that is in the
  APK. All three steps live in the repo (`assets-1.1/`, `tools/theme_native_1.2.py`,
  `tools/webview_assets_1.3.py`).

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same signing
key, so profiles, watch list, history, downloads and logins stay where they are.
`versionCode` 90 is above 89, so 1.0, 1.1, 1.2 and anything older will offer this release
normally.

## Verify

```
sha256sum Kuro-tv-1.3.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.3.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · source archive size is in its `.sha256` companion file.
- The next release is 1.4 with `versionCode` 91.
