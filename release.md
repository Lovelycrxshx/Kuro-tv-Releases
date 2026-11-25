# Kuro TV — Release Notes

Latest release: **1.5.6** (versionCode 133) — source available on GitHub Releases
and Google Drive.

## Latest — 1.5.6

### Updates

- **Additional settings in Account, no login required** — a new *Additional
  settings* row in the Account section opens themes, playback, privacy and
  updates for every visitor, signed in or not.
- **Discord custom RPC now requires a Discord login** — the Rich Presence and
  Presence customization panels show a lock notice and disable their controls
  until a Discord account is connected in Profile → Account.
- **Profile art: avatars and banners fully separate** — the combined Paired
  Sets view and Surprise pair action are removed; every PFP and every 16:9
  banner is chosen independently. Tab counts are now dynamic (43 entries,
  previously hardcoded 40).
- **Update button position fixed** — the header update button is repositioned
  for a clean fit beside the other header controls.
- Bug fixes and polish across the app.

### Features

- Unified multi-source search across 17+ streaming providers (default at
  startup)
- Domain health sweeper — probes 20 streaming domains every 10 minutes and
  prunes dead mirrors automatically
- Position-lock on server switch — remembers playback position across
  failover server switches
- Brute-force quality render — 1080p → 720p → 480p → 360p fallback on
  playback errors (HLS and direct URLs)
- Live theme accent preview — real catalogue, player and dialog samples that
  update instantly per accent swatch
- Teal default theme with 43 profile-art entries (avatars + matching 16:9
  banners), custom gallery uploads, and live preview stage
- Schedule, notifications, imports (MyAnimeList / AniList), tracker sync,
  backup/restore, and Discord Rich Presence

### Download

- `Kuro-1.5.6-libs18-fastfail-vc133-source.zip` — full Android source, ready
  to build (the APK is built by the maintainer from this zip)
- `Kuro-1.5.6-libs18-fastfail-vc133-source.txt` — source as plain text
- `Kuro-1.5.6-vc133-SHA256SUMS.txt` — checksums for verification

## Version history

| Version | versionCode | Highlights |
| --- | --- | --- |
| 1.5.6 | 133 | Additional settings (no login), Discord RPC login gate, separate PFP/banners, update button fix |
| 1.5.5 | 132 | Domain sweeper, position-lock, brute-force quality, unified search default, live theme preview |
| 1.5.4 | 108 | Classic-player 1.5.4 final |
| 1.5.3 | — | 1.5.3 release |
| 1.5.2 | — | 1.5.2 release |
| 1.5.1 | — | 1.5.1 release |
| 1.5 | — | 1.5 release |
| 1.4 | — | 1.4 release |
| 1.3 | — | 1.3 release |
| 1.2 | — | 1.2 release |
| 1.1 | — | 1.1 release |
