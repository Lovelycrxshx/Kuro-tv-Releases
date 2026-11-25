# Kuro TV — Releases

Kuro TV is an anime streaming app (Android). Latest release: **1.5.6**
(versionCode 133, versionName `1.5.6`) with a unified multi-source search over
17+ streaming providers, HLS playback, mirror failover, and a themeable UI.

All releases — APK and source — are hosted directly in this repository under
`releases/`.

## Releases

| File | Contents |
| --- | --- |
| `releases/Kuro-tv-1.5.6.apk` | Kuro TV 1.5.6 APK (install over the top; same package & signing key) |
| `releases/Kuro-1.5.6-libs18-fastfail-vc133-source.zip` | Full 1.5.6 Android source, ready to build |
| `releases/Kuro-1.5.6-libs18-fastfail-vc133-source.txt` | 1.5.6 source as plain text for quick browsing/diffing |
| `releases/Kuro-1.5.6-vc133-SHA256SUMS.txt` | Checksums for verification |

## What's new in 1.5.6

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

See `release.md` for the full release notes and `RELEASE-1.5.5.md` for the 1.5.5
release notes.
