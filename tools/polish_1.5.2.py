#!/usr/bin/env python3
"""Kuro 1.5.2 native polish:
  1. Monochrome 24dp status-bar vector (@drawable/ic_stat_kuro) + accent tint across
     download, updater, and playback-error notifications (resolves Known Issue #2),
     plus accurate versionName in diagnostics560().
  2. Inline notification-permission prompt in KuroUpdateDialog582 & Settings -> Kuro
     updates on Android 13+ / blocked channels (resolves Known Issue #1).
  3. Native episode checkpoint export/merge (KuroPlayback561 + MainActivity.Bridge)
     so local .json backups preserve Continue Watching minute positions across phones.

Idempotent. Run after theme_native_1.2.py, update_button_1.5.py, and offline_beta_1.5.py:
    python3 tools/polish_1.5.2.py <project-dir>
"""
import sys
from pathlib import Path

IC_STAT_KURO_XML = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Monochrome status-bar glyph: Kuro K + four-point sparkle on transparent canvas -->
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M5,4h2.8v6.9L14.2,4h3.4l-6.6,7.1L18,20h-3.5l-5.3,-6.9L7.8,14.6V20H5V4z"/>
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M18.5,2.5L19.4,4.9L21.8,5.8L19.4,6.7L18.5,9.1L17.6,6.7L15.2,5.8L17.6,4.9Z"/>
</vector>
"""

PLAYBACK_CHECKPOINTS_BLOCK = """
    // ---- 1.5.2: export & merge checkpoints for local .json backups --------
    private static boolean validBackupPrefKey152(String k) {
        if (validKey(k)) return true;
        if (k == null) return false;
        for (String prefix : new String[]{"intro-start:", "intro-end:", "outro-start:", "outro-end:"}) {
            if (k.startsWith(prefix)) return validKey(k.substring(prefix.length()));
        }
        return false;
    }

    static String exportCheckpoints152(Context c) {
        JSONObject out = new JSONObject();
        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int count = 0;
            for (java.util.Map.Entry<String, ?> entry : p.getAll().entrySet()) {
                if (count >= 2000) break;
                String k = entry.getKey();
                Object v = entry.getValue();
                if (!validBackupPrefKey152(k) || !(v instanceof Long)) continue;
                long ms = (Long) v;
                if (ms < 0 || ms > MAX_POSITION_MS) continue;
                if (validKey(k) && ms < 5000) continue;
                out.put(k, ms);
                count++;
            }
        } catch (Exception ignored) {}
        return out.toString();
    }

    static int mergeCheckpoints152(Context c, String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > 2000000) return 0;
        try {
            JSONObject obj = new JSONObject(raw);
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = p.edit();
            java.util.Iterator<String> keys = obj.keys();
            int merged = 0, seen = 0;
            while (keys.hasNext() && seen < 2000) {
                seen++;
                String k = keys.next();
                if (!validBackupPrefKey152(k)) continue;
                long ms = obj.optLong(k, -1L);
                if (ms < 0 || ms > MAX_POSITION_MS) continue;
                if (validKey(k) && ms < 5000) continue;
                if (!p.contains(k)) {
                    ed.putLong(k, ms);
                    merged++;
                }
            }
            if (merged > 0) ed.apply();
            return merged;
        } catch (Exception e) {
            return 0;
        }
    }
"""


def replace_once(src: str, old: str, new: str, label: str) -> str:
    if new in src:
        return src
    count = src.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 occurrence, found {count}")
    return src.replace(old, new, 1)


def write_stat_icon(project: Path) -> None:
    drawable = project / "res" / "drawable"
    drawable.mkdir(parents=True, exist_ok=True)
    (drawable / "ic_stat_kuro.xml").write_text(IC_STAT_KURO_XML, encoding="utf-8")


def patch_download_service(pkg: Path) -> None:
    f = pkg / "KuroDownloadService561.java"
    src = f.read_text(encoding="utf-8", errors="surrogateescape")
    src = replace_once(
        src,
        '.setSmallIcon(getApplicationInfo().icon).setContentTitle("Kuro.tv downloads")',
        '.setSmallIcon(R.drawable.ic_stat_kuro).setColor(KuroTheme12.accentColor()).setContentTitle("Kuro.tv downloads")',
        "KuroDownloadService561.setSmallIcon",
    )
    f.write_text(src, encoding="utf-8", errors="surrogateescape")


def patch_updater_notifications(pkg: Path) -> None:
    f = pkg / "KuroUpdater572.java"
    src = f.read_text(encoding="utf-8", errors="surrogateescape")
    src = replace_once(
        src,
        "            int icon = c.getApplicationInfo().icon;\n"
        "            Notification.Builder builder = new Notification.Builder(c, CHANNEL)\n"
        "                    .setSmallIcon(icon)\n"
        "                    .setColor(Color.rgb(174, 103, 230))",
        "            Notification.Builder builder = new Notification.Builder(c, CHANNEL)\n"
        "                    .setSmallIcon(R.drawable.ic_stat_kuro)\n"
        "                    .setColor(KuroTheme12.accentColor())",
        "KuroUpdater572.notifyReminder icon",
    )
    src = replace_once(
        src,
        "            Notification.Builder builder = new Notification.Builder(c, CHANNEL)\n"
        "                    .setSmallIcon(c.getApplicationInfo().icon)\n"
        "                    .setColor(Color.rgb(174, 103, 230))\n"
        "                    .setContentTitle(\"Kuro \" + version + \" is ready to install\")",
        "            Notification.Builder builder = new Notification.Builder(c, CHANNEL)\n"
        "                    .setSmallIcon(R.drawable.ic_stat_kuro)\n"
        "                    .setColor(KuroTheme12.accentColor())\n"
        "                    .setContentTitle(\"Kuro \" + version + \" is ready to install\")",
        "KuroUpdater572.notifyReady icon",
    )
    f.write_text(src, encoding="utf-8", errors="surrogateescape")

    svc = pkg / "KuroUpdaterDownloadService.java"
    ssrc = svc.read_text(encoding="utf-8", errors="surrogateescape")
    ssrc = replace_once(
        ssrc,
        "        Notification.Builder builder = new Notification.Builder(this, KuroUpdater572.CHANNEL)\n"
        "                .setSmallIcon(getApplicationInfo().icon)\n"
        "                .setColor(Color.rgb(174, 103, 230))",
        "        Notification.Builder builder = new Notification.Builder(this, KuroUpdater572.CHANNEL)\n"
        "                .setSmallIcon(R.drawable.ic_stat_kuro)\n"
        "                .setColor(KuroTheme12.accentColor())",
        "KuroUpdaterDownloadService.progress icon",
    )
    svc.write_text(ssrc, encoding="utf-8", errors="surrogateescape")


def patch_update_dialog(pkg: Path) -> None:
    f = pkg / "KuroUpdateDialog582.java"
    src = f.read_text(encoding="utf-8", errors="surrogateescape")

    src = replace_once(
        src,
        "        /** Hand the verified file to Android's installer. */\n"
        "        void installUpdate582();\n"
        "    }",
        "        /** Hand the verified file to Android's installer. */\n"
        "        void installUpdate582();\n\n"
        "        /** 1.5.2: ask Android for notification permission or open notification settings. */\n"
        "        default void requestUpdateNotifications152() {}\n"
        "    }",
        "KuroUpdateDialog582.Host",
    )

    src = replace_once(
        src,
        "    private final TextView title, chip, percent, detail, status;\n",
        "    private final TextView title, chip, percent, detail, status, notifBanner152;\n",
        "KuroUpdateDialog582.fields",
    )

    src = replace_once(
        src,
        "        View divider = new View(activity);\n"
        "        divider.setBackgroundColor(KuroDialogs581.DIVIDER);",
        "        notifBanner152 = text(\"Update alerts are off \\u00b7 Tap to allow notifications\", 12, KuroDialogs581.PURPLE_SOFT, true);\n"
        "        notifBanner152.setPadding(dp(12), dp(9), dp(12), dp(9));\n"
        "        notifBanner152.setBackground(pill(KuroDialogs581.SELECTED, KuroDialogs581.PURPLE));\n"
        "        notifBanner152.setFocusable(true);\n"
        "        notifBanner152.setClickable(true);\n"
        "        notifBanner152.setVisibility(View.GONE);\n"
        "        notifBanner152.setOnClickListener(v -> {\n"
        "            if (host != null) host.requestUpdateNotifications152();\n"
        "        });\n"
        "        LinearLayout.LayoutParams notifParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);\n"
        "        notifParams.topMargin = dp(10);\n"
        "        root.addView(notifBanner152, notifParams);\n\n"
        "        View divider = new View(activity);\n"
        "        divider.setBackgroundColor(KuroDialogs581.DIVIDER);",
        "KuroUpdateDialog582.notifBanner152",
    )

    src = replace_once(
        src,
        "        String note = status(s);\n"
        "        status.setText(note);\n"
        "        status.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);",
        "        String note = status(s);\n"
        "        status.setText(note);\n"
        "        status.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);\n"
        "        notifBanner152.setVisibility(KuroUpdater572.allowed(activity) ? View.GONE : View.VISIBLE);",
        "KuroUpdateDialog582.refresh notifBanner152",
    )

    f.write_text(src, encoding="utf-8", errors="surrogateescape")


def patch_playback_checkpoints(pkg: Path) -> None:
    f = pkg / "KuroPlayback561.java"
    src = f.read_text(encoding="utf-8", errors="surrogateescape")
    if "exportCheckpoints152" not in src:
        idx = src.rfind("}")
        if idx < 0:
            raise RuntimeError("KuroPlayback561.java: closing brace not found")
        src = src[:idx] + PLAYBACK_CHECKPOINTS_BLOCK + "\n}\n"
        f.write_text(src, encoding="utf-8", errors="surrogateescape")


def patch_main_activity(pkg: Path) -> None:
    f = pkg / "MainActivity.java"
    src = f.read_text(encoding="utf-8", errors="surrogateescape")

    # 1. Permission request code constant
    src = replace_once(
        src,
        "private static final int BACKUP_EXPORT560=5600,BACKUP_IMPORT560=5601,ERROR_PERMISSION560=5602;",
        "private static final int BACKUP_EXPORT560=5600,BACKUP_IMPORT560=5601,ERROR_PERMISSION560=5602,UPDATE_PERMISSION152=5729;",
        "MainActivity.UPDATE_PERMISSION152",
    )

    # 2. Refresh update dialog when notification permission result arrives
    src = replace_once(
        src,
        "else if(request==RPC_NOTIFICATION_PERMISSION564)discordRefresh563();}",
        "else if(request==RPC_NOTIFICATION_PERMISSION564)discordRefresh563();"
        "else if(request==UPDATE_PERMISSION152){if(KuroUpdateDialog582.isOpen())openUpdateDialog582();}}",
        "MainActivity.onRequestPermissionsResult",
    )

    # 3. Playback error notification monochrome status-bar icon + accent tint
    src = replace_once(
        src,
        'new Notification.Builder(this,"kuro-playback-errors560").setSmallIcon(getApplicationInfo().icon).setContentTitle("Kuro.tv playback")',
        'new Notification.Builder(this,"kuro-playback-errors560").setSmallIcon(R.drawable.ic_stat_kuro).setColor(KuroTheme12.accentColor()).setContentTitle("Kuro.tv playback")',
        "MainActivity.notifyPlayerError560 icon",
    )

    # 4. Accurate installed version in diagnostics560()
    src = replace_once(
        src,
        'return "Kuro.tv 5.73 \u00b7 Android "',
        'return "Kuro.tv "+KuroUpdater572.installedVersionName(this)+" \u00b7 Android "',
        "MainActivity.diagnostics560 version",
    )

    # 5. Settings -> Kuro updates: one-tap Allow notifications button when ungranted
    src = replace_once(
        src,
        'if(!status.optBoolean("allowed",true))dialog.row("Notifications","Blocked in Android");',
        'if(!status.optBoolean("allowed",true)){dialog.row("Notifications","Off \u2014 tap Allow notifications below");dialog.action("Allow notifications",false,this::requestUpdateNotifications152);}',
        "MainActivity.showUpdater572 allow notifications",
    )

    # 6. Wire requestUpdateNotifications152 into updateHost582 & MainActivity
    src = replace_once(
        src,
        " private final KuroUpdateDialog582.Host updateHost582=new KuroUpdateDialog582.Host(){\n"
        "  @Override public void retryUpdate582(){checkAndMaybeInstall582();}\n"
        "  @Override public void installUpdate582(){installDownloadedUpdate572();}\n"
        " };",
        " private void requestUpdateNotifications152(){\n"
        "  if(isFinishing())return;\n"
        "  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){\n"
        "   boolean asked=KuroUpdater572.prefs(this).getBoolean(\"update_permission_asked152\",false);\n"
        "   if(!asked||shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)){\n"
        "    KuroUpdater572.prefs(this).edit().putBoolean(\"update_permission_asked152\",true).apply();\n"
        "    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},UPDATE_PERMISSION152);\n"
        "    return;\n"
        "   }\n"
        "  }\n"
        "  openNotificationSettings();\n"
        " }\n"
        " private final KuroUpdateDialog582.Host updateHost582=new KuroUpdateDialog582.Host(){\n"
        "  @Override public void retryUpdate582(){checkAndMaybeInstall582();}\n"
        "  @Override public void installUpdate582(){installDownloadedUpdate572();}\n"
        "  @Override public void requestUpdateNotifications152(){MainActivity.this.requestUpdateNotifications152();}\n"
        " };",
        "MainActivity.updateHost582 notifications",
    )

    # 7. Expose checkpointsExport152 / checkpointsMerge152 on Bridge for backup560.js
    src = replace_once(
        src,
        '  @JavascriptInterface public void backupFile560(String action,String payload){if(payload!=null&&payload.length()<=6000000)main.post(()->chooseBackup560(action,payload));}',
        '  @JavascriptInterface public void backupFile560(String action,String payload){if(payload!=null&&payload.length()<=6000000)main.post(()->chooseBackup560(action,payload));}\n'
        '  @JavascriptInterface public String checkpointsExport152(){return KuroPlayback561.exportCheckpoints152(MainActivity.this);}\n'
        '  @JavascriptInterface public int checkpointsMerge152(String raw){return raw==null||raw.length()>2000000?0:KuroPlayback561.mergeCheckpoints152(MainActivity.this,raw);}',
        "MainActivity.Bridge checkpoints152",
    )

    f.write_text(src, encoding="utf-8", errors="surrogateescape")


BACKUP_560_JS = """'use strict';
/* Explicit local export and conservative local-wins import; credentials never exported. */
(()=>{
 const FORMAT='kuro-local-backup-1',JOURNAL='kuro-local-import-journal560-v1',SETTINGS='kuro-settings560-v1';
 const ARRAYS=['kuro-library','kuro-recent','kuro-categories-v1','kuro-episode-history-v1','kuro-imports-v1','kuro-watch-queue571-v1','kuro-anime-preferences571-v1'];
 const OBJECTS=['kuro-watched-v1','kuro-last-episode-v1'],PROFILE=['kuro-profile-v1','kuro-profile-art-v1'];
 const HIDDEN_KEY='kuro-hidden-15',HIDDEN_META_KEY='kuro-hidden-meta-152',CLEAN_KEY='kuro-download-clean-15',ACCENT_KEY='kuro-accent-1.1',AMOLED_KEY='kuro-amoled-152',CHECKPOINTS_KEY='kuro-checkpoints-152';
 const KNOWN_ACCENTS=['teal','violet','plum','graphite','sky','ice','royal','sapphire','indigo','emerald','mint','lime','citron','gold','amber','sunset','coral','rose','crimson','magenta'];
 const STRINGS_152=[ACCENT_KEY,AMOLED_KEY];
 const LOCAL_KEYS=[...ARRAYS,...OBJECTS,...PROFILE,SETTINGS,HIDDEN_KEY,HIDDEN_META_KEY,CLEAN_KEY,...STRINGS_152];
 const KEYS=[...LOCAL_KEYS,CHECKPOINTS_KEY];
 const plain=x=>!!x&&typeof x==='object'&&!Array.isArray(x),own=(x,k)=>Object.prototype.hasOwnProperty.call(x,k),limit=6000000;
 function recover(){let raw=localStorage.getItem(JOURNAL);if(!raw)return;try{let j=JSON.parse(raw);if(!plain(j)||j.v!==1||!plain(j.before)||Object.keys(j.before).some(k=>!LOCAL_KEYS.includes(k)||!(j.before[k]===null||typeof j.before[k]==='string')))throw Error();for(const k of Object.keys(j.before))localStorage.removeItem(k);for(const [k,v] of Object.entries(j.before))if(v!==null)localStorage.setItem(k,v);localStorage.removeItem(JOURNAL);if(own(j.before,SETTINGS))window.kuroSettings560?.reload?.();if(own(j.before,ACCENT_KEY))window.kuroAccent111?.apply?.(localStorage.getItem(ACCENT_KEY)||'violet');if(own(j.before,AMOLED_KEY))window.kuroAmoled152?.sync?.()}catch(_){window.kuroLocalRestoreWarning='A local backup restore was interrupted. Keep your app data; do not clear or uninstall.'}}
 try{recover()}catch(_){window.kuroLocalRestoreWarning='Local backup recovery could not finish. Keep your app data.'}
 function parseExisting(k,def){let raw=localStorage.getItem(k);if(raw===null)return def;try{let v=JSON.parse(raw);if(Array.isArray(def)?!Array.isArray(v):!plain(v))throw Error();return v}catch(_){throw Error('Existing '+k+' cannot be read. Nothing was changed.')}}
 function itemKey(item){try{if(!plain(item)||!unifiedCodes.includes(item.source)||typeof item.title!=='string'||!item.title.trim()||item.title.length>500||typeof item.url!=='string'||item.url.length>2048||!validUrl(item.url))return '';let a=new URL(item.url),b=new URL(base(item.source));if(!(a.hostname===b.hostname||a.hostname.endsWith('.'+b.hostname))||a.port||a.username||a.password)return '';return item.source+'|'+a.href}catch(_){return ''}}
 function pairKey(s){let n=String(s).indexOf('|');if(n<1)return '';let source=String(s).slice(0,n),url=String(s).slice(n+1);return itemKey({source,url,title:'x'})}
 function keyFor(k,x){if(k==='kuro-anime-preferences571-v1'){let id=itemKey(x?.item);return id&&['inherit','dub','sub','default'].includes(x.audio)&&['','Ryu','Volt','Warp','Ayame','HD-1'].includes(x.server)?id:''}if(k==='kuro-imports-v1')return plain(x)&&['mal','anilist'].includes(x.provider)&&/^[1-9][0-9]{0,18}$/.test(String(x.sourceId))&&typeof x.title==='string'&&x.title.trim()&&x.title.length<=300?x.provider+'|'+x.sourceId:'';if(k==='kuro-categories-v1'){let id=itemKey(x?.item);return id&&['watching','completed','on_hold','dropped','planned'].includes(x.status)?id:''}if(k==='kuro-episode-history-v1'||k==='kuro-watch-queue571-v1'){let id=itemKey(x?.item),n=String(x?.ep?.number||'');if(k==='kuro-watch-queue571-v1'&&((x?.ep?.url&&(!validUrl(x.ep.url)||String(x.ep.url).length>2048))||String(x?.ep?.name||'').length>160))return '';return id&&/^\\d{1,4}$/.test(n)&&Number(n)>0&&Number(n)<=5000?id+'|'+String(Number(n)):''}return itemKey(x)}
 // 1.5.4: kuro-profile-v1 holds {name}, while kuro-profile-art-v1 holds {pfp, banner} (art-11..art-34; legacy avatar key also accepted).
 const validArtId=v=>v===undefined||v===''||(typeof v==='string'&&/^art-[0-9]{2}$/.test(v));
 function validProfile(k,v){if(!plain(v))return false;if(k==='kuro-profile-art-v1')return validArtId(v.pfp)&&validArtId(v.avatar)&&validArtId(v.banner);return typeof v.name==='string'&&v.name.length<=60}
 function validHidden(v){return Array.isArray(v)&&v.length<=200&&v.every(x=>typeof x==='string'&&x.trim().length>0&&x.length<=200)}
 function validHiddenMeta(v){return plain(v)&&Object.keys(v).length<=200&&Object.entries(v).every(([k,row])=>k.length>0&&k.length<=200&&plain(row)&&typeof row.title==='string'&&row.title.trim().length>0&&row.title.length<=200&&(row.at===undefined||(typeof row.at==='number'&&Number.isFinite(row.at))))}
 function validClean(v){return plain(v)&&['off','watched','older','latest'].includes(v.mode)&&(v.days===undefined||(typeof v.days==='number'&&Number.isFinite(v.days)&&v.days>=1&&v.days<=365))}
 function validCheckpoints(v){return plain(v)&&Object.keys(v).length<=2000&&Object.entries(v).every(([k,ms])=>/^(?:(?:intro|outro)-(?:start|end):)?[0-9a-f]{64}$/.test(k)&&typeof ms==='number'&&Number.isFinite(ms)&&ms>=0&&ms<=86400000)}
 function validate(b){if(!plain(b)||b.format!==FORMAT||!plain(b.data)||Object.keys(b.data).some(k=>!KEYS.includes(k)))throw Error('This is not a supported Kuro local backup. Nothing was changed.');for(const [k,v] of Object.entries(b.data)){
  if(ARRAYS.includes(k)){if(!Array.isArray(v)||v.length>(['kuro-watch-queue571-v1','kuro-anime-preferences571-v1'].includes(k)?200:10000)||v.some(x=>!keyFor(k,x)))throw Error('The backup has an invalid list. Nothing was changed.')}
  else if(OBJECTS.includes(k)){if(!plain(v)||Object.keys(v).length>10000||Object.entries(v).some(([id,value])=>!pairKey(id)||(k==='kuro-watched-v1'?(!plain(value)||Object.entries(value).some(([n,flag])=>!/^\\d{1,4}$/.test(n)||Number(n)<1||Number(n)>5000||flag!==true)):!/^\\d{1,4}$/.test(String(value))||Number(value)<1||Number(value)>5000)))throw Error('The backup has invalid watched progress. Nothing was changed.')}
  else if(PROFILE.includes(k)&&!validProfile(k,v))throw Error('The backup has an invalid profile. Nothing was changed.');
  else if(k===SETTINGS&&(!plain(v)||!window.kuroSettings560?.validate?.(v)))throw Error('The backup has invalid settings. Nothing was changed.');
  else if(k===HIDDEN_KEY&&!validHidden(v))throw Error('The backup has an invalid hidden-titles list. Nothing was changed.');
  else if(k===HIDDEN_META_KEY&&!validHiddenMeta(v))throw Error('The backup has invalid hidden-title metadata. Nothing was changed.');
  else if(k===CLEAN_KEY&&!validClean(v))throw Error('The backup has invalid download settings. Nothing was changed.');
  else if(k===ACCENT_KEY&&(typeof v!=='string'||!KNOWN_ACCENTS.includes(v)))throw Error('The backup has an invalid accent theme. Nothing was changed.');
  else if(k===AMOLED_KEY&&(typeof v!=='string'||!['0','1'].includes(v)))throw Error('The backup has an invalid OLED preference. Nothing was changed.');
  else if(k===CHECKPOINTS_KEY&&!validCheckpoints(v))throw Error('The backup has invalid playback checkpoints. Nothing was changed.');}return b}
 // Remove credentials even if a provider or an older backup stored them inside an otherwise allowed entry.
 const secret=k=>/(?:password|passcode|passwd|credential|token|authorization|cookie|session|secret|api[_-]?key)|^(?:auth|key|sig|signature|jwt|code)$/i.test(k);
 function cleanUrl(value){try{if(!/^https?:\\/\\//i.test(value))return value;let u=new URL(value);u.username='';u.password='';for(const k of [...u.searchParams.keys()])if(secret(k))u.searchParams.delete(k);if(u.hash&&secret(u.hash))u.hash='';return u.href}catch(_){return value}}
 function scrub(v,depth=0){if(depth>24)throw Error('Backup data is nested too deeply. Nothing was changed.');if(typeof v==='string')return cleanUrl(v);if(Array.isArray(v))return v.map(x=>scrub(x,depth+1));if(!plain(v))return v;let out=Object.create(null);for(let [key,val] of Object.entries(v)){if(['__proto__','prototype','constructor'].includes(key))continue;let pipe=key.indexOf('|http');if(pipe<0&&secret(key))continue;if(pipe>=0)key=key.slice(0,pipe+1)+cleanUrl(key.slice(pipe+1));if(own(out,key))throw Error('Backup keys conflict after private URL data is removed. Nothing was changed.');out[key]=scrub(val,depth+1)}return out}
 function exportText(){let data={};for(const k of LOCAL_KEYS){let raw=localStorage.getItem(k);if(raw===null)continue;if(raw.length>limit)throw Error('Local data is too large to export.');if(STRINGS_152.includes(k)){if((k===ACCENT_KEY&&KNOWN_ACCENTS.includes(raw))||(k===AMOLED_KEY&&['0','1'].includes(raw)))data[k]=raw;continue}try{data[k]=scrub(JSON.parse(raw))}catch(_){throw Error('A local list could not be read; export was not created.')}}try{if(typeof window.KuroNative?.checkpointsExport152==='function'){let cp=JSON.parse(window.KuroNative.checkpointsExport152()||'{}');if(plain(cp)&&Object.keys(cp).length)data[CHECKPOINTS_KEY]=scrub(cp)}}catch(_){}const text=JSON.stringify({format:FORMAT,created:new Date().toISOString(),data});if(text.length>limit||new Blob([text]).size>limit)throw Error('Local data is too large to export.');return text}
 function preview(text){if(typeof text!=='string'||text.length>limit||new Blob([text]).size>limit)throw Error('Backup is too large or unreadable. Nothing was changed.');let b;try{b=JSON.parse(text)}catch(_){throw Error('The file is not valid JSON. Nothing was changed.')}validate(b);b=scrub(b);validate(b);const listCount=ARRAYS.map(k=>Array.isArray(b.data[k])?b.data[k].length:0).reduce((a,c)=>a+c,0)+(Array.isArray(b.data[HIDDEN_KEY])?b.data[HIDDEN_KEY].length:0);return {backup:b,summary:listCount}}
 function merge(backup,options={}){validate(backup);backup=scrub(backup);validate(backup);const next={},before={},counts={added:0,kept:0,updated:0};for(const k of ARRAYS){if(!own(backup.data,k))continue;const current=parseExisting(k,[]),seen=new Set(current.map(x=>keyFor(k,x)).filter(Boolean)),result=current.slice();if(['kuro-watch-queue571-v1','kuro-anime-preferences571-v1'].includes(k)&&(current.length>200||current.some(x=>!keyFor(k,x))))throw Error('The existing local collection cannot be read. Nothing was changed.');for(const x of backup.data[k]){let id=keyFor(k,x);if(seen.has(id)||(['kuro-watch-queue571-v1','kuro-anime-preferences571-v1'].includes(k)&&result.length>=200)){counts.kept++;continue}seen.add(id);result.push(k==='kuro-imports-v1'?{...x,adult:x.adult===true?true:null}:x);counts.added++}if(result.length!==current.length)next[k]=JSON.stringify(result)}
  if(own(backup.data,HIDDEN_KEY)){const current=parseExisting(HIDDEN_KEY,[]),seen=new Set(current),result=current.slice();for(const x of backup.data[HIDDEN_KEY]){const s=String(x).trim();if(!s)continue;if(seen.has(s)||result.length>=200){counts.kept++;continue}seen.add(s);result.push(s);counts.added++}if(result.length!==current.length)next[HIDDEN_KEY]=JSON.stringify(result)}
  if(own(backup.data,HIDDEN_META_KEY)){const current=parseExisting(HIDDEN_META_KEY,{}),result=Object.assign(Object.create(null),current);let dirty=false;for(const [id,row] of Object.entries(backup.data[HIDDEN_META_KEY])){if(!own(result,id)&&Object.keys(result).length<200){result[id]={title:String(row.title).slice(0,200),...(typeof row.at==='number'?{at:row.at}:{})};dirty=true}}if(dirty)next[HIDDEN_META_KEY]=JSON.stringify(result)}
  for(const k of OBJECTS){if(!own(backup.data,k))continue;const current=parseExisting(k,{}),result=Object.assign(Object.create(null),current);let dirty=false;for(const [id,value] of Object.entries(backup.data[k])){if(k==='kuro-watched-v1'){if(!own(result,id)){result[id]=value;dirty=true;counts.added++}else if(plain(result[id])){const row=Object.assign(Object.create(null),result[id]);for(const [ep,flag] of Object.entries(value))if(!own(row,ep)){row[ep]=flag;dirty=true;counts.added++}result[id]=row}else counts.kept++}else if(!own(result,id)){result[id]=value;dirty=true;counts.added++}else counts.kept++}if(dirty)next[k]=JSON.stringify(result)}
  for(const k of PROFILE){if(!own(backup.data,k))continue;if(localStorage.getItem(k)!==null){counts.kept++;continue}if(k==='kuro-profile-art-v1'){const pfpVal=String(backup.data[k].pfp||backup.data[k].avatar||'');next[k]=JSON.stringify({pfp:pfpVal,banner:String(backup.data[k].banner||'')});counts.added++}else{next[k]=JSON.stringify({name:String(backup.data[k].name||'').slice(0,60)});counts.added++}}
  if(own(backup.data,SETTINGS)){const current=localStorage.getItem(SETTINGS),restored=JSON.stringify(backup.data[SETTINGS]);if(current===null){next[SETTINGS]=restored;counts.added++}else if(options.applySettings===true&&current!==restored){next[SETTINGS]=restored;counts.updated++}else counts.kept++}
  if(own(backup.data,CLEAN_KEY)){const current=localStorage.getItem(CLEAN_KEY),restored=JSON.stringify({mode:backup.data[CLEAN_KEY].mode,days:Number(backup.data[CLEAN_KEY].days||7)});if(current===null){next[CLEAN_KEY]=restored;counts.added++}else if(options.applySettings===true&&current!==restored){next[CLEAN_KEY]=restored;counts.updated++}else counts.kept++}
  for(const k of STRINGS_152){if(!own(backup.data,k))continue;const current=localStorage.getItem(k),restored=String(backup.data[k]);if(current===null){next[k]=restored;counts.added++}else if(options.applySettings===true&&current!==restored){next[k]=restored;counts.updated++}else counts.kept++}
  const changed=Object.keys(next);if(changed.length){for(const k of changed)before[k]=localStorage.getItem(k);
   try{localStorage.setItem(JOURNAL,JSON.stringify({v:1,before}));for(const k of changed)localStorage.setItem(k,next[k]);localStorage.removeItem(JOURNAL)}catch(_){try{for(const k of Object.keys(before))localStorage.removeItem(k);for(const [k,v] of Object.entries(before))if(v!==null)localStorage.setItem(k,v);localStorage.removeItem(JOURNAL)}catch(__){}throw Error('Not enough storage to safely restore. Existing data was kept or will be recovered on restart.')}}
  if(own(backup.data,CHECKPOINTS_KEY)&&typeof window.KuroNative?.checkpointsMerge152==='function'){try{counts.added+=Number(window.KuroNative.checkpointsMerge152(JSON.stringify(backup.data[CHECKPOINTS_KEY]))||0)}catch(_){}}
  if(own(next,SETTINGS))window.kuroSettings560?.reload?.();if(own(next,ACCENT_KEY))window.kuroAccent111?.apply?.(next[ACCENT_KEY]);if(own(next,AMOLED_KEY))window.kuroAmoled152?.sync?.();if(own(next,'kuro-profile-art-v1')){try{const art=JSON.parse(next['kuro-profile-art-v1']);window.KuroNative?.profileArt581?.(art.pfp||art.avatar||'');window.kuroProfileArt581?.paint?.()}catch(_){}}window.kuroRefreshHeader560?.();return counts}
 window.kuroBackup560=Object.freeze({exportText,preview,merge});
})();
"""


def patch_web_backup(project: Path) -> None:
    assets = project / "assets"
    backup_file = assets / "backup560.js"
    if not backup_file.is_file():
        raise RuntimeError(f"backup560.js not found at {backup_file}")
    backup_file.write_text(BACKUP_560_JS, encoding="utf-8")

    settings_file = assets / "settings560.js"
    if not settings_file.is_file():
        raise RuntimeError(f"settings560.js not found at {settings_file}")
    ssrc = settings_file.read_text(encoding="utf-8", errors="surrogateescape")
    old_groups = (
        " const restoreGroups560=[\n"
        "  {id:'library',name:'Library and lists',keys:['kuro-library','kuro-categories-v1','kuro-imports-v1','kuro-watch-queue571-v1','kuro-anime-preferences571-v1']},\n"
        "  {id:'history',name:'Viewing history and progress',keys:['kuro-recent','kuro-episode-history-v1','kuro-watched-v1','kuro-last-episode-v1']},\n"
        "  {id:'profile',name:'Profile',keys:['kuro-profile-v1']},\n"
        "  {id:'settings',name:'Menu settings',keys:['kuro-settings560-v1']}\n"
        " ];"
    )
    new_groups = (
        " const restoreGroups560=[\n"
        "  {id:'library',name:'Library and lists',keys:['kuro-library','kuro-categories-v1','kuro-imports-v1','kuro-watch-queue571-v1','kuro-anime-preferences571-v1','kuro-hidden-15','kuro-hidden-meta-152']},\n"
        "  {id:'history',name:'Viewing history and progress',keys:['kuro-recent','kuro-episode-history-v1','kuro-watched-v1','kuro-last-episode-v1','kuro-checkpoints-152']},\n"
        "  {id:'profile',name:'Profile and artwork',keys:['kuro-profile-v1','kuro-profile-art-v1']},\n"
        "  {id:'settings',name:'Menu settings and theme',keys:['kuro-settings560-v1','kuro-accent-1.1','kuro-amoled-152','kuro-download-clean-15']}\n"
        " ];"
    )
    ssrc = replace_once(ssrc, old_groups, new_groups, "settings560.restoreGroups560")
    ssrc = replace_once(
        ssrc,
        "(data[k]&&typeof data[k]==='object'?Object.keys(data[k]).length:0)",
        "(data[k]&&typeof data[k]==='object'?Object.keys(data[k]).length:(typeof data[k]==='string'?1:0))",
        "settings560.restoreGroups count",
    )
    ssrc = replace_once(
        ssrc,
        "{applySettings:allowed.has('kuro-settings560-v1')}",
        "{applySettings:Boolean(box.querySelector('[data-restore-group=\"settings\"]')?.checked)}",
        "settings560.applySettings",
    )
    settings_file.write_text(ssrc, encoding="utf-8", errors="surrogateescape")


def patch(project: Path) -> int:
    pkg = project / "src" / "app" / "lovelycrxshx" / "kuro"
    if not pkg.is_dir():
        print(f"::error::Kuro package dir not found at {pkg}")
        return 1
    try:
        write_stat_icon(project)
        patch_download_service(pkg)
        patch_updater_notifications(pkg)
        patch_update_dialog(pkg)
        patch_playback_checkpoints(pkg)
        patch_main_activity(pkg)
        patch_web_backup(project)
    except RuntimeError as exc:
        print(f"::error::{exc}")
        return 1
    print("patched 1.5.2: ic_stat_kuro + update notification prompt + full 1.5.2 backup/restore + checkpoints")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: polish_1.5.2.py <project-dir>")
        sys.exit(2)
    sys.exit(patch(Path(sys.argv[1])))
