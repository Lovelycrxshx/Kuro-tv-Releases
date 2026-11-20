#!/usr/bin/env python3
"""Kuro 1.3 - make the WebView serve the overlay assets.

MainActivity intercepts every `https://kuro.local/` request and answers it from
the APK's assets, but only for the paths it lists one by one. The 1.1 theme
files (`theme-<accent>-1.1.css`, `theme-picker-1.1.css`, `theme-accent-1.1.js`)
were copied into `assets/` and linked from `index.html`, and were never added to
that list - so the WebView dropped all of them: the accent stylesheet never
loaded (the app stayed violet) and the picker card never appeared, which is why
1.1 and 1.2 looked unchanged on a phone even though the files were in the APK.

This tool derives the served list from the overlay directories themselves, so an
overlay can never be shadowed again, and patches MainActivity to consult it
before the hand-written list.

Usage:
    python3 tools/webview_assets_1.3.py <project-dir> <overlay-dir> [<overlay-dir> ...]

<project-dir> is the directory holding `src/app/lovelycrxshx/kuro/`. The tool is
idempotent and refuses to run on a tree it does not recognise.
"""
import os
import sys

PKG = os.path.join("src", "app", "lovelycrxshx", "kuro")
ANCHOR = "shouldInterceptRequest(WebView v,WebResourceRequest req){Uri u=req.getUrl();"
INSERT = ("if(KuroWebAssets13.served(u)){try{return new WebResourceResponse("
          "KuroWebAssets13.mime(u.getPath()),\"UTF-8\",getAssets().open(u.getPath().substring(1)));}"
          "catch(IOException ignored){return new WebResourceResponse(\"text/plain\",\"UTF-8\",404,"
          "\"Missing\",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}}")

MIME = {
    ".html": "text/html",
    ".css": "text/css",
    ".js": "application/javascript",
    ".json": "application/json",
    ".webp": "image/webp",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".svg": "image/svg+xml",
}

JAVA = '''package app.lovelycrxshx.kuro;

import android.net.Uri;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Kuro 1.3 - the overlay assets the WebView is allowed to serve.
 *
 * MainActivity answers `https://kuro.local/...` from the APK only for the paths
 * it knows about. The 1.1 theme files were in the APK and linked from
 * index.html but missing from that list, so the WebView dropped them: the app
 * stayed violet and the Account picker never appeared. This class is generated
 * from the overlay directories by tools/webview_assets_1.3.py, so whatever an
 * overlay ships is served.
 */
final class KuroWebAssets13 {
    private static final Set<String> SERVED = new HashSet<>(Arrays.asList(
%(paths)s
    ));

    private KuroWebAssets13() {
    }

    static boolean served(Uri uri) {
        return "https".equals(uri.getScheme())
                && "kuro.local".equals(uri.getHost())
                && uri.getPath() != null
                && SERVED.contains(uri.getPath());
    }

    static String mime(String path) {
        int dot = path.lastIndexOf('.');
        String ext = dot < 0 ? "" : path.substring(dot).toLowerCase(java.util.Locale.ROOT);
        switch (ext) {
            case ".html": return "text/html";
            case ".css": return "text/css";
            case ".js": return "application/javascript";
            case ".json": return "application/json";
            case ".webp": return "image/webp";
            case ".png": return "image/png";
            case ".jpg":
            case ".jpeg": return "image/jpeg";
            case ".svg": return "image/svg+xml";
            default: return "application/octet-stream";
        }
    }
}
'''


def die(msg):
    sys.stderr.write("webview_assets_1.3: %s\n" % msg)
    raise SystemExit(1)


def overlay_files(dirs):
    out = []
    for base in dirs:
        if not os.path.isdir(base):
            die("overlay directory not found: %s" % base)
        for root, _, names in os.walk(base):
            for name in sorted(names):
                rel = os.path.relpath(os.path.join(root, name), base)
                path = "/" + rel.replace(os.sep, "/")
                if path not in out:
                    out.append(path)
    return sorted(out)


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    project, overlays = argv[1], argv[2:]
    pkg = os.path.join(project, PKG)
    if not os.path.isdir(pkg):
        die("%s does not look like the Kuro source tree (no %s)" % (project, PKG))

    paths = overlay_files(overlays)
    if not any(p.startswith("/theme-") for p in paths):
        die("no theme files in the overlays - refusing to write an empty list")
    listed = ",\n".join('        "%s"' % p for p in paths)
    target = os.path.join(pkg, "KuroWebAssets13.java")
    with open(target, "w", encoding="utf-8") as fh:
        fh.write(JAVA % {"paths": listed})

    activity = os.path.join(pkg, "MainActivity.java")
    with open(activity, encoding="utf-8") as fh:
        text = fh.read()
    if INSERT not in text:
        if ANCHOR not in text:
            die("MainActivity.java: shouldInterceptRequest no longer starts with the expected line")
        text = text.replace(ANCHOR, ANCHOR + INSERT, 1)
        with open(activity, "w", encoding="utf-8") as fh:
            fh.write(text)

    print("overlay files served: %d" % len(paths))
    for p in paths:
        print("  %s" % p)
    print("generated: %s" % target)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
