# Kuro 5.83 — progress

Branch: `arena/98533644-kuro-tv-releases` · first cut: versionCode **86**, versionName **5.83**

## Cut 1 — the update window

5.82 installed a release the moment it was found. 5.83 puts a window in front of that: a
release waits **24 hours** (default) or **48 hours** after GitHub publishes it, and installs
itself when the wait is over. If the window has already passed when the app next looks, the
update happens at once — so the delay can postpone an update but never strand one.

New classes:

- `KuroUpdateSchedule583` — the policy. Delay in hours (0 / 24 / 48), `dueAt`/`due`/
  `remaining` arithmetic against the release's own `published_at`, the scheduled-state
  prefs, the one-shot job at the end of the window, and a throttled catch-up for resume.
- `KuroUpdateDueJob583` — a `JobService` (declared in the manifest) parked at the end of the
  window; it simply runs the normal updater pass, which now sees the window is over.

Touched:

- `KuroUpdater572` — `Release` carries `published_at` (`parseTime`, UTC, 0 when the reply has
  none); `run()` hands a found release to `KuroUpdateCenter582.autoStep`.
- `KuroUpdateCenter582` — `SCHEDULED` stage, `autoStep` (download now or defer), `checkNow(c,
  immediate)` so a person tapping **Update now** skips the wait, `scheduledLine`,
  delay-aware strings, `dueAt` on the snapshot.
- `KuroUpdateDialog582` — SCHEDULED chip, the due time in the detail line, **Update now** /
  **Close**.
- `MainActivity` — *Update delay* chooser (`updatesDelay583`), the delay row and scheduled
  line in the panel, resume catch-up.
- `assets/settings560.js` — the new row.

Covered by `tests/test-update-window-583.py` (window arithmetic, publish-time parsing, the
three escape hatches, the "never loosened" guarantees) and the 5.82 test, updated for the new
hand-off.

## Build record

| | |
| --- | --- |
| build run | `44743763167` success |
| aligned APK | `dist/Kuro-tv-5.83-aligned.apk`, 43,938,049 B |
| signed APK | 44,008,672 B |
| SHA-256 | `b853e87b446d2abd15d20915c84a47966f5af6b892849a8c92371a164125a26a` |
| source | `Kuro-tv-5.83-Source.zip`, 7,255,078 B (Drive `1oTF0Il3vazhE2V8ghLxVDtW4lojsRWFj`) |
| signer | CN=Kuro Anime, `1d37c3ca…e93e` |

## Next cuts (86 stays)

1. **DPAD / focus overhaul** — the highest-impact change left for Fire TV.
2. **Home v2 skeletons** — load state that mirrors the final layout.
3. **Airing calendar + new-episode notifications.**
4. **Theming v2** — Material 3 tokens, true AMOLED black, overscan-safe TV margins.
