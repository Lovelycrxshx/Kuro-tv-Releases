#!/usr/bin/env python3
"""Kuro 1.2 - give the native (Java) layer the same accents as the web layer.

1.1 re-hued the stylesheets, so every *web* pixel follows the picker in Account.
The Java side kept its own copy of the purple palette: the status and navigation
bars, the stock alert dialogs, the player options panel, the watch header, the
episode drawer, the download bar and the update dialog. This tool

  * collects every ARGB literal the native palette is built from,
  * re-hues each one into every accent with the same HLS transform that
    tools/theme_gen.py uses for the CSS, so the two layers agree colour for
    colour,
  * writes `KuroTheme12.java`, which holds that table, remembers the choice on
    the phone and repaints whatever is already registered with it,
  * rewrites the palette constants and inline literals to look their colour up
    through `KuroTheme12.col(...)`, and
  * gives the stock dialogs one theme per accent.

Usage:
    python3 tools/theme_native_1.2.py <project-dir>

<project-dir> is the directory that holds `src/app/lovelycrxshx/kuro/*.java`
and `res/values/styles.xml` (i.e. the root of the source zip). The tool is
idempotent: running it twice produces the same tree, and it refuses to run on a
tree it does not recognise instead of guessing.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from theme_gen import ACCENTS, remap_hex  # noqa: E402

PKG = os.path.join("src", "app", "lovelycrxshx", "kuro")
# Keep this order in step with theme_gen.ACCENTS: the generated Java table and
# the picker both address accents by index, and the dialog styles are named after
# the id.
ACCENT_ORDER = list(ACCENTS)         # picker order: signature, cool, warm
# The picker's swatch values.  An accent added to theme_gen.ACCENTS without an
# entry here still works: accent_hexes() falls back to its deployed stylesheet.
ACCENT_HEX = {
    "teal": "27d7c5", "violet": "a65be4", "plum": "d252e6", "graphite": "989898",
    "sky": "2e92d9", "ice": "25b4e0", "royal": "805be7", "sapphire": "4f7ce8",
    "indigo": "3f54dd", "emerald": "2ed984", "mint": "32e2ad", "lime": "8fe444",
    "citron": "e2e756", "gold": "e9c25a", "amber": "dc9b3b", "sunset": "de7548",
    "coral": "ed9183", "rose": "e968a9", "crimson": "de4866", "magenta": "e050c3",
}


def accent_hexes(project):
    """Accent id -> its swatch hex, straight out of the deployed stylesheet."""
    hexes = dict(ACCENT_HEX)
    for name in ACCENT_ORDER:
        if name in hexes:                  # the table wins: shipped look stays put
            continue
        path = os.path.join(project, "assets", "theme-%s-1.1.css" % name)
        try:
            with open(path, encoding="utf-8") as fh:
                m = re.search(r"--ku-accent:\s*#([0-9a-fA-F]{6})\s*;", fh.read())
        except OSError:
            m = None
        if m:
            hexes[name] = m.group(1).lower()
    return hexes

# Files whose `static final int` constants are the palette. Each entry lists the
# exact declaration line and the constant names in source order.
CONST_FILES = {
    "KuroDialogs581.java": [
        "    public static final int INK = 0xfff3eef7;",
        "    public static final int MUTED = 0xffb7a4c7;",
        "    public static final int PURPLE = 0xffa65be4;",
        "    public static final int PURPLE_SOFT = 0xffc088f4;",
        "    public static final int PANEL = 0xff100c18;",
        "    public static final int CARD = 0xff211229;",
        "    public static final int BORDER = 0xff665078;",
        "    public static final int SELECTED = 0xff2b1a3a;",
        "    public static final int DIVIDER = 0xff3a2c4a;",
    ],
    "KuroEpisodeDrawer581.java": [
        "    private static final int PANEL=0xff100c18, CARD=0xff211229, PURPLE=0xffa65be4, "
        "SELECTED=0xff2b1a3a, INK=0xfff3eef7, MUTED=0xffb7a4c7, BORDER=0xff665078;",
    ],
    "PlayerOptionsDialog.java": [
        " private static final int INK=0xfff7f1fc,MUTED=0xffb7a9c1,PURPLE=0xffb58bf1,"
        "CARD=0xff221b2b,BORDER=0xff594369;",
    ],
    "WatchHeader.java": [
        " private static final int BACKGROUND=0xff100c17,CHIP=0xff251730,"
        "BORDER=0xff4c3558,ICON=0xffe3d7ee;",
    ],
    "PlaybackPanel.java": [
        " private static final int INK=0xfff5edf9,MUTED=0xffb7a4c3;",
    ],
}

# Files that keep their literals inline; those literals get wrapped in col(...).
INLINE_FILES = [
    "MainActivity.java",
    "KuroDialogs581.java",
    "KuroEpisodeDrawer581.java",
    "PlayerOptionsDialog.java",
    "WatchHeader.java",
    "PlaybackPanel.java",
    "KuroUpdateDialog582.java",
    "KuroNativePlayer561.java",
    "KuroOfflineActivity561.java",
    "KuroCaptionStyle571.java",
]

ARGB = re.compile(r"0x([0-9a-fA-F]{8})\b")


def die(msg):
    sys.stderr.write("theme_native_1.2: %s\n" % msg)
    raise SystemExit(1)


def code_literals(text):
    """ARGB literals outside string/char literals and comments, with positions."""
    found = []
    i, n = 0, len(text)
    quote = None
    while i < n:
        c = text[i]
        if quote:
            if c == "\\":
                i += 2
                continue
            if c == quote:
                quote = None
            i += 1
            continue
        if c in "\"'":
            quote = c
            i += 1
            continue
        if text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j + 1
            continue
        if text.startswith("/*", i):
            j = text.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        m = ARGB.match(text, i)
        if m:
            found.append((i, m.group(0), m.group(1)))
            i = m.end()
            continue
        i += 1
    return found


def variants(base):
    """ARGB int -> list of ARGB ints, one per accent, alpha preserved."""
    alpha, rgb = base[:2], base[2:]
    out = []
    for name in ACCENT_ORDER:
        out.append(int(alpha + remap_hex(rgb, ACCENTS[name]), 16))
    return out


def collect_bases(project):
    bases = set()
    for name in INLINE_FILES:
        path = os.path.join(project, PKG, name)
        with open(path, encoding="utf-8") as fh:
            for _, _, digits in code_literals(fh.read()):
                bases.add(digits.lower())
    # the window floor is used through Color.rgb(10,7,15) as well
    bases.add("ff0a070f")
    return sorted(bases)


KURO_THEME_12 = '''package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kuro 1.2 - the native half of the accent theme.
 *
 * The web layer re-hues itself with a stylesheet per accent (theme-*-1.1.css,
 * chosen by the Account picker). The Java side - status and navigation bars,
 * stock alert dialogs, player options, watch header, episode drawer, download
 * bar, update dialog - uses its own baked-in palette, which stayed violet in
 * 1.1. This class carries the same five accents for that half: every literal
 * colour the native UI is built from is listed once in {@link #TABLE}, with the
 * value it becomes in each accent, generated by tools/theme_native_1.2.py.
 *
 * Widgets ask for a colour with {@code KuroTheme12.col(0xffa65be4)} - the 1.0
 * violet value is the key, so the source keeps reading like the palette it was
 * written with - and classes that cache their palette in fields register a
 * {@code retint()} runnable so an accent switch repaints them too.
 *
 * This file is generated; edit tools/theme_native_1.2.py instead.
 */
public final class KuroTheme12 {
    /** Accent ids, in picker order. */
    public static final String[] ACCENTS = {%(names)s};
    public static final String DEFAULT = "violet";

    private static final int VIOLET = 0;
    /** Base ARGB (the 1.0 violet value), then the value in each accent. */
    private static final int[][] TABLE = {
%(rows)s    };

    private static final int[] DIALOG_THEMES = {%(dialog_themes)s};

    private static final List<Runnable> LISTENERS = new ArrayList<>();
    private static int accent = VIOLET;
    private static boolean amoled;
    private static Map<Integer, Integer> map = table(VIOLET);
    private static boolean loaded;

    private KuroTheme12() {
    }

    private static Map<Integer, Integer> table(int index) {
        Map<Integer, Integer> out = new HashMap<>(TABLE.length * 2);
        for (int[] row : TABLE) {
            out.put(row[0], row[index + 1]);
        }
        return out;
    }

    /** Register a repaint callback; it also runs once, immediately. */
    public static void register(Runnable listener) {
        if (listener == null || LISTENERS.contains(listener)) {
            return;
        }
        LISTENERS.add(listener);
        listener.run();
    }

    public static String accent() {
        return ACCENTS[accent];
    }

    public static boolean amoled() {
        return amoled;
    }

    /** The colour {@code base} becomes in the current accent. Unknown values pass through. */
    public static int col(int base) {
        if (amoled) {
            switch (base) {
                case 0xff0a070f:
                case 0xff100c18:
                case 0xff100c17:
                case 0xff0d0914:
                case 0xff0e0916:
                case 0xff110b19:
                    return 0xff000000;
                case 0xff211229:
                case 0xff221b2b:
                case 0xff251730:
                case 0xff1b1325:
                case 0xff1a1224:
                    return 0xff0a0a0e;
                default:
                    break;
            }
        }
        Integer mapped = map.get(base);
        return mapped == null ? base : mapped;
    }

    /** The app floor: window, WebView and bar background. */
    public static int background() {
        return col(0xff0a070f);
    }

    /** The accent itself, for highlights and focus rings. */
    public static int accentColor() {
        return col(0xffa65be4);
    }

    /** Theme for the stock alert dialogs (KuroDialogs581.themed uses this). */
    public static int dialogTheme() {
        return DIALOG_THEMES[accent];
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("kuro-native-12", Context.MODE_PRIVATE);
    }

    /** Read the saved accent once, early in onCreate. */
    public static void init(Context context) {
        if (loaded || context == null) {
            return;
        }
        loaded = true;
        SharedPreferences p = prefs(context);
        String saved = p.getString("accent", DEFAULT);
        if (!p.getBoolean("default_violet_154", false)) {
            SharedPreferences.Editor ed = p.edit().putBoolean("default_violet_154", true);
            if ("teal".equals(saved)) {
                saved = DEFAULT;
                ed.putString("accent", DEFAULT);
            }
            ed.apply();
        }
        amoled = p.getBoolean("amoled", false);
        accent = indexOf(saved);
        apply();
    }

    private static int indexOf(String id) {
        for (int i = 0; i < ACCENTS.length; i++) {
            if (ACCENTS[i].equals(id)) {
                return i;
            }
        }
        return VIOLET;
    }

    /** Switch accent: persist, rebuild the map, repaint everything registered. */
    public static void set(Context context, String id) {
        int next = indexOf(id);
        if (context != null) {
            prefs(context).edit().putString("accent", ACCENTS[next]).apply();
        }
        loaded = true;
        if (next == accent) {
            return;
        }
        accent = next;
        apply();
        if (context instanceof Activity) {
            applyWindow((Activity) context);
        }
    }

    /** 1.5.2: toggle pure-black OLED surfaces alongside the chosen accent. */
    public static void setAmoled(Context context, boolean on) {
        if (context != null) {
            prefs(context).edit().putBoolean("amoled", on).apply();
        }
        loaded = true;
        if (on == amoled) {
            return;
        }
        amoled = on;
        apply();
        if (context instanceof Activity) {
            applyWindow((Activity) context);
        }
    }

    private static void apply() {
        map = table(accent);
        for (Runnable listener : new ArrayList<>(LISTENERS)) {
            listener.run();
        }
    }

    /** Tint the system bars to the current floor. */
    public static void applyWindow(Activity activity) {
        if (activity == null || activity.getWindow() == null) {
            return;
        }
        int floor = background();
        activity.getWindow().setStatusBarColor(floor);
        activity.getWindow().setNavigationBarColor(floor);
        android.view.View decor = activity.getWindow().getDecorView();
        if (decor != null) {
            // The bars are always dark whatever the accent, so keep the icons light.
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~8192 & ~16);
        }
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).kuroTheme12WindowApplied(floor);
        }
    }

    static boolean isKnown(String id) {
        for (String known : ACCENTS) {
            if (known.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
'''


def write_palette(project, bases):
    rows = []
    for base in bases:
        values = variants(base)
        rows.append("        {0x%s, %s}, // %s\n" % (
            base, ", ".join("0x%08x" % v for v in values), base[2:]))
    names = ", ".join('"%s"' % n for n in ACCENT_ORDER)
    themes = ",\n        ".join("R.style.KuroDialog%s" % n.capitalize() for n in ACCENT_ORDER)
    path = os.path.join(project, PKG, "KuroTheme12.java")
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(KURO_THEME_12 % {"names": names, "rows": "".join(rows),
                                  "dialog_themes": themes})
    return path, len(bases)


def retint_block(names):
    lines = ["    static {"]
    lines.append("        KuroTheme12.register(%s::retint);" % names["cls"])
    lines.append("    }")
    lines.append("")
    lines.append("    static void retint() {")
    for const, base in names["consts"]:
        lines.append("        %s = KuroTheme12.col(0x%s);" % (const, base))
    lines.append("    }")
    return "\n".join(lines)


def rewrite_constants(project):
    """Turn the `static final int` palette into fields the accent can repaint."""
    for filename, decls in CONST_FILES.items():
        path = os.path.join(project, PKG, filename)
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        if "KuroTheme12.register(" in text:
            continue                                     # already done
        consts = []
        for line in decls:
            if line not in text:
                die("%s: palette declaration changed, refusing to guess:\n  %s"
                    % (filename, line))
            for name, base in re.findall(r"([A-Z][A-Z0-9_]*)\s*=\s*0x([0-9a-fA-F]{8})", line):
                consts.append((name, base.lower()))
        names = {"cls": filename[:-5], "consts": consts}
        joined = "\n".join(decls)
        field_decl = "    static int %s;" % ", ".join(n for n, _ in consts)
        text = text.replace(joined, field_decl + "\n" + retint_block(names), 1)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)


def wrap_inline(project):
    """Fold every remaining palette literal in the native UI into col(...)."""
    total = 0
    for filename in INLINE_FILES:
        path = os.path.join(project, PKG, filename)
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        literals = code_literals(text)
        if not literals:
            continue
        out, last = [], 0
        for pos, literal, _ in literals:
            if text[max(0, pos - 16):pos] == "KuroTheme12.col(":
                continue                                  # already wrapped
            out.append(text[last:pos])
            out.append("KuroTheme12.col(%s)" % literal)
            last = pos + len(literal)
            total += 1
        out.append(text[last:])
        with open(path, "w", encoding="utf-8") as fh:
            fh.write("".join(out))
    return total


def patch_dialog_theme(project):
    """Send the framework alerts to the style of the current accent."""
    path = os.path.join(project, PKG, "KuroDialogs581.java")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    if "KuroTheme12.dialogTheme()" in text:
        return
    old = "new ContextThemeWrapper(context, R.style.KuroDialogTheme)"
    if old not in text:
        die("KuroDialogs581.java: themed() no longer uses R.style.KuroDialogTheme")
    text = text.replace(old, "new ContextThemeWrapper(context, KuroTheme12.dialogTheme())", 1)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)


def patch_main_activity(project):
    path = os.path.join(project, PKG, "MainActivity.java")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()

    bars = ("getWindow().setStatusBarColor(Color.rgb(10,7,15));"
            "getWindow().setNavigationBarColor(Color.rgb(10,7,15));")
    if bars in text:
        text = text.replace(
            bars,
            "KuroTheme12.init(this);KuroTheme12.applyWindow(this);", 1)

    floor = "Color.rgb(10,7,15)"
    while floor in text:
        text = text.replace(floor, "KuroTheme12.background()", 1)

    bridge = ("class Bridge{")
    method = ('class Bridge{\n  @android.webkit.JavascriptInterface public void themeAccent111('
              'final String id){if(id==null||!KuroTheme12.isKnown(id))return;'
              'runOnUiThread(()->{KuroTheme12.set(MainActivity.this,id);'
              'KuroTheme12.applyWindow(MainActivity.this);if(ui!=null)'
              'ui.setBackgroundColor(KuroTheme12.background());});}\n'
              '  @android.webkit.JavascriptInterface public void themeAmoled152('
              'final boolean on){runOnUiThread(()->{KuroTheme12.setAmoled(MainActivity.this,on);'
              'KuroTheme12.applyWindow(MainActivity.this);if(ui!=null)'
              'ui.setBackgroundColor(KuroTheme12.background());});}\n')
    if "themeAccent111" not in text:
        if bridge not in text:
            die("MainActivity.java: Bridge class not found")
        text = text.replace(bridge, method, 1)

    if "kuroTheme12WindowApplied" not in text:
        anchor = "void showEntry(){"
        if anchor not in text:
            die("MainActivity.java: showEntry() not found")
        hook = ("/** Called by KuroTheme12.applyWindow after an accent switch. */\n"
                "void kuroTheme12WindowApplied(int floor){if(ui!=null)"
                "ui.setBackgroundColor(floor);if(root!=null)root.setBackgroundColor(floor);}\n"
                + anchor)
        text = text.replace(anchor, hook, 1)

    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)


PANEL_BASE = "100c18"
BORDER_BASE = "665078"

PANEL_XML = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Kuro 1.2: rounded dialog panel for the %(name)s accent (generated by
     tools/theme_native_1.2.py). -->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#FF%(panel)s" />
    <corners android:radius="20dp" />
    <stroke
        android:width="1dp"
        android:color="#FF%(border)s" />
</shape>
'''

STYLES = '''<resources>
    <style name="KuroTheme" parent="android:style/Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:statusBarColor">@android:color/black</item>
        <item name="android:navigationBarColor">@android:color/black</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>

    <!-- Kuro 1.2: the stock alerts follow the accent picked in Account. The
         content-rich dialogs are built by KuroDialogs581 on top of the same
         palette; this only covers the framework alerts that still exist. -->
    <style name="KuroDialogBase" parent="android:style/Theme.Material.Dialog.Alert">
        <item name="android:windowBackground">@drawable/kuro_dialog_panel_violet</item>
        <item name="android:textColorPrimary">%TEAL_INK%</item>
        <item name="android:textColorSecondary">%TEAL_MUTED%</item>
        <item name="android:textColorPrimaryInverse">#FF0F0A16</item>
        <item name="android:backgroundDimAmount">0.78</item>
        <item name="android:windowMinWidthMajor">48%</item>
        <item name="android:windowMinWidthMinor">78%</item>
    </style>
%STYLES%
    <!-- Kept so an older call site or a stale resource reference still lands on
         a themed dialog; the current code asks KuroTheme12.dialogTheme(). -->
    <style name="KuroDialogTheme" parent="KuroDialogViolet"/>
</resources>
'''

STYLE_ITEM = '''    <style name="KuroDialog%(NAME)s" parent="KuroDialogBase">
        <item name="android:windowBackground">@drawable/kuro_dialog_panel_%(ACCENTNAME)s</item>
        <item name="android:colorAccent">#FF%(ACCENT)s</item>
        <item name="android:textColorHighlight">#66%(ACCENT)s</item>
    </style>
'''


def patch_styles(project):
    path = os.path.join(project, "res", "values", "styles.xml")
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    if "KuroDialogTeal" in text:
        return
    if "#FFA65BE4" not in text:
        die("res/values/styles.xml: the violet accent is gone, refusing to guess")
    hexes = accent_hexes(project)
    styles = "".join(
        STYLE_ITEM % {"NAME": name.capitalize(), "ACCENT": hexes[name].upper(),
                      "ACCENTNAME": name}
        for name in ACCENT_ORDER)
    drawables = os.path.join(project, "res", "drawable")
    for name in ACCENT_ORDER:
        panel = remap_hex(PANEL_BASE, ACCENTS[name])
        border = remap_hex(BORDER_BASE, ACCENTS[name])
        with open(os.path.join(drawables, "kuro_dialog_panel_%s.xml" % name),
                  "w", encoding="utf-8") as fh:
            fh.write(PANEL_XML % {"name": name, "panel": panel.upper(),
                                  "border": border.upper()})
    out = (STYLES.replace("%STYLES%", styles)
           .replace("%TEAL_INK%", "#FFF3EEF7")
           .replace("%TEAL_MUTED%", "#FFB7A4C7"))
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(out)


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 2
    project = argv[1]
    if not os.path.isdir(os.path.join(project, PKG)):
        die("%s does not look like the Kuro source tree (no %s)" % (project, PKG))

    bases = collect_bases(project)
    rewrite_constants(project)
    wrapped = wrap_inline(project)
    patch_main_activity(project)
    patch_dialog_theme(project)
    patch_styles(project)
    path, rows = write_palette(project, bases)

    print("palette rows : %d (%d accents)" % (rows, len(ACCENT_ORDER)))
    print("wrapped      : %d literals" % wrapped)
    print("dialog panels: %d drawables" % len(ACCENT_ORDER))
    print("generated    : %s" % path)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
