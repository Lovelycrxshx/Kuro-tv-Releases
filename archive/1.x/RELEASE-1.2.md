# Kuro 1.2

**versionCode 89 · 2026-11-20**

1.2 finishes the job 1.1 started: the *native* half of the app follows the theme colour
too. Same app, same data — the violet is gone everywhere.

## What changed

- **The system bars follow the accent.** The status bar and the navigation bar used to be
  painted from a violet constant. They now use the app floor of the chosen accent, so the
  top and bottom of the screen match what is under them instead of framing it in purple.
- **Stock Android dialogs follow the accent.** The framework alerts Kuro still shows
  (install prompts, "clear cache?", notices, download questions) used to keep the retired
  violet panel and violet button accent. Each of the five accents now has its own dialog
  style and its own panel drawable, so those dialogs match the rest of the screen.
- **Native panels and chrome follow the accent.** The player options panel, the watch
  header, the episode drawer, the player diagnostics sheet, the download/offline screen and
  the update dialog were all built from their own copy of the 1.0 palette. Every colour in
  them is now looked up through one table, `KuroTheme12`, with the value for each accent.
- **Switching is live.** Account → **Theme colour** now reaches the native layer as well:
  pick Crimson and the bars, the next dialog and every native panel that opens after it
  are crimson. The choice is saved twice on purpose — once for the web layer, once for the
  native layer — so a cold start paints the right colour before the page loads.
- **Teal is still the default.** Nothing to do; a fresh install opens in teal.

## Honest notes

- **Re-hued, not hand-picked.** Every native colour came from the same HLS transform that
  produced the 1.1 stylesheets (`tools/theme_gen.py`), applied by
  `tools/theme_native_1.2.py`, so the two halves of the app cannot drift apart colour for
  colour. Very desaturated chrome — dividers, borders, muted text — leans only part of the
  way to the accent, exactly like its CSS counterparts, because pushing grey all the way
  turns it sickly.
- **What does not switch mid-flight.** A dialog that is already open keeps the colours it
  was built with; it picks up the new accent the next time it opens. That is the same rule
  the web layer follows for anything it has already painted.
- **No player, catalogue, update or account behaviour changed.** This cut touches colour
  plumbing only. The features still queued for later (audio-track memory, auto-next
  countdown, level readout, artwork cache controls, in-app What's new) are **not** in this
  build, same as 1.1.
- **The splash stays black.** It is a logo on black by design, not chrome.
- **Source archive.** The build applies the 1.1 assets and then the 1.2 native rewrite to
  the 1.0 source tree; the published `Kuro-tv-1.2-Source.zip` is exactly that combination —
  the same program that is in the APK. Both steps live in the repo: `assets-1.1/`,
  `tools/theme_gen.py` and `tools/theme_native_1.2.py`.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same signing
key, so profiles, watch list, history, downloads and logins stay where they are.
`versionCode` 89 is above 88, so 1.0, 1.1 and anything older will offer this release
normally.

## Verify

```
sha256sum Kuro-tv-1.2.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.2.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · source archive size is in its `.sha256` companion file.
- The next release is 1.3 with `versionCode` 90.
