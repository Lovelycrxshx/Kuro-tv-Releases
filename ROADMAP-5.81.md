# Kuro 5.81 — improvement backlog

20 concrete improvements, ordered by what actually breaks the app for users on a Fire TV
stick or a phone. Baseline for every item is **5.80**: Media3 1.7.1, Discord Social SDK
1.11.24259, offline downloads, Cast (default receiver), PiP, in-app updater, WebKit
catalogue over the `hianime.fm` → `hianime.al` → `hianimes.es` fallback chain, Anime4K,
API 35 + 16 KB pages.

Effort is rough single-dev time: **S** ≤ 2 days · **M** 3–5 · **L** 6–10 · **XL** > 10.

| # | Improvement | Tier | Effort | Why it matters |
|---|---|---|---|---|
| 1 | Mirror health, ranking & circuit breaker | Reliability | M | Dead mirror = dead app; today it's a blind sequential try |
| 2 | Offline-first catalogue cache (Room) | Reliability | L | App is unusable with no network or while a host is down |
| 3 | Playback failure recovery at the same timestamp | Reliability | M | Mid-episode failure currently means hunting for your place |
| 4 | Resume & Continue-watching v2 | Reliability | M | Half-finished episodes are the #1 returning-user flow |
| 5 | Real subtitle support (ASS/SSA, styling, offset) | Playback | L | Anime is subtitle-first; SRT-only is a hard ceiling |
| 6 | Skip intro / skip outro | Playback | M | The single most-requested convenience in anime clients |
| 7 | Downloads v2 (queue, retry, quota, season) | Downloads | L | Current service is one-shot; no resume, no bulk, no limits |
| 8 | DPAD / focus overhaul for Android TV | UX | M | Focus jumps and dead ends are what "feels cheap" |
| 9 | Search v2 (offline index, fuzzy, filters, voice) | UX | M | Search is the second-most used screen after Home |
| 10 | Home v2 — rows, skeletons, no layout shift | UX | M | First screen decides retention; blank flash looks broken |
| 11 | Airing calendar + new-episode notifications | UX | M | Gives people a reason to open the app on a schedule |
| 12 | Profiles + kids mode with PIN | UX | L | Shared living-room device, mixed watch histories |
| 13 | Sleep timer, autoplay next, queue | UX | S | TV habit: fall asleep watching, wake up somewhere sane |
| 14 | Theming v2 (Material 3, AMOLED, overscan-safe) | UX | M | Cheap polish with high perceived value |
| 15 | Discord RPC v2 (privacy, buttons, backoff) | Integrations | S | 5.80 shares too little and can rate-limit |
| 16 | Cast & Google TV v2 | Integrations | L | Cast today is a bare receiver; no queue, no remote |
| 17 | Updater v2 — signature-pinned, resumable | Trust & safety | M | An updater that installs an unverified APK is a backdoor |
| 18 | Startup & size (baseline profile, R8, ABI split) | Performance | M | 55 MB and slow cold start hurt install conversion |
| 19 | Release pipeline v2 (reproducible, SBOM, smoke tests) | Ops | M | 5.80's publish flow was hand-verified; make it boring |
| 20 | Privacy & diagnostics (pin, no trackers, opt-in logs) | Trust & safety | M | Users of a sideloaded app need to be able to trust it |

---

## Tier 1 — Reliability & playback

### 1. Mirror health, ranking & circuit breaker
**What.** Replace the sequential "first host that answers wins" walk with a prober that ranks
hosts and remembers which ones fail.

- Probe all three hosts in parallel at launch (`HEAD /home`, 2.5 s budget) and cache a ranked
  list per network fingerprint (SSID / carrier / country) for 10 min.
- Circuit breaker per host: 3 consecutive failures → skip it for 15 min, then half-open with one
  probe. Log the transition so the user sees "hianime.fm unavailable, using hianime.al".
- Prefer the host that served the *last successful stream*, not just the last successful page.
- Expose a manual "switch source" in settings for when every probe lies.
- **Done when:** with two hosts blackholed via firewall, launch-to-catalogue is under 4 s and the
  UI names the live host.

### 2. Offline-first catalogue cache (Room)
**What.** Home, search results, show pages and episode lists survive a dead network.

- Room tables: `shows`, `episodes`, `sources`, `query_history`; store the normalised model, never
  raw HTML.
- Render cache immediately with a "updated 2 h ago · offline" chip, then revalidate in the
  background (stale-while-revalidate) and diff into the UI without a full reload.
- Cap the cache (e.g. 5 MB, LRU by last-viewed) so it never grows unbounded.
- **Done when:** airplane mode shows the previous Home and any cached show page instead of an error.

### 3. Playback failure recovery at the same timestamp
**What.** A mid-episode error should be invisible.

- Catch `PlaybackException` by type: `ERROR_CODE_IO_*` → retry once with backoff, then re-resolve
  the stream on the *next* healthy mirror and `seekTo` the position captured before the failure.
- Surface a one-line snackbar ("stream dropped, resumed on hianime.al") instead of a full-screen
  error, and only escalate to the error screen if all sources are exhausted.
- Track these events in the (opt-in) diagnostics log so field reports are actionable.
- **Done when:** killing the active host mid-episode resumes within ~5 s with no user action.

### 4. Resume & Continue-watching v2
**What.** Precise, per-episode, and shared across devices.

- Use Media3's `ResumptionStore` semantics in Room: `(showId, episodeId) → positionMs, durationMs,
  updatedAt, completedAt`.
- Mark watched at ≥ 90 %, never resume in the last 30 s, and offer "next episode" from the player.
- Keep in-progress *and* finished-next-up separate so a re-watch doesn't clobber history.
- Optional cross-device sync (the Supabase project already connected to this workspace can hold
  `watch_progress` rows keyed by an anonymous install token — no account required, opt-in).
- **Done when:** force-stop, relaunch, open the show: the Continue row resumes within 1 s of the
  last position, on any device sharing the token.

### 5. Real subtitle support (ASS/SSA, styling, offset)
**What.** Ship `libass` via a Media3 subtitle parser, not just SubRip/WebVTT.

- Bundle `libass` through NDK 27 (already in the toolchain) with 16 KB-aligned `.so` files —
  keep the alignment gate from 5.80.
- Honour font size, background opacity, outline, bottom inset; persist per-profile overrides.
- Manual sync offset ±10 s in 0.1 s steps, saved per show.
- Remember chosen language/track per show ("always prefer English subs").
- Convert ASS to SRT only as a *fallback* when rendering fails, never the default.
- **Done when:** a typeset ASS episode renders correctly on a Fire TV Stick 4K and survives a
  16 KB-page audit.

### 6. Skip intro / skip outro
**What.** A "Skip intro" button and seekbar marker.

- Fetch segment timestamps from AniSkip's public API by MAL/AniList id; cache per episode in Room;
  allow a manual "mark intro start/end" when no data exists.
- Show a focusable button for the last ~40 s of the window, dismiss on press, and fold the button
  into DPAD order *before* the seekbar so TV remote users reach it in one press.
- Also expose the markers to the seekbar preview so users can see where they are.
- **Done when:** popular shows show a working intro marker within 24 h of an episode airing.

## Tier 2 — UX & living room

### 7. Downloads v2 (queue, retry, quota, season)
**What.** Turn the one-shot foreground service into a real download manager.

- `WorkManager` unique work per episode, `NetworkType.UNMETERED` by default, exponential backoff,
  resume interrupted downloads via Media3 `DownloadManager` (already available) instead of
  restarting from zero.
- Queue UI with pause/resume/cancel-all, per-item progress, and a storage budget ("keep 10 GB").
- "Download season" and "download next 5 episodes" actions; quality selector per download.
- Integrity check on completion (size + container probe) and a "verify library" action.
- **Done when:** pulling the plug mid-download resumes from the byte offset on reconnect.

### 8. DPAD / focus overhaul
**What.** Make the remote feel deliberate.

- Persist focus per screen, restore on back-navigation, and never drop focus to the top-left.
- Explicit `nextFocusForward/Up/Down/Left/Right` on the player controls, poster grids, and rows.
- Long-press (OK held) opens the context menu: details, add to list, download, mark watched.
- Back should pop the player overlay first, then the screen — not exit the app.
- Hardware keys: `MEDIA_PLAY_PAUSE`, `MEDIA_NEXT/PREVIOUS`, `KEYCODE_TV_*`; ignore spurious
  `KEYCODE_BACK` double-fires.
- **Done when:** every screen is fully traverseable with 4 directions + OK, with a visible focus
  ring at all times (checked on a 1080p TV at 3 m).

### 9. Search v2
**What.** Fast, offline-tolerant, filtered.

- Local n-gram index over the cached catalogue for instant prefix results (≤ 100 ms), with remote
  search filling in behind it.
- 250 ms debounce, cancellable requests, query history with one-press clear, and typo tolerance
  (trigram similarity) plus romaji/english alias matching.
- Filters: genre, year, sub/dub, movie vs series, airing status; sort by newest/popularity.
- Voice search: handle `ACTION_RECOGNIZE_SPEECH` / TV's `SEARCH` intent so the leanback launcher
  and Google TV voice button land in Kuro's search.
- **Done when:** typing 3 characters shows results from cache before the network answers.

### 10. Home v2 — rows, skeletons, no layout shift
- Ordered rows: Continue watching → New episodes (followed) → Trending → Genres → My list.
- Skeleton placeholders with fixed heights so posters never jump; image loads via Coil with
  memory+disk cache and a low-res placeholder.
- Empty states that say what to do ("Nothing here yet — browse Trending"), not blank panels.
- Optional "compact posters" density setting for small TVs.
- **Done when:** cold start to interactive Home is under 1.5 s with zero layout shift after load.

### 11. Airing calendar + new-episode notifications
- Follow list in Room; a daily `WorkManager` sync of airing schedules.
- Local notifications (channel "New episodes"), deep-linking straight to the episode, with a
  per-show mute and a global off switch. No push infrastructure, no server.
- In-app "This week" calendar screen grouped by weekday.
- **Done when:** a followed show notifies within one daily sync of its episode appearing.

### 12. Profiles + kids mode
- Up to 4 local profiles (name + avatar), each with its own history, list, downloads and settings.
- Kids profile: content-rating filter, no Discord RPC, no downloads of mature titles, and a PIN
  gate to leave the profile.
- Storage: one Room DB, `profileId` on every user table; migration must preserve the current
  single-profile data as "Profile 1".
- **Done when:** switching profiles swaps Continue rows instantly and migration leaves existing
  history intact.

### 13. Sleep timer, autoplay next, queue
- Sleep timer: 15/30/45/60 min or "end of episode", with a fade-out and a 10 s "still watching?"
  prompt before stopping.
- Autoplay next episode with a 10 s cancellable countdown card; remember per-show override.
- A lightweight play queue (add to queue from any list) so binge sessions don't need the remote.
- **Done when:** the timer stops playback at the boundary and the app returns to a sane screen.

### 14. Theming v2
- Material 3 with dynamic accent (fallback palette on Android 8–11), plus an AMOLED black option
  for OLED TVs.
- Respect system dark mode and font scale; keep contrast ≥ 4.5:1 on all text.
- Overscan-safe insets (5 % safe area toggle) so nothing clips on older TVs.
- Poster density + "reduce motion" toggle.
- **Done when:** both themes pass a contrast check on every screen and nothing clips at 5 %
  overscan.

## Tier 3 — Integrations, performance, ops

### 15. Discord RPC v2
- Privacy levels: "always", "only when fullscreen", "never", plus per-profile. Default stays off.
- Add buttons ("Watch on Kuro", site link) and accurate `startTimestamp`/`endTimestamp` from
  position + duration so the bar tracks the episode.
- Respect the SDK's rate limits with a debouncer (≥ 15 s between updates), clear presence on
  pause/stop, and reconnect with backoff after Discord restarts.
- Make sure the OAuth callback (`app.lovelycrxshx.kuro://oauth`) is only ever handled from the SDK,
  never exposed to a WebView.
- **Done when:** presence survives a Discord restart and never exceeds the rate limit in a 2 h log.

### 16. Cast & Google TV v2
- Send the queue, not just the current item; hand off the position both ways on connect/disconnect.
- Media-style notification with a proper `MediaSession` so the TV/phone remote and Google TV's
  "Now playing" work.
- Optional Companion-style phone remote (transport + seek) over local discovery — no cloud.
- **Done when:** casting episode 3 of a queue continues to episode 4 on the TV, and disconnecting
  resumes on the phone at the TV's position.

### 17. Updater v2 — signature-pinned, resumable
- Before install, verify the downloaded APK's **signing certificate** matches the installed app's
  (not just the SHA-256 from a JSON), then hand it to the package installer.
- Resumable download (HTTP range) with a progress notification, Wi-Fi-only default, and a
  "what's new" sheet rendered from the release notes.
- Never auto-install: always a user tap, and a "skip this version" that sticks.
- Graceful failure: if the manifest is unreachable or the hash mismatches, keep working and retry
  tomorrow rather than nagging.
- **Done when:** a tampered APK is rejected by the updater with a clear message and the installed
  app is unchanged.

### 18. Startup & size
- Baseline profile + startup tracing (`androidx.profileinstaller`), lazy-init WebView and Discord
  off the critical path, and no disk I/O on the main thread at launch.
- R8 full mode + resource shrinking; audit for unused native libs and fonts.
- Ship per-ABI splits (`arm64-v8a` for TV/phones, `armeabi-v7a` for old sticks) plus a universal
  fallback APK — target ≤ 35 MB for arm64 and a cold start ≤ 1.2 s on a Fire TV Stick 4K.
- Keep the reproducible 16 KB alignment gate from 5.80.
- **Done when:** `apksigner verify` + alignment audit still pass and the arm64 artifact is ≤ 35 MB.

### 19. Release pipeline v2
- Auto-bump `versionCode`/`versionName` from a single source of truth; fail the build if the tag,
  `versionCode` and `index.html` disagree.
- Publish a `SHA256SUMS` file, a `*.apk.sha256`, an SBOM (CycloneDX via Gradle), and a
  machine-readable build manifest (commit, toolchain versions, signing-cert fingerprint).
- Nightly job that probes all three mirrors and files an issue when one dies.
- Reproducible-build check: rebuild from the published source zip and compare the signing-block-free
  hashes; publish the diff if they diverge.
- **Done when:** a release can be cut by pushing one `[release]` commit, with no manual steps and no
  red runs.

### 20. Privacy & diagnostics
- Certificate pinning per mirror with a documented pin-rotation path and a safe fallback to
  URL-allowlisting if a pin breaks — never a hard lockout.
- Strip the WebView's JS bridge to the minimum surface, disable file access and third-party cookies,
  and block known tracker domains.
- Opt-in crash/diagnostics log: local ring buffer, "export log" via `FileProvider` with the device
  model, version and mirror state — nothing leaves the device without the user sharing it.
- A plain-language privacy page next to the licence in settings.
- **Done when:** a network capture of a normal session shows only mirror hosts and Discord (if
  enabled), and the log export is verifiably local-only.

---

## Confirmed scope (user-selected)

Selected for the 5.81 line — **16 of 20**:
**1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15, 16, 17, 18, 19**

Deferred: **2** (offline catalogue cache), **12** (profiles + kids mode), **13** (sleep timer /
autoplay / queue), **20** (privacy & diagnostics).

That set is ≈ 74 dev-days, so it ships in three waves rather than one release — the wave split,
per-item implementation notes, dependency graph, risk register and the cost of the four deferrals
are in **[PLAN-5.81.md](PLAN-5.81.md)**.

| Wave | Version | Items | Theme |
|---|---|---|---|
| A | 5.81 | 1, 3, 4, 15, 17 | Reliability + trust |
| B | 5.82 | 6, 8, 10, 11, 14 | Living-room feel |
| C | 5.83 | 5, 7, 9, 16, 18, 19 | Heavy engineering |

**Out of scope for the whole line:** anything needing a server, an account system or a Play Store
presence. Kuro stays local-first and sideload-only.
