# Kuro 1.4

**versionCode 91 · 2026-11-20**

1.4 is the colour cut: **ten accents instead of five**, and an Account card that shows
what you are choosing before you choose it. Teal is still the default.

## What changed

- **Five more accents.** Beyond Teal, Crimson, Indigo, Amber and Violet there are now
  **Sky** (cool blue), **Emerald** (green), **Sunset** (orange-red), **Magenta** (hot
  pink) and **Graphite** — a neutral that keeps the app's depth with no hue at all.
  Every one is generated from the same 1.0 palette by the same HLS transform, so contrast,
  focus rings and disabled states stay exactly as legible as they were in Teal.
- **A richer picker.** Account → **Theme colour** now opens with:
  - a **live preview strip** — a mini card with an accent "Play" button, a ghost "+ List"
    chip and a nav row, painted from the accent's own palette values, so you can see the
    contrast before you commit;
  - **grouped swatches** — Signature (Teal, Violet, Graphite), Cool (Sky, Indigo,
    Emerald) and Warm (Amber, Sunset, Crimson, Magenta) — each tile showing its colour
    chip and a checkmark on the active one;
  - focus rings, press states and the "current" tag all driven by the active accent, so
    the card matches whatever you have selected.
- **The native half follows all ten.** The status bar, navigation bar, stock dialogs,
  player options panel, watch header, episode drawer, diagnostics sheet, offline screen
  and update dialog now have ten palette columns, ten dialog styles and ten panel
  drawables instead of five.

## Honest notes

- **Same palette, more hues.** Nothing was hand-picked: `tools/theme_gen.py` re-hues the
  original violet family to the target hue, and `tools/theme_native_1.2.py` writes the
  matching Java table. Graphite is the same transform with saturation taken to zero, which
  is why it reads as a silver-grey app rather than a washed-out one.
- **Violet is still the 1.0 look** and is still one tap away, in Signature.
- **Nothing else moved.** No playback, catalogue, update or account behaviour changed in
  this cut. The features still queued (audio-track memory, auto-next countdown, level
  readout, artwork cache controls, in-app What's new) remain queued.
- **A dialog already on screen** keeps its colours until it is opened again.
- **Source archive.** `Kuro-tv-1.4-Source.zip` is the 1.0 tree plus the 1.1 assets, the
  native palette rewrite and the WebView served-asset list — the same program that is in
  the APK. Everything that produces it is in the repo.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same signing
key, so profiles, watch list, history, downloads and logins stay where they are.
`versionCode` 91 is above 90, so 1.0–1.3 will offer this release normally.

## Verify

```
sha256sum Kuro-tv-1.4.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.4.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · source archive size is in its `.sha256` companion file.
- The next release is 1.5 with `versionCode` 92.
