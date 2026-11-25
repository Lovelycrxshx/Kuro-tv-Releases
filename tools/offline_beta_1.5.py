#!/usr/bin/env python3
"""1.5-beta shell pieces: progress readback + download housekeeping.

Everything here is additive; no existing method changes behaviour except that
the offline player now records "this download was opened once".

  1. KuroPlayback561 already keeps a checkpoint per episode. The web layer could
     write it but never read it, so "Continue watching" had no minutes. A new
     bridge method progress15(source, showIdentity, episode) asks the shell for
     the same key the native player writes.

  2. KuroOffline561 gains summary15 / clean15: count, storage used, how many were
     watched, and bulk removal (all / watched / watched-and-older-than / keep
     only the latest episode of each series). The download that is playing right
     now is never removed.

  3. KuroOfflineActivity561 marks a download as played when it opens and clears
     the "playing" flag when it closes, which is what makes "watched" mean
     something rather than "never started".

Idempotent. Usage: python3 tools/offline_beta_1.5.py <project dir>
"""
import re
import sys
from pathlib import Path

PKG = Path("src/app/lovelycrxshx/kuro")

OFFLINE_BLOCK = '''
    // ---- 1.5-beta: download housekeeping for the Settings screen ----------
    private static final String CLEAN_PREFS = "kuro-offline-clean15";
    private static volatile String active15 = "";

    static void markPlayed15(Context c, String key) {
        if (!KuroPlayback561.validKey(key)) return;
        c.getSharedPreferences(CLEAN_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("p:" + key, true).apply();
    }

    private static boolean played15(Context c, String key) {
        return KuroPlayback561.validKey(key) && c.getSharedPreferences(CLEAN_PREFS, Context.MODE_PRIVATE)
                .getBoolean("p:" + key, false);
    }

    /** The download that is playing right now; never removed, not even by "all". */
    static void setActive15(String key) { active15 = KuroPlayback561.validKey(key) ? key : ""; }

    private static boolean isActive15(Download d) {
        return !active15.isEmpty() && active15.equals(metadata(d).optString("key", ""));
    }

    /** Opened at least once and no checkpoint left: finished, or restarted. */
    private static boolean watched15(Context c, Download d) {
        String key = metadata(d).optString("key", "");
        return played15(c, key) && KuroPlayback561.read(c, key) == 0;
    }

    private static int episode15(String value) {
        if (value == null || value.isEmpty()) return -1;
        int n = 0, digits = 0;
        for (int i = 0; i < value.length() && i < 5; i++) {
            char ch = value.charAt(i);
            if (ch < '0' || ch > '9') break;
            n = n * 10 + (ch - '0');
            digits++;
        }
        return digits == 0 ? -1 : n;
    }

    private static long bytes15(Context c) {
        try {
            Cache store = cache(c);
            return store == null ? 0L : store.getCacheSpace();
        } catch (RuntimeException e) { return 0L; }
    }

    static String summary15(Context c) {
        int count = 0, ready = 0, watched = 0;
        try (DownloadCursor cursor = manager(c).getDownloadIndex().getDownloads()) {
            while (cursor.moveToNext() && count < 500) {
                Download d = cursor.getDownload();
                count++;
                if (d.state == Download.STATE_COMPLETED) ready++;
                if (watched15(c, d)) watched++;
            }
        } catch (Exception ignored) { }
        try {
            return new JSONObject().put("count", count).put("ready", ready)
                    .put("watched", watched).put("bytes", bytes15(c)).toString();
        } catch (org.json.JSONException e) {
            return "{\\"count\\":0,\\"ready\\":0,\\"watched\\":0,\\"bytes\\":0}";
        }
    }

    private static List<Download> all15(Context c) {
        List<Download> out = new ArrayList<>();
        try (DownloadCursor cursor = manager(c).getDownloadIndex().getDownloads()) {
            while (cursor.moveToNext() && out.size() < 500) out.add(cursor.getDownload());
        } catch (Exception ignored) { }
        return out;
    }

    private static int remove15(Context c, List<Download> doomed) {
        int removed = 0;
        for (Download d : doomed) {
            if (isActive15(d)) continue;
            try {
                DownloadService.sendRemoveDownload(c, KuroDownloadService561.class, d.request.id, false);
                removed++;
            } catch (RuntimeException ignored) { }
        }
        return removed;
    }

    /** mode: all | watched | older (needs days) | latest (one episode per title). */
    static int clean15(Context c, String mode, int days) {
        List<Download> all = all15(c);
        List<Download> doomed = new ArrayList<>();
        if ("all".equals(mode)) {
            doomed.addAll(all);
        } else if ("watched".equals(mode)) {
            for (Download d : all) if (watched15(c, d)) doomed.add(d);
        } else if ("older".equals(mode)) {
            long cutoff = System.currentTimeMillis()
                    - Math.max(1, Math.min(365, days)) * 24L * 60L * 60L * 1000L;
            for (Download d : all) {
                if (watched15(c, d) && d.updateTimeMs > 0 && d.updateTimeMs < cutoff) doomed.add(d);
            }
        } else if ("latest".equals(mode)) {
            java.util.HashMap<String, Integer> best = new java.util.HashMap<>();
            for (Download d : all) {
                JSONObject info = metadata(d);
                String title = info.optString("title", "").trim();
                int ep = episode15(info.optString("episode", ""));
                if (title.isEmpty() || ep < 0) continue;
                Integer seen = best.get(title);
                if (seen == null || ep > seen) best.put(title, ep);
            }
            for (Download d : all) {
                JSONObject info = metadata(d);
                String title = info.optString("title", "").trim();
                int ep = episode15(info.optString("episode", ""));
                Integer seen = best.get(title);
                if (seen == null || ep < 0 || ep >= seen) continue;
                doomed.add(d);
            }
        }
        return remove15(c, doomed);
    }
'''

BRIDGE_BLOCK = ''' @JavascriptInterface public long progress15(String source,String showIdentity,String episode){
  try{return KuroPlayback561.read(MainActivity.this,KuroPlayback561.key(source,showIdentity,episode));}catch(RuntimeException e){return 0L;}
 }
 @JavascriptInterface public String downloadsSummary15(){
  try{return KuroOffline561.summary15(MainActivity.this);}catch(RuntimeException e){return "{\\"count\\":0,\\"ready\\":0,\\"watched\\":0,\\"bytes\\":0}";}
 }
 @JavascriptInterface public int downloadsClean15(String action,int days){
  try{return KuroOffline561.clean15(MainActivity.this,action==null?"":action,days);}catch(RuntimeException e){return 0;}
 }
'''


def edit(path, transform, label):
    text = path.read_text(encoding="utf-8", errors="surrogateescape")
    updated = transform(text)
    if updated is None:
        print("  ok (already)  %s" % label)
        return 0
    path.write_text(updated, encoding="utf-8", errors="surrogateescape")
    print("  patched       %s" % label)
    return 1


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 2
    project = Path(argv[1])
    pkg = project / PKG
    if not pkg.is_dir():
        print("::error::%s does not look like the Kuro source tree" % project)
        return 1

    offline = pkg / "KuroOffline561.java"
    activity = pkg / "KuroOfflineActivity561.java"
    main_act = pkg / "MainActivity.java"
    for f in (offline, activity, main_act):
        if not f.is_file():
            print("::error::missing %s" % f)
            return 1

    changes = 0

    def offline_patch(src):
        if "clean15" in src:
            return None
        anchor = "    private static void actions(Activity a,Download download){"
        if anchor not in src:
            raise SystemExit("::error::KuroOffline561: actions() anchor not found")
        return src.replace(anchor, OFFLINE_BLOCK.lstrip("\n") + anchor, 1)

    changes += edit(offline, offline_patch, "KuroOffline561 housekeeping")

    def activity_patch(src):
        if "markPlayed15" in src:
            return None
        old = 'JSONObject meta=KuroOffline561.metadata(download);key=meta.optString("key","");'
        if old not in src:
            raise SystemExit("::error::KuroOfflineActivity561: openOffline anchor not found")
        src = src.replace(old, old + "KuroOffline561.markPlayed15(this,key);KuroOffline561.setActive15(key);", 1)
        destroy = "if(engine!=null){engine.release();engine=null;}super.onDestroy();}"
        if destroy not in src:
            raise SystemExit("::error::KuroOfflineActivity561: onDestroy anchor not found")
        src = src.replace(destroy,
                          'if(engine!=null){engine.release();engine=null;}KuroOffline561.setActive15("");super.onDestroy();}',
                          1)
        return src

    changes += edit(activity, activity_patch, "offline player marks played / active")

    def main_patch(src):
        if "downloadsClean15" in src:
            return None
        anchor = " @JavascriptInterface public void settingsAction560(String action){"
        if anchor not in src:
            raise SystemExit("::error::MainActivity: settingsAction560 anchor not found")
        return src.replace(anchor, BRIDGE_BLOCK + anchor, 1)

    changes += edit(main_act, main_patch, "KuroNative.progress15 + downloads bridge")

    print("offline_beta_1.5: %d change(s)" % changes)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
