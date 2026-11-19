# Kuro 5.81 — build progress

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
`AndroidManifest.xml` in the 5.81 source: `versionCode 84`, `versionName 5.81`.

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

## Still to do
1. Merge the page update to `main` so the published download page shows 5.81 (Pages serves from `main`).
2. Drive cleanup: move the superseded 5.80 APK, its checksum and the empty
   `Kuro-tv-5.81-Source.zip` revision (id `1_ofr8V7nVGlavv-sNVNziu5oaz6tCFoJ`, 0 bytes) to trash.
3. Feature work from [PLAN-5.81.md](PLAN-5.81.md) wave A (#1, #3, #4, #15, #17) and the profile
   section from [SPEC-PROFILES.md](SPEC-PROFILES.md) — the dialog theming was the presentation slice.
