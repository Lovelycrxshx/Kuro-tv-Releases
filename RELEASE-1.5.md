# Kuro 1.5

**versionCode 92 · 2026-11-20**

1.5 is the cut where the app starts remembering where you were, **twenty accents**
you can switch from Settings, a motion layer that is actually alive, and a poster
you can hold.

## What changed

- **Continue watching.** Home now opens with one rail of the episodes you have
  actually started, newest first: the poster, the episode number, a progress line
  in your accent colour and how many minutes you are in. Tap a card and it resumes
  that episode. The rail replaces the old "Recently watched" row instead of
  repeating it, and it disappears completely when there is nothing in progress.
  Minutes are read from the player's own saved position — if the shell has no
  position for an episode, the card simply drops the time rather than guessing.
- **Long press a poster.** Hold for about half a second on any poster — Home, My
  Lists, search results, a title page rail — and a compact sheet opens with
  **Open**, **Add to / Remove from My List** and **Hide this title**. A drag past
  12 px cancels it, so horizontal scrolling is never eaten, and a long press never
  also opens the title behind the sheet. Focus lands on the first row, Escape
  closes, and focus goes back to the poster, so it works from a keyboard or a
  remote.
- **What's new.** The first launch of a new build puts one card at the top of Home
  with what changed and a **Got it** button. It shows once per build. The same list
  stays in Settings → Developer → **What's new**.
- **Downloads upkeep.** Settings → **Manage downloads** opens a real summary —
  how many episodes, how much space — with **Delete watched episodes**, **Keep only
  the latest episode of each series**, **Delete all downloads**, and an auto-clean
  switch (off / 7 days / 30 days) that quietly sweeps finished and watched
  downloads at start-up. An episode that is open in the player is never deleted.
- **Twenty accents, chosen in Settings.** Signature (Teal, Violet, Plum, Graphite),
  Cool (Sky, Ice, Royal, Sapphire, Indigo, Emerald, Mint, Lime) and Warm (Citron,
  Gold, Amber, Sunset, Coral, Rose, Crimson, Magenta). Teal is still the default.
  The picker lives in Settings → Appearance, with a live preview strip and the
  current accent marked. The native half follows: status and navigation bars,
  dialogs, the player panel, watch header, episode drawer and the offline screen.
- **A motion layer.** The opening title sequence, press springs, scroll-linked
  shelves and the richer surface treatment ship as one layer. There is no ripple,
  and the Home banner stays out of the way.
- **One update button.** The header arrow is the only entry point to the updater.
  The Account "Keep Kuro current" card and the LOCAL/private profile card are gone
  — removed, not hidden.

## Honest notes

- **Two actions the spec listed are not here.** A long press offers Open, My List
  and Hide — no separate *Mark watched*, because it would bypass the app's own
  watched sync, and no separate *Not interested*, because it is the same thing as
  Hide. Hide is per phone and reversible from the same sheet.
- **The rail holds eight cards, not six**, and it replaces the app's history row.
- **Downloads are cleaned through the shell's own download index**, not a rewritten
  Downloads screen: the sheet drives `downloadsSummary15` / `downloadsClean15` and
  the app's existing per-episode downloads row does the actual removal. "Watched"
  means the episode was opened at least once and has no saved position left.
- **The beta builds you tested say `1.5-beta`; this one says `1.5`.** Same code,
  same bytes otherwise. A phone already on `1.5-beta` will not be prompted, because
  the version numbers compare equal — that is deliberate.
- **The updater still reads the public release page.** Before this release it
  pointed at 1.4, so every 1.5 beta reported "already up to date". With 1.5
  published, 1.4 and older installs get offered this build normally; the separate
  beta channel is still queued.
- **Source archive.** `Kuro-tv-1.5-Source.zip` is the 1.0 tree from Drive plus the
  1.1 assets, the 1.5 motion and feature layer, the native palette rewrite and the
  shell patches — the same program that is in the APK, built by the same workflow.

## Install

Install over the top. **No uninstall.** Same package `app.lovelycrxshx.kuro`, same
signing key, so profiles, watch list, history, downloads and logins stay put.
`versionCode` 92 is above 91, so 1.0–1.4 offer this release normally.

## Verify

```
sha256sum Kuro-tv-1.5.apk
# {{APK_SHA256}}

apksigner verify --print-certs Kuro-tv-1.5.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

## Notes

- APK {{APK_BYTES}} bytes · the source archive's size is in its `.sha256` companion.
- Feature #1 (a real published/beta update channel) and #4 (a "My week" airing view)
  are still queued and are not in this build.
