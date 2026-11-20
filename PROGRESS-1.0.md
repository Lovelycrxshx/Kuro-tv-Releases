# Kuro 1.0 — progress

Branch: `arena/98533644-kuro-tv-releases` · first cut: versionCode **87**, versionName **1.0**

## The renumbering

| | |
| --- | --- |
| versionName | 5.83 → **1.0** (the 5.x line ended at 5.83) |
| versionCode | 86 → **87** (still climbing, still the thing Android and the updater act on) |
| releases | v5.72 … v5.83 deleted; `sdk-cache` kept (build infrastructure, not a product release) |

The one code change is in `KuroUpdater572.isNewer`: `if (a[0] == 1 && b[0] >= 5) return true;`
— a 1.x release is newer than an installed 5.x build, so 1.1 and later arrive normally.

**This rule cannot bootstrap itself.** The comparison runs in the build that is already
installed, and 5.83 does not have the rule — it reads `1.0` as below `5.83` and will not
offer v1.0. So the one-time cost of the reset is a single manual install of 1.0 for anyone
on 5.x. If that is unacceptable, the alternative is a bridge release labelled `5.84`
(versionCode 87) that carries the rule: 5.x devices update to it automatically, and it then
sees 1.0. That needs v1.0 rebuilt at versionCode 88 and a promotion step once devices have
migrated. Covered by `tests/test-version-reset-10.py`.

Everything else is the 5.83 feature set, untouched.

## Build record

| | |
| --- | --- |
| build run | `44746605843` success (the previous run failed on a CI gate: `a[0]` was read as a character class, so the gate now uses `grep -F`) |
| aligned APK | `dist/Kuro-tv-1.0-aligned.apk`, 43,938,049 B |
| signed APK | 44,008,672 B |
| SHA-256 | `5f590b0e621ec65e548f2fb8ea641bd8229e8b02c90c0623b0e15aee271df565` |
| source | `Kuro-tv-1.0-Source.zip`, 7,256,556 B (Drive `1tvE3FJdC_PIxGBN_fLlmqJMmRzjy65sO`) |
| signer | CN=Kuro Anime, `1d37c3ca…e93e` |

## Next

1.1 / versionCode 88, then 1.2 / 89 — one step each, name follows the release.
