# Kuro 1.1 profile art — pairs 06–10

Ten avatar/banner pairs ship in 1.1: the original five (art-01…05) plus these.

| id | character | from |
|----|-----------|------|
| art-06 | Satoru Gojo — *Jujutsu Kaisen* | key art |
| art-07 | Sung Jinwoo — *Solo Leveling* | key art |
| art-08 | Frieren — *Frieren: Beyond Journey's End* | key art |
| art-09 | Yor Forger — *Spy x Family* | key art |
| art-10 | Makima — *Chainsaw Man* | key art |

All five titles are in the app's own catalogue (Anime#.txt entries 3, 4, 18, 10, 8).

## Files

- `pfp/art-NN.webp` — 256×256, square centre crop on the face (matches the 1.0 pack).
- `banner/art-NN.webp` — 1024×576, 16:9 crop from the same character's art, dark enough for the
  white text and the avatar chip that sit on top.
- `src/` — the untouched source images the crops came from (`SOURCES.md` has the URLs).
- `preview.png` — the whole set on one sheet.

Ready-to-ship copies live in `assets-1.1/profile-art/` (the build overlay).

## Swapping a character

Drop a replacement `src/` image in, re-crop to the sizes above, then replace the matching
`assets-1.1/profile-art/{pfp,banner}/art-NN.webp`. The picker reads the folder, so no code changes.

## Rights

These are third-party anime artworks (official/fan key art), used as user-selectable avatars inside
a third-party client — they are not original Kuro assets and are not covered by the repo LICENSE.
