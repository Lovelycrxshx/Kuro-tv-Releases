# Kuro profile section — spec (for 5.81)

> **Status:** design only. Nothing is implemented until the user says **"open"**.
> This pulls item **#12 (profiles + kids mode)** forward from "deferred" into the active scope,
> and adds a new item: **#21 anime-themed profile art + banner set** (replacing what ships today).

---

## 1. What gets removed

The user's instruction: *remove all existing profiles and banners present*. Turned into an
explicit, checkable list — each row is confirmed before anything is deleted.

| # | Asset / data | Where it lives today | Action | Recoverable? |
|---|---|---|---|---|
| 1 | Default profile avatars (the current built-in set) | `res/drawable-nodpi/` or `assets/profiles/` | **Delete** — replaced by the 6-theme set | In git history |
| 2 | Default profile banners | same | **Delete** | In git history |
| 3 | Implicit "default profile" row created on first launch | app DB | **Convert** to `Profile 1` — keeps watch history, gains new avatar | n/a (kept) |
| 4 | Home hero banner art baked with text | `res/drawable/` | **Delete** — text moves to live layout so it can be localised | In git history |
| 5 | Any placeholder "add profile" ghost cards in the picker | code/layout | **Rewrite** as part of the new picker | n/a |

**Not in this list, deliberately:** the four logo PNGs in the Drive *Kuro* folder. Those are app
branding (launcher/header), not profile art. If the user actually means those too, say so and they
get replaced only on explicit confirmation — deleting the app logo is not recoverable from Drive
trash once emptied.

---

## 2. Profile model

Up to **4 profiles**, stored in the same Room DB, every user-owned table gains `profileId`
(default `1` so the migration is a no-op for existing installs).

| Table | Per-profile? | Notes |
|---|---|---|
| `watch_state` | ✅ | history, resume points (see PLAN §A4) |
| `my_list` | ✅ | follow/watchlist |
| `downloads` | ⚠️ shared library, tagged | One Media3 `DownloadManager`; each entry records `profileId`. Separate dirs per profile would fragment ~10 GB of media — tag and filter instead |
| `settings` | ✅ | theme, subtitle prefs, source pin, RPC privacy |
| `follows` | ✅ | drives #11 notifications |
| `search_history` | ✅ | |

`profile` table: `id, name, avatarTheme, bannerTheme, isKids, pinHash (nullable), createdAt,
lastUsedAt, accentSeed`.

- **PIN:** stored as a salted hash (PBKDF2, 100k iters) — never plaintext, never in prefs.
- **Kids profile:** rating filter, Discord RPC forced off, downloads limited to allowed ratings, and
  leaving the profile requires the PIN. Enforced in the repositories, not just the UI.
- **Deleting a profile** deletes its history/list/follows; downloads are *untagged* (become
  "shared") rather than deleted — deleting a profile should never wipe 10 GB of video without a
  second confirm.

---

## 3. Screens & flows

**Picker ("Who's watching?")** — appears on cold start when more than one profile exists, or on
long-press of the profile chip. Cards are 2:3 with the theme banner as backdrop, avatar centre,
name below, "PIN" lock badge for protected ones. DPAD: left/right between cards, OK to enter, and
the focused card scales + brightens (the only motion on this screen).

**Switch without leaving playback** — the player's overflow menu gets "Switch profile"; it stops
playback, saves the resume point, and returns to the picker.

**Create / edit** — name (≤ 16 chars), avatar theme, banner theme, kids toggle, optional PIN.
Avatars are picked from the 6 themes as a horizontal row of large tiles, not a grid of tiny thumbs
(remote-friendly).

**Reset** — two levels, both under Settings → Profiles:
- *Reset art* — restores all avatars/banners to the shipped themes. Non-destructive.
- *Erase all profiles* — deletes profiles, history, lists, follows; keeps downloads. Requires typing
  `ERASE` on a phone or pressing-and-holding OK for 3 s on TV, plus the PIN of a non-kids profile.

**Back behaviour:** picker is not finishable by back (you must choose a profile); everything else
pops normally. Exit from the picker = OK on "Exit Kuro".

---

## 4. The anime theme set (6 themes)

All art **original** — no copyrighted character likenesses, no studio logos. This is not just legal
hygiene: the APK is GPL-3.0 and ships the assets, so they must be redistributable. Third-party art
is acceptable only if CC0/CC-BY and credited in-settings.

| ID | Profile theme | Palette | Avatar art direction | Banner art direction |
|---|---|---|---|---|
| `sakura` | **Sakura Drift** | `#F7A8C4` · `#2B1B33` · `#6C4AB6` | Girl silhouette on a rooftop rail, petals, dusk gradient, single backlight rim | Wide rooftop skyline at blue hour, petal bokeh, left third empty for copy |
| `neon` | **Neon Tokyo** | `#22D3EE` · `#FF2E97` · `#0B0B14` | Hooded figure under a neon sign glow, rain streaks, reflected signage | Rain-slick alley, wet reflections, magenta/cyan signage bokeh, dark left gradient |
| `shonen` | **Shōnen Sunset** | `#FF7A18` · `#16BFA8` · `#2A1633` | Spiky-hair silhouette, arms crossed, sun disc behind, lens flare streak | Rooftop edge into a huge low sun, dust motes, silhouette on the right |
| `mecha` | **Mecha Midnight** | `#5B8DEF` · `#FFB020` · `#0E1220` | Giant mecha eye close-up, panel seams, amber iris glow | Hangar interior, mecha shoulder silhouette, volumetric light shafts |
| `ink` | **Ghost Ink** | `#EDE6DB` · `#B3261E` · `#101010` | Fox mask in sumi-e ink wash, one vermillion stroke | Ink-wash mountains and a torii, red seal stamp bottom-right |
| `ramune` | **Ramune Summer** | `#7FD1FF` · `#FFFFFF` · `#FFD75E` | Bicycle against a seaside railing, midday haze, gingham ribbon | Coastal road, power lines, cumulus wall, high-key sky |

**Where each theme is used**
1. **Profile card** in the picker — banner as 2:3 crop backdrop, avatar as a 96 dp circle.
2. **Home hero** — banner as the top-of-screen backdrop behind the "Continue watching" callout,
   scrimmed so text stays ≥ 4.5:1.
3. **Settings header** — banner at 25 % opacity behind the profile chip.
4. **Accent colour** — the profile's accent seed tints focus rings, chips and the progress bar, so a
   profile *feels* like itself without restructuring the UI.

---

## 5. Art specifications (implementation-ready)

| Asset | Format | Size | Budget | Path |
|---|---|---|---|---|
| Avatar | WebP q85, square | 512 × 512 | ≤ 40 KB each | `res/drawable-nodpi/profile_av_<id>.webp` |
| Banner | WebP q80, 16:9 | 1920 × 1080 | ≤ 150 KB each | `res/drawable-nodpi/profile_bn_<id>.webp` |
| Picker backdrop | WebP q75, 3:4 | 720 × 960 | ≤ 90 KB each | `res/drawable-nodpi/profile_card_<id>.webp` |
| Fallback gradient | XML | — | — | `res/drawable/profile_grad_<id>.xml` |

- Total added size ≈ **1.4 MB** across 6 themes — acceptable against a ≤ 35 MB target, and it means
  the whole profile section works **offline** (TV boxes with no network still look right).
- **No text baked into any image.** All copy is live layout, so it can be translated and so rename
  doesn't need new art.
- **Text-safe zones:** left 45 % of every banner stays low-detail (scrim + copy); the subject sits
  right. Checked at 5 % overscan.
- Palette pairs are drawn from the theme table above; accent seed per theme is fixed so a profile
  can never end up with an unreadable focus ring.
- All six are generated at "open", downscaled, WebP-encoded, and committed under `res/` — no runtime
  downloads, no CDN.

---

## 6. Effect on the release plan

Profiles were **#12 in the deferred list**. Pulling them forward plus the new art item:

- #12 profiles + kids ≈ **8 d**, #21 art set + integration ≈ **2 d** → **+10 d**.
- Recommended placement: **wave B (5.82)** as a coherent "living-room identity" release with
  #6 skip-intro, #8 DPAD and #10 Home — profiles touch all three (Continue row per profile, picker
  traversal, per-profile hero banner).
- Wave B then becomes ~30 d. Alternative: make profiles its own **5.82** and push the rest to 5.83.

## 7. Acceptance criteria

1. Four profiles coexist; switching swaps Continue, My List, follows and subtitle prefs instantly,
   with no app restart.
2. Upgrading a 5.80 install lands its history in `Profile 1` with the `sakura` theme — nothing lost.
3. Kids profile can't reach a mature title from search, Home, or a deep link, and can't leave without
   the PIN (verified by instrumented test, not by eyeballing the UI).
4. `apksigner verify` + 16 KB alignment audit still pass; arm64 APK still ≤ 35 MB.
5. Every new screen fully traversable with 4 directions + OK, focus ring always visible.
6. All banner art leaves the left 45 % quiet and readable at 5 % overscan on a 1080p TV.
7. No copyrighted character likeness or studio mark in any shipped asset.

## 8. Decisions needed before "open"

1. **Deletion confirmation** — is the removal list in §1 correct, and are the four Drive logo PNGs
   *out* of scope (they're branding, not profile art)?
2. **Upgrade behaviour** — adopt existing history as `Profile 1` (recommended) or hard-wipe on
   first launch of the new build?
3. **Art source** — generate all six themes as original art (recommended, GPL-clean), or supply your
   own images for any of the six?
4. **Placement** — profiles as part of 5.82 with #6/#8/#10, or its own release?
