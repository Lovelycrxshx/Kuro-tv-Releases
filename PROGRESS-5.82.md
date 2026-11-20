# Kuro 5.82 — progress

Branch: `arena/98533644-kuro-tv-releases` · first cut: versionCode **85**, versionName **5.82**

## Cut 1 — version rollover (this build)

This cut exists for one reason: **the updater gates on a strictly newer version code.** 5.81 was
pinned at 84 for its whole line, so nothing built on it was ever offered to anyone already on
5.81. 85 is the first version code the in-app updater can see.

Work done:

- `build.gradle` and `AndroidManifest.xml` → `versionCode 85` / `versionName "5.82"`.
- Source re-zipped as `Kuro-tv-5.82-Source.zip` (7,234,614 B) and replaced on Drive
  (`1ygvb4HDRZXicUvueHqGYdSRt8utim8T0`); `build-current.yml` retargeted to the new branch and
  source id.
- `publish-5.81.yml` → `publish-5.82.yml`, tag `v5.82`, APK + checksum ids filled in.
- `RELEASE-5.82.md` written; `index.html` moved to 5.82 (badge, links, hash, size, What's new).

No Java was touched. The APK is byte-for-byte the 5.81 rev 11.5 code, re-signed under the same
key, carrying:

- skip suggestions for every episode, with source and user markers taking precedence and
  auto-skip never firing on a suggestion;
- updater v2 — pinned release signer, resumable download, SHA-256 verified before install.

| artifact | bytes | sha256 |
| --- | --- | --- |
| `Kuro-tv-5.82.apk` | 44,008,672 | `81f8b66ea6651b28cffdeca4f313f7e6e8e802a61f88ff91597e29b790ecb435` |
| `Kuro-tv-5.82-Source.zip` | 7,234,614 | — |

Signer: CN=Kuro Anime, `1D:37:C3:CA:28:ED:80:CC:48:46:01:A7:A7:90:7D:5C:43:97:0A:5D:57:D5:1A:A4:E9:14:75:A6:07:03:E9:3E`.

## Next cuts (85 stays)

5.82 feature work has not started. Candidates, in the order they were ranked for the 5.81 line:

1. **DPAD / focus overhaul** — the highest-impact change left for Fire TV. Every screen gets a
   deliberate focus path, larger hit targets on TV density, and no focus lost to decorative views.
2. **Home v2 skeletons** — load state that mirrors the final layout instead of a spinner.
3. **Airing calendar + new-episode notifications** — a schedule surface, opt-in per series.
4. **Theming v2** — Material 3 token pass, true AMOLED black, overscan-safe TV margins.

Each of these ships as an ordinary cut in this line: same `versionCode` 85, same tag `v5.82`
updated, install over the top. 5.83 opens the next version code.
