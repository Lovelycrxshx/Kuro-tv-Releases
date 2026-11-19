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
