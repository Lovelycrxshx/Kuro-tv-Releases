#!/usr/bin/env python3
"""Generate the 1.1 accent themes from the shipped 1.0 stylesheets.

The app's palette is a fluorescent purple family (hue ~272) spread over
hundreds of literal hex values.  Re-theming by hand would drift, so this
tool re-hues every purple-family colour in place, preserving lightness and
roughly preserving saturation, and leaves every other hue alone (error reds,
link blues, artwork-independent neutrals).

Usage:
    python3 tools/theme_gen.py <assets-dir> <out-dir>

<assets-dir> must be an extracted copy of the app's `assets/` folder (e.g.
`unzip dist/Kuro-tv-1.0-aligned.apk 'assets/*' -d build`).  <out-dir> gets
theme-<accent>-1.1.css for every accent in ACCENTS.
"""
import colorsys
import os
import re
import sys

# name -> (hue in degrees, anchor lightness, saturation scale).
# Anchor keeps every accent as visually heavy as the purple it replaces: teal
# and sky read lighter than purple at the same HLS lightness, crimson and amber
# read heavier. Saturation scale 0 turns an accent into a grey ramp (graphite),
# which keeps the depth of the palette without any hue at all.
ACCENTS = {
    # signature
    "teal":     (174, 0.50, 1.0),   # default
    "violet":   (272, 0.63, 1.0),   # the 1.0 look, kept as an escape hatch
    "graphite": (272, 0.60, 0.0),   # neutral: no hue, same depth
    # cool
    "sky":      (205, 0.52, 1.0),
    "indigo":   (232, 0.56, 1.0),
    "emerald":  (150, 0.52, 1.0),
    # warm
    "amber":    (36, 0.55, 1.0),
    "sunset":   (18, 0.58, 1.0),
    "crimson":  (348, 0.58, 1.0),
    "magenta":  (312, 0.60, 1.0),
}
DEFAULT = "teal"

# Files whose purple literals form the app's chrome.
SOURCES = ["style.css", "accounts542.css", "profile-art-arm.css",
           "profile-settings560.css", "smooth-ui.css", "library-tools571.css"]

PURPLE_LO, PURPLE_HI = 236.0, 330.0     # only this band is re-hued
MIN_SAT = 0.10                          # below this a colour is chrome grey
NEUTRAL_SAT = 0.30                      # below this the shift is half strength

HEX8 = re.compile(r"#([0-9a-fA-F]{8})(?![0-9a-fA-F\w])")
HEX6 = re.compile(r"#([0-9a-fA-F]{6})(?![0-9a-fA-F\w])")
HEX3 = re.compile(r"(?<![\w&])#([0-9a-fA-F]{3})(?![0-9a-fA-F\w])")
RGBA = re.compile(r"\brgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*(,\s*[0-9.]+\s*)?\)")


def _rgb(h):
    return [int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4)]


def _hex(rgb):
    return "".join("%02x" % max(0, min(255, int(round(c * 255)))) for c in rgb)


def _blend(h_from, h_to, t):
    d = ((h_to - h_from + 540) % 360) - 180
    return (h_from + d * t) % 360


def remap_hex(h, accent):
    """Return the re-hued hex (lowercase, no '#') or the original."""
    target_hue, anchor, sat_scale = accent
    r, g, b = _rgb(h)
    hue, light, sat = colorsys.rgb_to_hls(r, g, b)
    hue *= 360.0
    if sat < MIN_SAT or not (PURPLE_LO <= hue <= PURPLE_HI):
        return h.lower()
    if target_hue == 272 and sat_scale == 1.0:   # the 1.0 look: only tidy the case
        return h.lower()
    if light >= 0.88:
        # Ink and near-white tints stay neutral with just a hint of the accent.
        new_hue, new_sat, new_light = target_hue, min(sat, 0.08) * sat_scale, light
    elif light <= 0.25:
        # Backgrounds and panel tints keep their depth; they travel most of the
        # way to the accent so the app floor reads as the chosen colour.
        new_hue = target_hue
        new_sat = sat * sat_scale
        new_light = light
    elif sat < NEUTRAL_SAT:
        # Muted text keeps its lightness and leans only slightly, so greys
        # stay greys instead of turning sickly.
        new_hue = _blend(hue, target_hue, 0.35 + 0.5 * sat)
        new_sat = sat * 0.90 * sat_scale
        new_light = light
    else:
        new_hue = target_hue
        new_sat = min(1.0, 0.05 + sat * 0.90) * sat_scale
        new_light = max(0.26, min(0.97, anchor + (light - 0.63) * 0.85))
    return _hex(colorsys.hls_to_rgb(new_hue / 360.0, new_light, new_sat))


def transform(text, accent):
    def sub8(m):
        return "#" + remap_hex(m.group(1)[:6], accent) + m.group(1)[6:].lower()

    def sub6(m):
        return "#" + remap_hex(m.group(1), accent)

    def sub3(m):
        full = "".join(c * 2 for c in m.group(1))
        return "#" + remap_hex(full, accent)

    def subrgb(m):
        r, g, b = (int(m.group(i)) for i in (1, 2, 3))
        new = remap_hex("%02x%02x%02x" % (r, g, b), accent)
        if new == ("%02x%02x%02x" % (r, g, b)).lower():
            return m.group(0)
        triple = ", ".join(str(int(new[i:i + 2], 16)) for i in (0, 2, 4))
        return "rgb(%s)" % triple if m.group(4) is None else "rgba(%s%s)" % (triple, m.group(4))

    text = HEX8.sub(sub8, text)
    text = HEX6.sub(sub6, text)
    # Only rewrite short hex when it really is a colour, never inside #id text.
    text = HEX3.sub(sub3, text)
    text = RGBA.sub(subrgb, text)
    return text


def main(argv):
    if len(argv) != 3:
        print(__doc__)
        return 2
    assets, out = argv[1], argv[2]
    os.makedirs(out, exist_ok=True)
    sources = [f for f in SOURCES if os.path.exists(os.path.join(assets, f))]
    missing = [f for f in SOURCES if f not in sources]
    if missing:
        print("warning: missing stylesheets: %s" % ", ".join(missing))
    wrote = []
    for name, accent in ACCENTS.items():
        parts = ["/* Kuro 1.1 accent theme: %s (generated by tools/theme_gen.py) */\n" % name]
        for f in sources:
            with open(os.path.join(assets, f), encoding="utf-8") as fh:
                parts.append("/* %s */\n" % f + transform(fh.read(), accent) + "\n")
        path = os.path.join(out, "theme-%s-1.1.css" % name)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write("".join(parts))
        wrote.append((name, path, os.path.getsize(path)))
    for name, path, size in wrote:
        print("%-8s %7d bytes  %s" % (name, size, path))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
