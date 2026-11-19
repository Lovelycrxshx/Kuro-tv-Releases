# Ten changes worth making — pick the ones you want

Each item is one mockup in `design/`, plus what it actually costs and which part of the app it
touches. Reply with the numbers (e.g. "1, 3, 9") and I'll build them in that order inside the
current version — no version bump unless you ask.

All mockups use the app's own palette: `#100C18` panel, `#211229` card, `#A65BE4` accent,
`#F3EEF7` ink, `#B7A4C7` muted. They're concept art, not pixel specs.

---

### 1. Progress on the continue cards — `design/improve-01-continue-progress.png`

The continue row shipped in 5.81 shows title and episode but not *how far in* you are. Add a 4px
progress bar and "Ep 12 of 28", and make the whole row a single wide card instead of a carousel so
long titles stop truncating.

**Cost:** S · touches `accounts542.js` + `accounts542.css`. Needs the player to write a position
ratio per episode (the player already reports position for resume, so this is a storage field, not
new plumbing).

---

### 2. "Year in anime" card — `design/improve-02-year-in-anime.png`

A seasonal recap built entirely from local data: episodes, series, hours, streak, a 12-month bar
chart and your most-watched title. Plus "Share as image" so it exports to a PNG.

**Cost:** M · new card in `accounts542.js`, one canvas render for the export. Hours are estimated
from episode count and runtime unless the player starts recording real watch time (that's the
honest version — worth doing together).

---

### 3. Milestones — `design/improve-03-badges.png`

Eight badges earned by watching, never bought: 100 episodes, 7-day streak, night owl, first finish,
500 bookmarks and so on. Locked ones are dimmed with a padlock — visible progress, no nagging.

**Cost:** S–M · pure local computation over history; the badges are CSS glyphs, so no art assets.

---

### 4. PIN sheet — `design/improve-04-pin-sheet.png`

The gate that makes profiles and kids mode real: 4-digit PIN, big DPAD-friendly keys, one quiet
error line, no shake animation under reduced motion. Stored as `sha256(salt‖pin)` per profile.

**Cost:** S · reuses `KuroDialogs581`; needed before any of the multi-profile work ships.

---

### 5. Home hero — `design/improve-05-home-hero.png`

Today Home is a stack of rows. Give it one strong hero (continuing series first, then new episodes),
page dots, and "NEW" chips on this week's releases. This is item #10 in your list and the biggest
visual win on the main screen.

**Cost:** M · `app.js` home render + a small "what's new this week" derivation from the catalogue.

---

### 6. "Because you watched …" — `design/improve-06-because-you-watched.png`

A recommendation row seeded by your last finished show, from the AniList call that already exists
(`catalog573.js`). The footer line states exactly what leaves the device — title only, never history.

**Cost:** S · the recommendation fetch is already implemented; this is presentation + one seed rule.

---

### 7. Library queue with progress rings — `design/improve-07-library-queue.png`

Rework the library into wide rows with per-series progress rings and a sticky "Up next" bar that
starts the next unwatched episode. This is the realistic version of your queue item (#13) without
building a full playlist model yet.

**Cost:** M · `app.js` library render; "next unwatched" needs episode counts per series.

---

### 8. Search with filter chips — `design/improve-08-search-filters.png`

Chips for Genre / Year / Format / Dubbed / Sort above live results, matched text highlighted in
accent, and result counts while typing. Item #9.

**Cost:** M · search v2; the catalogue response already carries most of these fields, so it's
filtering + chips, not new network work.

---

### 9. Player gestures + HUD — `design/improve-09-player-gestures.png`

Double-tap to seek ±10s with a fading overlay, vertical brightness/volume drag, a thinner scrub bar
and a tidy control row. Makes the phone player feel native instead of like a web video element.

**Cost:** M–L · player layer (`KuroNativePlayer561` + the WebView player); rishest item here for
regressions, so it wants a device test pass rather than CI alone.

---

### 10. Episode drawer + next-up countdown — `design/improve-10-episode-drawer.png`

A side sheet listing episodes with the current one pinned, watched ones ticked, and an "auto next in
8s" countdown you can cancel. This is the ask behind item #13's autoplay half.

**Cost:** M · reuses the existing episode list data; the countdown is a timer in the player host.

---

## Two more I couldn't render this turn

The image tool caps at 10 per turn, so these are described only — say the word and they're first in
the next batch:

**11. Downloads board** — per-episode progress, a storage bar ("12.4 GB of 20 GB free") and
Ready/Queue segments. Item #7; the download service already reports progress, this is the screen.

**12. Airing calendar** — a week strip with dots for the shows releasing each day, notification
bells per show, and a "tonight" block at the top. Item #11.

---

## How I'd order them

1. **4 → 3 → 1** (PIN, milestones, progress) — everything there is local data, so one build, no
   network risk, and it makes the account page feel finished.
2. **6 → 7 → 8** (recommendations, library queue, search) — the browsing half of the app.
3. **5 → 10 → 9** (home hero, episode drawer, player gestures) — the parts that touch playback, so
   they want a real device to test on.

Tell me the numbers and I'll start at the top of that list.
