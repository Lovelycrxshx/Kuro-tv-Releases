# Kuro 1.1 — 30 candidate features and improvements

> **Selected by the owner for 1.1 — do not start until they say "now".**
>
> 3 · 6 · 9 · 12 · 13 · 15 · 20 · 26 · 27 · 28 · 29
>
> 3 Audio track picker with memory · 6 Auto-next countdown · 9 Opt-in level readout ·
> 12 Continue-watching rail v2 · 13 Search v2 · 15 Library sort and filters ·
> 20 Artwork cache controls · 26 Update channels · 27 In-app "What's new" ·
> 28 Signed update manifest · 29 Home v2 skeletons
>
> Not selected: 1, 2, 4, 5, 7, 8, 10, 11, 14, 16, 17, 18, 19, 21, 22, 23, 24, 25, 30.

> Also requested (no "now" needed, waiting on design choices): **more profile pictures and
> banners**, and **a change of the app's UI accent colour**. Current accent is violet
> `#a65be4` (`0xffa65be4` native), panel `#100c18`, ink `#f3eef7`, muted `#b7a4c7`.
> Existing art pack: 10 WebP files — 5 chibi avatars with a matching wide banner each.

Everything below is buildable inside the current architecture: a WebView UI over native
Media3/ExoPlayer, no server, no account required, distributed as a signed APK on GitHub
Releases. Effort is relative (S ≈ a day's work inside one cut, M ≈ a cut or two, L ≈ a
release of its own).

## Playback and subtitles

1. **Subtitle offset** — nudge ±0.1 s to ±30 s, remembered per series and per episode, with a
   one-tap reset. Fixes the "this release's subs are 2 s early" problem that no player setting
   solves today. *M · both*
2. **Subtitle styling v2** — font family, size, weight, outline vs background box, position
   (bottom / raised / top). `KuroCaptionStyle571` already owns the model; this is the UI and the
   extra presets. *S · both*
3. **Audio track picker with memory** — choose dub or sub (and commentary, when a release has
   one) once, and have it remembered per series instead of per session. *S · both*
4. **Playback speed memory and presets** — 0.25× steps, per-series memory, and speed shown in
   the player HUD instead of hidden in a menu. *S · both*
5. **Skip ranges drawn on the seek bar** — the intro and outro windows (real markers and
   suggestions, in different colours) as ticks, so you can see what the chip will do before you
   tap it. *S · TV especially*
6. **Auto-next with a visible countdown** — "Next episode in 10 s — Cancel", starting after the
   outro range ends when one exists, instead of a silent jump. *S · both*
7. **Gesture tuning screen** — seek step (5/10/15/30 s), brightness/volume sensitivity, double-tap
   zones on or off; all the gesture code exists, this makes it adjustable. *S · phone/tablet*
8. **Background audio mode** — keep the audio playing with the screen off, with a media
   notification; the native player already has the service plumbing. *M · phone*
9. **Level readout (opt-in)** — current HLS level, resolution, dropped frames and buffer health
   in a compact overlay for diagnosing a bad night. *S · both*
10. **Per-series video fit memory** — crop / fit / stretch remembered per series, with a "reset all
    fits" action, instead of one global setting. *S · both*

## Discovery and library

11. **Airing calendar** — a month/week view of what airs next for the series you follow, built
    from metadata already cached, plus opt-in new-episode notifications. *L · both*
12. **Continue-watching rail v2** — thumbnails, "resume at 12:34", episode title, and a
    per-device "finished" state that the account page and home agree on. *M · both*
13. **Search v2** — recent searches, fuzzy title matching, and genre/studio/year filters alongside
    the existing format and year chips. *M · both*
14. **Browse by genre and studio** — real browse pages from the catalogue instead of chips that
    only filter what happens to be loaded. *M · both*
15. **Library sort and filters** — alphabetical, recently watched, never started, completed,
    dropped; remembered per device. *S · both*
16. **Tracker conflict review** — when local progress and the tracker disagree, show both values
    side by side and let you pick, instead of the last write winning. *M · both*
17. **Up-next queue** — drag-to-reorder queue per series that survives a restart and is honoured
    by auto-next. *M · both*
18. **Local recommendations** — "Because you watched…" computed from history and cached metadata,
    no server, no upload. *M · both*
19. **Download manager v2** — pause/resume all, a storage cap, and auto-delete of watched episodes
    with a grace period. *M · both*
20. **Artwork cache controls** — cap the cache size, preload the next shelf only, and a clear
    action that does not touch your library. *S · both*

## Remote and TV

21. **DPAD and focus overhaul** — a deliberate focus order on every screen, larger hit targets at
    TV density, focus restored on back, no focus lost to decorative views. The single biggest
    quality gap left on Fire TV. *L · TV*
22. **Overscan calibration** — a screen where you shrink the safe area with the remote until the
    corner markers are visible, saved per device. *S · TV*
23. **Long-press context menu on posters** — play, details, add to list, download, without
    leaving the shelf. *M · TV*
24. **Remote shortcuts and hold gestures** — media keys (play/pause/next/prev) mapped inside the
    player, double-BACK to leave the player, hold-OK for the player menu. *S · TV*
25. **Watch-next row on the TV launcher** — publish the continue-watching queue through
    `TvProvider` so it appears on the Android TV home screen and Fire TV's recents. *M · TV*

## Updates and trust

26. **Update channels** — Stable and Beta, where Beta follows prereleases; the versionCode gate and
    the update window apply per channel. *M · both*
27. **In-app "What's new"** — after an update, a dismissible screen with the release notes, so the
    release notes actually reach people. *S · both*
28. **Signed update manifest** — publish a small `update.json` (version, SHA-256, minimum SDK) and
    verify its signature against an embedded public key, on top of the release JSON and the
    APK's own certificate. *M · both*

## Performance and identity

29. **Home v2 skeletons** — placeholders shaped like the final layout, so the home screen does not
    jump when artwork lands; plus a "reduced motion" switch that respects the system setting. *M · both*
30. **Startup budget and low-end mode** — measure cold start, defer Discord init and non-critical
    WebView layers, and offer a low-end profile that trims animations and image sizes. *M · both*

---

## Suggested 1.1 scope

A believable release is eight to ten items, not thirty. The cut I would ship:

| Item | Why now |
| --- | --- |
| 21 DPAD / focus overhaul | The one thing Fire TV users feel on every button press |
| 29 Home v2 skeletons | Removes the biggest visual jank on slow devices |
| 1 Subtitle offset | The most-requested playback fix in practice |
| 5 Skip ranges on the seek bar | Makes the existing skip work legible |
| 6 Auto-next countdown | Turns a surprise into a choice |
| 22 Overscan calibration | Cheap, and fixes "the edges are cut off" once per TV |
| 27 In-app What's new | Small, and makes every later release land better |
| 26 Update channels | Turns the release pipeline into a testing tool |

Everything else stays on the roadmap and ships in 1.2 onward, one or two per cut.
