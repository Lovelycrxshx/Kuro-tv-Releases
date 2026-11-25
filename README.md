# Kuro TV — Releases

Kuro TV is an anime streaming app (Android). Latest source release: **1.5.6**
(versionCode 133, versionName `1.5.6`) with a unified multi-source search over
17+ streaming providers, HLS playback, mirror failover, and a themeable UI.

This repository hosts the release source artifacts.

## Repository Layout

| Path | Contents |
| --- | --- |
| `google_drive/v1.5.6/` | **Latest** source release: `Kuro-1.5.6-libs18-fastfail-vc133-source.zip`, source as `.txt`, and `SHA256SUMS.txt` |
| `google_drive/v4/` | Superseded v4 source artifacts (six features: domain sweeper, position-lock, brute-force quality, unified search default, live theme preview) |

## Latest Release (1.5.6)

- `Kuro-1.5.6-libs18-fastfail-vc133-source.zip` — full Android source, ready to build
- Source as plain text for quick browsing/diffing
- `SHA256SUMS.txt` — checksums for verification

### What's new in 1.5.6

1. **Additional settings in Account — no login required.** A new *Additional settings*
   row in the Account section opens themes, playback, privacy and updates for every
   visitor, signed in or not.
2. **Discord custom RPC requires a Discord login.** The Rich Presence and Presence
   customization panels now show a lock notice and disable their controls until a
   Discord account is connected in Profile → Account.
3. **Profile art: avatars and banners fully separate.** The combined Paired Sets view
   and Surprise pair action are removed. Every avatar (PFP) and every 16:9 banner is
   chosen independently. Tab counts are now dynamic (43 entries).
4. **Update button position fixed** for a clean fit in the header beside the other
   header controls.
5. Bug fixes and polish across the app.

> The APK is built by the maintainer from the source zip — see the Drive artifacts
> for the download.
