# Kuro 5.81

`app.lovelycrxshx.kuro` · versionCode **84** · minSdk 26 (Android 8.0+) · targetSdk 35 (Android 15)

Signed with the original Kuro release key (`CN=Kuro Anime`), so this installs straight over
5.72 – 5.80. **Do not uninstall or clear data.**

```
SHA-256  76eb0595ea88c064ed824fa98c8409ba05e3fe7f88f4e79b47a17d8bbd06613d
Size     43,963,616 bytes
```

> **This is the fifth cut of 5.81, same versionCode 84 — it fixes the launch crash in the
> fourth cut.** Because the version code did not
> change, the in-app updater will not offer it (it only offers strictly higher builds). Install
> this one manually — over the top, no uninstall — to get the Cast screen removed, a quieter
> Discord presence loop, the browsing card that says what it is, the profile art on the player
> chip and per-episode resume labels in the episode drawer.

## What's new

**Rev 11.1 fixes the launch crash.** The fourth cut closed immediately on open: the new profile-art
preference was read from a field initialiser, which runs while the Activity is still being built,
before Android has given it a context — so `getSharedPreferences` threw and the process died. The
value is now read in `onCreate`, and the build workflow refuses to ship a build whose Activity
reaches for a context before `onCreate`.

**Rev 11 removes Cast and quiets the Discord presence.** The "Cast" button in the player is gone,
along with the cast screen, its theme, its intent hand-back and the Google Cast SDK dependency —
about 2 MB less to install and nothing that sends a stream URL to a TV.

- **Presence: less noise on the main thread.** The SDK pump used to wake every 100 ms no matter
  what; it now polls at 150 ms only while an update is actually moving, 500 ms while something is
  playing, and 1.5 s while the app is idle.
- **Presence: an honest browsing card.** With nothing playing it says "Looking for an anime to
  watch" over "Kuro.tv" instead of the old "Browsing Kuro / In the app" — and if you hide titles,
  it stays neutral ("In the app").
- **Presence: long episode names no longer end in a dangling quote.** Names are clipped to the
  120-byte line with an ellipsis, inside the quotes.
- **Presence: diagnosability.** The status card shows "Last accepted update" as *2 min ago* instead
  of a wall-clock stamp, counts down the retry while an update keeps failing, and history entries
  carry the SDK result code.
- **Presence: background window.** New setting — stop shortly after you leave Kuro (the old
  12-second behaviour, still the default), keep for 1 or 5 minutes, or keep updating while audio
  plays in the background.
- **Presence: "Rewatching"** appears when you open an episode that already had saved progress.
- **Profile art reaches the player chip.** The signed-in chip in the watch header shows your
  chosen avatar instead of initials, and falls back to initials when you pick none.
- **Backup covers the art choice.** "Profile art" is now part of a settings backup and restore.
- **Episode drawer shows per-episode progress.** Episodes you have started carry a "Resume 12:34"
  label, so you can tell where you left off without opening each one.

**Rev 9 brings back profile art — as a curated anime pack.** Five avatars and five matching
banners, drawn for Kuro in the app's palette: a flat-vector chibi bust on a solid colour tile for
the avatar, and a wide scene from that same character's world for the banner, so a pair always
looks like a set.

- **Pick it on the Account page** — "Profile art" sits under your activity strip: tap an avatar,
  tap a banner, both apply instantly. "Initials" and "None" clear the choice and give you the old
  monogram back.
- **Offline and on-device** — the ten WebP files ship inside the APK (about 470 KB) and the choice
  is stored locally; nothing is uploaded and nothing is fetched.
- **It survives re-renders** — the picker paints itself onto the profile page every time the page
  is drawn, including after the edit toggle, so your art cannot be lost by navigating.
- **One avatar everywhere** — the choice is not confined to the profile hero: the same art fills the
  header chip in the top bar and the round avatar in the account drawer, and it comes back after
  either of them is rebuilt.
- **Still no uploads** — there is no picture picker, no text field for a URL and no banner
  cropping UI. It is pick-from-the-pack only.

The banner crops from the left, so the character on the right third stays visible behind the
avatar on wide screens and the scene survives a centre crop on phones.

**Rev 8 fixes the four items that looked unchanged.** The hero deck, the NEW chip and the search
filters had been added to the classic home renderer in `app.js` — but the shipped app re-points
`home` and `searchPage` at the unified layer in `unified533.js`, so none of that code ran. All
four now live on the surfaces the app actually draws:

- **Hero deck** — `unifiedSpotlight()` feeds the shared deck from the top five ranked titles, falls
  back to your recents when the popularity lists are blocked, suppresses a title that is already
  on the first slide, and clears for every re-render, so it is never the old static block. Posters
  on the freshly-added shelf keep the **NEW** chip.
- **Search filter chips** — a chip bar above the results on the browse page: Format (TV, Movie,
  ONA, OVA, Special) and Year, each counted from the titles that have loaded, plus a
  "N of M loaded" line and a clear action. Facets come from each title's own metadata, so a chip
  only exists when the catalogue reports it. Filtering runs through the pager's item hook, so a
  page that loads after you tap is filtered too.
- **Player gestures and HUD** — in the native player, double-tap the left third to go back 10 s,
  the right third to go forward 10 s, the middle to play or pause; drag vertically on the left half
  for brightness and on the right half for volume, each showing a short fading readout. A single
  tap still opens the player's own controls.
- **Episode drawer** — a new Episodes icon in the watch header opens a right-hand drawer listing
  every episode, the current one pinned and marked with a play glyph and scrolled into view, a close
  button and a scrim, and the hardware back key closes it. Picking an episode switches without
  leaving the player. The existing cancellable "Next in 8 s" countdown is unchanged.

**Profiles are text only.** Every profile picture and banner is gone — the artwork picker, the
character presets, the &ldquo;Your art&rdquo; strip, the change-banner button and the picture uploads
have all been removed. Your profile is your username and a monogram of its initials, in the account
page, the profile drawer and the player's header chip. Old backups still import: only the saved
name is restored, and anything left over is dropped. Removing the artwork took 483 image files out
of the APK, which is why this build is **about 9 MB smaller** than the last one.

**The home hero is a deck.** The top of Home is now up to five real catalogue titles shown as
rounded artwork with a gradient copy block, the title, one Watch pill each, page dots that follow
the scroll, and swipe between slides. It auto-advances every 7 seconds but pauses on touch, in the
background, and when Android's animations are turned off. Posters on the freshly-added shelf carry
a **NEW** chip.

**Casting to Google TV actually works now.** The old hand-off was a bare list of device names
that dropped you on the TV with no controls and threw away your position on the phone.

- **Themed cast panel** — palette-matched sheet, device name, and a live state line
  ("Casting", "Paused", "Buffering", "Idle") read straight from the TV's media status.
- **Real timeline** — a 1-second ticker and a seek bar (1/1000 resolution, seeks on release)
  that stays in step with the TV, clamped just short of the end.
- **Transport that obeys the TV** — Play/Pause and Stop act on the session; the panel keeps
  playing on the TV or resumes locally with **Keep playing on TV** / **Resume here**.
- **Background continuation** — a `MediaSession` plus a media-style notification
  (channel `kuro_cast_582`) so the phone can be locked or put away while the TV keeps playing.
- **Position hand-back** — returning from the cast seeks the phone player to where the TV was
  left, and only if it actually moved (≥2 s), so you never lose your place.
- **Settings → Cast handoff** shows the same themed surface, focusable with a visible ring.

**A single motion language across the whole shell** (`assets/smooth-ui.css` + `smooth-ui.js`):

- spring-based press feedback, keyboard-only focus rings, and a blurred sticky header that
  gains a shadow as you scroll;
- entrance animations for dialogs, hero banners and page content (staggered, not all at once);
- **profile-section polish** — banner parallax that is transform-only (no repaint), an avatar
  grid that sweeps in when you change artwork, cards that lift on hover/focus, and artwork that
  fades in as it decodes instead of popping;
- crossfade page transitions where the WebView supports them, a `.ku-pill` toast helper, and
  skeleton shimmer for loading rows;
- everything collapses to no-motion under Android's *Remove animations* setting, and the
  haptic tick on press is a single 6 ms pulse.

**The account page now continues where you left off.** The last three opened episodes sit as
tappable cards (art, title, `Ep 12 · name`) right under your activity — one tap resumes. A quiet
streak line counts consecutive days with something opened; missing today does not break it, a
missed day resets it without a word, and it only appears at two days or more.

**The account page now answers "how much have I watched?"** — a three-up strip under the
hero (episodes, series tracked, saved bookmarks) built from data the page already had, a
"Your art" row showing the current avatar and banner choices that opens the artwork picker
on the right tab, and press feedback on the avatar chip. Nothing new to store, nothing new
to sync.

**Profiles and settings keep the app's own palette.** The 5.81 dialog toolkit is unchanged and
still covers every native alert — no stock-Material grey left in settings.

## Unchanged

Playback engine, catalogue parsing, mirrors, Discord presence, downloads, PiP and the
updater's behaviour are identical to the previous 5.81 builds — this cut adds the four wired fixes above,
nothing else. No playback or network behaviour was altered.

## Install

1. Install **Downloader** by AFTVnews.
2. Enable *Install unknown apps* for Downloader.
3. Paste this release's APK URL into Downloader and install.

Or sideload:

```bash
adb install -r Kuro-tv-5.81.apk
```

## Verify

```bash
sha256sum Kuro-tv-5.81.apk
# 76eb0595ea88c064ed824fa98c8409ba05e3fe7f88f4e79b47a17d8bbd06613d

apksigner verify --print-certs Kuro-tv-5.81.apk
# CN=Kuro Anime
# SHA-256 1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E
```

Built against Android 15 (API 35) with 16 KB page alignment: all 8 native libraries pass the
`zipalign -P 16` check. Signature schemes v2 + v3.

## Notes

- Kuro is a player / front-end. It hosts no media; catalogue and stream access happens locally
  on your device through the public sources you point it at.
- Licensed under GPL-3.0. Corresponding source for this build is attached as
  `Kuro-tv-5.81-Source.zip`.
