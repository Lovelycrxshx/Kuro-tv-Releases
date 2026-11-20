# Kuro 5.81 — confirmed scope & build plan

**Selected by the user (16 of 20):** 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15, 16, 17, 18, 19

**Not selected:** 2 (offline catalogue cache) · 12 (profiles + kids mode) · 13 (sleep timer /
autoplay / queue) · 20 (privacy & diagnostics). Consequences are listed in §4 — two of them
bite harder than they look.

**Reality check:** the selected set is ≈ **74 dev-days** of work (S 1.5 · M 4 · L 8).
That is not one release. It's three, shipped in dependency order. Wave 1 *is* 5.81.

---

## 1. Waves

| Wave | Version | Items | Effort | Theme |
|---|---|---|---|---|
| **A** | **5.81** | 1, 3, 4, 15, 17 | ~17.5 d | Reliability + trust: the app stops breaking and stops being a soft target |
| **B** | **5.82** | 6, 8, 10, 11, 14 | ~20 d | Living-room feel: the app stops feeling cheap |
| **C** | **5.83** | 5, 7, 9, 16, 18, 19 | ~36 d | Heavy engineering: subtitles, downloads, search, Cast, size, pipeline |

Why this order: every item in A is a bug users hit *today*, and A's changes are the foundation
B and C build on (ranked host list → stream recovery; watch-state schema → skip-intro and
new-episode deep links; cert-checked install path → any future signed release).

```
1 host ranking ──► 3 stream recovery ──► (C) 16 Cast handoff
       │
       └──► (C) 5 libass pipeline   (subtitle/font/ABI sizing feeds 18)

4 watch-state (Room) ──► 6 skip intro/markers
       │             └─► 11 notifications deep-link
       └─► 10 Home "Continue" row ──► 9 search index over cached rows

15 RPC v2 ── independent
17 cert-checked updater ──► 19 pipeline publishes its manifest
18 ABI splits ──► changes artifact matrix ──► 19 + index.html + 17 manifest
```

---

## 2. Per-item detail (beyond the roadmap line)

### A1 · Mirror health, ranking, circuit breaker — M (~4 d)
- Parallel probe of all three hosts at cold start (`HEAD /home`, 2.5 s cap). Reject a "healthy"
  response whose body length looks like an interstitial/consent page — the same size sanity check
  that keeps the Drive publisher from shipping HTML as an APK.
- Score = EWMA(latency, α 0.3) + failure penalty. Store `(networkKey, host, score, lastOk,
  failStreak)` in Room. **networkKey is a salted hash of SSID or `MOBILE:<carrier>`** — never the
  raw SSID, it's a location signal.
- Breaker: 3 consecutive failures → OPEN 15 min → HALF_OPEN single probe → CLOSED.
- Two independent health signals: *catalogue* fetch and *stream* fetch. A host can serve pages and
  fail CDN streams; rank on the stream signal when resolving playback.
- UI: a "Source: hianime.al (auto)" chip in settings that can be pinned manually.
- Test: `MockWebServer` per host; acceptance run with two hosts blackholed → launch-to-catalogue < 4 s.

### A3 · Playback failure recovery — M (~4 d)
- On `onPlayerError`: snapshot `positionMs`, classify the error.
  - `ERROR_CODE_IO_*` / HTTP 5xx / timeout → retry the same source once (1 s, 2 s backoff).
  - `ERROR_CODE_DECODER_INIT_FAILED` / decoder errors → drop one quality rung or force the SW
    decoder rather than switching hosts.
  - Otherwise → advance to the next host per A1's ranking, re-resolve the stream URL, rebuild the
    `MediaItem` with `setStartPositionMs(position)`, and re-apply the audio/subtitle track choice.
- One snackbar ("Stream dropped — resumed on hianime.al"). Full-screen error only when every host
  is exhausted. Hard cap of 3 hops per episode to avoid ping-pong.
- Emits a diagnostic event (only useful once #20 exists — see §4).

### A4 · Resume & Continue-watching v2 — M (~4 d)
- Room: `watch_state(id, showId, episodeId, positionMs, durationMs, updatedAt, completed, sourceHost)`.
- Write on: every 5 s while playing, on pause, on `onStop`, on app background. < 30 s watched isn't
  saved; ≥ 90 % marks complete and clears the resume point; never resume inside the last 30 s.
- Implement Media3's `ResumptionStore` so the downloads path shares the same semantics.
- "Continue" row = in-progress ordered by `updatedAt`; "Next up" is derived, not stored, so a
  re-watch can't corrupt history.
- **Optional cross-device sync** (Supabase project already connected to this workspace): table
  `watch_progress(device_token, show_id, ep_id, position_ms, duration_ms, updated_at)` with RLS
  scoped to the token, no account, opt-in. Merge rule: later `updatedAt` wins the resume point;
  `max(position)` only within the same episode.
- Migration must adopt whatever preference keys 5.80 uses today so nobody loses their place.

### A15 · Discord RPC v2 — S (~1.5 d)
- Privacy: `always / fullscreen-only / never`, default off (matches 5.80).
- Debounce to one presence update per ≥ 15 s, and only on state change — protects the rate limit.
- Real `startTimestamp`/`endTimestamp` from position + duration so the progress bar tracks the
  episode; `clearPresence` on pause/stop; reconnect with capped backoff after Discord restarts.
- Buttons require assets configured on the Discord application (dev portal) — if that isn't set up,
  ship timestamps + state and leave buttons off rather than showing a broken button.
- SDK init off the main thread, fails silently when Discord is absent.
- Never route the `app.lovelycrxshx.kuro://oauth` callback anywhere a WebView can reach.

### A17 · Updater v2 — cert-pinned, resumable — M (~4 d)
This is the one item with a security consequence, so it's in wave A.
- Before install: `getPackageArchiveInfo(path, GET_SIGNING_CERTIFICATES)` → compare the signing
  cert SHA-256 against the **installed** app's `signingInfo`. Reject on mismatch, on package-name
  mismatch, and on `versionCode` ≤ installed. Only then hand to `PackageInstaller` with
  `USER_ACTION_REQUIRED` (never silent).
- Resumable download via HTTP `Range` + ETag validation, progress notification, Wi-Fi-only default,
  cleanup of stale partials.
- Manifest JSON: `{version, versionCode, url, sha256, certSha256, notes}`. The hash is a convenience;
  **the cert check is the actual defence** — a tampered manifest still can't install an APK signed
  by anyone else.
- "What's new" sheet from `notes`; "skip this version" persists; on unreachable/mismatched manifest,
  stay quiet and retry next launch (max 3 attempts) instead of nagging.

### B6 · Skip intro / outro — M (~4 d)
- Data: AniSkip (`/v2/skip-times/{malId}/{ep}?types[]=op&types[]=ed`) — needs **MAL ids**, which the
  catalogue may not carry. Fallback chain: show's stored MAL/AniList id → title match via Jikan →
  no data (hide the control). Store the mapping and its confidence; never guess silently.
- Cache segments per episode in Room; add manual "mark intro start/end" for shows with no data.
- The button must sit **before** the seekbar in DPAD order so a remote reaches it in one press, and
  must not steal focus mid-playback (auto-dismiss after the window, ~40 s).

### B8 · DPAD / focus overhaul — M (~4 d)
- Persist focus per route (`SavedStateHandle`), restore on back, never fall to top-left.
- Explicit `focusProperties`/`nextFocus*` on: player controls, poster grids, row headers, dialogs.
  Grids need boundary rules or focus escapes into the sidebar.
- Long-press OK → context menu (details, add to list, download, mark watched).
- Back: close overlay → close menu → pop screen → double-back-to-exit with a toast.
- Player overlay's auto-hide timer pauses while anything inside the overlay holds focus.
- Media keys + `KEYCODE_TV_*` handling; guard against double-fired back events on cheap sticks.
- Acceptance: a 4-direction traversal matrix per screen, visible focus ring at all times, checked on
  a 1080p panel at 3 m.

### B10 · Home v2 — M (~4 d)
- Row pipeline: Continue (A4) → New episodes (B11) → Trending → Genres → My list, each row isolated
  so one failed source shows an error chip in *that row*, not a dead screen.
- Skeletons with the exact poster aspect ratio (2:3) so there's no reflow when data lands.
- Coil: memory + disk cache, low-res placeholder, and a conservative memory cache for 1 GB devices
  (Fire TV Stick Lite) — budget by `ActivityManager.memoryClass`.
- Empty states with an action ("Browse Trending"), refresh in the TV overflow menu, pull-to-refresh
  on touch.
- Target: cold start → interactive Home < 1.5 s, zero layout shift.

### B11 · Airing calendar + notifications — M (~4 d)
- Follow list in Room; daily `WorkManager` (network-constrained) against the source's schedule or
  AniList/Jikan `airedAt`. Expect ±a few hours of jitter — that's acceptable, say so in the UI.
- **Android 13+ needs `POST_NOTIFICATIONS` at runtime** — request it contextually the first time
  someone follows a show, never at launch. A sideloaded app that opens with a permission prompt
  gets uninstalled.
- Channel "New episodes", grouped notifications, deep link to episode, per-show mute, global off.
- "This week" screen grouped by weekday; a "check now" action for impatient users.

### B14 · Theming v2 — M (~4 d)
- Material 3 with dynamic accent on 12+, curated seed palettes below that; AMOLED pure-black option.
- Overscan: 5 % safe-area toggle on the root container, verified at 720p — old TVs clip otherwise.
- Contrast audit: every text-on-scrim pair ≥ 4.5:1 (poster titles over artwork are the usual failure).
- Poster density setting; reduce-motion switch that kills hero rotation and shared-element animations.

### C5 · Real subtitle support (ASS/SSA) — L (~8 d) · **highest technical risk**
- Media3 1.7.1 has no ASS renderer: ship a JNI `libass` decoder + a `Canvas`-backed subtitle view,
  or an `SubtitleDecoderFactory` that renders libass to bitmaps.
- Build for `arm64-v8a`, `armeabi-v7a`, `x86_64` with NDK 27 and `-Wl,-z,max-page-size=16384`; the
  5.80 alignment audit must extend to each new `.so`.
- Bundle a small default font set (~1 MB) + user font import; honour size/opacity/outline/bottom
  inset, persisted; ±10 s sync offset stored per show; remember track per show.
- ASS→SRT only as a render-failure fallback. Adds ~1–2 MB per ABI — which is why C18 (ABI splits)
  must be sized *after* this lands.

### C7 · Downloads v2 — L (~8 d)
- Media3 `DownloadManager`/`DownloadService` with `WorkManager` constraints; `NetworkType.UNMETERED`
  default; resume from byte offset (keep the failed `DownloadRequest`, call `resumeDownload`).
- Queue UI: pause/resume/cancel-all, per-item progress; storage budget (e.g. 10 GB) with LRU
  eviction; quality selector per download; "download season" throttled so the source isn't hammered.
- Integrity check on completion (bytes + probe) and a "verify library" action.
- Must survive an app update — downloads live in app-specific storage, so this requires the
  signature to stay identical (it does).

### C9 · Search v2 — M (~4 d)
- Room FTS5 over title + alt/romaji titles; prefix n-gram match for instant results (≤ 100 ms),
  remote results merged behind it; ranking = prefix boost × popularity.
- 250 ms debounce, cancellable, history (cap 20), filters (genre, year, sub/dub, movie/series,
  airing), sort by newest/popularity.
- TV voice: `SearchableInfo` + `android.intent.action.SEARCH` so the leanback launcher and Google TV
  voice button land inside Kuro's search.

### C16 · Cast & Google TV v2 — L (~8 d) · **depends on excluded #13**
- `media3-session` `MediaSession` is the bridge: it makes Google TV "Now playing", phone media
  controls and the TV remote work for free.
- Queue over the Default Media Receiver works via `QueueData` — but **there is no queue model in
  scope**, because #13 was excluded. Either (a) build a minimal `MediaQueue` inside this item
  (+2 d, ~10 d total), or (b) include the queue half of #13.
- Position handoff both ways on session start/end; resume on the phone where the TV stopped.
- Optional local phone remote (transport + seek) over `MediaRouter` discovery — no cloud.

### C18 · Startup & size — M (~4 d)
- Baseline profile (`profileinstaller`) + `Macrobenchmark` against 5.80 to prove the delta.
- R8 full mode + resource shrinking + `resConfigs`; audit for unused fonts/locales/native libs;
  PNG→WebP where it matters. Watch R8 vs JNI (Discord bridge), JS interface and Room reflection.
- ABI splits: `arm64-v8a`, `armeabi-v7a`, plus universal fallback → **artifact matrix changes**, so
  19, `index.html`, the Downloader instructions and 17's manifest all move with it.
- Targets: arm64 ≤ 35 MB, cold start ≤ 1.2 s on a Fire TV Stick 4K; 16 KB audit re-run per APK.

### C19 · Release pipeline v2 — M (~4 d)
- Single `version.properties` drives Gradle, the tag, `index.html` (templated, not hand-edited) and
  the updater manifest. CI fails if tag ≠ versionName ≠ versionCode ↔ versionName map ≠ page text.
- Attach `SHA256SUMS`, per-file `.sha256`, CycloneDX SBOM, and a build manifest (commit, toolchain,
  signing-cert fingerprint).
- Nightly cron probes all three mirrors, writes `docs/mirror-status.json` (which `index.html` can
  render as a live status strip) and opens an issue when one dies.
- Reproducible-build report: rebuild from the published source zip, diff the output, publish the
  result. Treat full determinism with native libs as a stretch goal — report, don't gate.
- Signing stays local (per standing constraint: no keystore or password in CI); CI receives
  pre-signed artifacts, gates the hash, publishes.

---

## 3. Risk register

| Risk | Item | Mitigation |
|---|---|---|
| libass + 16 KB alignment on 3 ABIs | 5 | Spike it first in wave C; fall back to SRT-only release if it can't be verified |
| No queue model in scope | 16 | Fold a minimal queue into 16, or take half of 13 |
| ABI splits ripple into page, updater, pipeline | 18 | Land 18 before 19 finalises artifacts; generate rather than hand-edit |
| MAL-id coverage for skip times | 6 | Confidence-scored title match, hide the control when unsure |
| Notification permission on sideload | 11 | Ask contextually after a follow, never at launch |
| 16 items ≈ 74 d presented as "one release" | all | Ship in three waves; don't cut a 5.81 that touches 16 subsystems |
| R8 full mode breaks JNI/reflection | 18 | Keep rules for Discord bridge, WebView JS interface, Room |

## 4. What dropping 2, 12, 13, 20 costs

- **No #2 (catalogue cache).** With every mirror down or no network, launch lands on an error screen
  — A1+A3 make the *failure* graceful but don't give users anything to look at. A cheap subset
  (persist the last Home payload + last 20 show pages, 1–2 d) buys most of the value; worth
  considering for wave B.
- **No #12 (profiles).** Fine — but it also means the watch-state schema in A4 should still carry a
  `profileId` column defaulting to 1, so adding profiles later isn't another migration.
- **No #13 (sleep timer / autoplay / queue).** Autoplay-next is the one anime habit that isn't
  covered anywhere else in this scope, and #16 wants the queue. Cheap to add (S, 1.5 d) — the
  highest value-per-day item in the whole backlog.
- **No #20 (privacy).** Consequence: no cert pinning for mirrors, and A3's recovery events have
  nowhere to go (no diagnostics log). Acceptable, but the WebView bridge minimisation is worth
  folding into #17's release, since it's the same trust story.

## 5. Open questions

1. **Wave A as 5.81 (1, 3, 4, 15, 17), or all 16 in one long 5.81?** Recommend the former.
2. **#16:** minimal queue inside it (+2 d), or take the queue half of #13?
3. **#4 sync:** wire the optional Supabase progress sync now, or leave the table for later?
4. **#6:** is a MAL id present in the catalogue for each show, or do we need title matching?
