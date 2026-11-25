# Kuro roadmap — 1.x

Nothing here needs a server or an account: Kuro stays local-first. Each item ships as an
ordinary cut in the 1.x line — same `versionCode` climbed by one, released, and offered to
every install by the updater.

## Next up

1. **An update channel with a beta ring** — a published/beta split so a beta newer than the
   latest release is offered instead of reading "already up to date", which is what every 1.5
   beta did until 1.5 itself was published.
2. **DPAD / focus overhaul** — the highest-impact change left for Fire TV. A deliberate focus
   path on every screen, larger hit targets at TV density, and no focus ever lost to a
   decorative view.
3. **Airing calendar and new-episode notifications** — a schedule surface and opt-in per
   series, built on data the app already has.
4. **Theming v2 (TV overscan & Material 3 pass)** — a Material 3 token pass and overscan-safe
   margins for televisions (true AMOLED black shipped in 1.5.2).

## Shipped

- **1.5.4** — Cinematic Title Detail v2 (banner hero, franchise watch-order strip, filler
  badges & Skip Filler, batch offline downloads), YouTube-style Watch page (5 servers per page +
  Other servers switcher, 3-row scrollable episode playlist, YouTube Desktop horizontal
  recommended anime shelf), Smart Multi-Filter Browse & native Android Voice Search, Desktop-site
  Left-Rail & Android TV / Fire TV Leanback DPAD UI, Encrypted DNS (DoH) + 8-lane catalog engine
  + Multi-layer Ad-Guard, 40 curated anime & manga Profile Art sets (`art-11`–`art-50`), and
  Two-Way AniList / MyAnimeList Sync & Discord Hub.
- **1.5.3** — Universal OLED Segmented Chapters & Pills player skin across all server embeds,
  HLS, native Media3 and offline players, fullscreen bottom swipe-up Up Next & Episodes drawer,
  non-blocking App-UI Auto-Next floating card, and direct GitHub Releases `.apk` downloads across
  the repository `README`, `index.html`, and the Discord Rich Presence Download button.
- **1.5.2** — Pure black (OLED) mode across all twenty accents, Home & Browse shimmer loading
  skeletons, Hidden titles manager in Settings, full local `.json` Backup & restore (including
  profile art, theme/OLED, hidden titles, auto-clean and episode resume checkpoints), monochrome
  status-bar icon, inline Android 13+ notification permission in Kuro updates, and slimmed
  `Source.zip`.
- **1.5.1** — "You are on the latest version" state in Kuro updates, worker-thread update
  checks, and a smoother 3.6-second cold-launch title sequence.
- **1.5** — continue watching, long press on a poster, what's new after an update, downloads
  upkeep, twenty accents in Settings, the motion layer and the header update button.
- **1.4** — ten accents and a richer theme picker, with the native dialogs following.
- **1.3** — the WebView actually serves the overlay assets, so a theme can no longer ship dead.
- **1.2** — the native layer follows the theme.
- **1.1** — switchable theme colour.
- **1.0** — the version line restarted; the updater learns that 1.x follows 5.x.
- **5.83** — the update window: a release installs itself 24 hours after publication (48 by
  choice), or on the next check once that window has passed.
- **5.82** — automatic updating end to end, and the themed download screen.
- **5.81 rev 11.5** — skip suggestions on every episode, and updater v2 (pinned signer,
  resumable download).
