#!/usr/bin/env python3
"""Keep the theme picker's colour table in step with the generated themes.

The picker is two things that must never disagree:

  * the ACCENTS array in assets-1.1/theme-accent-1.1.js - id, label, group and
    the swatch the tile shows;
  * one `html[data-accent=<id>]` block in assets-1.1/theme-picker-1.1.css that
    paints the live preview (--pv-panel, --pv-card, --pv-ink, --pv-muted) plus
    the `.accent-swatch111` rule with that accent's colour.

Only accents that are missing are written, so the ten styles that shipped in
1.4 keep their hand-tuned preview colours byte for byte.  Run with --check to
verify the two files match the table without touching them.

Usage:
    python3 tools/theme_picker_gen.py <repo-root> [--check]
"""
import colorsys
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from theme_gen import ACCENTS as THEMES  # noqa: E402  (single source of truth)

# id, label, group, swatch.  The swatch is the colour the tile shows; it is the
# accent's own colour, not the lifted --ku-accent token, so it reads as a solid
# chip against the dark card.
TABLE = [
    ("teal",     "Teal",     "Signature", "#27d7c5"),
    ("violet",   "Violet",   "Signature", "#a65be4"),
    ("plum",     "Plum",     "Signature", "#d252e6"),
    ("graphite", "Graphite", "Signature", "#989898"),
    ("sky",      "Sky",      "Cool",      "#2e92d9"),
    ("ice",      "Ice",      "Cool",      "#25b4e0"),
    ("royal",    "Royal",    "Cool",      "#805be7"),
    ("sapphire", "Sapphire", "Cool",      "#4f7ce8"),
    ("indigo",   "Indigo",   "Cool",      "#3f54dd"),
    ("emerald",  "Emerald",  "Cool",      "#2ed984"),
    ("mint",     "Mint",     "Cool",      "#32e2ad"),
    ("lime",     "Lime",     "Cool",      "#8fe444"),
    ("citron",   "Citron",   "Warm",      "#e2e756"),
    ("gold",     "Gold",     "Warm",      "#e9c25a"),
    ("amber",    "Amber",    "Warm",      "#dc9b3b"),
    ("sunset",   "Sunset",   "Warm",      "#de7548"),
    ("coral",    "Coral",    "Warm",      "#ed9183"),
    ("rose",     "Rose",     "Warm",      "#e968a9"),
    ("crimson",  "Crimson",  "Warm",      "#de4866"),
    ("magenta",  "Magenta",  "Warm",      "#e050c3"),
]

GROUPS = ["Signature", "Cool", "Warm"]


def rgb(hex_str):
    h = hex_str.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def hx(r, g, b):
    return "#%02x%02x%02x" % (
        max(0, min(255, int(round(r * 255)))),
        max(0, min(255, int(round(g * 255)))),
        max(0, min(255, int(round(b * 255)))),
    )


def preview_colours(swatch):
    """A dark panel and card tinted by the accent, plus ink and a pale line."""
    r, g, b = (v / 255.0 for v in rgb(swatch))
    hue, light, sat = colorsys.rgb_to_hls(r, g, b)
    if sat < 0.05:                       # graphite: an honest grey ramp
        panel = hx(light * 0 + 0.071, 0.071, 0.071)
        card = hx(0.118, 0.118, 0.118)
        ink = hx(0.949, 0.949, 0.949)
        muted = hx(0.714, 0.714, 0.714)
        return panel, card, ink, muted
    panel = hx(*colorsys.hls_to_rgb(hue, 0.070, 0.33))
    card = hx(*colorsys.hls_to_rgb(hue, 0.115, 0.39))
    ink = hx(*colorsys.hls_to_rgb(hue, 0.950, 0.12))
    muted = hx(*colorsys.hls_to_rgb(hue, 0.712, 0.09))
    return panel, card, ink, muted


def check_table():
    ids = [row[0] for row in TABLE]
    if len(set(ids)) != len(ids):
        raise SystemExit("duplicate accent id in TABLE")
    if sorted(ids) != sorted(THEMES):
        missing = sorted(set(THEMES) - set(ids))
        extra = sorted(set(ids) - set(THEMES))
        raise SystemExit("TABLE and theme_gen.ACCENTS disagree "
                         "(themes only: %s, table only: %s)" % (missing, extra))
    for row in TABLE:
        if row[2] not in GROUPS:
            raise SystemExit("unknown group %r for %s" % (row[2], row[0]))


def js_array():
    width = max(len(r[1]) for r in TABLE)
    lines = []
    for name, label, group, swatch in TABLE:
        lines.append('  { id: %-11s label: %-11s group: %-11s swatch: %s },'
                     % ("'%s'," % name, "'%s'," % label,
                        "'%s'," % group, "'%s'" % swatch))
    return "\n".join(lines)


def css_blocks(existing):
    out = []
    for name, _label, _group, swatch in TABLE:
        if ("html[data-accent=%s]{" % name) in existing:
            continue
        panel, card, ink, muted = preview_colours(swatch)
        out.append("html[data-accent=%s]{--pv-accent:%s;--pv-panel:%s;--pv-card:%s;"
                   "--pv-ink:%s;--pv-muted:%s}" % (name, swatch, panel, card, ink, muted))
    return out


def css_swatches(existing):
    out = []
    for name, _label, _group, swatch in TABLE:
        if (".accent-tile111[data-accent=%s] .accent-swatch111{" % name) in existing:
            continue
        out.append(".accent-tile111[data-accent=%s] .accent-swatch111{background:%s}"
                   % (name, swatch))
    return out


def main(argv):
    check_only = "--check" in argv
    args = [a for a in argv[1:] if not a.startswith("--")]
    root = Path(args[0]) if args else Path(__file__).resolve().parent.parent
    check_table()

    js_path = root / "assets-1.1" / "theme-accent-1.1.js"
    css_path = root / "assets-1.1" / "theme-picker-1.1.css"
    js = js_path.read_text(encoding="utf-8")
    css = css_path.read_text(encoding="utf-8")

    m = re.search(r"var ACCENTS = \[\n(.*?)\n \];", js, re.S)
    if not m:
        raise SystemExit("could not find the ACCENTS array in %s" % js_path)
    if check_only:
        ok = m.group(1) == js_array()
        missing_css = css_blocks(css) + css_swatches(css)
        if ok and not missing_css:
            print("theme picker is in step: %d accents, every colour present" % len(TABLE))
            return 0
        print("theme picker is out of step:"
              + ("" if ok else " ACCENTS array differs;")
              + (" %d colour block(s) missing" % len(missing_css) if missing_css else ""))
        return 1

    if m.group(1) != js_array():
        js = js[:m.start(1)] + js_array() + js[m.end(1):]
        js_path.write_text(js, encoding="utf-8")
        print("rewrote the ACCENTS array: %d accents" % len(TABLE))

    blocks = css_blocks(css)
    if blocks:
        last = css.rfind("html[data-accent=")
        line_end = css.index("\n", css.index("}", last)) + 1
        css = css[:line_end] + "\n".join(blocks) + "\n" + css[line_end:]
        print("added %d preview block(s)" % len(blocks))

    swatches = css_swatches(css)
    if swatches:
        if ".accent-swatch111{background" in css:
            last = css.rfind(".accent-swatch111{background")
            line_end = css.index("\n", last) + 1
            css = css[:line_end] + "\n".join(swatches) + "\n" + css[line_end:]
        else:
            css += "\n" + "\n".join(swatches) + "\n"
        print("added %d swatch rule(s)" % len(swatches))

    css_path.write_text(css, encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
