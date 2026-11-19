# Kuro 5.81 — build progress

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

## Staged, not built
- `Kuro-tv-5.81-Source.zip` — uploaded to the Drive **Kuro** folder
  (id `1_ofr8V7nVGlavv-sNVNziu5oaz6tCFoJ`, 16,270,964 B).
- `.github/workflows/build-5.81.yml` — downloads that zip, asserts the 5.81 changes are
  present, builds JNI + `assembleRelease` on SDK 35 / build-tools 35.0.1 / NDK r27c,
  zipaligns `-P 16`, and commits the aligned unsigned APK plus the build log back to the
  branch. No repository secrets: the Discord Social SDK arrives as a short-lived signed URL
  in the commit message.

**Blocked on:** a fresh Discord Social SDK signed URL
(`git commit --allow-empty -m "[build81] sdk=<url>"`), because `app/build.gradle` fails
without `KURO_DISCORD_SDK_AAR` and the sandbox cannot download it. The Drive source zip must
also be link-shared while the job runs, then set back to private.

## Still to do for a 5.81 release
1. Green compile run, then sign the aligned APK with the existing key (`CN=Kuro Anime`) and
   audit 16 KB alignment — same procedure as 5.80.
2. Publish `v5.81` with APK + source + checksums, update `index.html`, and keep 5.80 only as
   history (superseded builds still go to Drive trash, per the standing cleanup rule).
3. Feature work from [PLAN-5.81.md](PLAN-5.81.md) wave A (#1, #3, #4, #15, #17) and the
   profile section from [SPEC-PROFILES.md](SPEC-PROFILES.md).
