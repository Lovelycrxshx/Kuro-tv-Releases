# Kuro 1.5.1

**versionCode 93 · 2026-11-19**

1.5.1 is a polish and updater fix release on top of 1.5: **Kuro updates** now
tells you clearly when you are already on the latest build instead of getting
stuck on a stale `100%` bar, and the opening title animation plays for a full
**3.6 seconds** from frame one.

## What changed

- **"You are on the latest version" in Kuro updates.** When an update check finds
  no newer release on GitHub, the **Kuro updates** dialog now displays an
  **UP TO DATE** chip and **"You are on the latest version"**, hides the progress
  bar, and clears any leftover byte counters from an earlier download so a stale
  `100%` readout and perpetually pulsing indeterminate bar never linger on screen.
- **Quieter, non-blocking update checks.** Tapping the header update arrow runs
  the check on a background worker thread (`checkAndMaybeInstall582`) so the UI
  stays responsive while GitHub answers, and background schedule ticks no longer
  pop the update dialog uninvited.
- **Longer 3.6-second opening animation.** The launch sequence (`#km-intro-15`)
  now holds for **3.6 seconds** (`INTRO_HOLD = 3600`, `.km-iris` wipe at `3.0s`
  for `0.54s`) with a deliberate staggered entrance across the poster cards,
  accent sweep and wordmark. Poster card image population was fixed so freshly
  loaded catalogue posters fill into the intro deck up to 1.8 s after start-up,
  and the native 2-second black boot veil (`dismissEntry15`) lifts as soon as the
  WebView renders so the opening sequence is never hidden behind it. Tap-to-skip
  and `prefers-reduced-motion` still bypass the intro immediately.
- **What's new updated for 1.5.1.** Home shows a one-time **New in 1.5.1** card on
  first launch after updating, and the same notes remain available in Settings →
  Developer → **What's new**.

## Carried forward from 1.5

- **Continue watching** rail on Home with episode number, accent progress bar and
  saved playhead minutes.
- **Long press a poster** anywhere for **Open**, **Add to / Remove from My List**
  and **Hide this title**.
- **Downloads upkeep** in Settings → **Manage downloads** (summary, delete watched,
  keep latest per series, delete all, and 7-day / 30-day auto-clean).
- **Twenty accents** across both the web UI and native dialogs, bars, player panel
  and offline screen.
- **Download button on Discord Rich Presence** linking to the
  [download page](https://lovelycrxshx.github.io/Kuro-tv-Releases/), with a Show /
  Hide toggle in Settings → Discord Rich Presence → Presence customization.

## Honest notes

- **Source archive.** `Kuro-tv-1.5.1-Source.zip` is the 1.0 tree from Drive plus
  the 1.1 assets, the 1.5 / 1.5.1 motion and feature layer, the native palette
  rewrite and the shell patches — the exact program compiled into the APK.
- **In-app upgrade from 1.5.** Because `1.5.1` (`versionCode` 93) compares above
  `1.5` (`versionCode` 92), devices running 1.0–1.5 are offered 1.5.1 automatically
  by the in-app updater.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same
signing key, so profiles, watch list, history, downloads and logins stay put.
`versionCode` 93 is above 92, so 1.0–1.5 offer this release normally.

## Verify

```
sha256sum Kuro-tv-1.5.1.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.5.1.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · the source archive's size is in its `.sha256` companion.
- Feature #1 (a real published/beta update channel) and #4 (a "My week" airing view)
  remain queued on [ROADMAP.md](ROADMAP.md).
