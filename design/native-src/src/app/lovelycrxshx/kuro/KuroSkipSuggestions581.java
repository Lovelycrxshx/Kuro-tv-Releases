package app.lovelycrxshx.kuro;

/**
 * Suggested intro/outro windows for episodes whose source ships no markers.
 *
 * Anime episodes almost always open with a ~90 s opening theme and end with an
 * ending theme, but the HLS route Kuro uses carries no chapter data, so a Skip
 * button would never appear. These windows are deliberately conservative: they
 * only exist on real-length episodes, they never overlap, and they are always
 * labelled as suggestions so nothing is skipped automatically on a guess.
 */
final class KuroSkipSuggestions581 {
    /** Below this, an episode is a short or a special and guessing would be rude. */
    static final long MIN_EPISODE_MS = 120_000L;
    static final long INTRO_START_MS = 8_000L;
    static final long INTRO_END_MS = 98_000L;
    static final long OUTRO_LENGTH_MS = 90_000L;
    /** Leave the last seconds alone so the next-episode hand-off still happens. */
    static final long OUTRO_TAIL_MS = 2_000L;

    private KuroSkipSuggestions581() {}

    static KuroPlayback561.Range intro(long durationMs) {
        if (durationMs < MIN_EPISODE_MS) return null;
        long end = Math.min(INTRO_END_MS, durationMs - MIN_EPISODE_MS / 2);
        return end > INTRO_START_MS ? new KuroPlayback561.Range(INTRO_START_MS, end) : null;
    }

    static KuroPlayback561.Range outro(long durationMs) {
        if (durationMs < MIN_EPISODE_MS) return null;
        long end = durationMs - OUTRO_TAIL_MS;
        long start = end - OUTRO_LENGTH_MS;
        return start > INTRO_END_MS && end > start ? new KuroPlayback561.Range(start, end) : null;
    }
}
