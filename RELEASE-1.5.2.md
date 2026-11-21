# Kuro 1.5.2

**versionCode 94 · 2026-11-19**

1.5.2 adds **Pure black (OLED)** surfaces across all twenty accents, layout-matched
**shimmer loading skeletons** on Home and Browse, a **Hidden titles** manager in
Settings, a complete **local `.json` Backup & restore** pass (including native
episode resume checkpoints and profile artwork), a monochrome Android status-bar
glyph, and an inline Android 13+ notification-permission prompt inside **Kuro
updates**.

## What changed

- **Pure black (OLED) mode.** Settings → Appearance and startup gains a **Pure black
  (OLED)** switch right beneath the twenty-colour Theme Picker. When App theme is
  Dark, turning it on switches both the WebView floor and the native Android
  window, status bar, navigation bar, dialogs, episode drawer and watch header to
  true `#000000` black with `#0A0A0E` elevated cards while keeping your chosen
  accent vibrant.
- **Home & Browse shimmer skeletons.** Loading shelves on Home and catalogue grids
  on Home, Browse and Simulcasts now display layout-matched `2:3` poster skeletons
  (and a full-height hero placeholder) instead of jumping vertically when artwork
  arrives. Shimmer animations pause automatically when `prefers-reduced-motion` is
  enabled, while screen-reader `aria-live` status lines remain intact.
- **Hidden titles manager.** Settings → Privacy and links adds a **Hidden titles**
  row showing how many anime you have hidden from Home, opening a sheet to restore
  individual titles in one tap or **Unhide all**.
- **Complete local `.json` Backup & restore.** Fixed a 1.1+ validation bug where
  backups containing `kuro-profile-art-v1` (`{avatar, banner}`) were rejected on
  import, added a one-tap **Backup & restore (.json)** shortcut in Settings →
  Quick actions, and expanded local export/merge to cover your 20-colour accent,
  OLED mode, hidden titles, download auto-clean schedule, and native episode
  playhead checkpoints (`KuroPlayback561`) so **Continue watching** resumes at the
  exact minute on a new phone.
- **Monochrome status-bar icon.** Replaced the opaque launcher icon on download,
  updater and playback-error notifications with a transparent-backed `24dp`
  monochrome vector (`@drawable/ic_stat_kuro`) tinted with your active accent, so
  Android no longer renders notifications as a solid white square.
- **Notification permission in Kuro updates.** On Android 13+ (or whenever app
  notifications are turned off), both the **Kuro update** screen and Settings →
  Kuro updates surface a one-tap **Allow notifications** prompt instead of failing
  silently in the background.
- **Slimmer source archive.** `Kuro-tv-1.5.2-Source.zip` packages only the clean
  Gradle source tree (~8 MiB) instead of bundling historical build outputs.

## Carried forward from 1.5.1 & 1.5

- **"You are on the latest version"** state and worker-thread checks in **Kuro
  updates**, plus the **3.6-second** cold-launch title sequence.
- **Continue watching** rail on Home, **long-press poster quick actions**,
  **Downloads upkeep**, **twenty accents** across web and native layers, and the
  **Discord Rich Presence Download button**.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same
signing key, so profiles, watch list, history, downloads and logins stay put.
`versionCode` 94 is above 93, so 1.0–1.5.1 offer this release automatically.

## Verify

```
sha256sum Kuro-tv-1.5.2.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.5.2.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · the source archive's size is in its `.sha256` companion.
