# Kuro 5.83

**versionCode 86 · 2026-11-19**

Kuro now waits before it replaces itself. A release installs itself **24 hours after it is
published** — 48 hours if you prefer — and if that window has already passed when the app
next looks, the update happens immediately. The delay can postpone an update; it can never
strand one.

## What's new

### The update window

- **Measured from the release, not from your phone.** The wait starts at GitHub's own
  `published_at` time. A device that has been off for three days and a phone that was awake
  the whole time both end up current on their next check, instead of each waiting their own
  24 hours from whenever they noticed.
- **Three choices, one setting.** Settings → Kuro updates → *Update delay*:
  - **After 24 hours** — the default: tonight's release installs itself tomorrow;
  - **After 48 hours** — the quiet end of the window, still fully automatic;
  - **As soon as it is found** — the 5.82 behaviour, no wait at all.
- **The window is visible.** The Kuro updates panel and the update screen show
  `SCHEDULED` with the exact time — *"Kuro 5.84 is scheduled · installs itself on 21 Nov,
  18:05 (in 14 h)"* — and **Update now** skips the wait when you do not want to wait.
- **Three independent ways it gets done**, so a dropped job cannot stall you:
  1. a one-shot job parked at the end of the window;
  2. the daily check, which sees the window is over and downloads;
  3. a throttled catch-up whenever you open the app.

  Any one of them is enough. That is what makes "must be updated within the window" true on
  a device that sleeps, loses the network, or has Kuro closed for days.

### Nothing else moved

The download is still resumable, still verified against the published SHA-256 and the pinned
certificate (`1D:37:C3:CA:…:03:E9:3E`, CN=Kuro Anime), still governed by the Wi-Fi-only
setting, and Android still shows its own install confirmation — Kuro cannot install silently
and does not claim to. A cancelled install is still remembered per version, so nothing loops.

## Also in 5.83

- **versionCode 86.** 5.82 was 85, so the updater offers this build to anyone on 5.82 or
  older.
- 5.82 cut 2's automatic updating and the themed download screen, unchanged: check →
  download → verify → Android's installer, with the progress, speed and time-left readout.
- 5.81 rev 11.5's skip suggestions on every episode and updater v2, unchanged.

## Unchanged

Playback engine, catalogue parsing, mirrors, Discord presence, offline downloads, PiP,
profiles, the gallery, search filters, the hero deck, casting and every theme behave exactly
as they did in 5.82.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same signing
key. From this build on, updates can arrive by themselves — with a day of quiet first, unless
you turn that off.

## Verify

```
sha256sum Kuro-tv-5.83.apk
# b853e87b446d2abd15d20915c84a47966f5af6b892849a8c92371a164125a26a

apksigner verify --print-certs Kuro-tv-5.83.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK 44,008,672 bytes · source archive 7,255,078 bytes.
- `versionCode` stays 86 for the rest of the 5.83 line; 5.84 opens the next code.
- The window is measured from publication, so a release that has been out for a week installs
  on the next check no matter which delay you chose.
- Android decides when background work runs, so a window that ends at 3 a.m. may install at
  7 a.m. Opening Kuro runs a check straight away, which is the fastest way to force it.
