#!/usr/bin/env python3
"""1.5 / 1.5.1: in-app update button bridge, up-to-date dialog state, and boot veil.

1. Exposes KuroNative.updateButton15() on MainActivity.Bridge and delegates to
   checkAndMaybeInstall582() so KuroUpdateCenter582.checkNow(this, true) runs on
   the worker pool instead of throwing NetworkOnMainThreadException on the UI thread.
2. Clears stale dlBytes / dlTotal / dlSpeed / assetSize when the installed build
   is already current, and adds Snapshot.upToDate() so KuroUpdateDialog582 shows
   "You are on the latest version" ("UP TO DATE" chip, no spinning ProgressBar,
   no leftover "100%").
3. Dismisses MainActivity.showEntry()'s boot veil as soon as index.html finishes
   loading so the #km-intro-15 launch sequence is visible from its first frame.

Idempotent: running twice changes nothing.
"""

import sys
from pathlib import Path

BRIDGE_ANCHOR = " @JavascriptInterface public void settingsAction560(String action){"
ACTIVITY_ANCHOR = " private void openUpdateDialog582(){"

BRIDGE = (
    " @JavascriptInterface public void updateButton15()"
    "{main.post(()->MainActivity.this.updateButton15());}\n"
)

ACTIVITY = """ /* 1.5.1: the in-app Update button. Run the check off the main thread via
  * checkAndMaybeInstall582() and open the theme-aware update screen immediately. */
 private void updateButton15(){
  if(isFinishing())return;
  KuroUpdater572.prefs(this).edit().putString("stage",KuroUpdateCenter582.CHECKING).putString("lastError","").apply();
  checkAndMaybeInstall582();
 }
"""


def replace_once(src: str, old: str, new: str, label: str) -> str:
    if new in src:
        return src
    if old not in src:
        raise RuntimeError(f"missing anchor for {label}")
    return src.replace(old, new, 1)


def patch_main_activity(pkg: Path) -> None:
    main = pkg / "MainActivity.java"
    if not main.is_file():
        raise RuntimeError(f"MainActivity.java not found at {main}")
    src = main.read_text(encoding="utf-8", errors="surrogateescape")

    # Upgrade any earlier main-thread updateButton15() implementation if present.
    old_btn = (
        " private void updateButton15(){\n"
        "  if(isFinishing())return;\n"
        "  try{KuroUpdateCenter582.checkNow(this,true);}catch(RuntimeException ignored){}\n"
        "  openUpdateDialog582();\n"
        " }\n"
    )
    if old_btn in src:
        src = src.replace(old_btn, ACTIVITY, 1)
    elif "private void updateButton15(){" not in src:
        if BRIDGE_ANCHOR not in src:
            raise RuntimeError("could not find Bridge anchor (settingsAction560)")
        src = src.replace(BRIDGE_ANCHOR, BRIDGE + BRIDGE_ANCHOR, 1)
        if ACTIVITY_ANCHOR not in src:
            raise RuntimeError("could not find openUpdateDialog582 anchor")
        src = src.replace(ACTIVITY_ANCHOR, ACTIVITY + ACTIVITY_ANCHOR, 1)

    # Keep background catch-up checks quiet unless a real transfer/verification is running.
    src = replace_once(
        src,
        "if(snap.busy()&&player==null&&nativePlayer561==null&&KuroUpdateDialog582.autoOpenAllowed(MainActivity.this))openUpdateDialog582();",
        "if((snap.transferring()||snap.verifying())&&player==null&&nativePlayer561==null&&KuroUpdateDialog582.autoOpenAllowed(MainActivity.this))openUpdateDialog582();",
        "MainActivity.updateWatch582",
    )

    # Dismiss the native boot veil as soon as index.html loads so #km-intro-15 plays in full view.
    src = replace_once(
        src,
        'if("https://kuro.local/index.html".equals(url)){uiReady=true;openPendingNews();',
        'if("https://kuro.local/index.html".equals(url)){uiReady=true;dismissEntry15();openPendingNews();',
        "MainActivity.onPageFinished dismissEntry15",
    )

    bg_expr = "KuroTheme12.background()" if "KuroTheme12.background()" in src else "Color.rgb(10,7,15)"
    old_entry = (
        'void showEntry(){FrameLayout splash=new FrameLayout(this);splash.setBackgroundColor(Color.BLACK);'
        'ImageView logo=new ImageView(this);int resId=getResources().getIdentifier("splash_logo","drawable",getPackageName());'
        'if(resId!=0)logo.setImageResource(resId);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);'
        'FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,dp(220),Gravity.CENTER);lp.setMargins(dp(20),0,dp(20),0);'
        'splash.addView(logo,lp);root.addView(splash,new FrameLayout.LayoutParams(-1,-1));'
        'logo.setAlpha(0f);logo.setScaleX(.93f);logo.setScaleY(.93f);'
        'logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setInterpolator(new android.view.animation.DecelerateInterpolator()).setDuration(700).start();'
        'main.postDelayed(()->splash.animate().alpha(0f).setDuration(500).withEndAction(()->{if(root!=null)root.removeView(splash);}).start(),1500);}'
    )
    new_entry = (
        "private FrameLayout entrySplash15;"
        "private void dismissEntry15(){FrameLayout s=entrySplash15;if(s==null)return;entrySplash15=null;"
        "s.animate().alpha(0f).setDuration(200).withEndAction(()->{if(root!=null)root.removeView(s);}).start();}\n"
        f" void showEntry(){{FrameLayout splash=new FrameLayout(this);splash.setBackgroundColor({bg_expr});"
        "splash.setClickable(false);entrySplash15=splash;root.addView(splash,new FrameLayout.LayoutParams(-1,-1));"
        "main.postDelayed(this::dismissEntry15,650);}"
    )
    if "dismissEntry15(){FrameLayout" not in src:
        if old_entry not in src:
            raise RuntimeError("missing anchor for MainActivity.showEntry")
        src = src.replace(old_entry, new_entry, 1)

    main.write_text(src, encoding="utf-8", errors="surrogateescape")


def patch_updater(pkg: Path) -> None:
    f = pkg / "KuroUpdater572.java"
    if not f.is_file():
        raise RuntimeError(f"KuroUpdater572.java not found at {f}")
    src = f.read_text(encoding="utf-8", errors="surrogateescape")

    old_adv = (
        '        if (!isNewer(release.version, installedVersion)) {\n'
        '            if (p.getInt("notifications", 0) != 0 || !p.getString("stage", "").isEmpty() || !p.getString("tag", "").isEmpty()) {\n'
        '                p.edit().putInt("notifications", 0).putLong("lastNotifyAt", 0).putInt("downloadFailures", 0)\n'
        '                        .putString("stage", "").putString("tag", "").putString("version", "")\n'
        '                        .putString("lastError", "").putLong("lastDownloadAt", 0).apply();\n'
        '            }\n'
        '            return Step.none("up_to_date");\n'
        '        }'
    )
    new_adv = (
        '        if (!isNewer(release.version, installedVersion)) {\n'
        '            if (p.getInt("notifications", 0) != 0 || !p.getString("stage", "").isEmpty()\n'
        '                    || !p.getString("tag", "").isEmpty()\n'
        '                    || p.getLong("dlBytes", 0) != 0 || p.getLong("dlTotal", 0) != 0\n'
        '                    || p.getLong("assetSize", 0) != 0) {\n'
        '                p.edit().putInt("notifications", 0).putLong("lastNotifyAt", 0).putInt("downloadFailures", 0)\n'
        '                        .putString("stage", "").putString("tag", "").putString("version", "")\n'
        '                        .putLong("dlBytes", 0).putLong("dlTotal", 0).putLong("dlSpeed", 0)\n'
        '                        .putLong("assetSize", 0)\n'
        '                        .putString("lastError", "").putLong("lastDownloadAt", 0).apply();\n'
        '            }\n'
        '            return Step.none("up_to_date");\n'
        '        }'
    )
    src = replace_once(src, old_adv, new_adv, "KuroUpdater572.advance")
    f.write_text(src, encoding="utf-8", errors="surrogateescape")


def patch_center(pkg: Path) -> None:
    f = pkg / "KuroUpdateCenter582.java"
    if not f.is_file():
        raise RuntimeError(f"KuroUpdateCenter582.java not found at {f}")
    src = f.read_text(encoding="utf-8", errors="surrogateescape")

    old_check = (
        '        if (!KuroUpdater572.isNewer(release.version, KuroUpdater572.installedVersionName(c))) {\n'
        '            p.edit().putString("stage", "").putString("lastError", "")\n'
        '                    .putString("latestSeen", release.version).putString("latestTag", release.tag).apply();\n'
        '            return false;\n'
        '        }\n'
        '        if (immediate || KuroUpdateSchedule583.dueNow(c, release.publishedAt)) {\n'
        '            KuroUpdateSchedule583.clear(c);\n'
        '            startDownload(c, release);\n'
        '        } else if (!autoEnabled(c)) {\n'
        '            // Reminders-only mode keeps its 5.72 behaviour; nothing is scheduled here.\n'
        '            return true;\n'
        '        } else {'
    )
    new_check = (
        '        if (!KuroUpdater572.isNewer(release.version, KuroUpdater572.installedVersionName(c))) {\n'
        '            p.edit().putString("stage", "").putString("lastError", "")\n'
        '                    .putString("tag", "").putString("version", "")\n'
        '                    .putLong("dlBytes", 0).putLong("dlTotal", 0).putLong("dlSpeed", 0)\n'
        '                    .putLong("assetSize", 0)\n'
        '                    .putString("latestSeen", release.version).putString("latestTag", release.tag).apply();\n'
        '            return false;\n'
        '        }\n'
        '        if (immediate || KuroUpdateSchedule583.dueNow(c, release.publishedAt)) {\n'
        '            KuroUpdateSchedule583.clear(c);\n'
        '            startDownload(c, release);\n'
        '        } else if (!autoEnabled(c)) {\n'
        '            // Reminders-only mode keeps its 5.72 behaviour; nothing is scheduled here.\n'
        '            p.edit().putString("stage", "").apply();\n'
        '            return true;\n'
        '        } else {'
    )
    src = replace_once(src, old_check, new_check, "KuroUpdateCenter582.checkNow")

    old_snap = (
        '    static final class Snapshot {\n'
        '        final String stage, version, error;\n'
        '        final long bytes, total, speed, updatedAt, dueAt;\n'
        '        final int percent;\n\n'
        '        Snapshot(String stage, String version, String error, long bytes, long total, long speed, long updatedAt) {\n'
        '            this(stage, version, error, bytes, total, speed, updatedAt, 0L);\n'
        '        }\n\n'
        '        Snapshot(String stage, String version, String error, long bytes, long total, long speed, long updatedAt, long dueAt) {\n'
        '            this.dueAt = dueAt;\n'
        '            this.stage = stage == null ? "" : stage;\n'
        '            this.version = version == null ? "" : version;\n'
        '            this.error = error == null ? "" : error;\n'
        '            this.bytes = bytes;\n'
        '            this.total = total;\n'
        '            this.speed = speed;\n'
        '            this.updatedAt = updatedAt;\n'
        '            this.percent = total > 0 ? (int) Math.max(0, Math.min(100, bytes * 100L / total)) : -1;\n'
        '        }\n\n'
        '        boolean checking() { return CHECKING.equals(stage); }\n'
        '        boolean scheduled() { return SCHEDULED.equals(stage); }\n'
        '        boolean verifying() { return VERIFYING.equals(stage); }\n'
        '        boolean ready() { return READY.equals(stage); }\n'
        '        boolean failed() { return ERROR.equals(stage) || (!error.isEmpty() && stage.isEmpty()); }\n'
        '        boolean paused() { return WAITING_WIFI.equals(stage); }\n\n'
        '        boolean transferring() {\n'
        '            return DOWNLOADING.equals(stage) || QUEUED.equals(stage) || WAITING_WIFI.equals(stage);\n'
        '        }\n\n'
        '        /** Anything the update screen should stay open for. */\n'
        '        boolean busy() { return checking() || transferring() || verifying(); }\n'
        '    }\n\n'
        '    static Snapshot snapshot(Context c) {\n'
        '        SharedPreferences p = prefs(c);\n'
        '        long total = p.getLong("dlTotal", p.getLong("assetSize", 0));\n'
        '        long bytes = p.getLong("dlBytes", 0);\n'
        '        if (total <= 0) total = p.getLong("assetSize", 0);\n'
        '        return new Snapshot(p.getString("stage", ""), p.getString("version", ""),\n'
        '                p.getString("lastError", ""), bytes, total,\n'
        '                p.getLong("dlSpeed", 0), p.getLong("dlUpdatedAt", 0),\n'
        '                p.getLong("dueAt583", 0));\n'
        '    }'
    )
    new_snap = (
        '    static final class Snapshot {\n'
        '        final String stage, version, error, installed, latestSeen;\n'
        '        final long bytes, total, speed, updatedAt, dueAt;\n'
        '        final int percent;\n\n'
        '        Snapshot(String stage, String version, String error, long bytes, long total, long speed, long updatedAt) {\n'
        '            this(stage, version, error, bytes, total, speed, updatedAt, 0L, "", "");\n'
        '        }\n\n'
        '        Snapshot(String stage, String version, String error, long bytes, long total, long speed, long updatedAt, long dueAt) {\n'
        '            this(stage, version, error, bytes, total, speed, updatedAt, dueAt, "", "");\n'
        '        }\n\n'
        '        Snapshot(String stage, String version, String error, long bytes, long total, long speed, long updatedAt,\n'
        '                 long dueAt, String installed, String latestSeen) {\n'
        '            this.dueAt = dueAt;\n'
        '            this.stage = stage == null ? "" : stage;\n'
        '            this.version = version == null ? "" : version;\n'
        '            this.error = error == null ? "" : error;\n'
        '            this.installed = installed == null ? "" : installed;\n'
        '            this.latestSeen = latestSeen == null ? "" : latestSeen;\n'
        '            this.bytes = bytes;\n'
        '            this.total = total;\n'
        '            this.speed = speed;\n'
        '            this.updatedAt = updatedAt;\n'
        '            this.percent = total > 0 ? (int) Math.max(0, Math.min(100, bytes * 100L / total)) : -1;\n'
        '        }\n\n'
        '        boolean checking() { return CHECKING.equals(stage); }\n'
        '        boolean scheduled() { return SCHEDULED.equals(stage); }\n'
        '        boolean verifying() { return VERIFYING.equals(stage); }\n'
        '        boolean ready() { return READY.equals(stage); }\n'
        '        boolean failed() { return ERROR.equals(stage) || (!error.isEmpty() && stage.isEmpty()); }\n'
        '        boolean paused() { return WAITING_WIFI.equals(stage); }\n\n'
        '        boolean transferring() {\n'
        '            return DOWNLOADING.equals(stage) || QUEUED.equals(stage) || WAITING_WIFI.equals(stage);\n'
        '        }\n\n'
        '        /** True when a check completed and GitHub has no newer release than what is installed. */\n'
        '        boolean upToDate() {\n'
        '            return stage.isEmpty() && error.isEmpty() && !latestSeen.isEmpty()\n'
        '                    && !KuroUpdater572.isNewer(latestSeen, installed);\n'
        '        }\n\n'
        '        /** Anything the update screen should stay open for. */\n'
        '        boolean busy() { return checking() || transferring() || verifying(); }\n'
        '    }\n\n'
        '    static Snapshot snapshot(Context c) {\n'
        '        SharedPreferences p = prefs(c);\n'
        '        long total = p.getLong("dlTotal", p.getLong("assetSize", 0));\n'
        '        long bytes = p.getLong("dlBytes", 0);\n'
        '        if (total <= 0) total = p.getLong("assetSize", 0);\n'
        '        return new Snapshot(p.getString("stage", ""), p.getString("version", ""),\n'
        '                p.getString("lastError", ""), bytes, total,\n'
        '                p.getLong("dlSpeed", 0), p.getLong("dlUpdatedAt", 0),\n'
        '                p.getLong("dueAt583", 0), KuroUpdater572.installedVersionName(c),\n'
        '                p.getString("latestSeen", ""));\n'
        '    }'
    )
    src = replace_once(src, old_snap, new_snap, "KuroUpdateCenter582.Snapshot")

    old_detail = (
        '            return text.toString();\n'
        '        }\n'
        '        return "Kuro checks the public release page about once a day.";\n'
        '    }'
    )
    new_detail = (
        '            return text.toString();\n'
        '        }\n'
        '        if (s.upToDate()) {\n'
        '            String cur = s.installed.isEmpty() ? s.latestSeen : s.installed;\n'
        '            return cur.isEmpty()\n'
        '                    ? "No newer release was found on GitHub."\n'
        '                    : "Kuro " + cur + " is installed — no newer release was found.";\n'
        '        }\n'
        '        return "Kuro checks the public release page about once a day.";\n'
        '    }'
    )
    src = replace_once(src, old_detail, new_detail, "KuroUpdateCenter582.detail")

    old_sum = (
        '        return autoEnabled(c)\n'
        '                ? "New releases install by themselves " + KuroUpdateSchedule583.delayLabel(KuroUpdateSchedule583.delayHours(c))\n'
        '                        + " after publication, or on the next check once that window has passed."\n'
        '                : "Kuro checks for new releases and reminds you; you start the download.";'
    )
    new_sum = (
        '        int dh = KuroUpdateSchedule583.delayHours(c);\n'
        '        String when = dh <= 0 ? "as soon as a release is published" : ("after " + dh + " hours from publication");\n'
        '        return autoEnabled(c)\n'
        '                ? "New releases install by themselves " + when + ", or on the next check once that window has passed."\n'
        '                : "Kuro checks for new releases and reminds you; you start the download.";'
    )
    src = replace_once(src, old_sum, new_sum, "KuroUpdateCenter582.summary")

    f.write_text(src, encoding="utf-8", errors="surrogateescape")


def patch_dialog(pkg: Path) -> None:
    f = pkg / "KuroUpdateDialog582.java"
    if not f.is_file():
        raise RuntimeError(f"KuroUpdateDialog582.java not found at {f}")
    src = f.read_text(encoding="utf-8", errors="surrogateescape")

    old_refresh = (
        '        percent.setText(s.percent >= 0 && !s.scheduled() ? s.percent + "%" : "—");\n'
        '        if (s.percent >= 0 && (s.transferring() || s.verifying() || s.ready())) {\n'
        '            bar.setIndeterminate(false);\n'
        '            bar.setProgress(Math.max(0, Math.min(1000, s.percent * 10)));\n'
        '        } else if (s.ready()) {\n'
        '            bar.setIndeterminate(false);\n'
        '            bar.setProgress(1000);\n'
        '        } else {\n'
        '            bar.setIndeterminate(true);\n'
        '        }'
    )
    new_refresh = (
        '        if (s.transferring() || s.verifying() || s.ready()) {\n'
        '            percent.setVisibility(View.VISIBLE);\n'
        '            percent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);\n'
        '            percent.setText(s.ready() ? "100%" : (s.percent >= 0 ? s.percent + "%" : "0%"));\n'
        '            bar.setVisibility(View.VISIBLE);\n'
        '            if (s.ready()) {\n'
        '                bar.setIndeterminate(false);\n'
        '                bar.setProgress(1000);\n'
        '            } else if (s.percent >= 0) {\n'
        '                bar.setIndeterminate(false);\n'
        '                bar.setProgress(Math.max(0, Math.min(1000, s.percent * 10)));\n'
        '            } else {\n'
        '                bar.setIndeterminate(true);\n'
        '            }\n'
        '        } else if (s.checking()) {\n'
        '            percent.setVisibility(View.GONE);\n'
        '            bar.setVisibility(View.VISIBLE);\n'
        '            bar.setIndeterminate(true);\n'
        '        } else if (s.upToDate()) {\n'
        '            percent.setVisibility(View.VISIBLE);\n'
        '            percent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);\n'
        '            percent.setText("You are on the latest version");\n'
        '            bar.setVisibility(View.GONE);\n'
        '        } else {\n'
        '            percent.setVisibility(View.GONE);\n'
        '            bar.setVisibility(View.GONE);\n'
        '        }'
    )
    src = replace_once(src, old_refresh, new_refresh, "KuroUpdateDialog582.refresh")

    old_key_chip = (
        '    private static String key(KuroUpdateCenter582.Snapshot s) {\n'
        '        if (s.ready()) return "ready";\n'
        '        if (s.failed()) return "failed";\n'
        '        if (s.checking()) return "checking";\n'
        '        if (s.verifying()) return "verifying";\n'
        '        if (s.transferring()) return "transferring";\n'
        '        if (s.scheduled()) return "scheduled";\n'
        '        return "idle";\n'
        '    }\n\n'
        '    private static String chip(KuroUpdateCenter582.Snapshot s) {\n'
        '        if (s.ready()) return "READY";\n'
        '        if (s.verifying()) return "VERIFYING";\n'
        '        if (s.checking()) return "CHECKING";\n'
        '        if (s.paused()) return "WAITING";\n'
        '        if (s.scheduled()) return "SCHEDULED";\n'
        '        if (s.failed()) return "FAILED";\n'
        '        if (s.transferring()) return "DOWNLOADING";\n'
        '        return "IDLE";\n'
        '    }'
    )
    new_key_chip = (
        '    static String key(KuroUpdateCenter582.Snapshot s) {\n'
        '        if (s.ready()) return "ready";\n'
        '        if (s.failed()) return "failed";\n'
        '        if (s.checking()) return "checking";\n'
        '        if (s.verifying()) return "verifying";\n'
        '        if (s.transferring()) return "transferring";\n'
        '        if (s.scheduled()) return "scheduled";\n'
        '        if (s.upToDate()) return "up_to_date";\n'
        '        return "idle";\n'
        '    }\n\n'
        '    static String chip(KuroUpdateCenter582.Snapshot s) {\n'
        '        if (s.ready()) return "READY";\n'
        '        if (s.verifying()) return "VERIFYING";\n'
        '        if (s.checking()) return "CHECKING";\n'
        '        if (s.paused()) return "WAITING";\n'
        '        if (s.scheduled()) return "SCHEDULED";\n'
        '        if (s.failed()) return "FAILED";\n'
        '        if (s.transferring()) return "DOWNLOADING";\n'
        '        if (s.upToDate()) return "UP TO DATE";\n'
        '        return "IDLE";\n'
        '    }'
    )
    src = replace_once(src, old_key_chip, new_key_chip, "KuroUpdateDialog582.chip")

    f.write_text(src, encoding="utf-8", errors="surrogateescape")


def patch(project: Path) -> int:
    pkg = project / "src" / "app" / "lovelycrxshx" / "kuro"
    try:
        patch_main_activity(pkg)
        patch_updater(pkg)
        patch_center(pkg)
        patch_dialog(pkg)
    except RuntimeError as exc:
        print(f"::error::{exc}")
        return 1
    print("patched: updateButton15 (worker thread) + up-to-date dialog + non-blocking boot veil")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: update_button_1.5.py <project dir>")
        sys.exit(2)
    sys.exit(patch(Path(sys.argv[1])))
