#!/usr/bin/env python3
"""Kuro 1.5.4-beta native & WebView bridge patcher.

Implements:
- #2 Cinematic Title Detail Page v2:
  - KuroCatalog573 'detail154' (bannerImage, averageScore, duration, studios,
    streamingEpisodes thumbnails, franchise watch-order relations)
  - KuroOffline561.confirmBatchDownload154 + Bridge.batchDownload154
  - HiAnime ssl-item-filler extraction + Skip Filler auto-next skip support
- #3 Widescreen Left-Rail Leanback UI & Glowing DPAD Focus (ONLY when Desktop site is ON):
  - MainActivity.applyWindowPreferences560 wide-viewport sync + Bridge.siteView154
  - settings560.js live data-site-view attribute & 'kuro-settings560-changed' event
- #4 Smart Multi-Filter Browse & Voice Command Search:
  - AndroidManifest.xml RECOGNIZE_SPEECH <queries> intent (zero RECORD_AUDIO permission)
  - MainActivity RecognizerIntent voiceSearch154 + hasVoiceSearch154 + result callback
  - KuroCatalog573 'metaBatch154' (batch AniList ratings/genres/seasons/status + Surprise Me)
- KuroUpdater572: safe numeric parsing for '-beta' version tags.
"""

from __future__ import annotations

import pathlib
import sys


def patch_once(text: str, marker: str, old: str, new: str, label: str) -> str:
    if marker in text:
        return text
    if old not in text:
        raise SystemExit(f"ui_1.5.4: missing anchor for {label}")
    return text.replace(old, new, 1)


def patch_manifest(root: pathlib.Path) -> None:
    path = root / "AndroidManifest.xml"
    text = path.read_text(encoding="utf-8")
    text = patch_once(
        text,
        "android.speech.action.RECOGNIZE_SPEECH",
        "<intent><action android:name=\"com.discord.socialsdk.rpc.IDiscordRpcService\"/></intent></queries>",
        "<intent><action android:name=\"com.discord.socialsdk.rpc.IDiscordRpcService\"/></intent>"
        "<intent><action android:name=\"android.speech.action.RECOGNIZE_SPEECH\"/></intent></queries>",
        "AndroidManifest.xml RECOGNIZE_SPEECH query",
    )
    path.write_text(text, encoding="utf-8")


def patch_updater(root: pathlib.Path) -> None:
    path = root / "src/app/lovelycrxshx/kuro/KuroUpdater572.java"
    text = path.read_text(encoding="utf-8")
    text = patch_once(
        text,
        "clean154",
        'String[] parts = version.split("\\\\.");',
        'String clean154 = versionFromTag(version); String[] parts = (clean154.isEmpty() ? version : clean154).split("\\\\.");',
        "KuroUpdater572 beta tag numbers()",
    )
    path.write_text(text, encoding="utf-8")


def patch_catalog(root: pathlib.Path) -> None:
    path = root / "src/app/lovelycrxshx/kuro/KuroCatalog573.java"
    text = path.read_text(encoding="utf-8")
    text = patch_once(
        text,
        "metaBatch154",
        'if(input==null||input.length()>300)throw new IOException("Invalid request");',
        'if(input==null||input.length()>("metaBatch154".equals(action)?2400:300))throw new IOException("Invalid request");',
        "KuroCatalog573 input limit",
    )
    extra_actions = (
        '  }else if("detail154".equals(action)){\n'
        '   long id=p.optLong("id",0);String provider=p.optString("provider",""),search=p.optString("search","").trim();\n'
        '   String fields="{id idMal isAdult title{romaji english} bannerImage coverImage{extraLarge large} averageScore duration episodes status format season seasonYear genres studios(isMain:true){nodes{name}} nextAiringEpisode{airingAt episode} streamingEpisodes{title thumbnail} relations{edges{relationType node{id idMal type isAdult title{romaji english} format status episodes startDate{year month day}}}}}";\n'
        '   if(id>=1&&id<=999999999&&(provider.equals("anilist")||provider.equals("mal"))){v.put("id",id);String field=provider.equals("mal")?"idMal":"id";query="query($id:Int){Media("+field+":$id,type:ANIME,isAdult:false)"+fields+"}";}\n'
        '   else if(search.length()>=1&&search.length()<=140){v.put("search",search);query="query($search:String){Media(search:$search,type:ANIME,isAdult:false,sort:SEARCH_MATCH)"+fields+"}";}\n'
        '   else throw new IOException("Invalid detail lookup");\n'
        '  }else if("metaBatch154".equals(action)){\n'
        '   if(p.optBoolean("random",false)){\n'
        '    int page=Math.max(1,Math.min(12,p.optInt("page",1)));v.put("page",page);\n'
        '    query="query($page:Int){Page(page:$page,perPage:24){media(type:ANIME,isAdult:false,sort:POPULARITY_DESC,averageScore_greater:74){id idMal isAdult title{romaji english} coverImage{large} bannerImage averageScore format status episodes season seasonYear genres}}}";\n'
        '   }else if(p.optJSONArray("ids")!=null){\n'
        '    JSONArray rawIds=p.optJSONArray("ids"),cleanIds=new JSONArray();\n'
        '    for(int i=0;i<rawIds.length()&&cleanIds.length()<36;i++){long x=rawIds.optLong(i,0);if(x>=1&&x<=999999999)cleanIds.put(x);}\n'
        '    if(cleanIds.length()==0)throw new IOException("Empty IDs");v.put("ids",cleanIds);\n'
        '    query="query($ids:[Int]){Page(page:1,perPage:36){media(id_in:$ids,type:ANIME,isAdult:false){id idMal isAdult title{romaji english} averageScore format status episodes season seasonYear genres}}}";\n'
        '   }else if(p.optJSONArray("titles")!=null){\n'
        '    JSONArray names=p.optJSONArray("titles");if(names.length()==0||names.length()>8)throw new IOException("Invalid title batch");\n'
        '    StringBuilder params=new StringBuilder("query("),body=new StringBuilder("{");int count=0;\n'
        '    for(int i=0;i<names.length();i++){String name=names.optString(i,"").trim();if(name.length()<2||name.length()>140)continue;if(count>0)params.append(",");params.append("$q").append(count).append(":String");body.append("m").append(count).append(":Media(search:$q").append(count).append(",type:ANIME,isAdult:false,sort:SEARCH_MATCH){id idMal isAdult title{romaji english} averageScore format status episodes season seasonYear genres} ");v.put("q"+count,name);count++;}\n'
        '    if(count==0)throw new IOException("Empty title batch");\n'
        '    query=params.append(")").append(body).append("}").toString();\n'
        '   }else throw new IOException("Invalid batch lookup");\n'
        '  }else throw new IOException("Invalid action");'
    )
    text = patch_once(
        text,
        '"detail154".equals(action)',
        '  }else throw new IOException("Invalid action");',
        extra_actions,
        "KuroCatalog573 detail154 & metaBatch154",
    )
    path.write_text(text, encoding="utf-8")


def patch_offline(root: pathlib.Path) -> None:
    path = root / "src/app/lovelycrxshx/kuro/KuroOffline561.java"
    text = path.read_text(encoding="utf-8")
    batch_method = """
    static void confirmBatchDownload154(Activity activity, String rawJson) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || rawJson == null || rawJson.length() > 250000) return;
        final List<JSONObject> valid = new ArrayList<>();
        try {
            org.json.JSONArray arr = new org.json.JSONArray(rawJson);
            for (int i = 0; i < arr.length() && valid.size() < 30; i++) {
                JSONObject row = arr.optJSONObject(i);
                if (row == null) continue;
                String addr = row.optString("address", "");
                if (!KuroPlayback561.allowedHls(addr, "zenkai", "hls")) continue;
                valid.add(row);
            }
        } catch (Exception ignored) {}
        if (valid.isEmpty()) {
            Toast.makeText(activity, "No direct HLS streams available for batch offline download on this source.", Toast.LENGTH_LONG).show();
            return;
        }
        String showTitle = limit(valid.get(0).optString("title", "Anime"), 90);
        String firstEp = limit(valid.get(0).optString("episode", "1"), 16);
        String lastEp = limit(valid.get(valid.size() - 1).optString("episode", firstEp), 16);
        String rangeLabel = valid.size() == 1 ? ("Episode " + firstEp) : ("Episodes " + firstEp + " – " + lastEp + " (" + valid.size() + " episodes)");
        new AlertDialog.Builder(KuroDialogs581.themed(activity))
                .setTitle("Batch download " + valid.size() + (valid.size() == 1 ? " episode?" : " episodes?"))
                .setMessage(showTitle + "\\n" + rangeLabel + "\\n\\nQueues verified direct HLS streams into Kuro offline storage. Only store content you have permission to keep.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Queue " + valid.size(), (d, w) -> {
                    int queued = 0;
                    for (JSONObject row : valid) {
                        try {
                            String addr = row.optString("address", "");
                            String key = row.optString("key", "");
                            String t = limit(row.optString("title", "Video"), 120);
                            String ep = limit(row.optString("episode", ""), 20);
                            String id = "kuro561:" + Integer.toHexString((addr + "|" + key).hashCode());
                            JSONObject meta = new JSONObject().put("title", t).put("episode", ep).put("key", KuroPlayback561.validKey(key) ? key : "");
                            DownloadRequest req = new DownloadRequest.Builder(id, android.net.Uri.parse(addr))
                                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                                    .setData(meta.toString().getBytes(StandardCharsets.UTF_8))
                                    .build();
                            DownloadService.sendAddDownload(activity, KuroDownloadService561.class, req, false);
                            queued++;
                        } catch (Exception ignored) {}
                    }
                    if (queued > 0) {
                        try { DownloadService.sendResumeDownloads(activity, KuroDownloadService561.class, false); } catch (Exception ignored) {}
                        Toast.makeText(activity, "Queued " + queued + (queued == 1 ? " episode" : " episodes") + " for offline playback.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(activity, "Could not start batch download on this device.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }
"""
    text = patch_once(
        text,
        "confirmBatchDownload154",
        "    private static String limit(String text,int max){",
        batch_method + "    private static String limit(String text,int max){",
        "KuroOffline561 confirmBatchDownload154",
    )
    path.write_text(text, encoding="utf-8")


def patch_main_activity(root: pathlib.Path) -> None:
    path = root / "src/app/lovelycrxshx/kuro/MainActivity.java"
    text = path.read_text(encoding="utf-8")

    # 1. Request code constant
    text = patch_once(
        text,
        "VOICE_SEARCH_REQ154",
        "private static final int BACKUP_EXPORT560=5600,BACKUP_IMPORT560=5601,ERROR_PERMISSION560=5602,UPDATE_PERMISSION152=5729;",
        "private static final int BACKUP_EXPORT560=5600,BACKUP_IMPORT560=5601,ERROR_PERMISSION560=5602,UPDATE_PERMISSION152=5729,VOICE_SEARCH_REQ154=15401;",
        "MainActivity VOICE_SEARCH_REQ154",
    )

    # 2. Voice search result in onActivityResult
    text = patch_once(
        text,
        "finishVoiceSearch154",
        "if(requestCode==BACKUP_EXPORT560||requestCode==BACKUP_IMPORT560){finishBackup560(requestCode,resultCode,data);return;}",
        "if(requestCode==BACKUP_EXPORT560||requestCode==BACKUP_IMPORT560){finishBackup560(requestCode,resultCode,data);return;}"
        "if(requestCode==VOICE_SEARCH_REQ154){finishVoiceSearch154(resultCode,data);return;}",
        "MainActivity onActivityResult voiceSearch154",
    )

    # 3. Wide viewport + desktop site sync in applyWindowPreferences560 + voice search helper
    voice_and_desktop = (
        "int zoom=p.getInt(\"textZoom\",100);boolean desktopUi154=\"desktop\".equals(p.getString(\"siteView\",\"mobile\"));"
        "if(ui!=null){ui.getSettings().setTextZoom(zoom);ui.getSettings().setUseWideViewPort(desktopUi154);ui.getSettings().setLoadWithOverviewMode(desktopUi154);"
        "if(uiReady)ui.evaluateJavascript(\"window.kuroSyncDesktopSite154&&window.kuroSyncDesktopSite154(\"+desktopUi154+\")\",null);}"
    )
    text = patch_once(
        text,
        "desktopUi154",
        'int zoom=p.getInt("textZoom",100);if(ui!=null)ui.getSettings().setTextZoom(zoom);',
        voice_and_desktop,
        "MainActivity applyWindowPreferences560 desktopUi154",
    )

    # 4. Skip-filler resolution for native Auto-Next & Fullscreen Next Episode + Voice Search launcher
    helpers_154 = """ private int nextPlayableEpisode154(JSONObject wi,int fromIdx){
  JSONArray eps=wi==null?null:wi.optJSONArray("episodes");
  if(eps==null||fromIdx<0||fromIdx>=eps.length())return -1;
  if(wi.optBoolean("skipFiller154",false)){
   for(int i=fromIdx;i<eps.length();i++){JSONObject row=eps.optJSONObject(i);if(row==null||!row.optBoolean("filler154",false))return i;}
  }
  return fromIdx;
 }
 private void startVoiceSearch154(){
  try{
   Intent i=new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   i.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   i.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS,3);
   i.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,"Search anime, genres, or studios");
   startActivityForResult(i,VOICE_SEARCH_REQ154);
  }catch(Exception e){
   Toast.makeText(this,"Voice search is unavailable on this device.",Toast.LENGTH_SHORT).show();
   if(ui!=null&&uiReady)ui.evaluateJavascript("window.kuroOnVoiceSearchError154&&window.kuroOnVoiceSearchError154()",null);
  }
 }
 private void finishVoiceSearch154(int resultCode,Intent data){
  if(resultCode!=RESULT_OK||data==null)return;
  ArrayList<String> list=data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
  if(list==null||list.isEmpty())return;
  String best="";
  for(String s:list){if(s!=null&&!s.trim().isEmpty()){best=s.trim();break;}}
  if(best.isEmpty())return;
  if(best.length()>120)best=best.substring(0,120);
  final String spoken=best;
  main.post(()->{if(ui!=null&&uiReady)ui.evaluateJavascript("window.kuroOnVoiceSearch154&&window.kuroOnVoiceSearch154("+JSONObject.quote(spoken)+")",null);});
 }
"""
    text = patch_once(
        text,
        "nextPlayableEpisode154",
        " private void cancelAutoNext573(){",
        helpers_154 + " private void cancelAutoNext573(){",
        "MainActivity helpers_154",
    )

    text = patch_once(
        text,
        "rawNext154",
        " private void offerAutoNext573(String session,int next){\n  if(!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!session.equals(watchSession)||!getPreferences(MODE_PRIVATE).getBoolean(\"auto_next\",false))return;",
        " private void offerAutoNext573(String session,int rawNext154){\n  final int next=nextPlayableEpisode154(watchInfo561,rawNext154);\n  if(next<0||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!session.equals(watchSession)||!getPreferences(MODE_PRIVATE).getBoolean(\"auto_next\",false))return;",
        "MainActivity offerAutoNext573 skipFiller154",
    )

    text = patch_once(
        text,
        "nextPlayableEpisode154(wi,cur+1)",
        "    if(eps!=null&&cur>=0&&cur+1<eps.length()){\n     if(customView!=null||enhancedFullscreen)resumeFullscreenUntil153=android.os.SystemClock.elapsedRealtime()+15000L;\n     chooseEpisode581(cur+1);\n    }",
        "    int target154=nextPlayableEpisode154(wi,cur+1);\n    if(eps!=null&&cur>=0&&target154>=0&&target154<eps.length()){\n     if(customView!=null||enhancedFullscreen)resumeFullscreenUntil153=android.os.SystemClock.elapsedRealtime()+15000L;\n     chooseEpisode581(target154);\n    }",
        "MainActivity fullscreenActions153 skipFiller154",
    )

    # 5. Store skipFiller154 on watchInfo561 inside openWatch
    text = patch_once(
        text,
        '"skipFiller154"',
        'watchInfo561=new JSONObject().put("title",title).put("episode",episode).put("episodeName",info.optString("episodeName","")).put("episodeImage",info.optString("episodeImage",""))',
        'watchInfo561=new JSONObject().put("title",title).put("episode",episode).put("episodeName",info.optString("episodeName","")).put("episodeImage",info.optString("episodeImage","")).put("skipFiller154",info.optBoolean("skipFiller154",false))',
        "MainActivity watchInfo561 skipFiller154",
    )

    # 6. Bridge methods for Voice Search, Batch Download & Desktop Site state
    bridge_additions = (
        '  @JavascriptInterface public boolean hasVoiceSearch154(){try{return getPackageManager().resolveActivity(new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH),0)!=null;}catch(Exception e){return false;}}\n'
        '  @JavascriptInterface public void voiceSearch154(){main.post(()->{if(!isFinishing()&&!isDestroyed())startVoiceSearch154();});}\n'
        '  @JavascriptInterface public void batchDownload154(String raw){if(raw!=null&&raw.length()<=250000)main.post(()->{if(!isFinishing()&&!isDestroyed())KuroOffline561.confirmBatchDownload154(MainActivity.this,raw);});}\n'
        '  @JavascriptInterface public String siteView154(){return settings560().getString("siteView","mobile");}\n'
        '  @JavascriptInterface public void play(String payload){'
    )
    text = patch_once(
        text,
        "voiceSearch154()",
        '  @JavascriptInterface public void play(String payload){',
        bridge_additions,
        "MainActivity Bridge 1.5.4 methods",
    )

    path.write_text(text, encoding="utf-8")


def patch_js_hooks(root: pathlib.Path) -> None:
    # 1. settings560.js: sync data-site-view + custom event + clearer Site view subtitle
    s_path = root / "assets/settings560.js"
    s_text = s_path.read_text(encoding="utf-8")
    s_text = patch_once(
        s_text,
        "kuro-settings560-changed",
        "function apply(){document.documentElement.dataset.kuroTheme=state.theme;",
        "function apply(){document.documentElement.dataset.kuroTheme=state.theme;document.documentElement.dataset.siteView=state.siteView;",
        "settings560.js dataset.siteView",
    )
    s_text = patch_once(
        s_text,
        "CustomEvent('kuro-settings560-changed'",
        "try{if(native())KuroNative.applySettings560(JSON.stringify(state))}catch(_){}}",
        "try{if(native())KuroNative.applySettings560(JSON.stringify(state))}catch(_){}try{window.dispatchEvent(new CustomEvent('kuro-settings560-changed',{detail:Object.assign({},state)}))}catch(_){}}",
        "settings560.js dispatchEvent",
    )
    s_text = patch_once(
        s_text,
        "widescreen left-rail UI",
        "select('siteView','Site view','Changes the player website view on the next player.',['Mobile','Desktop'])",
        "select('siteView','Site view','Desktop enables the widescreen left-rail UI, DPAD focus, and desktop player view.',['Mobile','Desktop'])",
        "settings560.js siteView description",
    )
    s_path.write_text(s_text, encoding="utf-8")

    # 2. app.js & unified-episodes533.js: capture HiAnime ssl-item-filler flag
    app_path = root / "assets/app.js"
    app_text = app_path.read_text(encoding="utf-8")
    app_text = patch_once(
        app_text,
        "ssl-item-filler",
        "episodes=Array.from(ed.querySelectorAll('.ep-item')).map(x=>({number:x.getAttribute('data-number')||x.textContent.trim(),name:(x.querySelector('.ep-name')?.getAttribute('title')||x.querySelector('.ep-name')?.textContent||x.getAttribute('title')||'').trim(),url:validUrl(abs(x.getAttribute('href'),'dk'))})).filter(x=>x.url)",
        "episodes=Array.from(ed.querySelectorAll('.ep-item')).map(x=>({number:x.getAttribute('data-number')||x.textContent.trim(),name:(x.querySelector('.ep-name')?.getAttribute('title')||x.querySelector('.ep-name')?.textContent||x.getAttribute('title')||'').trim(),filler:x.classList.contains('ssl-item-filler')||x.getAttribute('data-filler')==='true',url:validUrl(abs(x.getAttribute('href'),'dk'))})).filter(x=>x.url)",
        "app.js dk ssl-item-filler",
    )
    app_path.write_text(app_text, encoding="utf-8")

    ue_path = root / "assets/unified-episodes533.js"
    ue_text = ue_path.read_text(encoding="utf-8")
    ue_text = patch_once(
        ue_text,
        "ssl-item-filler",
        "rows=[...epdoc.querySelectorAll('.ep-item[data-number][href]')].map(x=>({number:x.getAttribute('data-number'),name:(x.querySelector('.ep-name')?.getAttribute('title')||x.querySelector('.ep-name')?.textContent||x.getAttribute('title')||'').trim(),url:validUrl(abs(x.getAttribute('href'),s))}));",
        "rows=[...epdoc.querySelectorAll('.ep-item[data-number][href]')].map(x=>({number:x.getAttribute('data-number'),name:(x.querySelector('.ep-name')?.getAttribute('title')||x.querySelector('.ep-name')?.textContent||x.getAttribute('title')||'').trim(),filler:x.classList.contains('ssl-item-filler')||x.getAttribute('data-filler')==='true',url:validUrl(abs(x.getAttribute('href'),s))}));",
        "unified-episodes533.js ssl-item-filler",
    )
    ue_text = patch_once(
        ue_text,
        "ex154.filler=true",
        "for(const ep of found.episodes){if(already.has(ep.number))continue;",
        "for(const ep of found.episodes){if(already.has(ep.number)){if(ep.filler){const ex154=episodes.find(z=>String(z.number)===String(ep.number));if(ex154)ex154.filler=true;}continue;}",
        "unified-episodes533.js merge filler",
    )
    ue_path.write_text(ue_text, encoding="utf-8")

    # 3. failover531.js: pass filler154 + skipFiller154 into KuroNative.play payload
    f_path = root / "assets/failover531.js"
    f_text = f_path.read_text(encoding="utf-8")
    f_text = patch_once(
        f_text,
        "filler154",
        "let e=episodes.map(x=>({number:String(x.number).slice(0,20),name:String(x.name||'').slice(0,160)}));",
        "let e=episodes.map(x=>({number:String(x.number).slice(0,20),name:String(x.name||'').slice(0,160),filler154:Boolean(window.kuroIsEpisodeSkipped154&&window.kuroIsEpisodeSkipped154(selected,x))}));",
        "failover531.js episodes filler154",
    )
    f_text = patch_once(
        f_text,
        "skipFiller154:",
        "let payload={session:chain.id,",
        "let payload={session:chain.id,skipFiller154:Boolean(window.kuroShouldSkipFiller154&&window.kuroShouldSkipFiller154(selected)),",
        "failover531.js payload skipFiller154",
    )
    f_path.write_text(f_text, encoding="utf-8")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: ui_1.5.4.py <kuro-source-dir>")
    root = pathlib.Path(sys.argv[1]).resolve()
    patch_manifest(root)
    patch_updater(root)
    patch_catalog(root)
    patch_offline(root)
    patch_main_activity(root)
    patch_js_hooks(root)
    print("Applied Kuro 1.5.4-beta Detail v2, Desktop-Site Left-Rail Leanback, and Smart Browse/Voice patches.")


if __name__ == "__main__":
    main()
