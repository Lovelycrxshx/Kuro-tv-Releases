# Kuro Feature & Concept Reference

This file records all design concepts and feature directions the user has asked
to remember. Each entry maps to a concept preview in `kuro-concepts/`. These are
**design concepts only** — no code changes have been made unless noted.

---

## 1. Profile Art / Image Generation (Account section) — concepts 01–04

The user asked for image-generation improvements in the Account/Profile section.

| Preview | Feature |
|---------|---------|
| `01-ai-profile-art-studio.png` | AI Profile Art Studio — generate profile art from scratch |
| `02-style-variants-grid.png` | Style variants grid — multiple art styles to choose from |
| `03-theme-matched-art.png` | Theme-matched art — art that matches the app's theme |
| `04-live-preview-stage.png` | Live preview stage — see art before applying |

## 2. Image Generation outside the account section — concepts 05–08

The user wanted image generation placed in non-account surfaces too.

| Preview | Feature |
|---------|---------|
| `05-watch-screen-key-visual.png` | Watch-screen key visual generated art |
| `06-home-hero-banner-gen.png` | Home hero banner generation |
| `07-detail-key-visual-studio.png` | Detail page key visual studio |
| `08-gallery-ai-art.png` | Gallery AI art |

## 3. Kuro Chat (AI chat) — concepts 09–12

AI chat responding to anime requests via the Arena AI API.

| Preview | Feature |
|---------|---------|
| `09-kuro-chat-main.png` | Main Kuro Chat conversation |
| `10-kuro-chat-quick-prompts.png` | Quick prompts for fast requests |
| `11-kuro-chat-recommendation-card.png` | Recommendation card with a **Watch** button |
| `12-kuro-chat-embedded-browse.png` | Kuro Chat embedded in the browse layout |

## 4. News Section Logos — concepts 13–14

**User instruction:** The Crunchyroll and Anime Trending logos were showing a
generic "K". Give them proper logos — Crunchyroll its own logo, and Anime
Trending a good logo.

| Preview | Feature |
|---------|---------|
| `13-crunchyroll-logo.png` | Proper **Crunchyroll** logo (orange `#F47521` + orb mark) for the news section |
| `14-anime-trending-logo.png` | Proper **Anime Trending** logo (gradient trend-arrow wordmark) for the news section |

## 5. Profile Art section — collapsible / compact — concept 15

**User instruction:** Make the Profile Art section **short** using a
**click-to-expand** design, because the current full section pushes other
options down. Change the Profile Art section so other options can be seen
easily.

| Preview | Feature |
|---------|---------|
| `15-profile-art-collapsible.png` | Compact **click-to-expand** Profile Art card: thumbnail + "Tap to expand options" + chevron. Keeps the section short so other options stay visible. |

---

## Summary of standing design direction

1. **Profile Art section** → compact click-to-expand card (concept 15), so
   other options are not pushed down and stay easily visible.
2. **News logos** → replace the generic "K" with proper brand logos:
   - Crunchyroll → its own logo (concept 13)
   - Anime Trending → a good logo (concept 14)
3. **Kuro Chat** → AI chat for anime requests with main conversation, quick
   prompts, recommendation card + Watch button, and embedded-in-browse
   layout (concepts 09–12).
4. **Image generation** → expanded beyond the account section into watch,
   home, detail, and gallery surfaces (concepts 01–08).

## Status

- All 15 previews exist as generated mockups in `kuro-concepts/`.
- **No code changes implemented yet** — these are design references only.
- Implement only when the user explicitly asks.
