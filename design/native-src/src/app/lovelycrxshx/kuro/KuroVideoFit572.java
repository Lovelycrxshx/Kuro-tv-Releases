package app.lovelycrxshx.kuro;

/**
 * Fit or crop for Kuro's own native video surfaces only.
 *
 * This changes the picture inside Kuro's ExoPlayer view. It never touches the window,
 * display cutout, rotation or fullscreen handling, so those existing behaviours stay
 * exactly as they were.
 */
final class KuroVideoFit572 {
    static final String FIT = "fit", CROP = "crop";
    static final String PREF = "native_fit572";
    private KuroVideoFit572() {}

    static String normalize(String value) {
        return CROP.equals(value) ? CROP : FIT;
    }

    static String next(String value) {
        return CROP.equals(normalize(value)) ? FIT : CROP;
    }

    /** Chip label describes what tapping does next. */
    static String actionLabel(String value) {
        return CROP.equals(normalize(value)) ? "Fit" : "Crop";
    }

    /** Spoken description, so the control is not label-only. */
    static String description(String value) {
        return CROP.equals(normalize(value))
                ? "Picture is cropped to fill the screen. Switch to fit to see the whole frame."
                : "Picture fits inside the screen with black bars when needed. Switch to crop.";
    }
}
