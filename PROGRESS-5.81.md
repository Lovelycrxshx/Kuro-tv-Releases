# Kuro 5.81 — build progress

## Version policy (owner instruction)

**The version is never bumped unless the owner says so.** `versionName 5.81` / `versionCode 84`
stay in `build.gradle` and `AndroidManifest.xml` while improvements accumulate in this line.
The build workflow asserts the *existing* version rather than a new one, and release notes /
`index.html` keep pointing at 5.81.

Consequence to keep in mind: an APK rebuilt with the same `versionCode` cannot be offered by the
in-app updater (it only offers a strictly higher code), so an improved 5.81 has to be sideloaded
until the owner asks for a bump.

**Status: released.** `v5.81` is live and is the latest release —
`Kuro-tv-5.81.apk` (55,021,231 B, sha256 `209b799d525450a59d36811c3c46238acd1fe92b6085bb544f87cdc0a60c9fe1`),
`Kuro-tv-5.81.apk.sha256`, `Kuro-tv-5.81-Source.zip` (16,270,964 B). Version 5.81 / versionCode 84,
signed with the existing `CN=Kuro Anime` key (cert SHA-256 `1d37c3ca…03e93e`), v2+v3, all 8 native
libs 16 KB-aligned. Drive copies are private again.

Working notes for the 5.81 line. Scope and waves are in
[PLAN-5.81.md](PLAN-5.81.md); the profile section is specced in
[SPEC-PROFILES.md](SPEC-PROFILES.md).

## Shipped in the working tree (not yet signed/released)

### Themed dialogs — item #21's "UI theme richer" pass
The three dialogs in the user's screenshots (Kuro updates, Update reminder spacing, Update
download network) were stock `AlertDialog`s: platform grey panel, default teal accent, flat
list rows — a second visual language next to the app's own dark-violet cards.

- **`src/.../KuroDialogs581.java`** (new) — a small dialog toolkit that reuses the palette
  already established by `PlayerOptionsDialog` (`#100C18` panel, `#211229` cards, `#665078`
  border, `#A65BE4` accent, `#F3EEF7` ink, `#B7A4C7` muted). Provides:
  - rounded 20 dp panel with a faded violet accent strip, fade + rise animation;
  - `row()` label/value lines, `note()` paragraphs, `section()` group labels;
  - `body(report)` which turns the updater's `describe()` text into label/value rows
    without moving any formatting logic out of `KuroUpdater572`;
  - `choice(labels, hints, selected, listener)` radio rows with focus ring, check mark,
    selected state and DPAD key handling;
  - `action(label, primary, runnable)` footer buttons with `StateListDrawable` focus
    states, so a remote never loses the cursor;
  - initial focus goes to the primary action or the selected row.
- **`res/values/styles.xml`** — `KuroDialogTheme` (`Theme.Material.Dialog.Alert` parent)
  with the app's panel drawable, violet accent and ink/muted text colours.
- **`res/drawable/kuro_dialog_panel.xml`** (new) — the rounded panel used by the style.
- **`MainActivity.java`**
  - `showUpdater572()`, `showUpdateInterval573()`, `showUpdateNetwork573()` rebuilt on the
    new toolkit (status as rows, honest "Android decides when background work runs" note,
    Install / Check now / Turn off / Close as focusable actions);
  - the remaining **15** stock alert dialogs now build from
    `KuroDialogs581.themed(this)`, so nothing in settings is grey-on-grey any more.

Checks run locally: all 52 Java files and 11 XML files parse; every framework API the new
class touches was verified to exist in `android-35/android.jar`
(`Dialog`, `GradientDrawable(+Orientation)`, `StateListDrawable`, `TextView.setLetterSpacing`,
`ViewTreeObserver`, `ContextThemeWrapper`, `Window`, `MarginLayoutParams.setMarginStart`, …).

### Version
Unchanged: `versionCode 84`, `versionName 5.81` in both `build.gradle` (the value AGP actually
uses) and `AndroidManifest.xml`. See the version policy above.

## Build pipeline (used for the 5.81 release)
- `Kuro-tv-5.81-Source.zip` — uploaded to the Drive **Kuro** folder
  (id `1_ofr8V7nVGlavv-sNVNziu5oaz6tCFoJ`, 16,270,964 B).
- `.github/workflows/build-5.81.yml` — downloads that zip, asserts the 5.81 changes are
  present, builds JNI + `assembleRelease` on SDK 35 / build-tools 35.0.1 / NDK r27c,
  zipaligns `-P 16`, and commits the aligned unsigned APK plus the build log back to the
  branch. No repository secrets: the Discord Social SDK arrives as a short-lived signed URL
  in the commit message.

Resolved while cutting the release:
- the project pins `org.gradle.jvmargs=-Xmx512m`, which OOMs AGP's `:collectReleaseDependencies`;
  CI overrides it to 4 GB rather than editing the published source;
- the SDK zip contains several AARs — the krisp/debug one has no `com.discord.socialsdk`, so the
  AAR is now chosen by path (`lib/release/discord_partner_sdk.aar`), not by `find` order;
- **AGP ignores the manifest version**: the first green build was still 5.80/83. `build.gradle`
  is the source of truth, and the workflow now asserts `versionCode 84` on the built artifact.

### Cast handoff rebuild (in the current version)
See [CAST.md](CAST.md). Replaces the 73-line device picker with a themed panel, a real
`MediaSession` + notification, a working timeline, and a position hand-back so the phone
resumes where the TV stopped (`KuroNativePlayer561.seekTo582`, `MainActivity.returnFromCast582`).
Packaged as `Kuro-tv-5.81-Source.zip` rev 2 (Drive id `185taXBjaZwmosfLheCIRXdw1uLCS145w`).

## Still to do
1. Merge the page update to `main` so the published download page shows 5.81 (Pages serves from `main`).
2. Drive cleanup: move the superseded 5.80 APK, its checksum and the empty
   `Kuro-tv-5.81-Source.zip` revision (id `1_ofr8V7nVGlavv-sNVNziu5oaz6tCFoJ`, 0 bytes) to trash.
3. Build + sign the current version once a fresh Discord SDK URL is available:
   `git commit --allow-empty -m "[build] sdk=<url>"`.
4. Feature work from [PLAN-5.81.md](PLAN-5.81.md) wave A (#1, #3, #4, #15, #17) and the profile
   section from [SPEC-PROFILES.md](SPEC-PROFILES.md) — the dialog theming was the presentation slice.

### Re-cut of the current version (cast + motion layer) — shipped
Run `44575516108` built rev 4 of the source (the rev-3 compile failure was a single missing
field, `castStart561`, never inserted by an earlier patch). Signed locally with the existing
key, published as a replacement for 5.81 with the **same versionCode 84**:

```
APK 55,045,938 B · SHA-256 bcfe3e2f05f8e13b36469d71abcb71ff4f3c31a9744217247055f82d2a25f891
cert CN=Kuro Anime (unchanged) · v2+v3 signatures
```

Drive: APK `1lbKoY6dk649lMcMWRdr0kFHYczi85aKW`, sha `1ty61WZz_IajSQD0jhiuy-HyyzEp7Cg4P`,
source `1ujNX216o4mJGeRpO6e3yXoIwzXDSeQYA` (all public).
Because the code is unchanged the in-app updater cannot offer it — this one is a manual
install over the top.

### Wave 2 (rev 7, same versionCode 84) — in progress

User-selected improvements: **10, 9, 8, 5** from `IMPROVEMENTS-GALLERY.md`, plus
**"remove all profile pictures and banners from the app"**.

Done in the working source tree (`work/source/Kuro-tv-5.81-Source`, rev-6 base):

- **Profiles are text only.** `accounts542.js` lost `CHARACTER_ART`, `CARD_ART`, `readCardArt`,
  `showCardArt`, `showCharacters`, the avatar/banner/art pickers, the "Change banner, Anime &
  History backgrounds" button and the `kuroPresets571` mount points. Hero, drawer identity and
  header chip now render a **monogram** (`.profile-mono571`, initials of the saved name).
  `WatchHeader.java` draws that monogram in the native watch header instead of a bitmap;
  `Bridge.profileHeader560` now accepts initials only (≤16 chars) and no longer decodes base64.
  `backup560.js` validates `{name}` profiles, imports strip stale image fields, and
  `settings560.js`/`privacy571.js` no longer list card-art storage.
- **323 + 160 preset/character/avatar/banner JPEGs deleted** (483 files, 14 MB → 3.0 MB assets),
  plus `preset-manifest571.js/.json`, `profile-presets571.js` and their `<script>` tags.
- **Home hero (item 5).** `renderSpotlight` is now a swipeable deck of up to five real catalogue
  titles: rounded artwork, gradient copy block, one Watch pill per slide, snap scrolling and
  page dots that stay in sync (auto-advance every 7 s, paused on touch, reduced-motion and
  background). The freshly-added shelf marks posters with a **NEW** chip. The continue rail has
  no progress bar because the app stores no playback position — not faked.
- New offline harness `tests/smoke-profile571.cjs` (jsdom) boots the real page with a stubbed
  account bridge, opens the profile view, and asserts no picture/banner markup survives and the
  hero deck builds slides, dots and pills. Both pass.

Still to do: **8** search filter chips, **9** player gestures + HUD, **10** episode drawer +
auto-next countdown; then repack, Drive, `[build]`, sign, upload, `[release]`, `index.html`,
merge.

### Wave 2 shipped as a 5.81 re-cut (rev 7, versionCode 84)

```
APK      Kuro-tv-5.81.apk · 45,437,725 B (was 55,045,938 B)
SHA-256  541f1b73ad1ad32ec92a3bba880cedbd67a3a829a4b0f4c28632812be10c960a
Source   Kuro-tv-5.81-Source.zip · 6,778,856 B (was 16,283,649 B)
cert     CN=Kuro Anime (unchanged, SHA-256 1d37c3ca…)
```

Build run **44592292525** (`[build]` on this branch), publish run **44593259680** (`[release]`).
Drive folder now holds exactly one copy of each: APK `1rdI8LetQXVYZtEeo2K-26c328KmbSU3e`,
sha `1vLeyQKRFcS22C3l-6U5L5Zdydm9_cAXk`, source `1pQgzHzATuw6UDz_X5H0eP6AqMZUbpMuk`;
the two superseded sources (incl. the rev-6 one) and the old APK/sha were trashed.

Two pipeline fixes are needed every time this branch changes:
`build-current.yml` and `publish-5.81.yml` still filtered on the previous editing branch
(`arena/d9a61230-…`) — both now point at `arena/c7139fd4-…`. The publish size gate refused
anything under 50 MB as a "Drive interstitial page"; with the artwork gone the APK is 45 MB,
so the floor is now 30 MB (an interstitial is a few KB). New gates assert the APK carries
`profile-mono571` and `hero-track581`, and that no `preset571-`/`profile-avatar547` artwork
is shipped.

### Rev 8 — why the last cut looked unchanged, and what is actually wired now

The rev-7 hero deck was built in `app.js`, but the live app never runs that code: `app.js`
re-points the whole shell at the unified layer (`home=unifiedHome;searchPage=unifiedSearchPage`),
and `unifiedHome`/`unifiedSearchPage` live in `unified533.js`. So the deck, the NEW chip and the
chips that were added to `app.js` were dead code on device. That is the "not working" report.

Rev 8 moves the features onto the surfaces the app really renders:

- **5 (hero)** — `unifiedSpotlight()` now feeds the shared deck, `unifiedHomeData()` passes the top
  five ranked titles, `unifiedHome()` clears the deck for each render and seeds it from your
  history when the popularity lists are blocked, so the hero is never the old static block.
- **5 (NEW chip)** — `unifiedCard()/unifiedCards()` take an `isNew` flag; the freshly-added shelf
  and its cache path both pass it.
- **8 (chips)** — a chip bar above `#results` on the browse page: Format (TV/Movie/ONA/OVA/Special)
  and Year, counted from the titles that have loaded, each chip carrying its own count, plus a
  "N of M loaded" line and a Clear action. Facets come from each title's own metadata, so a chip
  only exists when the catalogue reports that data — no invented year, format or dub flag.
  Filtering is enforced through `renderPagerCards`'s item hook, so it also applies to pages that
  load after the chip was set.
- **9 (gestures)** — native player: double-tap left thirds seeks −10 s, right thirds +10 s (centre
  toggles play), vertical drag on the left half is brightness and on the right half is volume,
  each with a fading readout pill. A plain tap still reaches PlayerView's own controls.
- **10 (episode drawer)** — new `KuroEpisodeDrawer581`: an "Episodes" icon in the native watch
  header opens a right-hand drawer listing `watchInfo561.episodes`, the current episode pinned and
  marked, tap switches episode through `window.kuroChooseEpisode(index)`. Auto-next already
  shipped (`autoNextDialog573`, cancellable countdown), so it now has a manual companion.

Verified offline: `tests/smoke-unified581.cjs` (hero deck through `unifiedSpotlight`, duplicate
suppression, NEW chip markup, chip facets/counts/combined filter) and `tests/smoke-profile571.cjs`
both pass; all four touched Java files parse; the build guards were extended and pass locally.

### Rev 8 shipped (2026-11-19)

- Build run `44596547566` — success; the first attempt (`44595951472`) failed on one typo,
  `gradientHud()` instead of `gradientHud581()` in the HUD constructor. Fixed, re-cut, rebuilt.
- Signed locally with the original `CN=Kuro Anime` key recovered from Drive
  (`Kuro-Anime-Signing-Key.p12`, legacy PKCS#12 → `openssl pkcs12 -legacy` → modern.p12, alias
  `kuroanime`). Cert fingerprint unchanged: `1D:37:C3:CA:…:3E`.
- **APK 45,441,821 B, sha256 `264dba12268c6e6d8cebcdeef4ed15277ba4962dcdbcaad0c5c00cde388fca60`**,
  release `v5.81` re-cut (publish run `44597435272`, all gates green including the new rev-8 gates),
  merged to `main` as `2169c42` via squash (binary-free), Pages deployed.
- Drive: APK `1hAnxwYtEhRQMogbKCTqEAmMNY-1waWNx`, sha `1Rqeu8yLaKG6jTDpt6PAP_kCk4r0Dz8jC`,
  source `1_qX9fJFFXIcTVDcBHLQBmKL_3PIUwHNs` (also the `SOURCE_ID` both build workflows fetch).
- The chat-visible SDK URL in the build commit expires an hour after issue; a fresh one is needed
  for the next build.

### Rev 9 — profile art pack (2026-11-19)

- Source is pulled from Drive through the Google Drive connector into `google_drive/` — the sandbox
  cannot reach `drive.google.com` or `release-assets.githubusercontent.com` over curl, so the
  connector is the only way to get the source zip in.
- New: `assets/profile-art/pfp|banner/art-01..05.webp` (10 files, 472 KB), `assets/profile-art-arm.js`
  (self-painting picker + `kuro-profile-art-v1` store), `assets/profile-art-arm.css`, linked from
  `index.html`. `MainActivity`'s WebView asset whitelist gained the three paths and an `image/webp`
  MIME branch — without that the images 404 inside the WebView.
- Build run `44604508021` (first attempt failed on a transient Gradle/Kotlin dependency resolution
  error, retried unchanged). APK **45,905,627 B**, sha256
  **`f7b069d220c7805913df1885e67a148c6cba321a3dedc17238ec5038020e8965`**; release `v5.81` refreshed by
  publish run `44605323359` with new gates that require `profile-art-arm.js|css` in `index.html` and
  at least ten `profile-art/{pfp,banner}/NN.webp` files inside the APK.
- Drive: source `19mIs0kzSaVjceQAfzTClOUYgUe4GO4Cl`, APK `1dSuhp93onn9Vlj_vrac1e791lp6U_MQ-`,
  sha `1z9Sj8CKWLyC8M-2VJEKFdlMqVMMzHkcC` (all public).
- Proof: `tests/smoke-profile-art581.cjs` (jsdom) — picker injected, 6+6 tiles, 10 thumbnails,
  avatar `<img>` and banner `background-image` painted on click, choice persisted, restored after a
  page re-render, cleared back to initials, all ten WebPs valid RIFF containers. "problems none".
- Pairs 06–20 are still queued in `profile-art/MANIFEST.md`; the same pipeline adds them (convert →
  drop in `assets/profile-art/`, extend the `IDS` array, re-zip, rebuild).

### Rev 10 — one avatar everywhere, and the SDK stops expiring (2026-11-20)

- Bug: the chosen art only painted the profile hero. `accounts542.js` builds the header badge
  (`refreshHeader560` → `#header-login > .header-mono571`) and the drawer identity
  (`.profile-drawer560 .profile-identity560 .profile-mono571`) itself, so both kept the monogram.
- Fix: `paintMonos()` wears the avatar as a `background-image` on `.profile-mono571.has-art581`
  (text made transparent, `background-size:cover`), and the module now observes the whole body
  (debounced through `setTimeout 0`) instead of only `#main`, so a rebuilt badge or a drawer that
  is created later is painted too. Clearing removes it from all three surfaces.
- Build `44671708973`; signed APK **45,905,627 B**, sha256
  **`d5815edb1cf469bc14a0343c71f4bb21ff05528a5bb5fb425abf39cc8e443240`**; publish run
  `44672314373` refreshed `v5.81`. Drive: source `1nJHRkb5xXTJRN_ixVJXO2OYO7TiR2YV9`,
  APK `130M_aSgLGQiM-3WdJ3Xq5UuUf47Nk82g`, sha `18lJzH23ijPSPaxbvXo0DSgrphZS1khdW`.
- **SDK caching:** the runner now looks for the zip on a `sdk-cache` release before asking for a
  signed URL, and parks it there when it does download one (asset `discord-sdk.zip`, 788 MB).
  `build-current.yml` no longer has the hard "extract sdk= from the message" step. Verified with
  build `44672439457`, triggered by `[build]` with **no** `sdk=` in the message — it reused the
  cached copy and built clean, so signed URLs are only needed when the SDK version changes.
- Test: `tests/smoke-profile-art581.cjs` extended to the badge, a late-created drawer, a rebuilt
  badge, and clearing — "problems none".

## Rev 11 — Cast removed, presence overhaul (released)

- **Cast is gone.** Deleted `KuroCastActivity561`, `KuroCastOptions561` and `KuroCastTheme`
  (`res/values/cast_styles.xml`), the manifest activity + `OPTIONS_PROVIDER_CLASS_NAME` meta-data,
  the `play-services-cast-framework:22.1.0` dependency, the `player-cast-561` chip, `cast561()`,
  `returnFromCast582()`, `CAST_REQUEST561`, `castStart561`, `castSucceeded561` and
  `webPausedForCast561` (plus the resume nudge that only existed for it). The stale
  `KuroCast561Test` host test was dropped with the class. APK drops from 45.9 MB to 43.9 MB.
- **RPC 2 — pump backoff.** `KuroDiscordRpc563.Pump` no longer wakes every 100 ms: 150 ms while an
  update is in flight (`hotUntil`), 500 ms while something is playing, 1.5 s when idle.
- **RPC 3 — browsing card.** `KuroPresence564.browsing()` now returns
  “Looking for an anime to watch” / “Kuro.tv”, and stays neutral (“In the app”) when titles are
  hidden. The unused `KuroPresence563.watching/browsing` builders and `castStart561`-era code are
  gone (RPC e).
- **RPC 6 — truncation.** The episode name is clipped inside the quotes with an ellipsis, with the
  separator, both quotes and the ellipsis reserved inside the 120-byte budget.
- **RPC 10 — diagnosability.** Status shows relative “last accepted” times, counts the retry down
  while updates keep failing (`Retry.waitFor`), and history lines carry the SDK result code.
- **RPC c/d.** “Rewatching” appears when an episode that already had saved progress is opened
  (`rewatch563` set once at open, passed through `Options.rewatch`). New `away` preference
  (`quick` default = the old 12 s stop, `min1`, `min5`, `keep`) controls how long presence
  survives backgrounding; `keep` also lets the presence poll keep running in the background.
- **Improvements 1, 2, 4.** `WatchHeader.setProfile(signedIn, mono, artId)` decodes
  `profile-art/pfp/art-NN.webp` into the signed-in chip (circular clip, initials fallback), fed by
  the `profileArt581(String)` bridge that `profile-art-arm.js` calls whenever the choice changes;
  `backup560.js` now exports `kuro-profile-art-v1`; the episode drawer shows a “Resume 12:34”
  label per started episode via `KuroEpisodeDrawer581.setProgress(Progress)`.
- **Not done, and why.** Genre + Dubbed chips (improvement 3) need per-card metadata the unified
  catalogue does not carry — items only expose `meta` (format/year) and `title`, so the chips would
  never match. The season switcher (part of 4) has no season data either. Dead WebView whitelist
  pruning (5) needs a per-source host audit before removing entries.
- Build `44675856463`; signed APK **43,947,232 B**, sha256
  `95ad74ec96ca3656a1f57f32cdaaf21f51818c3038c74ca889e25dd511e8e1ca`, header-only signature
  verified against `CN=Kuro Anime` (`1d37c3ca…`); source `7,246,069 B`. Publish run
  `44676375461` refreshed `v5.81`. Drive folder back to 11 files (old APK/sha and two older source
  zips trashed).

## Rev 11.1 — launch crash fixed (released)

- **Cause.** Rev 11 declared `private String profileArt581=getSharedPreferences(...)…`. A field
  initialiser runs inside the Activity constructor, and `attach()` only supplies the base context
  afterwards, so `getSharedPreferences` threw a NullPointerException before `onCreate` — the app
  closed the instant it was opened.
- **Fix.** The field is a plain `""` now and the preference is read as the first statement in
  `onCreate`; `WatchHeader` still falls back to initials whenever the id is empty or unreadable.
- **Guard.** `build-current.yml` now fails if anything *before* `onCreate` in `MainActivity`
  touches `getSharedPreferences`, `getResources` or `getSystemService` (`early` check), so this
  class of crash cannot ship again. The check was verified against the buggy line (1) and the
  fixed file (0).
- Build `44677089469`; signed APK **43,963,616 B**, sha256
  `76eb0595ea88c064ed824fa98c8409ba05e3fe7f88f4e79b47a17d8bbd06613d`, `CN=Kuro Anime`
  (`1d37c3ca…`); source `7,246,067 B`. Publish run `44677577182` refreshed `v5.81`. Drive holds
  only the newest APK, checksum and source (superseded rev-11 copies trashed).

## Rev 11.2 — Discord Rich Presence actually connects (released)

- **Cause.** In `native/kuro_discord_rpc563.cpp` the current-account client was built with
  `SetApplicationId(...)` and then never `Connect()`ed: only the linked/OAuth client had a
  `Connect()` call. Every `nativePublish` therefore called `UpdateRichPresence` on a client that
  was not `Ready`, the SDK rejected it (or dropped it), and nothing ever appeared in Discord.
  The pump's own polling also never re-tried a connection, so the state could not recover.
- **Fix.** New `newDirect()` helper: creates the client, installs a status callback, calls
  `Connect()`, and records `directState` (0 idle / 1 connecting / 2 ready / -1 refused).
  `publishDirect()` publishes only when Ready and re-publishes `lastActivity` the moment the
  connection becomes Ready. `keepDirect()` runs from every pump and retries `Connect()` every 3 s
  while the client is `Disconnected` (so opening Discord later still works). `nativeInit` and
  `nativeReconnectCurrent` both go through `newDirect()`, and `nativeDirectState` exposes the state.
- **Java.** The pump paces to the connection instead of a fixed guess: 150 ms while an update is in
  flight, 250 ms while connecting, 1 s while refused, 500 ms while playing, 1.5 s idle. The status
  card reads "Connecting to the Discord app…", "The Discord app refused the connection" or
  "Waiting for the Discord app", plus a "Current Discord app link: connected/connecting/not
  connected yet" line.
- **Tests.** `tests/KuroDiscordNative564Test.cpp` now asserts the client is `Ready` after init and
  that `nativeDirectState` goes 1 → 2 across a pump; `tests/test-discord-rpc563-static.py` asserts
  the new helpers and the `away` preference, and its stale fixed-12-second assertion was replaced
  with the new expression. Both pass locally (the native test compiles the JNI glue against the
  fake SDK header).
- Build `44678627093`; signed APK **43,967,712 B**, sha256
  `d215fa1aade2cec8690040a075b34534f64d10eaddaebcf6254e95609a88a615`; source `7,246,813 B`.
  Publish run `44679089873` refreshed `v5.81`; Drive trimmed to the newest three files.

## Rev 11.3 — the SDK can see Discord (released)

- **Evidence.** The status card showed "Current Discord app: Connecting to the Discord app…" and
  "Current Discord app link: connecting" while the Discord app was open and signed in on the same
  phone — so `Connect()` was issued (rev 11.2) but the handshake never finished.
- **Cause.** Android 11+ hides installed packages unless the manifest declares them, and
  `AndroidManifest.xml` had **no `<queries>` element at all**. The Social SDK discovers the Discord
  app by package name, so `com.discord` was invisible and the connection could never complete.
- **Fix.** `<queries><package android:name="com.discord"/></queries>` before `<application>`; a
  handshake still unfinished after 12 s is now torn down and restarted (6 s backoff) instead of
  hanging; `directError` carries `Client::Error` from the status callback and the card shows
  "refused (SDK code N)" when the SDK rejects the connection.
- **Guards.** `build-current.yml` fails if the `com.discord` package declaration disappears, and
  `tests/test-discord-rpc563-static.py` asserts both the manifest query and `nativeDirectError`.
  The native host test still passes against the fake SDK.
- Build `44679512089`; signed APK **43,984,096 B**, sha256
  `6ce4d18897066753b978f27b2f761a507fab6044950e7c48dd6813c1c6491fc7` (compiled manifest verified
  to contain `com.discord`); source `7,247,110 B`; publish `44680044668` refreshed `v5.81`; Drive
  trimmed to the newest three files.

## Rev 11.3.1 — trace the SDK handshake

- The current-account client now records the SDK's own status/error pairs (`trace` ring in
  `native/kuro_discord_rpc563.cpp`, exposed through `nativeTraceCount/nativeTraceStatus/nativeTraceError`)
  and the status card prints them as `SDK events: …`, so a stuck "Connecting" says which statuses the
  SDK actually delivered and with which error code.
- `DiscordSocialSdkInit.setEngineActivity(this)` is called at the top of `MainActivity.onCreate`, before
  the presence loop starts, so the SDK engine is bound to the Activity from the first frame.
- The presence pump runs on the main looper (`new Handler(context.getMainLooper())`) instead of a private
  `HandlerThread`: the SDK delivers callbacks on the looper, so a Ready/Disconnected transition is now
  published on the very next pump (50 ms while the direct client is not Ready) rather than up to 1.5 s later.
- Static test extended: manifest `com.discord` query, `nativeDirectError`, the trace natives and the
  early `setEngineActivity` call are all asserted; native test covers the trace API.

## Rev 11.3.2 — say it when Discord never answers

- The presence loop tracks how long the current-account client has been not-Ready (`unreadySince`,
  reset the moment the SDK reports Ready) and the card adds, after 20 s:
  "The Discord app has not answered the handshake for N s; open Discord, keep it running and leave
  Activity sharing on."
- Static test asserts the new line so it cannot be dropped by a later refactor.
- Shipped as the ninth 5.81 cut with the SDK event trace from 11.3.1.
