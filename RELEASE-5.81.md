# Kuro 5.81

`app.lovelycrxshx.kuro` · versionCode **84** · minSdk 26 (Android 8.0+) · targetSdk 35 (Android 15)

Signed with the original Kuro release key (`CN=Kuro Anime`), so this installs straight over
5.72 – 5.80. **Do not uninstall or clear data.**

```
SHA-256  541f1b73ad1ad32ec92a3bba880cedbd67a3a829a4b0f4c28632812be10c960a
Size     45,437,725 bytes
```

> **This is a re-cut of 5.81, same versionCode 84.** Because the version code did not change,
> the in-app updater will not offer it (it only offers strictly higher builds). Install this
> one manually — over the top, no uninstall — to get the casting screen, the new motion layer,
> the text-only profiles and the home hero deck.

## What's new

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
updater's behaviour are identical to the previous 5.81 build — this cut adds the cast screen
and the motion layer, nothing else. No playback or network behaviour was altered.

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
# 541f1b73ad1ad32ec92a3bba880cedbd67a3a829a4b0f4c28632812be10c960a

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
