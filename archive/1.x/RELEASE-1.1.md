# Kuro 1.1

**versionCode 88 · 2026-11-20**

1.1 is a look-and-feel cut: the app stops being purple. Same app, same data, new coat.

## What changed

- **Theme colour, switchable.** Teal is the default. Account → **Theme colour** offers
  Teal, Crimson, Indigo and Amber, and keeps **Violet** as the 1.0 look so you can go back
  without reinstalling. The choice is saved on the phone and applies everywhere the app
  paints itself: home, browse, search, library, account, dialogs, player controls.
  Every colour in the 1.0 palette was re-hued rather than hand-picked one by one, so
  contrast, focus rings and disabled states stay exactly as legible as they were.
- **Profile art: ten pairs now, not five.** Five new avatar + banner pairs, matched:
  Gojo (*Jujutsu Kaisen*), Sung Jinwoo (*Solo Leveling*), Frieren (*Frieren: Beyond
  Journey's End*), Yor (*Spy X Family*), Makima (*Chainsaw Man*) — all titles already in
  the catalogue. The picker in Account scrolls, and each banner is still cropped from the
  left so the scene stays readable behind your avatar.
- **Nothing else moved.** No playback, catalogue, update or account behaviour changed in
  this cut. The features queued for 1.1 proper (audio-track memory, auto-next countdown,
  level readout, artwork cache controls, in-app What's new) are not in this build — this
  is the theme and art release; they follow next.

## Honest notes

- **The native surfaces are still violet.** The status bar and the update/download dialog
  are Java-side and carry the old purple constant; only the web layer re-hues in this
  build. Recolouring those needs the source tree, not just the assets, so it lands with
  the next cut that touches Java.
- **Source archive.** The build applies the 1.1 assets as an overlay on the 1.0 source
  tree, and the published `Kuro-tv-1.1-Source.zip` is exactly that combination — the same
  program that is in the APK. The overlay is in the repo under `assets-1.1/`, and
  `tools/theme_gen.py` regenerates the five theme stylesheets from the 1.0 palette.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same signing
key, so profiles, watch list, history, downloads and logins stay where they are.
`versionCode` 88 is above 87, so 1.0 and anything older will offer this release normally.

## Verify

```
sha256sum Kuro-tv-1.1.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.1.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · source archive size is in its `.sha256` companion file.
- The next release is 1.2 with `versionCode` 89.
