#!/usr/bin/env python3
"""Kuro 1.5.4 native & WebView bridge patcher.

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
        "<intent><action android:name=\"android.speech.action.RECOGNIZE_SPEECH\"/></intent>"
        "<intent><action android:name=\"android.intent.action.VIEW\"/><category android:name=\"android.intent.category.BROWSABLE\"/><data android:scheme=\"https\"/></intent></queries>",
        "AndroidManifest.xml RECOGNIZE_SPEECH & https browser queries",
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
        'if(input==null||input.length()>("metaBatch154".equals(action)||"watchFeed154".equals(action)?2400:300))throw new IOException("Invalid request");',
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
        '  }else if("watchFeed154".equals(action)){\n'
        '   long id=p.optLong("id",0);String search=p.optString("search","").trim();\n'
        '   String recFields="{id idMal isAdult title{romaji english} bannerImage coverImage{extraLarge large} averageScore episodes format seasonYear genres studios(isMain:true){nodes{name}}}";\n'
        '   String mediaFields="{id bannerImage coverImage{extraLarge large} averageScore duration seasonYear genres streamingEpisodes{title thumbnail} recommendations(page:1,perPage:14,sort:RATING_DESC){nodes{mediaRecommendation"+recFields+"}}}";\n'
        '   String fb="fallback:Page(page:1,perPage:16){media(type:ANIME,isAdult:false,sort:POPULARITY_DESC)"+recFields+"}";\n'
        '   if(id>=1&&id<=999999999){v.put("id",id);query="query($id:Int){Media(id:$id,type:ANIME,isAdult:false)"+mediaFields+" "+fb+"}";}\n'
        '   else if(search.length()>=1&&search.length()<=140){v.put("search",search);query="query($search:String){Media(search:$search,type:ANIME,isAdult:false,sort:SEARCH_MATCH)"+mediaFields+" "+fb+"}";}\n'
        '   else{query="query{"+fb+"}";}\n'
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
    static boolean validDownloadId155(String id) {
        return KuroPlayback561.validKey(id) || (id != null && id.matches("kuro561:[0-9a-fA-F]{1,8}"));
    }

    private static String resolveBatchCheckpointKey155(String rawKey) {
        if (KuroPlayback561.validKey(rawKey)) return rawKey;
        if (rawKey != null && rawKey.startsWith("zenkai|")) {
            int first = rawKey.indexOf('|');
            int last = rawKey.lastIndexOf('|');
            if (first > 0 && last > first + 1 && last + 1 < rawKey.length()) {
                String showUrl = rawKey.substring(first + 1, last);
                String epNum = rawKey.substring(last + 1);
                String computed = KuroPlayback561.key("zenkai", showUrl, epNum);
                if (KuroPlayback561.validKey(computed)) return computed;
            }
        }
        return "";
    }

    static void confirmBatchDownload154(Activity activity, String rawJson) {
        if (!KuroDialogs581.canShow155(activity) || rawJson == null || rawJson.length() > 250000) return;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(KuroDialogs581.themed(activity))
                .setTitle("Batch download " + valid.size() + (valid.size() == 1 ? " episode?" : " episodes?"))
                .setMessage(showTitle + "\\n" + rangeLabel + "\\n\\nQueues verified direct HLS streams into Kuro offline storage. Only store content you have permission to keep.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Queue " + valid.size(), (d, w) -> {
                    int queued = 0;
                    try { manager(activity); } catch (RuntimeException ignored) {}
                    for (JSONObject row : valid) {
                        try {
                            String addr = row.optString("address", "");
                            String ckey = resolveBatchCheckpointKey155(row.optString("key", ""));
                            String t = limit(row.optString("title", "Video"), 120);
                            String ep = limit(row.optString("episode", ""), 20);
                            String id = KuroPlayback561.sha(ckey + "\\n" + addr);
                            JSONObject meta = new JSONObject().put("title", t).put("episode", ep).put("key", ckey);
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
                });
        KuroDialogs581.safeShowBuilder155(builder, activity);
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

    if "resumeFullscreenUntil153" in text:
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

    # 7. Android 15 (targetSdk 35) & all-API edge-to-edge WindowInsets safe-area fix
    insets_helpers = """ private androidx.core.view.WindowInsetsCompat lastWindowInsets154;
 private int fallbackStatusBarHeight154(){
  try{int id=getResources().getIdentifier("status_bar_height","dimen","android");return id>0?getResources().getDimensionPixelSize(id):0;}catch(RuntimeException e){return 0;}
 }
 private void applyRootInsets154(androidx.core.view.WindowInsetsCompat insets){
  if(root==null)return;
  if(customView!=null||enhancedFullscreen||isInPictureInPictureMode()){
   if(root.getPaddingLeft()!=0||root.getPaddingTop()!=0||root.getPaddingRight()!=0||root.getPaddingBottom()!=0)root.setPadding(0,0,0,0);
   return;
  }
  if(insets==null){
   int topFb=(!isInMultiWindowMode()&&getResources().getConfiguration().orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE)?fallbackStatusBarHeight154():0;
   if(root.getPaddingTop()!=topFb||root.getPaddingLeft()!=0||root.getPaddingRight()!=0||root.getPaddingBottom()!=0)root.setPadding(0,topFb,0,0);
   return;
  }
  int barMask=androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout();
  androidx.core.graphics.Insets vis=insets.getInsets(barMask);
  androidx.core.graphics.Insets ign=insets.getInsetsIgnoringVisibility(barMask);
  androidx.core.graphics.Insets ime=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime());
  int l=Math.max(vis.left,ign.left);
  int t=Math.max(vis.top,ign.top);
  if(t<=0&&!isInMultiWindowMode()&&getResources().getConfiguration().orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE)t=fallbackStatusBarHeight154();
  int r=Math.max(vis.right,ign.right);
  int b=Math.max(Math.max(vis.bottom,ign.bottom),ime.bottom);
  if(root.getPaddingLeft()!=l||root.getPaddingTop()!=t||root.getPaddingRight()!=r||root.getPaddingBottom()!=b){
   root.setPadding(l,t,r,b);
  }
 }
 private void syncWindowSystemBars154(){
  Window w=getWindow();if(w==null)return;
  View decor=w.getDecorView();
  boolean fs=customView!=null||enhancedFullscreen||isInPictureInPictureMode();
  if(Build.VERSION.SDK_INT>=28){
   WindowManager.LayoutParams lp=w.getAttributes();
   int wantCutout=fs?WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES:WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
   if(lp.layoutInDisplayCutoutMode!=wantCutout){lp.layoutInDisplayCutoutMode=wantCutout;w.setAttributes(lp);}
  }
  if(decor!=null){
   androidx.core.view.WindowInsetsControllerCompat ctrl=androidx.core.view.WindowCompat.getInsetsController(w,decor);
   if(ctrl!=null){
    ctrl.setAppearanceLightStatusBars(false);
    ctrl.setAppearanceLightNavigationBars(false);
    if(fs){
     ctrl.setSystemBarsBehavior(androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
     ctrl.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
    }else{
     ctrl.show(androidx.core.view.WindowInsetsCompat.Type.systemBars());
    }
   }
  }
  applyRootInsets154(lastWindowInsets154);
  if(root!=null)androidx.core.view.ViewCompat.requestApplyInsets(root);
 }
 private void setupEdgeToEdgeInsets154(){
  Window w=getWindow();
  if(w!=null)androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w,false);
  if(root!=null){
   root.setBackgroundColor(KuroTheme12.background());
   androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
    lastWindowInsets154=insets;
    applyRootInsets154(insets);
    return androidx.core.view.WindowInsetsCompat.CONSUMED;
   });
   syncWindowSystemBars154();
  }
 }
"""
    text = patch_once(
        text,
        "setupEdgeToEdgeInsets154()",
        " private SharedPreferences settings560(){",
        insets_helpers + " private SharedPreferences settings560(){",
        "MainActivity setupEdgeToEdgeInsets154 helpers",
    )
    text = patch_once(
        text,
        "setContentView(root);setupEdgeToEdgeInsets154();",
        "KuroTheme12.init(this);KuroTheme12.applyWindow(this);root=new FrameLayout(this);setContentView(root);",
        "KuroTheme12.init(this);KuroTheme12.applyWindow(this);root=new FrameLayout(this);setContentView(root);setupEdgeToEdgeInsets154();",
        "MainActivity onCreate setupEdgeToEdgeInsets154",
    )
    text = patch_once(
        text,
        "if(root!=null){root.setBackgroundColor(floor);syncWindowSystemBars154();}",
        "if(ui!=null)ui.setBackgroundColor(floor);if(root!=null)root.setBackgroundColor(floor);",
        "if(ui!=null)ui.setBackgroundColor(floor);if(root!=null){root.setBackgroundColor(floor);syncWindowSystemBars154();}",
        "MainActivity kuroTheme12WindowApplied syncWindowSystemBars154",
    )
    text = patch_once(
        text,
        "getWindow().getDecorView().setSystemUiVisibility(5894);syncWindowSystemBars154();applyPlayerOptions(null);",
        "getWindow().getDecorView().setSystemUiVisibility(5894);applyPlayerOptions(null);",
        "getWindow().getDecorView().setSystemUiVisibility(5894);syncWindowSystemBars154();applyPlayerOptions(null);",
        "MainActivity onShowCustomView syncWindowSystemBars154",
    )
    text = patch_once(
        text,
        "getWindow().getDecorView().setSystemUiVisibility(enhancedFullscreen?5894:0);syncWindowSystemBars154();",
        "getWindow().getDecorView().setSystemUiVisibility(enhancedFullscreen?5894:0);",
        "getWindow().getDecorView().setSystemUiVisibility(enhancedFullscreen?5894:0);syncWindowSystemBars154();",
        "MainActivity hideFullscreen syncWindowSystemBars154",
    )
    text = patch_once(
        text,
        "getWindow().getDecorView().setSystemUiVisibility(5894);syncWindowSystemBars154();watchPlayerBox.requestLayout();",
        "getWindow().getDecorView().setSystemUiVisibility(5894);watchPlayerBox.requestLayout();",
        "getWindow().getDecorView().setSystemUiVisibility(5894);syncWindowSystemBars154();watchPlayerBox.requestLayout();",
        "MainActivity enterEnhancedFullscreen syncWindowSystemBars154",
    )
    text = patch_once(
        text,
        "if(customView==null)getWindow().getDecorView().setSystemUiVisibility(0);syncWindowSystemBars154();",
        "if(customView==null)getWindow().getDecorView().setSystemUiVisibility(0);",
        "if(customView==null)getWindow().getDecorView().setSystemUiVisibility(0);syncWindowSystemBars154();",
        "MainActivity exitEnhancedFullscreen syncWindowSystemBars154",
    )
    text = patch_once(
        text,
        "super.onPictureInPictureModeChanged(inPip,config);syncWindowSystemBars154();",
        "super.onPictureInPictureModeChanged(inPip,config);",
        "super.onPictureInPictureModeChanged(inPip,config);syncWindowSystemBars154();",
        "MainActivity onPictureInPictureModeChanged syncWindowSystemBars154",
    )
    text = patch_once(
        text,
        "super.onConfigurationChanged(config);syncWindowSystemBars154();",
        "super.onConfigurationChanged(config);",
        "super.onConfigurationChanged(config);syncWindowSystemBars154();",
        "MainActivity onConfigurationChanged syncWindowSystemBars154",
    )
    text = patch_once(
        text,
        "super.onResume();activityResumed=true;syncWindowSystemBars154();",
        "super.onResume();activityResumed=true;",
        "super.onResume();activityResumed=true;syncWindowSystemBars154();",
        "MainActivity onResume syncWindowSystemBars154",
    )

    # 8. Always re-sweep cosmetic ad guard on player frame finish even when DOCUMENT_START is active
    text = patch_once(
        text,
        "/*kuroAdSweep154*/",
        'if(!adFrameMode&&settings560().getBoolean("adBlock",true))v.evaluateJavascript(assetText("player-ad-guard541.js"),null);',
        'if(settings560().getBoolean("adBlock",true))/*kuroAdSweep154*/v.evaluateJavascript(assetText("player-ad-guard541.js"),null);',
        "MainActivity onPageFinished player-ad-guard541 resweep",
    )

    path.write_text(text, encoding="utf-8")


def patch_offline_activity_insets(root: pathlib.Path) -> None:
    path = root / "src/app/lovelycrxshx/kuro/KuroOfflineActivity561.java"
    text = path.read_text(encoding="utf-8")
    text = patch_once(
        text,
        "setDecorFitsSystemWindows",
        'LinearLayout layout=new LinearLayout(this);layout.setBackgroundColor(Color.BLACK);layout.setOrientation(LinearLayout.VERTICAL);setContentView(layout);',
        'if(getWindow()!=null)androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(),false);'
        'LinearLayout layout=new LinearLayout(this);layout.setBackgroundColor(Color.BLACK);layout.setOrientation(LinearLayout.VERTICAL);setContentView(layout);'
        'androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(layout,(v,insets)->{'
        'if(isInPictureInPictureMode()){v.setPadding(0,0,0,0);return androidx.core.view.WindowInsetsCompat.CONSUMED;}'
        'int m=androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout();'
        'androidx.core.graphics.Insets b=insets.getInsetsIgnoringVisibility(m);'
        'v.setPadding(b.left,b.top,b.right,b.bottom);return androidx.core.view.WindowInsetsCompat.CONSUMED;});'
        'androidx.core.view.ViewCompat.requestApplyInsets(layout);',
        "KuroOfflineActivity561 WindowInsets",
    )
    text = patch_once(
        text,
        "requestApplyInsets((View)top.getParent())",
        'if(top!=null)top.setVisibility(inPip?View.GONE:View.VISIBLE);',
        'if(top!=null){top.setVisibility(inPip?View.GONE:View.VISIBLE);if(top.getParent() instanceof View)androidx.core.view.ViewCompat.requestApplyInsets((View)top.getParent());}',
        "KuroOfflineActivity561 PiP insets refresh",
    )
    path.write_text(text, encoding="utf-8")


PLAYER_AD_GUARD_154_JS = r"""/* Kuro 1.5.4 player-only multi-layer cosmetic & In-Page-Push ad guard.
 * Blocks floating Ad/IPP/SocialBar banners (light DOM, open/closed Shadow DOM,
 * and cross-origin banner iframes) without touching video, controls, subtitles,
 * or human-verification challenges. */
(function () {
  'use strict';
  try {
    if (location.hostname === 'kuro.local') return;
    if (window.__kuroAdGuard541) {
      if (typeof window.__kuroAdGuardScan154 === 'function') window.__kuroAdGuardScan154();
      return;
    }
    window.__kuroAdGuard541 = true;

    const selectors = [
      '[data-ad-slot]', '[data-ad-unit]', '[data-ad-zone]', '[data-ad-zone-id]',
      'ins.adsbygoogle', 'iframe[id^="google_ads_iframe"]',
      '.adsbox', '.ad-popup', '.ads-popup', '.ad-overlay', '.ads-overlay',
      '.advertisement-overlay', '[id^="exoclick-"]', '[class^="exo-native-widget"]',
      '[class*="exo-ipp"]', '[id*="exo-ipp"]', '[class*="in-page-push"]', '[id*="in-page-push"]',
      '[class*="social-bar"]', '[id*="social-bar"]', '[class*="adspyglass"]', '[id*="adspyglass"]',
      '[id^="asg-"]', '[class^="asg-"]', '[data-asg-zone]', '[data-exo-zone]',
      'iframe[src*="adsterra"]', 'iframe[src*="exoclick"]', 'iframe[src*="exosrv"]',
      'iframe[src*="monetag"]', 'iframe[src*="adspyglass"]', 'iframe[src*="clickadu"]',
      'iframe[src*="hilltopads"]', 'iframe[src*="galaksion"]', 'iframe[src*="popads"]',
      'iframe[src*="profitablecpmrate"]', 'iframe[src*="highperformanceformat"]',
      'iframe[src*="topcreativeformat"]', 'iframe[src*="juicyads"]', 'iframe[src*="trafficjunky"]'
    ].join(',');

    const verification = '#challenge-form,#challenge-stage,.cf-challenge,.cf-turnstile,.h-captcha,.g-recaptcha,[src*="challenges.cloudflare.com"],[src*="hcaptcha.com"],[src*="recaptcha"]';
    const controls = [
      'video', 'audio', 'canvas', '[role="slider"]',
      '.vjs-control-bar', '.jw-controls', '.plyr__controls', '.video-controls', '.player-controls',
      '.art-bottom', '.art-controls', '.dplayer-controller', '#kuro-oled-hud153',
      '.jw-captions', '.vjs-text-track-display', '.plyr__captions', '.art-subtitle',
      '[id*="subtitle"]', '[class*="subtitle"]', '[class*="caption"]'
    ].join(',');

    let observer = null, timer = 0;
    const pending = new Set(), retriedBadges = new WeakSet();
    const shadowHosts = new WeakMap();
    const observedRoots = new WeakSet();

    function verificationActive() {
      const t = (document.title || '').trim();
      if (/^(just a moment|attention required|verify you are human|access denied)/i.test(t)) return true;
      if (/challenges\.cloudflare\.com|hcaptcha\.com|recaptcha\.net/i.test(location.hostname)) return true;
      return !!document.querySelector(verification);
    }

    // Block script-triggered popup windows inside third-party embeds (never on challenge pages).
    try {
      if (!verificationActive()) {
        const noopOpen = function () { return null; };
        window.open = noopOpen;
      }
    } catch (_) {}

    function isMainPlayerIframe(ifr) {
      if (!ifr || ifr.tagName !== 'IFRAME') return false;
      const src = String(ifr.getAttribute('src') || '');
      if (/cloudflare|turnstile|hcaptcha|recaptcha|challenge|verify/i.test(src)) return true;
      const r = ifr.getBoundingClientRect();
      if (r.width >= Math.max(220, innerWidth * 0.72) && r.height >= Math.max(140, innerHeight * 0.65)) return true;
      return false;
    }

    function ownsProtectedIframe(el) {
      if (!el || el.tagName === 'IFRAME') return false;
      const frames = el.querySelectorAll('iframe');
      if (!frames.length) return false;
      for (let i = 0; i < frames.length; i++) {
        const f = frames[i];
        const src = String(f.getAttribute('src') || '');
        if (/cloudflare|turnstile|hcaptcha|recaptcha|challenge|verify/i.test(src)) return true;
        // Protect unknown iframes unless clearly tiny/banner-sized while a real player exists.
        const r = f.getBoundingClientRect();
        const hasMainMedia = !!document.querySelector('video') || Array.prototype.some.call(document.querySelectorAll('iframe'), isMainPlayerIframe);
        if (!hasMainMedia || r.height > 190 || r.height === 0) return true;
      }
      return false;
    }

    function protectedNode(el) {
      if (!el || el.nodeType !== 1 || /^(HTML|BODY|HEAD|STYLE|SCRIPT|VIDEO|AUDIO|SOURCE|TRACK|CANVAS)$/.test(el.tagName)) return true;
      try {
        if (el.matches(controls) || el.closest(controls) || el.closest(verification)) return true;
        if (el.querySelector(verification) || el.querySelector(controls)) return true;
      } catch (_) {
        return true;
      }
      if (el.tagName === 'IFRAME' && isMainPlayerIframe(el)) return true;
      if (ownsProtectedIframe(el)) return true;
      return false;
    }

    function hide(el) {
      if (protectedNode(el)) return false;
      el.style.setProperty('display', 'none', 'important');
      el.style.setProperty('visibility', 'hidden', 'important');
      el.style.setProperty('opacity', '0', 'important');
      el.style.setProperty('pointer-events', 'none', 'important');
      el.style.setProperty('max-height', '0px', 'important');
      el.setAttribute('data-kuro-ad-hidden', '1');
      return true;
    }

    function parentAcrossShadow(node) {
      if (!node) return null;
      if (node.parentElement) return node.parentElement;
      const rootNode = typeof node.getRootNode === 'function' ? node.getRootNode() : null;
      if (rootNode && shadowHosts.has(rootNode)) return shadowHosts.get(rootNode);
      if (rootNode && rootNode.host) return rootNode.host;
      return null;
    }

    function notifyParentIfEntireFrameIsAd() {
      try {
        if (window === window.top) return;
        if (document.querySelector('video,audio,canvas') || verificationActive()) return;
        if (innerHeight > 0 && innerHeight <= 210 && innerWidth >= 140) {
          document.documentElement.style.setProperty('background', 'transparent', 'important');
          document.documentElement.style.setProperty('display', 'none', 'important');
          if (document.body) document.body.style.setProperty('display', 'none', 'important');
          window.parent.postMessage('__kuro_ad_frame_154__', '*');
        }
      } catch (_) {}
    }

    function closeCue(el) {
      if (el.querySelector('button[aria-label*="Close" i],button[title*="Close" i],[data-close],[class*="close" i],[id*="close" i]')) return true;
      const children = el.querySelectorAll('button,span,a,div,i,b');
      for (let i = 0; i < Math.min(children.length, 60); i++) {
        if (/^(?:×|✕|✖|⨯|╳|x|close)$/i.test((children[i].textContent || '').trim())) return true;
      }
      // SVG close icon in top-right/right corner of a floating notification banner
      const svgs = el.querySelectorAll('svg');
      if (svgs.length > 0 && svgs.length <= 6) {
        const er = el.getBoundingClientRect();
        for (let i = 0; i < svgs.length; i++) {
          const sr = svgs[i].getBoundingClientRect();
          if (sr.width >= 6 && sr.width <= 32 && sr.height >= 6 && sr.height <= 32 && sr.left >= er.left + er.width * 0.65) {
            return true;
          }
        }
      }
      return false;
    }

    function inspectBadge(badge) {
      const text = (badge.textContent || '').trim();
      if (!/^(?:ad|ads|advertisement|sponsored|adchoices|ad\s*choices)$/i.test(text)) return;
      if (badge.children && badge.children.length > 2) return;
      if (protectedNode(badge)) return;

      let el = parentAcrossShadow(badge);
      let bestTarget = null;
      for (let depth = 0; el && depth < 12; depth++, el = parentAcrossShadow(el)) {
        if (el === document.body || el === document.documentElement) break;
        if (protectedNode(el)) break;
        const css = getComputedStyle(el), r = el.getBoundingClientRect();
        if (!/^(?:absolute|fixed|sticky)$/.test(css.position)) continue;
        if (r.width < 80 || r.height < 22 || r.height > Math.max(340, innerHeight * 0.78)) continue;
        // Any non-protected floating overlay card explicitly carrying a standalone
        // "Ad" / "Sponsored" badge (including Adsterra Social Bar & ExoClick IPP) is an ad.
        const br = badge.getBoundingClientRect();
        const badgeStyled = getComputedStyle(badge).backgroundColor !== 'rgba(0, 0, 0, 0)' || (br.width > 0 && br.width <= 76 && br.height <= 36);
        const copy = (el.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 1200);
        if (badgeStyled || closeCue(el) || /\b(?:message\s+from|secret\s+video|xxx|\[720p\]|\[1080p\]|\.mp4)\b/i.test(copy)) {
          bestTarget = el;
        }
      }

      if (bestTarget) {
        hide(bestTarget);
        notifyParentIfEntireFrameIsAd();
        return;
      }

      // If the entire small iframe IS the ad banner and the badge sits on a static wrapper inside it
      if (window !== window.top && innerHeight > 0 && innerHeight <= 200 && !document.querySelector('video')) {
        let wrap = parentAcrossShadow(badge);
        while (wrap && wrap !== document.body && wrap !== document.documentElement && !protectedNode(wrap)) {
          hide(wrap);
          wrap = parentAcrossShadow(wrap);
        }
        notifyParentIfEntireFrameIsAd();
        return;
      }

      if (!retriedBadges.has(badge)) {
        retriedBadges.add(badge);
        setTimeout(() => { if (badge.isConnected && !verificationActive()) inspectBadge(badge); }, 550);
      }
    }

    function inspectSuspiciousOverlay(el) {
      if (!el || el.nodeType !== 1 || protectedNode(el)) return;
      const css = getComputedStyle(el);
      if (!/^(?:absolute|fixed)$/.test(css.position)) return;
      const r = el.getBoundingClientRect();
      // 1. Transparent full-screen click-hijack <a target="_blank"> overlay over the video
      if (el.tagName === 'A' && el.getAttribute('target') === '_blank' && r.width >= innerWidth * 0.5 && r.height >= innerHeight * 0.5) {
        if ((parseFloat(css.opacity) || 1) < 0.08 || !(el.textContent || '').trim()) {
          hide(el);
          return;
        }
      }
      // 2. Unbadged In-Page Push / adult fake-video notification strip
      if (r.width >= 150 && r.height >= 34 && r.height <= 170) {
        const copy = (el.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 600);
        if (/\b(?:1\s+message\s+from|secret\s+videos?\s*xxx|live\s*jasmin|stripchat|chaturbate|jerkmate|hookup\s+tonight|hot\s+girls?\s+near)\b/i.test(copy) && (closeCue(el) || el.querySelector('img'))) {
          hide(el);
          notifyParentIfEntireFrameIsAd();
        }
      }
    }

    function scan(root) {
      if (!root || (root.nodeType !== 1 && root.nodeType !== 11)) return;
      if (verificationActive()) { if (observer) observer.disconnect(); pending.clear(); return; }
      if (root.nodeType === 1) {
        if (root.matches(selectors)) { hide(root); notifyParentIfEntireFrameIsAd(); }
        inspectSuspiciousOverlay(root);
        if (/^(SPAN|SMALL|B|STRONG|LABEL|DIV|I|EM|SUP)$/.test(root.tagName) && root.textContent && root.textContent.length <= 24) {
          inspectBadge(root);
        }
        if (root.shadowRoot) observeShadow(root.shadowRoot, root);
      }
      const marked = root.querySelectorAll ? root.querySelectorAll(selectors) : [];
      for (let i = 0; i < Math.min(marked.length, 300); i++) {
        hide(marked[i]);
        notifyParentIfEntireFrameIsAd();
      }
      const nodes = root.querySelectorAll ? root.querySelectorAll('span,small,b,strong,label,div,i,em,sup,a') : [];
      for (let i = 0; i < Math.min(nodes.length, 1800); i++) {
        const n = nodes[i];
        if (n.shadowRoot) observeShadow(n.shadowRoot, n);
        if (n.textContent && n.textContent.length <= 24) inspectBadge(n);
        if (n.tagName === 'DIV' || n.tagName === 'A') inspectSuspiciousOverlay(n);
      }
      // Inspect accessible same-origin / about:blank / srcdoc banner iframes
      const iframes = root.querySelectorAll ? root.querySelectorAll('iframe') : [];
      for (let i = 0; i < Math.min(iframes.length, 24); i++) {
        const ifr = iframes[i];
        if (isMainPlayerIframe(ifr)) continue;
        try {
          const doc = ifr.contentDocument;
          if (doc && doc.documentElement) scan(doc.documentElement);
        } catch (_) {}
      }
    }

    function flush() {
      timer = 0;
      const batch = Array.from(pending); pending.clear();
      for (const node of batch) scan(node);
    }

    function enqueue(node) {
      if (!node) return;
      if (node.nodeType === 3) node = node.parentElement;
      if (!node || (node.nodeType !== 1 && node.nodeType !== 11)) return;
      pending.add(node);
      if (!timer) timer = setTimeout(flush, 80);
    }

    function observeShadow(sr, host) {
      if (!sr || observedRoots.has(sr)) return;
      observedRoots.add(sr);
      if (host) shadowHosts.set(sr, host);
      try {
        const mo = new MutationObserver(records => {
          for (const rec of records) {
            if (rec.type === 'characterData') enqueue(rec.target);
            else for (const n of rec.addedNodes) enqueue(n);
          }
        });
        mo.observe(sr, { childList: true, subtree: true, characterData: true });
        enqueue(sr);
      } catch (_) {}
    }

    // Hook attachShadow at document-start so closed & open Shadow DOM ad widgets are caught too.
    try {
      const origAttachShadow = Element.prototype.attachShadow;
      if (typeof origAttachShadow === 'function') {
        Element.prototype.attachShadow = function (init) {
          const sr = origAttachShadow.call(this, init);
          observeShadow(sr, this);
          return sr;
        };
      }
    } catch (_) {}

    // Child ad-frame -> parent iframe hider
    window.addEventListener('message', function (ev) {
      if (!ev || ev.data !== '__kuro_ad_frame_154__' || verificationActive()) return;
      const frames = document.querySelectorAll('iframe');
      for (let i = 0; i < frames.length; i++) {
        if (frames[i].contentWindow === ev.source && !isMainPlayerIframe(frames[i])) {
          hide(frames[i]);
        }
      }
    });

    observer = new MutationObserver(records => {
      for (const record of records) {
        if (record.type === 'characterData') enqueue(record.target);
        else for (const node of record.addedNodes) enqueue(node);
      }
    });
    observer.observe(document, { childList: true, subtree: true, characterData: true });

    window.__kuroAdGuardScan154 = function () {
      if (document.documentElement) enqueue(document.documentElement);
    };
    if (document.documentElement) enqueue(document.documentElement);
    document.addEventListener('DOMContentLoaded', () => enqueue(document.documentElement), { once: true });
    window.addEventListener('load', () => enqueue(document.documentElement), { once: true });
  } catch (_) { /* Cosmetic filtering must never interrupt playback. */ }
})();
"""


def patch_player_ad_blocker(root: pathlib.Path) -> None:
    # 1. Expand PlayerAdBlocker.java NETWORKS with In-Page-Push / SocialBar / Monetag / AdSpyglass networks
    ab_path = root / "src/app/lovelycrxshx/kuro/PlayerAdBlocker.java"
    ab_text = ab_path.read_text(encoding="utf-8")
    extra_networks = (
        '"onclicka.com","onclickperformance.com","trafficstars.com","clickadu.com","trafficfactory.biz",'
        '"adsterratech.com","profitablecpmrate.com","profitableratecpm.com","highperformanceformat.com","topcreativeformat.com",'
        '"cloudatacdn.com","wpadmngr.com","wpncdn.com","tsyndicate.com","twinrdsrv.com","twinred.com",'
        '"adcash.com","acscdn.com","ufpcdn.com","sufrcdn.com","onclasrv.com",'
        '"monetag.com","alwingulla.com","gizokraijaw.net","glizauvo.net","upskittyan.com","ptauxofi.net","darnob.com","thaudray.com",'
        '"galaksion.com","pushame.com","hilltopads.net","hilltopads.com","htlbid.com",'
        '"adspyglass.com","asgcdn.com","sbbrdy.com","mondiad.com","mndsrv.com",'
        '"clickaine.com","rollerads.com","richads.com","evadav.com","push.house",'
        '"ero-advertising.com","plugrush.com","yllix.com","adtng.com","adskeeper.com","adskeeper.co.uk",'
        '"mgid.com","taboola.com","outbrain.com","revcontent.com","bidvertiser.com","bidgear.com","pubfuture.com","acint.net","rtmark.net"));'
    )
    ab_text = patch_once(
        ab_text,
        '"profitablecpmrate.com"',
        '"onclicka.com","onclickperformance.com","trafficstars.com","clickadu.com","trafficfactory.biz"));',
        extra_networks,
        "PlayerAdBlocker NETWORKS expansion",
    )
    ab_path.write_text(ab_text, encoding="utf-8")

    # 2. Replace assets/player-ad-guard541.js with hardened 1.5.4 multi-layer guard
    guard_path = root / "assets/player-ad-guard541.js"
    guard_path.write_text(PLAYER_AD_GUARD_154_JS, encoding="utf-8")


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
        "let e=episodes.map(x=>{let o={number:String(x.number).slice(0,20),name:String(x.name||'').slice(0,160),filler154:Boolean(window.kuroIsEpisodeSkipped154&&window.kuroIsEpisodeSkipped154(selected,x))};if(x.filler||/\\bfiller\\b/i.test(String(x.name||'')))o.f=1;else if(/\\brecap\\b/i.test(String(x.name||'')))o.r=1;try{if(typeof watchedHas==='function'&&watchedHas(selected,x.number))o.w=1}catch(_){}return o});",
        "failover531.js episodes filler154 + f/r/w",
    )
    f_text = patch_once(
        f_text,
        "skipFiller154:",
        "let payload={session:chain.id,",
        "let payload={session:chain.id,skipFiller154:Boolean(window.kuroShouldSkipFiller154&&window.kuroShouldSkipFiller154(selected)),",
        "failover531.js payload skipFiller154",
    )
    f_path.write_text(f_text, encoding="utf-8")

    # 4. route-state538.js & smooth-ui.css: never auto-scroll down on Home when entering the app
    rs_path = root / "assets/route-state538.js"
    if rs_path.is_file():
        rs_text = rs_path.read_text(encoding="utf-8")
        rs_text = patch_once(
            rs_text,
            "history.scrollRestoration='manual'",
            " const key='kuro-page-checkpoint-v1';",
            " try{if('scrollRestoration' in history)history.scrollRestoration='manual'}catch(_){}\n const key='kuro-page-checkpoint-v1';",
            "route-state538.js manual scrollRestoration",
        )
        rs_text = patch_once(
            rs_text,
            "if(page==='home'){scrollTarget=null;window.scrollTo(0,0);return}",
            " function restoreScroll(page,y){\n  scrollTarget={view:page,y:Math.round(safeNumber(y,100000))};",
            " function restoreScroll(page,y){\n  if(page==='home'){scrollTarget=null;window.scrollTo(0,0);return}\n  scrollTarget={view:page,y:Math.round(safeNumber(y,100000))};",
            "route-state538.js no Home auto-scroll on startup",
        )
        rs_path.write_text(rs_text, encoding="utf-8")

    su_path = root / "assets/smooth-ui.css"
    if su_path.is_file():
        su_text = su_path.read_text(encoding="utf-8")
        su_text = su_text.replace("  scroll-behavior: smooth;\n", "  scroll-behavior: auto;\n")
        su_path.write_text(su_text, encoding="utf-8")


KURO_TV_BANNER_XML = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="320dp"
    android:height="180dp"
    android:viewportWidth="320"
    android:viewportHeight="180">
    <path
        android:fillColor="#0A0710"
        android:pathData="M0,0h320v180h-320z" />
    <path
        android:fillColor="#160E23"
        android:pathData="M10,10h300v160h-300z" />
    <path
        android:strokeColor="#3D2659"
        android:strokeWidth="2"
        android:pathData="M10,10h300v160h-300z" />
    <path
        android:fillColor="#A65BE4"
        android:fillAlpha="0.18"
        android:pathData="M86,90m-44,0a44,44 0,1 1,88 0a44,44 0,1 1,-88 0" />
    <path
        android:fillColor="#B478EE"
        android:pathData="M72,62h10v24l20,-24h13l-21,25l23,31h-13l-17,-23l-5,6v17h-10z" />
    <path
        android:fillColor="#F4F1FA"
        android:pathData="M148,70h9v17l15,-17h11l-16,18l17,22h-11l-12,-16l-4,4v12h-9z" />
    <path
        android:fillColor="#F4F1FA"
        android:pathData="M189,70h9v24c0,5 3,8 8,8s8,-3 8,-8v-24h9v24c0,10 -7,17 -17,17s-17,-7 -17,-17z" />
    <path
        android:fillColor="#F4F1FA"
        android:pathData="M231,70h17c9,0 15,5 15,13c0,6 -3,10 -9,12l10,15h-10l-9,-14h-5v14h-9zM240,78v11h7c4,0 6,-2 6,-5.5s-2,-5.5 -6,-5.5z" />
    <path
        android:fillColor="#B478EE"
        android:pathData="M286,70c11,0 19,9 19,20s-8,20 -19,20s-19,-9 -19,-20s8,-20 19,-20zM286,78c-6,0 -10,5 -10,12s4,12 10,12s10,-5 10,-12s-4,-12 -10,-12z" />
    <path
        android:fillColor="#A65BE4"
        android:fillAlpha="0.88"
        android:pathData="M148,118h96v4h-96z" />
</vector>
"""


KURO_FAST_NET_154_JAVA = r"""package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Kuro 1.5.4 high-speed HTTP/2 connection pool + Encrypted DNS-over-HTTPS (DoH)
 * ISP unblock resolver (Cloudflare 1.1.1.1 & Google 8.8.8.8 over IP-literal TLS).
 */
final class KuroFastNet154 {
    static final class SourceResult {
        final byte[] bytes;
        final String contentType;
        SourceResult(byte[] bytes, String contentType) {
            this.bytes = bytes;
            this.contentType = contentType == null ? "" : contentType;
        }
    }

    private static final class DnsEntry {
        final List<InetAddress> addresses;
        final long expiresAtMs;
        DnsEntry(List<InetAddress> addresses, long expiresAtMs) {
            this.addresses = addresses;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private static final class CatalogCacheEntry {
        final String body;
        final long expiresAtMs;
        CatalogCacheEntry(String body, long expiresAtMs) {
            this.body = body;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private static final long DNS_TTL_MS = 10L * 60L * 1000L;
    private static final long CATALOG_TTL_MS = 90L * 1000L;
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private static volatile String dohMode = "auto"; // "auto" | "always" | "off"
    private static final ConcurrentHashMap<String, DnsEntry> dnsCache = new ConcurrentHashMap<>();
    private static final Set<String> poisonedHosts = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static final Map<String, CatalogCacheEntry> catalogCache = Collections.synchronizedMap(
            new LinkedHashMap<String, CatalogCacheEntry>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CatalogCacheEntry> eldest) {
                    return size() > 64;
                }
            });

    static final ConnectionPool POOL = new ConnectionPool(12, 3, TimeUnit.MINUTES);

    // Bootstrap client hits numeric IP literals (1.1.1.1 / 8.8.8.8 / 1.0.0.1) directly over TLS,
    // requiring zero system DNS resolution even when carrier DNS is poisoned or offline.
    private static final OkHttpClient DOH_CLIENT = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(4, 2, TimeUnit.MINUTES))
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    static final Dns DNS = new Dns() {
        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            if (hostname == null || hostname.isEmpty()) throw new UnknownHostException("Empty host");
            String host = hostname.trim().toLowerCase(Locale.ROOT);
            if ("localhost".equals(host) || isNumericIpLiteral(host)) {
                return Dns.SYSTEM.lookup(hostname);
            }

            long now = System.currentTimeMillis();
            DnsEntry cached = dnsCache.get(host);
            if (cached != null && cached.expiresAtMs > now && !cached.addresses.isEmpty()) {
                return cached.addresses;
            }

            String mode = dohMode;
            if ("off".equals(mode)) {
                List<InetAddress> sys = Dns.SYSTEM.lookup(hostname);
                putDnsCache(host, sys, now + DNS_TTL_MS);
                return sys;
            }

            boolean forceDoh = "always".equals(mode) || poisonedHosts.contains(host);
            if (!forceDoh) {
                try {
                    List<InetAddress> sys = Dns.SYSTEM.lookup(hostname);
                    List<InetAddress> clean = filterValidPublicAddresses(sys);
                    if (!clean.isEmpty()) {
                        putDnsCache(host, clean, now + DNS_TTL_MS);
                        return clean;
                    }
                    markPoisoned(host);
                } catch (UnknownHostException ignored) {
                    // Fall through to encrypted DoH when carrier DNS blocks or fails the domain.
                }
            }

            List<InetAddress> doh = resolveViaDoh(host);
            if (!doh.isEmpty()) {
                putDnsCache(host, doh, now + DNS_TTL_MS);
                return doh;
            }

            // Final fallback to system DNS if DoH endpoints are blocked on a restricted firewall.
            List<InetAddress> fallback = Dns.SYSTEM.lookup(hostname);
            if (!fallback.isEmpty()) {
                putDnsCache(host, fallback, now + 60000L);
                return fallback;
            }
            throw new UnknownHostException("Could not resolve " + hostname);
        }
    };

    static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .dns(DNS)
            .connectionPool(POOL)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(14, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    private KuroFastNet154() {}

    static void setDohMode(String mode) {
        String normalized = "always".equals(mode) ? "always" : ("off".equals(mode) ? "off" : "auto");
        if (!normalized.equals(dohMode)) {
            dohMode = normalized;
            dnsCache.clear();
        }
    }

    static String getDohMode() {
        return dohMode;
    }

    static void clearCaches() {
        dnsCache.clear();
        poisonedHosts.clear();
        catalogCache.clear();
        try {
            POOL.evictAll();
        } catch (RuntimeException ignored) {}
    }

    static void trimMemory(int level) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, DnsEntry> e : dnsCache.entrySet()) {
            DnsEntry v = e.getValue();
            if (v == null || v.expiresAtMs <= now) dnsCache.remove(e.getKey());
        }
        if (level >= 10) {
            catalogCache.clear();
        }
        if (level >= 15 || level >= 40) {
            try {
                POOL.evictAll();
            } catch (RuntimeException ignored) {}
        }
    }

    private static void putDnsCache(String host, List<InetAddress> addrs, long expiresAtMs) {
        if (host == null || addrs == null || addrs.isEmpty()) return;
        if (dnsCache.size() > 256) dnsCache.clear();
        dnsCache.put(host, new DnsEntry(addrs, expiresAtMs));
    }

    private static void markPoisoned(String host) {
        if (host == null || host.isEmpty()) return;
        if (poisonedHosts.size() > 256) poisonedHosts.clear();
        poisonedHosts.add(host);
    }

    private static boolean isNumericIpLiteral(String host) {
        if (host.indexOf(':') >= 0) return true;
        return host.matches("^[0-9]{1,3}(\\.[0-9]{1,3}){3}$");
    }

    private static List<InetAddress> filterValidPublicAddresses(List<InetAddress> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<InetAddress> out = new ArrayList<>(list.size());
        for (InetAddress a : list) {
            if (a == null) continue;
            // Filter carrier DNS sinkholes (0.0.0.0, 127.0.0.1, 10.x.x.x, 192.168.x.x, 169.254.x.x)
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) {
                continue;
            }
            out.add(a);
        }
        return out;
    }

    private static List<InetAddress> resolveViaDoh(String host) {
        String encoded = Uri.encode(host);
        String[] endpoints = new String[] {
            "https://1.1.1.1/dns-query?name=" + encoded + "&type=A",
            "https://8.8.8.8/resolve?name=" + encoded + "&type=A",
            "https://1.0.0.1/dns-query?name=" + encoded + "&type=A",
            "https://9.9.9.9:5053/dns-query?name=" + encoded + "&type=A",
            "https://1.1.1.1/dns-query?name=" + encoded + "&type=AAAA",
            "https://8.8.8.8/resolve?name=" + encoded + "&type=AAAA"
        };
        for (String url : endpoints) {
            Request req = new Request.Builder()
                    .url(url)
                    .header("Accept", "application/dns-json")
                    .header("User-Agent", "Kuro.tv/1.5.4")
                    .get()
                    .build();
            try (Response resp = DOH_CLIENT.newCall(req).execute()) {
                if (resp.code() != 200 || resp.body() == null) continue;
                byte[] bytes = readCapped(resp.body(), 65536);
                JSONObject json = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                JSONArray answers = json.optJSONArray("Answer");
                if (answers == null || answers.length() == 0) continue;
                List<InetAddress> resolved = new ArrayList<>();
                for (int i = 0; i < answers.length() && resolved.size() < 8; i++) {
                    JSONObject row = answers.optJSONObject(i);
                    if (row == null) continue;
                    int type = row.optInt("type", 0);
                    if (type != 1 && type != 28) continue;
                    String data = row.optString("data", "").trim();
                    if (data.isEmpty() || (!isNumericIpLiteral(data))) continue;
                    try {
                        InetAddress parsed = InetAddress.getByName(data);
                        InetAddress bound = InetAddress.getByAddress(host, parsed.getAddress());
                        if (!bound.isAnyLocalAddress() && !bound.isLoopbackAddress() && !bound.isSiteLocalAddress()) {
                            resolved.add(bound);
                        }
                    } catch (Exception ignored) {}
                }
                if (!resolved.isEmpty()) return resolved;
            } catch (Exception ignored) {}
        }
        return Collections.emptyList();
    }

    private static byte[] readCapped(ResponseBody body, int maxBytes) throws IOException {
        try (InputStream in = body.byteStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                if (n > maxBytes - out.size()) throw new IOException("Response too large");
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static boolean shouldRetryWithDoh(IOException e, String host) {
        if (!"auto".equals(dohMode) || host == null || poisonedHosts.contains(host)) return false;
        return (e instanceof UnknownHostException)
                || (e instanceof ConnectException)
                || (e instanceof SocketTimeoutException)
                || (e instanceof SSLException);
    }

    static SourceResult fetchSource(URI u, Map<String, String> headers, int maxBytes) throws Exception {
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
        for (int attempt = 0; attempt < 2; attempt++) {
            Request.Builder rb = new Request.Builder().url(u.toURL()).get();
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) rb.header(e.getKey(), e.getValue());
                }
            }
            try (Response resp = CLIENT.newCall(rb.build()).execute()) {
                int code = resp.code();
                if (code != 200) throw new Exception("Source returned HTTP " + code + " (no verification bypass)");
                ResponseBody body = resp.body();
                byte[] bytes = body == null ? new byte[0] : readCapped(body, maxBytes);
                return new SourceResult(bytes, resp.header("Content-Type", ""));
            } catch (IOException ioe) {
                if (attempt == 0 && shouldRetryWithDoh(ioe, host)) {
                    markPoisoned(host);
                    dnsCache.remove(host);
                    continue;
                }
                throw ioe;
            }
        }
        throw new IOException("Network error");
    }

    static byte[] fetchPoster(URI u, int maxBytes) throws IOException {
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
        for (int attempt = 0; attempt < 2; attempt++) {
            Request req = new Request.Builder()
                    .url(u.toURL())
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "image/webp,image/avif,image/*,*/*;q=0.8")
                    .get()
                    .build();
            try (Response resp = CLIENT.newCall(req).execute()) {
                if (resp.code() != 200 || resp.body() == null) return null;
                return readCapped(resp.body(), maxBytes);
            } catch (IOException ioe) {
                if (attempt == 0 && shouldRetryWithDoh(ioe, host)) {
                    markPoisoned(host);
                    dnsCache.remove(host);
                    continue;
                }
                throw ioe;
            }
        }
        return null;
    }

    static String postAnilistCatalog(String payloadJson, int maxBytes) throws Exception {
        long now = System.currentTimeMillis();
        CatalogCacheEntry hit = catalogCache.get(payloadJson);
        if (hit != null && hit.expiresAtMs > now) {
            return hit.body;
        }
        byte[] raw = payloadJson.getBytes(StandardCharsets.UTF_8);
        String host = "graphql.anilist.co";
        for (int attempt = 0; attempt < 2; attempt++) {
            Request req = new Request.Builder()
                    .url("https://graphql.anilist.co")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Kuro.tv/5.73 (public catalogue)")
                    .post(RequestBody.create(JSON_MEDIA, raw))
                    .build();
            try (Response resp = CLIENT.newCall(req).execute()) {
                if (resp.code() != 200 || resp.body() == null) throw new IOException("Catalogue unavailable");
                String result = new String(readCapped(resp.body(), maxBytes), StandardCharsets.UTF_8);
                JSONObject parsed = new JSONObject(result);
                if (parsed.has("errors") || parsed.optJSONObject("data") == null) {
                    throw new IOException("Catalogue unavailable");
                }
                catalogCache.put(payloadJson, new CatalogCacheEntry(result, System.currentTimeMillis() + CATALOG_TTL_MS));
                return result;
            } catch (IOException ioe) {
                if (attempt == 0 && shouldRetryWithDoh(ioe, host)) {
                    markPoisoned(host);
                    dnsCache.remove(host);
                    continue;
                }
                throw ioe;
            }
        }
        throw new IOException("Catalogue unavailable");
    }

    private static final MediaType FORM_MEDIA = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    private static final OkHttpClient ACCOUNT_CLIENT = CLIENT.newBuilder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    private static final OkHttpClient PREFLIGHT_CLIENT = CLIENT.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    private static volatile String cachedDiscordRedirect154 = null;

    static JSONObject accountRequest(String method, URI u, Map<String, String> headers, String body) throws Exception {
        String host = u.getHost();
        if (!"https".equals(u.getScheme()) || host == null || u.getUserInfo() != null || u.getPort() != -1
                || !(host.equals("myanimelist.net") || host.equals("api.myanimelist.net")
                || host.equals("discord.com") || host.equals("graphql.anilist.co"))) {
            throw new AccountError("Blocked an unsafe account request.");
        }
        if (!("GET".equals(method) || "POST".equals(method) || "PUT".equals(method))) {
            throw new AccountError("Blocked an unsupported account request.");
        }
        if ("PUT".equals(method) && !("api.myanimelist.net".equals(host)
                && u.getPath() != null && u.getPath().matches("/v2/anime/[0-9]{1,10}/my_list_status"))) {
            throw new AccountError("Blocked an unsafe tracker update.");
        }
        byte[] rawBody = null;
        MediaType bodyType = JSON_MEDIA;
        if (body != null) {
            rawBody = body.getBytes(StandardCharsets.UTF_8);
            if (rawBody.length > 30000) throw new AccountError("Request too large.");
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    if (e.getKey() != null && "content-type".equalsIgnoreCase(e.getKey()) && e.getValue() != null) {
                        MediaType parsed = MediaType.parse(e.getValue());
                        if (parsed != null) bodyType = parsed;
                        break;
                    }
                }
            }
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        for (int attempt = 0; attempt < 2; attempt++) {
            RequestBody reqBody = null;
            if (rawBody != null) {
                reqBody = RequestBody.create(bodyType, rawBody);
            } else if ("POST".equals(method) || "PUT".equals(method)) {
                reqBody = RequestBody.create((MediaType) null, new byte[0]);
            }
            Request.Builder rb = new Request.Builder()
                    .url(u.toURL())
                    .method(method, reqBody)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Kuro.tv/1.5.4 (Android)");
            if (headers != null) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    if (h.getKey() != null && h.getValue() != null) rb.header(h.getKey(), h.getValue());
                }
            }
            try (Response resp = ACCOUNT_CLIENT.newCall(rb.build()).execute()) {
                int status = resp.code();
                if (status < 200 || status >= 300) {
                    if (status == 429) {
                        long seconds = 60;
                        try {
                            seconds = Math.max(1, Math.min(86400, Long.parseLong(resp.header("Retry-After", "60"))));
                        } catch (Exception ignored) {}
                        throw new AccountError("Provider rate-limited the request (HTTP 429); retry after " + seconds + " seconds.");
                    }
                    if (status == 403) throw new AccountError("Provider blocked the request (HTTP 403). Check access before retrying manually.");
                    if (status == 401) throw new AccountError("Sign-in was rejected or the session expired (HTTP 401). Check your setup or sign in again.");
                    if (status >= 300 && status < 400) throw new AccountError("The provider sent an unexpected redirect. No credentials were forwarded.");
                    throw new AccountError("The provider rejected the request (HTTP " + status + "). Check the account details and provider settings.");
                }
                ResponseBody respBody = resp.body();
                if (respBody == null) return new JSONObject();
                byte[] bytes;
                try {
                    bytes = readCapped(respBody, 3000000);
                } catch (IOException tooLarge) {
                    throw new AccountError("Provider response too large.");
                }
                String text = new String(bytes, StandardCharsets.UTF_8).trim();
                return text.isEmpty() ? new JSONObject() : new JSONObject(text);
            } catch (IOException ioe) {
                if (attempt == 0 && shouldRetryWithDoh(ioe, lowerHost)) {
                    markPoisoned(lowerHost);
                    dnsCache.remove(lowerHost);
                    continue;
                }
                throw new AccountError("Could not reach the provider. Check your connection and setup.");
            }
        }
        throw new AccountError("Could not reach the provider. Check your connection and setup.");
    }

    static String resolveDiscordRedirect154(String clientId) {
        if (!"1569077670074122250".equals(clientId)) {
            return AccountOAuth.DISCORD_REDIRECT;
        }
        String hit = cachedDiscordRedirect154;
        if (hit != null) return hit;
        String[] candidates = new String[] {
            KuroDiscordOAuth564.REDIRECT,
            AccountOAuth.DISCORD_REDIRECT
        };
        for (String cand : candidates) {
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    String form = "client_id=" + Uri.encode(clientId)
                            + "&grant_type=authorization_code"
                            + "&code=kuro_preflight_probe_154"
                            + "&redirect_uri=" + Uri.encode(cand)
                            + "&code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
                    Request req = new Request.Builder()
                            .url("https://discord.com/api/oauth2/token")
                            .header("Accept", "application/json")
                            .header("User-Agent", "Kuro.tv/1.5.4 (Android)")
                            .post(RequestBody.create(FORM_MEDIA, form.getBytes(StandardCharsets.UTF_8)))
                            .build();
                    try (Response resp = PREFLIGHT_CLIENT.newCall(req).execute()) {
                        ResponseBody rb = resp.body();
                        String txt = rb == null ? "" : new String(readCapped(rb, 16384), StandardCharsets.UTF_8);
                        if (txt.contains("invalid_grant")) {
                            cachedDiscordRedirect154 = cand;
                            return cand;
                        }
                        break;
                    }
                } catch (IOException ioe) {
                    if (attempt == 0 && shouldRetryWithDoh(ioe, "discord.com")) {
                        markPoisoned("discord.com");
                        dnsCache.remove("discord.com");
                        continue;
                    }
                    break;
                } catch (Exception ignored) {
                    break;
                }
            }
        }
        return KuroDiscordOAuth564.REDIRECT;
    }

    private static boolean validBrowserPackage154(String pkg, String selfPkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        if (pkg.equals(selfPkg) || "android".equals(pkg) || pkg.startsWith("com.android.internal")) return false;
        if ("com.discord".equals(pkg) || pkg.startsWith("com.discord.")) return false;
        return true;
    }

    static boolean openInBrowser154(Activity a, String target) {
        if (a == null || target == null || target.isEmpty()) return false;
        try {
            Uri uri = Uri.parse(target);
            Intent browser = new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE);
            PackageManager pm = a.getPackageManager();
            String selfPkg = a.getPackageName();
            String chosenPkg = null;
            if (pm != null) {
                Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org/")).addCategory(Intent.CATEGORY_BROWSABLE);
                ResolveInfo def = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
                if (def != null && def.activityInfo != null && validBrowserPackage154(def.activityInfo.packageName, selfPkg)) {
                    chosenPkg = def.activityInfo.packageName;
                }
                if (chosenPkg == null) {
                    List<ResolveInfo> list = pm.queryIntentActivities(probe, PackageManager.MATCH_ALL);
                    if (list == null || list.isEmpty()) list = pm.queryIntentActivities(probe, 0);
                    if (list != null) {
                        String[] preferred = new String[] {
                            "com.android.chrome", "com.brave.browser", "com.sec.android.app.sbrowser",
                            "org.mozilla.firefox", "com.microsoft.emmx", "com.opera.browser",
                            "com.vivaldi.browser", "com.kiwibrowser.browser"
                        };
                        for (String pref : preferred) {
                            for (ResolveInfo ri : list) {
                                if (ri != null && ri.activityInfo != null && pref.equals(ri.activityInfo.packageName)) {
                                    chosenPkg = pref;
                                    break;
                                }
                            }
                            if (chosenPkg != null) break;
                        }
                        if (chosenPkg == null) {
                            for (ResolveInfo ri : list) {
                                if (ri != null && ri.activityInfo != null && validBrowserPackage154(ri.activityInfo.packageName, selfPkg)) {
                                    chosenPkg = ri.activityInfo.packageName;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (chosenPkg != null) {
                browser.setPackage(chosenPkg);
                try {
                    a.startActivity(browser);
                    return true;
                } catch (Exception ignored) {
                    browser.setPackage(null);
                }
            }
            a.startActivity(browser);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
"""


def patch_perf_and_compat_154(root: pathlib.Path) -> None:
    # 1. Write KuroFastNet154.java and TV Leanback banner vector drawable
    fast_net_path = root / "src/app/lovelycrxshx/kuro/KuroFastNet154.java"
    fast_net_path.write_text(KURO_FAST_NET_154_JAVA, encoding="utf-8")

    banner_path = root / "res/drawable/kuro_tv_banner.xml"
    banner_path.parent.mkdir(parents=True, exist_ok=True)
    banner_path.write_text(KURO_TV_BANNER_XML, encoding="utf-8")

    # 2. AndroidManifest.xml: Android TV / Fire TV Leanback + optional touchscreen + foldable offline configChanges
    manifest_path = root / "AndroidManifest.xml"
    m_text = manifest_path.read_text(encoding="utf-8")
    m_text = patch_once(
        m_text,
        "android.software.leanback",
        '<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/>',
        '<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/>'
        '<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>'
        '<uses-feature android:name="android.software.leanback" android:required="false"/>',
        "AndroidManifest optional touchscreen & leanback features",
    )
    m_text = patch_once(
        m_text,
        "@drawable/kuro_tv_banner",
        'android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher">',
        'android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher" android:banner="@drawable/kuro_tv_banner">',
        "AndroidManifest TV banner",
    )
    m_text = patch_once(
        m_text,
        "LEANBACK_LAUNCHER",
        '<intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>',
        '<intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/><category android:name="android.intent.category.LEANBACK_LAUNCHER"/></intent-filter>',
        "AndroidManifest LEANBACK_LAUNCHER category",
    )
    m_text = patch_once(
        m_text,
        'KuroOfflineActivity561" android:exported="false" android:resizeableActivity="true" android:supportsPictureInPicture="true" android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout|uiMode|density|layoutDirection"',
        'KuroOfflineActivity561" android:exported="false" android:resizeableActivity="true" android:supportsPictureInPicture="true" android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"',
        'KuroOfflineActivity561" android:exported="false" android:resizeableActivity="true" android:supportsPictureInPicture="true" android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout|uiMode|density|layoutDirection"',
        "AndroidManifest KuroOfflineActivity561 foldable/TV configChanges",
    )
    manifest_path.write_text(m_text, encoding="utf-8")

    # 3. KuroHttp561.java: share DoH resolver & connection pool for native HLS playback
    http_path = root / "src/app/lovelycrxshx/kuro/KuroHttp561.java"
    http_text = http_path.read_text(encoding="utf-8")
    http_text = patch_once(
        http_text,
        "KuroFastNet154.DNS",
        "private static final OkHttpClient CLIENT=new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).addInterceptor(new SameProxy()).build();",
        "private static final OkHttpClient CLIENT=new OkHttpClient.Builder().dns(KuroFastNet154.DNS).connectionPool(KuroFastNet154.POOL).followRedirects(false).followSslRedirects(false).connectTimeout(12,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).addInterceptor(new SameProxy()).build();",
        "KuroHttp561 DoH & connection pool",
    )
    http_path.write_text(http_text, encoding="utf-8")

    # 4. KuroCatalog573.java: pooled HTTP/2 + 90s LRU cache + DoH fallback
    cat_path = root / "src/app/lovelycrxshx/kuro/KuroCatalog573.java"
    cat_text = cat_path.read_text(encoding="utf-8")
    old_lookup = (
        ' public static String lookup(String action,String input) throws Exception {\n'
        '  JSONObject data=request(action,input);HttpURLConnection c=(HttpURLConnection)new URL("https://graphql.anilist.co").openConnection();\n'
        '  c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","Kuro.tv/5.73 (public catalogue)");\n'
        '  try{try(OutputStream out=c.getOutputStream()){out.write(data.toString().getBytes(StandardCharsets.UTF_8));}if(c.getResponseCode()!=200)throw new IOException("Catalogue unavailable");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>700000)throw new IOException("Response too large");out.write(b,0,n);}String result=out.toString("UTF-8");JSONObject parsed=new JSONObject(result);if(parsed.has("errors")||parsed.optJSONObject("data")==null)throw new IOException("Catalogue unavailable");return result;}}finally{c.disconnect();}\n'
        ' }'
    )
    new_lookup = (
        ' public static String lookup(String action,String input) throws Exception {\n'
        '  JSONObject data=request(action,input);\n'
        '  return KuroFastNet154.postAnilistCatalog(data.toString(),700000);\n'
        ' }'
    )
    cat_text = patch_once(
        cat_text,
        "KuroFastNet154.postAnilistCatalog",
        old_lookup,
        new_lookup,
        "KuroCatalog573 pooled HTTP/2 + DoH lookup",
    )
    cat_path.write_text(cat_text, encoding="utf-8")

    # 5. KuroNativePlayer561.java & KuroOfflineActivity561.java: automatic hardware -> software codec fallback
    np_path = root / "src/app/lovelycrxshx/kuro/KuroNativePlayer561.java"
    np_text = np_path.read_text(encoding="utf-8")
    np_text = patch_once(
        np_text,
        "setEnableDecoderFallback(true)",
        "engine=new ExoPlayer.Builder(activity).setMediaSourceFactory(media)",
        "engine=new ExoPlayer.Builder(activity).setRenderersFactory(new androidx.media3.exoplayer.DefaultRenderersFactory(activity).setEnableDecoderFallback(true)).setMediaSourceFactory(media)",
        "KuroNativePlayer561 decoder fallback",
    )
    np_path.write_text(np_text, encoding="utf-8")

    oa_path = root / "src/app/lovelycrxshx/kuro/KuroOfflineActivity561.java"
    oa_text = oa_path.read_text(encoding="utf-8")
    oa_text = patch_once(
        oa_text,
        "setEnableDecoderFallback(true)",
        "engine=new ExoPlayer.Builder(this).setMediaSourceFactory(factory)",
        "engine=new ExoPlayer.Builder(this).setRenderersFactory(new androidx.media3.exoplayer.DefaultRenderersFactory(this).setEnableDecoderFallback(true)).setMediaSourceFactory(factory)",
        "KuroOfflineActivity561 decoder fallback",
    )
    oa_path.write_text(oa_text, encoding="utf-8")

    # 6. MainActivity.java: adaptive multi-core pool, memory trim resilience, WebView crash shield, LruCache posters, DoH & async Perf bridge
    main_path = root / "src/app/lovelycrxshx/kuro/MainActivity.java"
    text = main_path.read_text(encoding="utf-8")

    text = patch_once(
        text,
        "availableProcessors()*2",
        "private final ExecutorService pool=Executors.newFixedThreadPool(3);",
        "private final ExecutorService pool=Executors.newFixedThreadPool(Math.max(6,Math.min(10,Runtime.getRuntime().availableProcessors()*2)));",
        "MainActivity adaptive multi-core worker pool",
    )

    text = patch_once(
        text,
        "onTrimMemory(int level)",
        "@Override protected void onDestroy(){",
        "@Override public void onTrimMemory(int level){super.onTrimMemory(level);KuroFastNet154.trimMemory(level);if(level>=15||level>=60)responses.clear();if(posterMemCache154!=null){if(level>=15||level>=60)posterMemCache154.evictAll();else if(level>=10||level>=20)posterMemCache154.trimToSize(Math.max(1024,posterMemCache154.maxSize()/2));}}@Override public void onLowMemory(){super.onLowMemory();KuroFastNet154.trimMemory(80);responses.clear();if(posterMemCache154!=null)posterMemCache154.evictAll();}@Override protected void onDestroy(){",
        "MainActivity onTrimMemory and onLowMemory resilience",
    )

    text = patch_once(
        text,
        "onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){if(v!=ui)",
        "@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}",
        "@Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){if(v!=ui)return true;try{if(root!=null)root.removeView(v);v.destroy();}catch(RuntimeException ignored){}ui=null;uiReady=false;main.post(()->{if(!isFinishing()&&!isDestroyed())recreate();});return true;}@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}",
        "MainActivity ui WebView crash shield",
    )

    text = patch_once(
        text,
        "initPerf154();applyWindowPreferences560();",
        'applyWindowPreferences560();ui.loadUrl("https://kuro.local/index.html");',
        'initPerf154();applyWindowPreferences560();ui.loadUrl("https://kuro.local/index.html");',
        "MainActivity onCreate initPerf154",
    )

    perf_helpers = """ private android.util.LruCache<String,Bitmap> posterMemCache154;
 private Boolean cachedIsTv154;
 private Boolean cachedLowRam154;
 private Integer cachedRamMb154;
 private volatile long cachedTempBytes154=-1L;
 private volatile boolean cacheScanRunning154=false;
 private android.util.LruCache<String,Bitmap> posterCache154(){
  if(posterMemCache154!=null)return posterMemCache154;
  int maxKb=(int)(Runtime.getRuntime().maxMemory()/1024L);
  int capKb=Math.max(4096,Math.min(12288,maxKb/12));
  posterMemCache154=new android.util.LruCache<String,Bitmap>(capKb){
   @Override protected int sizeOf(String key,Bitmap value){return Math.max(1,value.getAllocationByteCount()/1024);}
  };
  return posterMemCache154;
 }
 private SharedPreferences perfPrefs154(){return getSharedPreferences("kuro-perf-154",MODE_PRIVATE);}
 private boolean isTelevisionDevice154(){
  if(cachedIsTv154!=null)return cachedIsTv154;
  boolean tv=false;
  try{
   UiModeManager um=(UiModeManager)getSystemService(UI_MODE_SERVICE);
   if(um!=null&&um.getCurrentModeType()==android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)tv=true;
   else{
    android.content.pm.PackageManager pm=getPackageManager();
    tv=pm!=null&&(pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)||pm.hasSystemFeature("amazon.hardware.fire_tv"));
   }
  }catch(RuntimeException ignored){}
  cachedIsTv154=tv;
  return tv;
 }
 private int totalRamMb154(){
  if(cachedRamMb154!=null)return cachedRamMb154;
  int mb=4096;
  try{
   ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
   if(am!=null){
    ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();
    am.getMemoryInfo(mi);
    if(mi.totalMem>0)mb=(int)(mi.totalMem/(1024L*1024L));
   }
  }catch(RuntimeException ignored){}
  cachedRamMb154=mb;
  return mb;
 }
 private boolean isLowRamDevice154(){
  if(cachedLowRam154!=null)return cachedLowRam154;
  boolean low=false;
  try{
   ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
   if(am!=null&&am.isLowRamDevice())low=true;
  }catch(RuntimeException ignored){}
  if(!low)low=totalRamMb154()<=3900;
  cachedLowRam154=low;
  return low;
 }
 private void initPerf154(){
  SharedPreferences sp=perfPrefs154();
  KuroFastNet154.setDohMode(sp.getString("dohMode","auto"));
  refreshTempBytesAsync154();
 }
 private long folderBytesBounded154(File f,int depth,int[] budget){
  if(f==null||depth>6||budget[0]<=0||!f.exists())return 0L;
  budget[0]--;
  if(f.isFile())return Math.max(0L,f.length());
  File[] kids=f.listFiles();if(kids==null)return 0L;
  long sum=0L;
  for(int i=0;i<kids.length&&budget[0]>0;i++)sum+=folderBytesBounded154(kids[i],depth+1,budget);
  return sum;
 }
 private void refreshTempBytesAsync154(){
  if(cacheScanRunning154)return;
  cacheScanRunning154=true;
  final File dir=getCacheDir();
  try{
   pool.execute(()->{
    try{
     cachedTempBytes154=folderBytesBounded154(dir,0,new int[]{2500});
    }catch(RuntimeException ignored){
     if(cachedTempBytes154<0L)cachedTempBytes154=0L;
    }finally{
     cacheScanRunning154=false;
    }
   });
  }catch(RuntimeException ignored){cacheScanRunning154=false;}
 }
 private void deleteCacheChildren154(File dir,int depth){
  if(dir==null||depth>6||!dir.isDirectory())return;
  File[] kids=dir.listFiles();if(kids==null)return;
  for(File k:kids){
   if(k.isDirectory())deleteCacheChildren154(k,depth+1);
   try{k.delete();}catch(RuntimeException ignored){}
  }
 }
 private String buildPerfStatusJson154(){
  SharedPreferences sp=perfPrefs154();
  long bytes=cachedTempBytes154;
  if(bytes<0L)refreshTempBytesAsync154();
  double cacheMb=bytes<0L?-1.0:Math.round((bytes/(1024.0*1024.0))*10.0)/10.0;
  try{
   return new JSONObject()
    .put("dohMode",sp.getString("dohMode","auto"))
    .put("isTv",isTelevisionDevice154())
    .put("lowRam",isLowRamDevice154())
    .put("ramMb",totalRamMb154())
    .put("cacheMb",cacheMb)
    .toString();
  }catch(Exception e){return "{}";}
 }
"""

    text = patch_once(
        text,
        "private android.util.LruCache<String,Bitmap> posterMemCache154;",
        " private androidx.core.view.WindowInsetsCompat lastWindowInsets154;",
        perf_helpers + " private androidx.core.view.WindowInsetsCompat lastWindowInsets154;",
        "MainActivity performance & compatibility helpers",
    )

    text = patch_once(
        text,
        'isTelevisionDevice154()?"desktop":"mobile"',
        'boolean desktopUi154="desktop".equals(p.getString("siteView","mobile"));',
        'boolean desktopUi154="desktop".equals(p.getString("siteView",isTelevisionDevice154()?"desktop":"mobile"));',
        "MainActivity applyWindowPreferences560 TV auto-desktop",
    )

    old_bridge_get = (
        '     boolean miruro="www.miruro.cx".equals(u.getHost()),kitsuneEpisodes="kitsunee.moe".equals(u.getHost())&&u.getPath()!=null&&u.getPath().matches("/api/episodes/anikoto/[0-9]{1,8}");int maxBytes=kitsuneEpisodes?6500000:2500000;c=(HttpURLConnection)u.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent",SOURCE_UA);c.setRequestProperty("Accept","text/html,application/json,*/*");c.setRequestProperty("X-Requested-With","XMLHttpRequest");\n'
        '     if("aniwaves.tv".equals(u.getHost())||"anikoto.cz".equals(u.getHost())) c.setRequestProperty("Referer","https://"+u.getHost()+"/");\n'
        '     if(miruro){c.setRequestProperty("Origin",MIRURO_ORIGIN);c.setRequestProperty("Referer",MIRURO_ORIGIN+"/");String cookie=android.webkit.CookieManager.getInstance().getCookie(MIRURO_ORIGIN+"/");if(cookie!=null&&!cookie.isEmpty()&&cookie.length()<=8192)c.setRequestProperty("Cookie",cookie);}\n'
        '     int code=c.getResponseCode();if(code!=200)throw new Exception("Source returned HTTP "+code+" (no verification bypass)");\n'
        '     byte[] bytes;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(n>maxBytes-out.size())throw new Exception("Response too large");out.write(b,0,n);}bytes=out.toByteArray();}\n'
        '     String type=c.getContentType();if(miruro&&u.getPath()!=null&&u.getPath().startsWith("/api/v1/anime")&&type!=null&&type.toLowerCase(Locale.ROOT).contains("application/octet-stream"))bytes=MiruroCatalog.decode(bytes,2500000);'
    )
    new_bridge_get = (
        '     boolean miruro="www.miruro.cx".equals(u.getHost()),kitsuneEpisodes="kitsunee.moe".equals(u.getHost())&&u.getPath()!=null&&u.getPath().matches("/api/episodes/anikoto/[0-9]{1,8}");int maxBytes=kitsuneEpisodes?6500000:2500000;\n'
        '     Map<String,String> reqHeaders154=new LinkedHashMap<>();reqHeaders154.put("User-Agent",SOURCE_UA);reqHeaders154.put("Accept","text/html,application/json,*/*");reqHeaders154.put("X-Requested-With","XMLHttpRequest");\n'
        '     if("aniwaves.tv".equals(u.getHost())||"anikoto.cz".equals(u.getHost()))reqHeaders154.put("Referer","https://"+u.getHost()+"/");\n'
        '     if(miruro){reqHeaders154.put("Origin",MIRURO_ORIGIN);reqHeaders154.put("Referer",MIRURO_ORIGIN+"/");String cookie=android.webkit.CookieManager.getInstance().getCookie(MIRURO_ORIGIN+"/");if(cookie!=null&&!cookie.isEmpty()&&cookie.length()<=8192)reqHeaders154.put("Cookie",cookie);}\n'
        '     KuroFastNet154.SourceResult net154=KuroFastNet154.fetchSource(u,reqHeaders154,maxBytes);byte[] bytes=net154.bytes;String type=net154.contentType;\n'
        '     if(miruro&&u.getPath()!=null&&u.getPath().startsWith("/api/v1/anime")&&type!=null&&type.toLowerCase(Locale.ROOT).contains("application/octet-stream"))bytes=MiruroCatalog.decode(bytes,2500000);'
    )
    text = patch_once(
        text,
        "KuroFastNet154.fetchSource(u,reqHeaders154,maxBytes)",
        old_bridge_get,
        new_bridge_get,
        "MainActivity Bridge.get pooled HTTP/2 + DoH",
    )

    perf_bridge_methods = (
        '  @JavascriptInterface public String siteView154(){return settings560().getString("siteView",isTelevisionDevice154()?"desktop":"mobile");}\n'
        '  @JavascriptInterface public String perfGet154(){return buildPerfStatusJson154();}\n'
        '  @JavascriptInterface public String perfSet154(String raw){\n'
        '   if(raw==null||raw.length()>2000)return buildPerfStatusJson154();\n'
        '   try{\n'
        '    JSONObject j=new JSONObject(raw);SharedPreferences.Editor ed=perfPrefs154().edit();\n'
        '    if(j.has("dohMode")){String d=j.optString("dohMode","auto");if("auto".equals(d)||"always".equals(d)||"off".equals(d)){ed.putString("dohMode",d);KuroFastNet154.setDohMode(d);}}\n'
        '    ed.apply();\n'
        '   }catch(Exception ignored){}\n'
        '   return buildPerfStatusJson154();\n'
        '  }\n'
        '  @JavascriptInterface public String clearTempCache154(){\n'
        '   long before=Math.max(0L,cachedTempBytes154);\n'
        '   cachedTempBytes154=0L;\n'
        '   responses.clear();KuroFastNet154.clearCaches();\n'
        '   try{posterCache154().evictAll();}catch(RuntimeException ignored){}\n'
        '   final File dir=getCacheDir();\n'
        '   try{pool.execute(()->{deleteCacheChildren154(dir,0);cachedTempBytes154=folderBytesBounded154(dir,0,new int[]{2500});});}catch(RuntimeException ignored){}\n'
        '   main.post(()->{if(ui!=null&&!isFinishing()&&!isDestroyed())try{ui.clearCache(true);}catch(RuntimeException ignored){}});\n'
        '   double freedMb=Math.max(0.0,Math.round((before/(1024.0*1024.0))*10.0)/10.0);\n'
        '   try{return new JSONObject().put("freedMb",freedMb).put("cacheMb",0.0).toString();}catch(Exception e){return "{}";}\n'
        '  }'
    )
    text = patch_once(
        text,
        "perfGet154()",
        '  @JavascriptInterface public String siteView154(){return settings560().getString("siteView","mobile");}',
        perf_bridge_methods,
        "MainActivity Bridge perfGet154/perfSet154/clearTempCache154",
    )

    text = patch_once(
        text,
        "onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){if(v!=player)",
        "@Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){super.onReceivedError(v,request,error);",
        "@Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){if(v!=player)return true;lastPlayerIssue573=\"The embedded player ran low on memory; Kuro recovered without crashing.\";try{if(watchPlayerBox!=null)watchPlayerBox.removeView(v);v.destroy();}catch(RuntimeException ignored){}player=null;main.post(()->{if(!isFinishing()&&!isDestroyed()&&playerLayer!=null){Toast.makeText(MainActivity.this,\"Player recovered from low memory. Try another server.\",Toast.LENGTH_LONG).show();closePlayer();}});return true;}@Override public void onReceivedError(WebView v,WebResourceRequest request,WebResourceError error){super.onReceivedError(v,request,error);",
        "MainActivity player WebView crash shield",
    )

    old_poster = (
        ' void loadPoster(ImageView target,String address){pool.execute(()->{try{URI u=new URI(address);String h=u.getHost();if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||!("hianime.ms".equals(h)||"hianime.dk".equals(h)||"cdn.hianime.dk".equals(h)||"s4.anilist.co".equals(h)||"cdn.noitatnemucod.net".equals(h)))return;HttpURLConnection c=(HttpURLConnection)u.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","Mozilla/5.0");if(c.getResponseCode()!=200){c.disconnect();return;}byte[] bytes;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>2000000){c.disconnect();return;}out.write(buf,0,n);}bytes=out.toByteArray();}c.disconnect();android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>6000||bounds.outHeight>6000)return;android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inSampleSize=Math.max(1,Math.max(bounds.outWidth/300,bounds.outHeight/450));android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(bitmap!=null)main.post(()->{if(target.isAttachedToWindow())target.setImageBitmap(bitmap);});}catch(Exception ignored){}});}'
    )
    new_poster = (
        ' void loadPoster(ImageView target,String address){'
        'if(target==null||address==null||address.isEmpty())return;'
        'Bitmap hit154=posterCache154().get(address);if(hit154!=null&&!hit154.isRecycled()){target.setImageBitmap(hit154);return;}'
        'target.setTag(address);'
        'pool.execute(()->{try{URI u=new URI(address);String h=u.getHost();if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||!("hianime.ms".equals(h)||"hianime.dk".equals(h)||"cdn.hianime.dk".equals(h)||"s4.anilist.co".equals(h)||"cdn.noitatnemucod.net".equals(h)))return;'
        'byte[] bytes=KuroFastNet154.fetchPoster(u,2000000);if(bytes==null||bytes.length==0)return;'
        'android.graphics.BitmapFactory.Options bounds=new android.graphics.BitmapFactory.Options();bounds.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>6000||bounds.outHeight>6000)return;'
        'android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options();opts.inSampleSize=Math.max(1,Math.max(bounds.outWidth/300,bounds.outHeight/450));opts.inPreferredConfig=Bitmap.Config.RGB_565;'
        'android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(bitmap!=null){posterCache154().put(address,bitmap);main.post(()->{if(target.isAttachedToWindow()&&address.equals(target.getTag()))target.setImageBitmap(bitmap);});}'
        '}catch(Exception ignored){}});}'
    )
    text = patch_once(
        text,
        "hit154=posterCache154().get(address)",
        old_poster,
        new_poster,
        "MainActivity loadPoster LruCache + pooled HTTP/2",
    )

    main_path.write_text(text, encoding="utf-8")


PLAYER_ENDED_154_JS = """'use strict';
(()=>{
 if(window.__kuroEnded553)return;
 window.__kuroEnded553=true;
 let sent85=false,sentEnd=false,lastCheck154=0;
 const maxTimeMap=new WeakMap();
 function trackMax(v){
  const prev=maxTimeMap.get(v)||0;
  if(Number.isFinite(v.currentTime)&&v.currentTime>prev){
   maxTimeMap.set(v,v.currentTime);
   return v.currentTime;
  }
  return prev;
 }
 function checkVideo154(v,fromEndedEvent){
  if(!(v instanceof HTMLVideoElement))return;
  const dur=v.duration;
  if(!Number.isFinite(dur)||dur<45)return;
  const cur=Number.isFinite(v.currentTime)?v.currentTime:0;
  const peak=Math.max(cur,trackMax(v));
  if(!sent85&&(cur>=dur*0.85||peak>=dur*0.85)){
   sent85=true;
   try{window.KuroFinished553?.postMessage('progress85')}catch(_){}
  }
  if(!sentEnd){
   const genuinelyEnded=(fromEndedEvent||v.ended)&&(cur>=dur*0.80||peak>=dur*0.85);
   const atTailEnd=!v.seeking&&v.paused&&dur>=45&&cur>=dur-1.0&&peak>=dur*0.92;
   if(genuinelyEnded||atTailEnd){
    sentEnd=true;
    try{window.KuroFinished553?.postMessage('ended')}catch(_){}
   }
  }
 }
 document.addEventListener('ended',e=>checkVideo154(e.target,true),true);
 document.addEventListener('pause',e=>checkVideo154(e.target,false),true);
 document.addEventListener('timeupdate',e=>{
  if(e.target instanceof HTMLVideoElement)trackMax(e.target);
  const now=Date.now();
  if(now-lastCheck154<1500)return;
  lastCheck154=now;
  checkVideo154(e.target,false);
 },true);
})();
"""


def patch_accounts_and_sync_154(root: pathlib.Path) -> None:
    # 1. AccountHttp.java -> route through KuroFastNet154 (Encrypted DoH + HTTP/2 pool)
    ah_path = root / "src/app/lovelycrxshx/kuro/AccountHttp.java"
    ah_text = ah_path.read_text(encoding="utf-8")
    if "KuroFastNet154.accountRequest" not in ah_text:
        ah_new = """package app.lovelycrxshx.kuro;

import org.json.JSONObject;
import java.net.URI;
import java.util.Map;

interface AccountTransport {
 JSONObject request(String method,String url,Map<String,String> headers,String body) throws Exception;
}

/** Authenticated requests never follow redirects and never leave known HTTPS hosts. */
final class AccountHttp implements AccountTransport {
 public JSONObject request(String method,String address,Map<String,String> headers,String body) throws Exception {
  URI u=new URI(address);
  return KuroFastNet154.accountRequest(method,u,headers,body);
 }
}
"""
        ah_path.write_text(ah_new, encoding="utf-8")

    # 2. AccountOAuth.java -> support both Social SDK redirect and custom scheme redirect for Discord
    ao_path = root / "src/app/lovelycrxshx/kuro/AccountOAuth.java"
    ao_text = ao_path.read_text(encoding="utf-8")
    ao_text = patch_once(
        ao_text,
        "authorizeDiscord154",
        '  if("discord".equals(provider)){\n'
        '   if(!publicId(provider,id))throw new AccountError("Add your Discord application ID in Account setup first.");\n'
        '   return "https://discord.com/oauth2/authorize?"+form(map("response_type","code","client_id",id,"redirect_uri",DISCORD_REDIRECT,"scope","identify","state",state,"code_challenge",challenge(verifier),"code_challenge_method","S256"));\n'
        '  }',
        '  if("discord".equals(provider)){\n'
        '   return authorizeDiscord154(id,DISCORD_REDIRECT,state,verifier);\n'
        '  }',
        "AccountOAuth.authorize discord delegate",
    )
    ao_text = patch_once(
        ao_text,
        "static String authorizeDiscord154(",
        " static final class Callback {",
        ' static String authorizeDiscord154(String id,String redirect,String state,String verifier) throws Exception {\n'
        '  if(!publicId("discord",id))throw new AccountError("Add your Discord application ID in Account setup first.");\n'
        '  String redir=(redirect==null||redirect.isEmpty())?DISCORD_REDIRECT:redirect;\n'
        '  return "https://discord.com/oauth2/authorize?"+form(map("response_type","code","client_id",id,"redirect_uri",redir,"scope","identify","state",state,"code_challenge",challenge(verifier),"code_challenge_method","S256"));\n'
        ' }\n'
        " static final class Callback {",
        "AccountOAuth.authorizeDiscord154 helper",
    )
    ao_text = patch_once(
        ao_text,
        "KuroDiscordOAuth564.SCHEME.equalsIgnoreCase",
        '  if(!SCHEME.equals(u.getScheme())||!"oauth".equals(u.getHost())||u.getUserInfo()!=null||u.getPort()!=-1||u.getRawFragment()!=null)throw new AccountError("Invalid sign-in return link.");\n'
        '  String p=u.getPath(),kind;\n'
        '  if("/mal".equals(p))kind="mal";else if("/discord".equals(p))kind="discord";else throw new AccountError("Invalid sign-in return link.");',
        '  String p=u.getPath(),kind;\n'
        '  if(KuroDiscordOAuth564.SCHEME.equalsIgnoreCase(u.getScheme())&&u.getRawAuthority()==null&&"/authorize/callback".equals(p)&&u.getRawFragment()==null)kind="discord";\n'
        '  else if(SCHEME.equals(u.getScheme())&&"oauth".equals(u.getHost())&&u.getUserInfo()==null&&u.getPort()==-1&&u.getRawFragment()==null){if("/mal".equals(p))kind="mal";else if("/discord".equals(p))kind="discord";else throw new AccountError("Invalid sign-in return link.");}\n'
        '  else throw new AccountError("Invalid sign-in return link.");',
        "AccountOAuth.parseCallback dual Discord redirect",
    )
    ao_path.write_text(ao_text, encoding="utf-8")

    # 3. KuroAccounts.java -> auto-detect registered Discord redirect URI & preserve it across token exchange
    ka_path = root / "src/app/lovelycrxshx/kuro/KuroAccounts.java"
    ka_text = ka_path.read_text(encoding="utf-8")
    ka_text = patch_once(
        ka_text,
        "resolveDiscordRedirect154",
        '  JSONObject pending=pending(provider,id);\n'
        '  return AccountOAuth.authorize(provider,id,str(pending,"state"),str(pending,"verifier"));',
        '  JSONObject pending=pending(provider,id);\n'
        '  if("discord".equals(provider)){String redirect154=KuroFastNet154.resolveDiscordRedirect154(id);pending.put("redirect",redirect154);write("pending."+provider,pending);return AccountOAuth.authorizeDiscord154(id,redirect154,str(pending,"state"),str(pending,"verifier"));}\n'
        '  return AccountOAuth.authorize(provider,id,str(pending,"state"),str(pending,"verifier"));',
        "KuroAccounts.begin discord redirect resolution",
    )
    ka_text = patch_once(
        ka_text,
        'String redirect154=str(pending,"redirect");',
        '  }else if("discord".equals(provider))response=token("https://discord.com/api/oauth2/token",formHeaders(""),AccountOAuth.map("client_id",str(pending,"id"),"grant_type","authorization_code","code",cb.code,"redirect_uri",AccountOAuth.DISCORD_REDIRECT,"code_verifier",str(pending,"verifier")));',
        '  }else if("discord".equals(provider)){String redirect154=str(pending,"redirect");if(redirect154.isEmpty())redirect154=AccountOAuth.DISCORD_REDIRECT;response=token("https://discord.com/api/oauth2/token",formHeaders(""),AccountOAuth.map("client_id",str(pending,"id"),"grant_type","authorization_code","code",cb.code,"redirect_uri",redirect154,"code_verifier",str(pending,"verifier")));}',
        "KuroAccounts.callback discord redirect exchange",
    )
    ka_path.write_text(ka_text, encoding="utf-8")

    # 4. KuroDiscordOAuth564.java -> explicit web browser selection + pending state discriminator
    kd_path = root / "src/app/lovelycrxshx/kuro/KuroDiscordOAuth564.java"
    kd_text = kd_path.read_text(encoding="utf-8")
    kd_text = patch_once(
        kd_text,
        "KuroFastNet154.openInBrowser154",
        '   Intent browser=new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE);\n'
        '   // Prefer the user\'s default web browser, so they can sign in there as a different account.\n'
        '   Intent probe=new Intent(Intent.ACTION_VIEW,Uri.parse("https://example.org/")).addCategory(Intent.CATEGORY_BROWSABLE);\n'
        '   ResolveInfo ri=a.getPackageManager().resolveActivity(probe,PackageManager.MATCH_DEFAULT_ONLY);\n'
        '   if(ri!=null&&ri.activityInfo!=null&&!a.getPackageName().equals(ri.activityInfo.packageName)&&!"android".equals(ri.activityInfo.packageName))browser.setPackage(ri.activityInfo.packageName);\n'
        '   a.startActivity(browser);return true;',
        '   if(!KuroFastNet154.openInBrowser154(a,uri.toString())){KuroDiscordVault564.remove(a,"pending");return false;}\n'
        '   return true;',
        "KuroDiscordOAuth564.begin browser bypass",
    )
    kd_text = patch_once(
        kd_text,
        "hasMatchingPending154",
        " static Result consume(Context c,Intent intent){",
        ' static boolean hasMatchingPending154(Context c,Intent intent){\n'
        '  if(!isCallback(intent))return false;\n'
        '  try{\n'
        '   byte[] raw=KuroDiscordVault564.load(c,"pending");if(raw==null)return false;\n'
        '   String pending=new String(raw,StandardCharsets.UTF_8);Arrays.fill(raw,(byte)0);\n'
        '   JSONObject p=new JSONObject(pending);String expected=p.optString("state",""),received=one(intent.getData(),"state");\n'
        '   return !expected.isEmpty()&&!received.isEmpty()&&MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),received.getBytes(StandardCharsets.US_ASCII));\n'
        '  }catch(Exception ignored){return false;}\n'
        ' }\n'
        " static Result consume(Context c,Intent intent){",
        "KuroDiscordOAuth564.hasMatchingPending154",
    )
    kd_path.write_text(kd_text, encoding="utf-8")

    # 5. TrackerSync553.java -> fix episode jump progress + automatic AniList/MAL ID resolution
    ts_path = root / "src/app/lovelycrxshx/kuro/TrackerSync553.java"
    ts_text = ts_path.read_text(encoding="utf-8")
    ts_text = patch_once(
        ts_text,
        "season$1",
        '.replaceAll("(?i)\\\\b(?:dub|sub|dubbed|subbed)\\\\b","")',
        '.replaceAll("(?i)\\\\b(?:dub|sub|dubbed|subbed)\\\\b","")\n   .replaceAll("(?i)\\\\b([2-9])(?:nd|rd|th)\\\\s+season\\\\b","season$1")',
        "TrackerSync553.normalized season ordinal",
    )
    ts_text = patch_once(
        ts_text,
        "episodes.length()-1",
        " private static int nextProgress(int existing,BitSet episodes){int n=existing;while(n<MAX_EPISODE&&episodes.get(n+1))n++;return n;}",
        " private static int nextProgress(int existing,BitSet episodes){int highest=episodes.length()-1;return Math.max(existing,Math.max(0,Math.min(MAX_EPISODE,highest)));}",
        "TrackerSync553.nextProgress non-contiguous episode support",
    )
    auto_resolve_helper = """ private JSONObject autoResolveIds154(String provider,JSONObject request,String access){
  String rawTitle=request.optString("title","").trim();
  String qTitle=rawTitle.replaceAll("(?i)\\\\s*[\\\\(\\\\[]\\\\s*(?:dub|sub|dubbed|subbed)\\\\s*[\\\\)\\\\]]\\\\s*"," ").trim();
  if(qTitle.length()<2)qTitle=rawTitle;
  if(qTitle.length()<2||qTitle.length()>160)return null;
  Set<String> wanted=requestedNames(request);
  try{
   String q="query($s:String!){Page(page:1,perPage:10){media(search:$s,type:ANIME,isAdult:false,sort:SEARCH_MATCH){id idMal type format episodes title{english romaji native} synonyms}}}";
   JSONObject page=graph("",q,new JSONObject().put("s",qTitle)).optJSONObject("Page");
   JSONArray media=page==null?null:page.optJSONArray("media");
   if(media!=null){
    JSONObject first=null;
    for(int i=0;i<media.length();i++){
     JSONObject m=media.optJSONObject(i);if(m==null||m.optInt("id",0)<=0)continue;
     if(first==null)first=m;
     Set<String> cand=new HashSet<>();metadataNames(cand,m);
     for(String w:wanted){if(cand.contains(w))return new JSONObject().put("aid",m.optInt("id",0)).put("mid",Math.max(0,m.optInt("idMal",0)));}
    }
    if(first!=null){
     Set<String> cand=new HashSet<>();metadataNames(cand,first);
     for(String w:wanted)for(String c:cand){if(w.length()>=6&&c.length()>=6&&(w.startsWith(c)||c.startsWith(w)))return new JSONObject().put("aid",first.optInt("id",0)).put("mid",Math.max(0,first.optInt("idMal",0)));}
    }
   }
  }catch(Exception ignored){}
  if("mal".equals(provider)&&access!=null&&!access.isEmpty()){
   try{
    String shortQ=qTitle.length()>64?qTitle.substring(0,64):qTitle;
    String addr="https://api.myanimelist.net/v2/anime?"+AccountOAuth.form(AccountOAuth.map("q",shortQ,"limit","8","fields","title,alternative_titles,media_type,num_episodes"));
    JSONObject res=http.request("GET",addr,headers(access,false),null);
    JSONArray data=res.optJSONArray("data");
    if(data!=null){
     for(int i=0;i<data.length();i++){
      JSONObject row=data.optJSONObject(i);JSONObject node=row==null?null:row.optJSONObject("node");
      if(node==null||node.optInt("id",0)<=0)continue;
      Set<String> cand=new HashSet<>();metadataNames(cand,node);
      for(String w:wanted){
       if(cand.contains(w))return new JSONObject().put("aid",0).put("mid",node.optInt("id",0));
       for(String c:cand){if(w.length()>=6&&c.length()>=6&&(w.startsWith(c)||c.startsWith(w)))return new JSONObject().put("aid",0).put("mid",node.optInt("id",0));}
      }
     }
    }
   }catch(Exception ignored){}
  }
  return null;
 }
"""
    ts_text = patch_once(
        ts_text,
        "autoResolveIds154(",
        " JSONObject update(String provider,JSONObject request,String access) throws Exception {\n"
        '  if(!"mal".equals(provider)&&!"anilist".equals(provider))throw new AccountError("Choose MyAnimeList or AniList to sync.");\n'
        '  int aid=id(request,"anilistId"),mid=id(request,"malId");if(aid==0&&mid==0)throw new AccountError("MATCH_REQUIRED: Choose the exact anime before syncing.");',
        auto_resolve_helper
        + " JSONObject update(String provider,JSONObject request,String access) throws Exception {\n"
        '  if(!"mal".equals(provider)&&!"anilist".equals(provider))throw new AccountError("Choose MyAnimeList or AniList to sync.");\n'
        '  int aid=id(request,"anilistId"),mid=id(request,"malId");boolean autoMatched154=false;\n'
        '  if(aid==0&&mid==0){JSONObject res154=autoResolveIds154(provider,request,access);if(res154!=null){aid=res154.optInt("aid",0);mid=res154.optInt("mid",0);autoMatched154=(aid>0||mid>0);}}\n'
        '  if(aid==0&&mid==0)throw new AccountError("MATCH_REQUIRED: Choose the exact anime before syncing.");',
        "TrackerSync553.update autoResolveIds154",
    )
    ts_text = patch_once(
        ts_text,
        "if(!autoMatched154)verifyNames(request,ani,mal);",
        "  verifyNames(request,ani,mal);",
        "  if(!autoMatched154)verifyNames(request,ani,mal);",
        "TrackerSync553.update autoMatched154 verify bypass",
    )
    ts_path.write_text(ts_text, encoding="utf-8")

    # 6. MainActivity.java -> browser bypass, dual Discord OAuth routing, and 85% episode completion sync
    ma_path = root / "src/app/lovelycrxshx/kuro/MainActivity.java"
    ma_text = ma_path.read_text(encoding="utf-8")
    ma_text = patch_once(
        ma_text,
        "||KuroDiscordOAuth564.isCallback(intent));}",
        'private static boolean isAccountIntent(Intent intent){return intent!=null&&Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null&&AccountOAuth.SCHEME.equals(intent.getData().getScheme())&&"oauth".equals(intent.getData().getHost());}',
        'private static boolean isAccountIntent(Intent intent){return intent!=null&&intent.getData()!=null&&((Intent.ACTION_VIEW.equals(intent.getAction())&&AccountOAuth.SCHEME.equals(intent.getData().getScheme())&&"oauth".equals(intent.getData().getHost()))||KuroDiscordOAuth564.isCallback(intent));}',
        "MainActivity.isAccountIntent Discord callback fallback",
    )
    ma_text = patch_once(
        ma_text,
        "KuroFastNet154.openInBrowser154(this,target)",
        ' private void openAccountBrowser(String target){\n'
        '  try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(target));i.addCategory(Intent.CATEGORY_BROWSABLE);startActivity(i);}catch(Exception e){try{postAccountEvent(new JSONObject().put("error","No browser is available to open the provider sign-in."));}catch(Exception ignored){}}\n'
        ' }',
        ' private void openAccountBrowser(String target){\n'
        '  if(!KuroFastNet154.openInBrowser154(this,target)){try{postAccountEvent(new JSONObject().put("error","No browser is available to open the provider sign-in."));}catch(Exception ignored){}}\n'
        ' }',
        "MainActivity.openAccountBrowser explicit browser",
    )
    ma_text = patch_once(
        ma_text,
        "discordLinkStatus154()",
        '  @JavascriptInterface public void discordAction564(String action){if(Arrays.asList("clear","current","linked","test","link","unlink").contains(action))main.post(()->{if(!isFinishing()&&!isDestroyed())MainActivity.this.discordAction564(action);});}',
        '  @JavascriptInterface public void discordAction564(String action){if(Arrays.asList("clear","current","linked","test","link","linkDirect154","unlink").contains(action))main.post(()->{if(!isFinishing()&&!isDestroyed())MainActivity.this.discordAction564(action);});}\n'
        '  @JavascriptInterface public String discordLinkStatus154(){try{boolean linked=KuroDiscordVault564.hasStored(MainActivity.this,"refresh");boolean appInstalled=false;try{getPackageManager().getPackageInfo("com.discord",0);appInstalled=true;}catch(Exception ignored){}return new JSONObject().put("linked",linked).put("enabled",discordEnabled563()).put("appInstalled",appInstalled).put("status",discordRpc563==null?"":discordRpc563.status()).toString();}catch(Exception e){return "{}";}}',
        "MainActivity.Bridge discordLinkStatus154 + linkDirect154",
    )
    ma_text = patch_once(
        ma_text,
        "KuroDiscordOAuth564.hasMatchingPending154(this,intent)",
        ' private void handleDiscordOAuth564(Intent intent){\n'
        '  KuroDiscordOAuth564.Result r=KuroDiscordOAuth564.consume(this,intent);if(r==null)return;\n'
        '  if(!r.error.isEmpty()){Toast.makeText(this,r.error,Toast.LENGTH_LONG).show();return;}\n'
        '  if(!discordEnabled563()){Toast.makeText(this,"Turn on Discord Rich Presence, then link again.",Toast.LENGTH_LONG).show();return;}\n'
        '  if(discordRpc563!=null){discordRpc563.start();discordRpc563.exchange(r.code,r.verifier);Toast.makeText(this,"Connecting the linked Discord account…",Toast.LENGTH_LONG).show();}\n'
        ' }',
        ' private void handleDiscordOAuth564(Intent intent){\n'
        '  if(!KuroDiscordOAuth564.hasMatchingPending154(this,intent))return;\n'
        '  KuroDiscordOAuth564.Result r=KuroDiscordOAuth564.consume(this,intent);if(r==null)return;\n'
        '  if(!r.error.isEmpty()){Toast.makeText(this,r.error,Toast.LENGTH_LONG).show();return;}\n'
        '  if(discordPrefs564!=null)discordPrefs564.set("linked","on");\n'
        '  if(!discordEnabled563()){getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).edit().putBoolean("enabled",true).apply();discordEnabledChanged563(true);}\n'
        '  if(discordRpc563!=null){discordRpc563.start();discordRpc563.exchange(r.code,r.verifier);Toast.makeText(this,"Connecting the linked Discord account…",Toast.LENGTH_LONG).show();Runnable notify154=()->{if(ui!=null&&!isFinishing()&&!isDestroyed())ui.evaluateJavascript("window.dispatchEvent(new CustomEvent(\'kuro-discord-rpc-changed154\'))",null);};main.post(notify154);main.postDelayed(notify154,1800);main.postDelayed(notify154,4500);}\n'
        ' }',
        "MainActivity.handleDiscordOAuth564 pending state routing + auto-enable + UI notify",
    )
    ma_text = patch_once(
        ma_text,
        'if("linkDirect154".equals(action))',
        '  if(!discordEnabled563()){Toast.makeText(this,"Turn on Discord Rich Presence first.",Toast.LENGTH_SHORT).show();return;}',
        '  if("linkDirect154".equals(action)){if(discordPrefs564!=null)discordPrefs564.set("linked","on");if(!discordEnabled563()){getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).edit().putBoolean("enabled",true).apply();discordEnabledChanged563(true);}if(!KuroDiscordOAuth564.begin(this))Toast.makeText(this,"Could not open secure Discord authorization on this phone.",Toast.LENGTH_LONG).show();return;}\n'
        '  if(!discordEnabled563()){if("link".equals(action)){getSharedPreferences("kuro-discord-rpc563",MODE_PRIVATE).edit().putBoolean("enabled",true).apply();discordEnabledChanged563(true);}else{Toast.makeText(this,"Turn on Discord Rich Presence first.",Toast.LENGTH_SHORT).show();return;}}',
        "MainActivity.discordAction564 linkDirect154 + auto-enable on link",
    )
    ma_text = patch_once(
        ma_text,
        "kuro-discord-rpc-changed154-unlink",
        'discordRpc563.unlink();Toast.makeText(this,"Linked account disconnected on this phone.",Toast.LENGTH_SHORT).show();',
        'discordRpc563.unlink();Toast.makeText(this,"Linked account disconnected on this phone.",Toast.LENGTH_SHORT).show();/* kuro-discord-rpc-changed154-unlink */if(ui!=null)ui.evaluateJavascript("window.dispatchEvent(new CustomEvent(\'kuro-discord-rpc-changed154\'))",null);',
        "MainActivity.discordAction564 unlink UI notify",
    )
    ma_text = patch_once(
        ma_text,
        "/* k154 iframe+85pct completion */",
        '   WebViewCompat.addWebMessageListener(view,"KuroFinished553",Collections.singleton("*"),(sender,message,origin,isMainFrame,reply)->{\n'
        '    if(sender!=player||!isMainFrame||!activityResumed||sent[0]||verification[0]||message.getType()!=WebMessageCompat.TYPE_STRING||!"ended".equals(message.getData())||origin==null||!"https".equals(origin.getScheme())||!playerHost(origin.getHost())||!session.equals(watchSession))return;\n'
        '    String loaded=sender.getUrl();Uri current=loaded==null?null:Uri.parse(loaded);if(current==null||!"https".equals(current.getScheme())||!origin.getHost().equals(current.getHost()))return;\n'
        '    long attempt=playerAttempt;String check="(function(){try{var a=document.querySelectorAll(\'video\');if(a.length!==1)return false;var v=a[0];return !!(v.ended&&Number.isFinite(v.duration)&&v.duration>=180&&v.currentTime>=v.duration*.98)}catch(e){return false}})()";\n'
        '    sender.evaluateJavascript(check,result->{if(!"true".equals(result)||sender!=player||attempt!=playerAttempt||!session.equals(watchSession)||verification[0]||sent[0]||ui==null)return;sent[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(number)+")",null);});\n'
        '   });',
        '   WebViewCompat.addWebMessageListener(view,"KuroFinished553",Collections.singleton("*"),(sender,message,origin,isMainFrame,reply)->{\n'
        '    /* k154 iframe+85pct completion */\n'
        '    if(sender!=player||!activityResumed||sent[0]||verification[0]||message.getType()!=WebMessageCompat.TYPE_STRING||!"ended".equals(message.getData())||origin==null||!"https".equals(origin.getScheme())||!playerHost(origin.getHost())||!session.equals(watchSession))return;\n'
        '    if(ui==null)return;sent[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(number)+")",null);\n'
        '   });',
        "MainActivity.setupCompletionFrames iframe + 85pct completion",
    )
    ma_text = patch_once(
        ma_text,
        "position<duration*.85)return;",
        "long duration=nativePlayer561.duration(),position=nativePlayer561.position();if(duration<180000||position<duration*.98)return;",
        "long duration=nativePlayer561.duration(),position=nativePlayer561.position();if(duration<180000||position<duration*.85)return;",
        "MainActivity.nativeEnded561 85pct threshold",
    )
    ma_text = patch_once(
        ma_text,
        "dur154>=180000L&&pos154>=dur154*85L/100L",
        "void closePlayer(){cancelAutoNext573();",
        'void closePlayer(){cancelAutoNext573();if(nativePlayer561!=null&&watchInfo561!=null&&!watchSession.isEmpty()&&ui!=null){long dur154=nativePlayer561.duration(),pos154=nativePlayer561.position();String ep154=watchInfo561.optString("episode","");if(dur154>=180000L&&pos154>=dur154*85L/100L&&ep154.matches("[1-9][0-9]{0,3}"))ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(watchSession)+","+JSONObject.quote(ep154)+")",null);}',
        "MainActivity.closePlayer 85pct native watch mark",
    )
    ma_text = patch_once(
        ma_text,
        "/* k154 stream-player 85pct */",
        'if(v.optBoolean("ended",false)&&duration>=5000&&position>=duration*.98)KuroPlayback561.completed(MainActivity.this,resumeKey561);else KuroPlayback561.save(MainActivity.this,resumeKey561,position,duration);',
        '/* k154 stream-player 85pct */ if(duration>=180000&&position>=duration*.85&&watchInfo561!=null&&!watchSession.isEmpty()&&ui!=null){String ep154=watchInfo561.optString("episode","");if(ep154.matches("[1-9][0-9]{0,3}"))ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(watchSession)+","+JSONObject.quote(ep154)+")",null);}if(v.optBoolean("ended",false)&&duration>=5000&&position>=duration*.98)KuroPlayback561.completed(MainActivity.this,resumeKey561);else KuroPlayback561.save(MainActivity.this,resumeKey561,position,duration);',
        "MainActivity.setupProgress561 85pct direct HLS watch mark",
    )
    ma_path.write_text(ma_text, encoding="utf-8")

    # 7. assets/player-ended553.js -> 85% episode completion in top-level & embedded player frames
    pe_path = root / "assets/player-ended553.js"
    pe_path.write_text(PLAYER_ENDED_154_JS, encoding="utf-8")

    # 8. assets/tracker-sync553.js -> auto-resolve IDs without premature 'match' trap + live Sync Now API
    ts_js_path = root / "assets/tracker-sync553.js"
    ts_js = ts_js_path.read_text(encoding="utf-8")
    ts_js = patch_once(
        ts_js,
        "autoTried154",
        "if(!state)state={v:1,queue:[],links:{},initialized:{},auto:{anilist:true,mal:true},cooldown:{},seq:0};",
        "if(!state)state={v:1,queue:[],links:{},initialized:{},auto:{anilist:true,mal:true},cooldown:{},seq:0};\n"
        " else{for(const j of state.queue){if(j&&j.state==='match'&&!j.autoTried154){j.state='pending';j.error='';j.retryAt=0;j.autoTried154=true}}}",
        "tracker-sync553.js promote untried match jobs",
    )
    ts_js = patch_once(
        ts_js,
        "/* k154 native auto-resolve */",
        "  const ident=identity(item);if(!ident.anilistId&&!ident.malId)job.state='match';\n  job.rev=++state.seq;",
        "  /* k154 native auto-resolve */ job.autoTried154=true;\n  job.rev=++state.seq;",
        "tracker-sync553.js upsert allow pending without pre-existing ID",
    )
    ts_js = patch_once(
        ts_js,
        "/* k154 drain auto-resolve */",
        "  const ident=identity(job.item);\n  if(!ident.anilistId&&!ident.malId){job.state='match';job.error='Choose the exact anime.';persist();paint();schedule();return}",
        "  const ident=identity(job.item); /* k154 drain auto-resolve */",
        "tracker-sync553.js drain delegate ID lookup to TrackerSync553",
    )
    ts_js = patch_once(
        ts_js,
        "kuro-tracker-last-sync-154",
        "    if(result.conflict){current.state='conflict';current.error='An existing provider status differs from Kuro.';current.tries=0}\n    else state.queue=state.queue.filter(j=>j!==current);",
        "    if(result.conflict){current.state='conflict';current.error='An existing provider status differs from Kuro.';current.tries=0}\n    else{state.queue=state.queue.filter(j=>j!==current);try{let m=JSON.parse(localStorage.getItem('kuro-tracker-last-sync-154')||'{}');m[captured.provider]=Date.now();localStorage.setItem('kuro-tracker-last-sync-154',JSON.stringify(m))}catch(_){}}",
        "tracker-sync553.js record last sync timestamp",
    )
    ts_js = patch_once(
        ts_js,
        "nextAt=Date.now()+1200;schedule(1200);",
        "nextAt=Date.now()+6500;schedule(6500);",
        "nextAt=Date.now()+1200;schedule(1200);",
        "tracker-sync553.js faster queue drain",
    )
    ts_js = patch_once(
        ts_js,
        "kuro-tracker-sync-paint154",
        " window.kuroSyncPaint553=paint;",
        " const origPaint154=paint;paint=function(){origPaint154();try{window.dispatchEvent(new CustomEvent('kuro-tracker-sync-paint154'))}catch(_){}};\n"
        " window.kuroSyncPaint553=paint;\n"
        " window.kuroTrackerSyncNow154=async function(provider){\n"
        "  if(unreadable)return {ok:false};\n"
        "  background=false;nextAt=0;\n"
        "  if(window.kuroTrackerRequest553){try{const s=await window.kuroTrackerRequest553('status',{});connections=s.connections||{}}catch(_){}}\n"
        "  const targets=provider?[provider]:providers;\n"
        "  for(const p of targets){\n"
        "   state.cooldown[p]=0;\n"
        "   const o=owner(p);\n"
        "   if(o&&enabled(p)){delete state.initialized[p+'|'+o];backfill(p,o)}\n"
        "   for(const j of state.queue){if(j&&j.provider===p&&(j.state==='blocked'||j.state==='pending'||(j.state==='match'&&!j.userSkipped154))){j.state='pending';j.retryAt=0;j.error='';j.rev=++state.seq}}\n"
        "  }\n"
        "  persist();paint();\n"
        "  await drain();\n"
        "  return {ok:true};\n"
        " };",
        "tracker-sync553.js kuroTrackerSyncNow154 + paint event",
    )
    ts_js_path.write_text(ts_js, encoding="utf-8")

    # 9. assets/accounts542.js -> update existing import progress/status + direct 1-tap Connect & Silent Import APIs
    ac_path = root / "assets/accounts542.js"
    ac_text = ac_path.read_text(encoding="utf-8")
    old_put_imports = (
        "  let old=readImports(),keys=new Set(old.map(x=>String(x.provider)+':'+String(x.sourceId))),added=0,skipped=0,rows=old.slice();\n"
        "  for(const x of response.items){\n"
        "   if(!x||x.provider!==response.provider||!/^[1-9][0-9]{0,18}$/.test(String(x.sourceId))||typeof x.title!=='string'||!x.title.trim())continue;\n"
        "   let key=x.provider+':'+String(x.sourceId);if(keys.has(key)){skipped++;continue}keys.add(key);\n"
        "   let image='';try{let u=new URL(String(x.image||''));if(u.protocol==='https:'&&!u.username&&!u.password&&!u.port)image=u.href}catch{}\n"
        "   rows.push({provider:x.provider,sourceId:String(x.sourceId),malId:Number(x.malId)||0,anilistId:Number(x.anilistId)||0,title:x.title.trim().slice(0,300),image,status:String(x.status||'').slice(0,40),progress:Math.max(0,Math.min(100000,Number(x.progress)||0)),total:Math.max(0,Math.min(100000,Number(x.total)||0)),adult:typeof x.adult==='boolean'?x.adult:null});added++;\n"
        "  }\n"
        "  try{localStorage.setItem(IMPORT_KEY,JSON.stringify(rows))}catch{throw Error('Not enough local storage for the import. Existing lists were not changed.')}\n"
        "  return `${added} added, ${skipped} already present and kept. Open My Lists to see your imports.${response.partial?' Partial import: '+String(response.reason||'some entries could not be read.'):''}`;"
    )
    new_put_imports = (
        "  let old=readImports(),idxMap=new Map(),added=0,updated154=0,skipped=0,rows=old.slice();\n"
        "  rows.forEach((r,idx)=>{if(r)idxMap.set(String(r.provider)+':'+String(r.sourceId),idx)});\n"
        "  for(const x of response.items){\n"
        "   if(!x||x.provider!==response.provider||!/^[1-9][0-9]{0,18}$/.test(String(x.sourceId))||typeof x.title!=='string'||!x.title.trim())continue;\n"
        "   let key=x.provider+':'+String(x.sourceId);\n"
        "   let image='';try{let u=new URL(String(x.image||''));if(u.protocol==='https:'&&!u.username&&!u.password&&!u.port)image=u.href}catch{}\n"
        "   let nextRow={provider:x.provider,sourceId:String(x.sourceId),malId:Number(x.malId)||0,anilistId:Number(x.anilistId)||0,title:x.title.trim().slice(0,300),image,status:String(x.status||'').slice(0,40),progress:Math.max(0,Math.min(100000,Number(x.progress)||0)),total:Math.max(0,Math.min(100000,Number(x.total)||0)),adult:typeof x.adult==='boolean'?x.adult:null};\n"
        "   if(idxMap.has(key)){\n"
        "    let pos=idxMap.get(key),prev=rows[pos];\n"
        "    if(prev&&(prev.status!==nextRow.status||prev.progress!==nextRow.progress||prev.total!==nextRow.total||(!prev.image&&nextRow.image))){rows[pos]=Object.assign({},prev,nextRow,{image:nextRow.image||prev.image});updated154++;}else{skipped++;}\n"
        "    continue;\n"
        "   }\n"
        "   idxMap.set(key,rows.length);rows.push(nextRow);added++;\n"
        "  }\n"
        "  try{localStorage.setItem(IMPORT_KEY,JSON.stringify(rows));let m=JSON.parse(localStorage.getItem('kuro-tracker-last-sync-154')||'{}');m[response.provider]=Date.now();localStorage.setItem('kuro-tracker-last-sync-154',JSON.stringify(m))}catch{throw Error('Not enough local storage for the import. Existing lists were not changed.')}\n"
        "  return `${added} added, ${updated154} updated, ${skipped} unchanged.${response.partial?' Partial import: '+String(response.reason||'some entries could not be read.'):''}`;"
    )
    ac_text = patch_once(
        ac_text,
        "updated154",
        old_put_imports,
        new_put_imports,
        "accounts542.js putImports live status & progress sync",
    )
    ac_text = patch_once(
        ac_text,
        "window.kuroSilentImport154?.('anilist')",
        "try{let r=await request('anilistPin',{token});closeModal();placeholder(r.message);await status();if(view==='profile')profile()}",
        "try{let r=await request('anilistPin',{token});closeModal();placeholder(r.message);await status();if(view==='profile')profile();window.kuroTrackerSyncNow154?.('anilist');window.kuroSilentImport154?.('anilist')}",
        "accounts542.js auto-sync on AniList PIN connect",
    )
    direct_helpers_js = (
        " window.kuroDirectConnect154=async provider=>{\n"
        "  if(!['discord','mal','anilist'].includes(provider))return;\n"
        "  try{\n"
        "   let result=await request('begin',{provider});\n"
        "   if(provider==='anilist'){login('pin');message(result.message)}\n"
        "   else{placeholder(result.message||'Continue in your browser, then return to Kuro.')}\n"
        "  }catch(e){placeholder(e.message||'Could not start sign-in.')}\n"
        " };\n"
        " window.kuroSilentImport154=async provider=>{\n"
        "  if(!['mal','anilist'].includes(provider))return null;\n"
        "  let r=await request('import',{provider});\n"
        "  let info=putImports(r);\n"
        "  if(view==='profile')profile();\n"
        "  else if(view==='library')library();\n"
        "  return info;\n"
        " };\n"
        " window.kuroAccountEvent=async event=>{\n"
        "  if(event?.error){if(document.querySelector('#account-overlay'))message(event.error,true);else placeholder(event.error);return}\n"
        "  if(watchAccountMode){if(event?.message)message(event.message)}else if(document.querySelector('#account-overlay'))closeModal();\n"
        "  if(event?.message&&!watchAccountMode)placeholder(event.message);try{await status()}catch{}if(view==='profile')profile();\n"
        "  if(event?.kind==='mal'||event?.kind==='anilist'){window.kuroTrackerSyncNow154?.(event.kind);window.kuroSilentImport154?.(event.kind).catch(()=>{})}\n"
        " };"
    )
    ac_text = patch_once(
        ac_text,
        "window.kuroDirectConnect154=",
        " window.kuroAccountEvent=async event=>{\n"
        "  if(event?.error){message(event.error,true);return}\n"
        "  if(watchAccountMode){if(event?.message)message(event.message)}else if(document.querySelector('#account-overlay'))closeModal();\n"
        "  if(event?.message&&!watchAccountMode)placeholder(event.message);try{await status()}catch{}if(view==='profile')profile();\n"
        " };",
        direct_helpers_js,
        "accounts542.js kuroDirectConnect154, kuroSilentImport154 & kuroAccountEvent toast fallback",
    )
    ac_path.write_text(ac_text, encoding="utf-8")


def patch_watch_ui_154(root: pathlib.Path) -> None:
    # 1. WatchHeader.java: remove top circular episodes drawer button + add live refreshTheme155() & tinted kuro-logo.png
    wh_path = root / "src/app/lovelycrxshx/kuro/WatchHeader.java"
    wh_text = wh_path.read_text(encoding="utf-8")
    wh_text = patch_once(
        wh_text,
        "/* k154 watch-episodes header button removed */",
        '  addView(icon("watch-episodes","Episodes",2,()->{if(this.listener!=null)this.listener.episodes();}),new LinearLayout.LayoutParams(dp(48),dp(48)));\n',
        '  /* k154 watch-episodes header button removed */\n',
        "WatchHeader remove watch-episodes button",
    )
    wh_text = patch_once(
        wh_text,
        "private Bitmap baseLogo155;",
        ' private final Listener listener; private FrameLayout accountTarget; private TextView accountWord; private TextView accountMono; private ImageView accountArt;',
        ' private final Listener listener; private FrameLayout accountTarget; private TextView accountWord; private TextView accountMono; private ImageView accountArt; private Bitmap baseLogo155; private ImageView logoView155; private TextView logoFallback155; private final java.util.ArrayList<FrameLayout> iconFrames155=new java.util.ArrayList<>();',
        "WatchHeader theme fields",
    )
    wh_text = patch_once(
        wh_text,
        "KuroTheme12.tintLogoBitmap(bitmap)",
        '  if(bitmap!=null)logo.setImageBitmap(bitmap);\n'
        '  home.addView(logo,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(48),Gravity.CENTER_VERTICAL));\n'
        '  if(bitmap==null){TextView fallback=new TextView(context);fallback.setText("kuro.tv");fallback.setTextSize(18);fallback.setTypeface(Typeface.DEFAULT,Typeface.BOLD_ITALIC);fallback.setTextColor(ICON);fallback.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);home.addView(fallback,new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT,Gravity.CENTER_VERTICAL|Gravity.START));}',
        '  logoView155=logo;baseLogo155=bitmap;if(bitmap!=null)logo.setImageBitmap(KuroTheme12.tintLogoBitmap(bitmap));\n'
        '  home.addView(logo,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(48),Gravity.CENTER_VERTICAL));\n'
        '  if(bitmap==null){TextView fallback=new TextView(context);logoFallback155=fallback;fallback.setText("kuro.tv");fallback.setTextSize(18);fallback.setTypeface(Typeface.DEFAULT,Typeface.BOLD_ITALIC);fallback.setTextColor(ICON);fallback.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);home.addView(fallback,new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT,Gravity.CENTER_VERTICAL|Gravity.START));}',
        "WatchHeader tint logo bitmap",
    )
    wh_text = patch_once(
        wh_text,
        "public void refreshTheme155()",
        ' private FrameLayout icon(String tag,String description,int which,Runnable click){\n'
        '  FrameLayout frame=target(tag,description,click);Icon glyph=new Icon(getContext(),which);glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);glyph.setBackground(shape(CHIP,BORDER,17));frame.addView(glyph,new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.CENTER));return frame;\n'
        ' }',
        ' private FrameLayout icon(String tag,String description,int which,Runnable click){\n'
        '  FrameLayout frame=target(tag,description,click);Icon glyph=new Icon(getContext(),which);glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);glyph.setBackground(shape(CHIP,BORDER,17));frame.addView(glyph,new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.CENTER));iconFrames155.add(frame);return frame;\n'
        ' }\n'
        ' public void refreshTheme155(){\n'
        '  retint();setBackgroundColor(BACKGROUND);\n'
        '  if(logoView155!=null&&baseLogo155!=null)logoView155.setImageBitmap(KuroTheme12.tintLogoBitmap(baseLogo155));\n'
        '  if(logoFallback155!=null)logoFallback155.setTextColor(ICON);\n'
        '  for(int i=0;i<iconFrames155.size();i++){FrameLayout fr=iconFrames155.get(i);fr.setForeground(new RippleDrawable(ColorStateList.valueOf(KuroTheme12.col(0x447f55a0)),null,null));if(fr.getChildCount()>0&&fr.getChildAt(0) instanceof Icon){Icon ic=(Icon)fr.getChildAt(0);ic.setBackground(shape(CHIP,BORDER,17));ic.paint.setColor(ICON);ic.invalidate();}}\n'
        '  if(accountTarget!=null)accountTarget.setForeground(new RippleDrawable(ColorStateList.valueOf(KuroTheme12.col(0x447f55a0)),null,null));\n'
        '  if(accountWord!=null){accountWord.setTextColor(ICON);accountWord.setBackground(shape(CHIP,BORDER,17));}\n'
        '  if(accountMono!=null){accountMono.setTextColor(ICON);accountMono.setBackground(shape(CHIP,BORDER,30));}\n'
        '  if(accountArt!=null)accountArt.setBackground(shape(CHIP,BORDER,30));\n'
        ' }',
        "WatchHeader refreshTheme155 method",
    )
    wh_path.write_text(wh_text, encoding="utf-8")

    # 2. MainActivity.java — Watch Screen v3 (Concept 1):
    #    - Quick Episode Action Strip directly below classic player (< Prev | Episode X • Title | Next > | Auto-Next pill)
    #    - Compact Server Switcher card with segmented SUB | DUB tabs + horizontal server chips + Tools/Diagnostics pills
    #    - Upgraded 3-row YouTube-style Episode Playlist card with 16:9 thumbnails, progress bars, Watched/Now Playing/FILLER pills
    #    - Upgraded Anime Details card (score + genre chips) + horizontal 16:9 More Anime shelf
    ma_path = root / "src/app/lovelycrxshx/kuro/MainActivity.java"
    ma_text = ma_path.read_text(encoding="utf-8")

    old_drawer = (
        ' void openEpisodeDrawer581(){\n'
        '  JSONObject info=watchInfo561;JSONArray eps=info==null?null:info.optJSONArray("episodes");\n'
        '  if(eps==null||eps.length()==0){Toast.makeText(this,"No episode list for this title.",Toast.LENGTH_SHORT).show();return;}\n'
        '  if(playerLayer==null)return;\n'
        '  if(episodeDrawer581==null)episodeDrawer581=new KuroEpisodeDrawer581(this,root,index->chooseEpisode581(index));episodeDrawer581.setProgress(number->{\n'
        '    JSONObject wi=watchInfo561;if(wi==null||number==null||number.isEmpty())return "";\n'
        '    long at=KuroPlayback561.read(this,KuroPlayback561.key(wi.optString("source"),wi.optString("showIdentity"),number));\n'
        '    if(at<=0)return "";\n'
        '    long sec=at/1000;return String.format(java.util.Locale.ROOT,"Resume %d:%02d",sec/60,sec%60);\n'
        '   });\n'
        '  if(episodeDrawer581.isOpen()){episodeDrawer581.close();return;}\n'
        '  episodeDrawer581.show(eps,info.optInt("current",0));\n'
        ' }'
    )
    new_drawer = (
        ' void openEpisodeDrawer581(){\n'
        '  /* k154 episode slider removed */\n'
        '  if(episodeDrawer581!=null){episodeDrawer581.close();episodeDrawer581=null;}\n'
        ' }'
    )
    ma_text = patch_once(
        ma_text,
        "/* k154 episode slider removed */",
        old_drawer,
        new_drawer,
        "MainActivity disable openEpisodeDrawer581 slider",
    )

    old_top_controls = (
        '  ScrollView scroll=new ScrollView(this);watchScroll=scroll;scroll.setFillViewport(false);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(20),dp(18),dp(30));scroll.addView(content);playerLayer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));\n'
        '  final boolean canNext=current+1<eps.length();JSONObject following=canNext?eps.optJSONObject(current+1):null;\n'
        '  String followingNumber=canNext?(following!=null?following.optString("number",""):eps.optString(current+1,"")):"";\n'
        '  PlaybackPanel controls=new PlaybackPanel(this,info.optString("episode",""),followingNumber,canNext,\n'
        '   getPreferences(MODE_PRIVATE).getBoolean("autoplay",true),getPreferences(MODE_PRIVATE).getBoolean("auto_next",false),getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false),new PlaybackPanel.Listener(){\n'
        '    @Override public void nextEpisode(){int next=current+1;if(next>=eps.length())return;closePlayer();ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);}\n'
        '    @Override public void autoplayChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("autoplay",on).apply();if(nativePlayer561!=null)nativePlayer561.setAutoplay(on);if(player!=null){player.getSettings().setMediaPlaybackRequiresUserGesture(!on);player.evaluateJavascript("window.__kuroAutoplay="+(on?"true":"false")+";if(window.__kuroSetAutoplay561)window.__kuroSetAutoplay561("+(on?"true":"false")+");"+(on?"document.querySelectorAll(\'video\').forEach(function(v){if(v.paused){try{var p=v.play();if(p&&p.catch)p.catch(function(){})}catch(e){}}});":""),null);}Toast.makeText(MainActivity.this,on?"Autoplay requested; some players require a tap.":"Autoplay off for new videos.",Toast.LENGTH_SHORT).show();}\n'
        '    @Override public void autoNextChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",on).apply();Toast.makeText(MainActivity.this,on?"Auto next shows a cancellable countdown when a supported player reports an ending.":"Auto next off.",Toast.LENGTH_SHORT).show();}\n'
        '    @Override public void autoSkipChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_skip",on).apply();updateSkipFrames();Toast.makeText(MainActivity.this,nativePlayer561!=null?(on?"Auto skip uses available or saved markers.":"Auto skip off."):on?(skipFrameMode?"Auto skip will try visible Skip controls.":"Auto skip is limited on this WebView; try updating Android System WebView."):"Auto skip off.",Toast.LENGTH_SHORT).show();}\n'
        '   });\n'
        '  content.addView(controls,new LinearLayout.LayoutParams(-1,-2));addGap(content,12);addDirectTools561(content);'
    )

    ma_text = patch_once(
        ma_text,
        "watchRetheme155",
        'void kuroTheme12WindowApplied(int floor){if(ui!=null)ui.setBackgroundColor(floor);if(root!=null){root.setBackgroundColor(floor);syncWindowSystemBars154();}}',
        'private Runnable watchRetheme155;\n'
        'void kuroTheme12WindowApplied(int floor){if(ui!=null)ui.setBackgroundColor(floor);if(root!=null){root.setBackgroundColor(floor);syncWindowSystemBars154();}if(playerLayer!=null)playerLayer.setBackgroundColor(KuroTheme12.col(0xff110917));if(watchHeader instanceof WatchHeader)((WatchHeader)watchHeader).refreshTheme155();if(watchRetheme155!=null&&playerLayer!=null)watchRetheme155.run();}',
        "MainActivity kuroTheme12WindowApplied live watchRetheme155",
    )
    ma_text = patch_once(
        ma_text,
        "watchRetheme155=null;",
        'watchPlayerBox=null;watchHeader=null;watchScroll=null;',
        'watchRetheme155=null;watchPlayerBox=null;watchHeader=null;watchScroll=null;',
        "MainActivity closePlayer clear watchRetheme155",
    )

    new_top_controls = (
        '  ScrollView scroll=new ScrollView(this);watchScroll=scroll;scroll.setBackgroundColor(KuroTheme12.col(0xff0d0914));scroll.setFillViewport(false);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(14),dp(12),dp(14),dp(28));scroll.addView(content);playerLayer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));\n'
        '  final int nextPlayable155=nextPlayableEpisode154(info,current+1);final boolean canNext=nextPlayable155>=0&&nextPlayable155<eps.length();JSONObject following=canNext?eps.optJSONObject(nextPlayable155):null;\n'
        '  String followingNumber=canNext?(following!=null?following.optString("number",""):eps.optString(nextPlayable155,"")):"";\n'
        '  int prevScan155=current-1;if(info.optBoolean("skipFiller154",false)){while(prevScan155>=0){JSONObject pe=eps.optJSONObject(prevScan155);if(pe==null||!pe.optBoolean("filler154",false))break;prevScan155--;}if(prevScan155<0&&current>0)prevScan155=current-1;}final int prevPlayable155=prevScan155;final boolean canPrev155=prevPlayable155>=0;\n'
        '  final TextView[] autoNextQuickPill155={null};final boolean[] syncingAutoNext155={false};\n'
        '  final java.util.function.Consumer<Boolean> styleAutoNextPill155=on->{if(autoNextQuickPill155[0]==null)return;autoNextQuickPill155[0].setTextColor(on?KuroTheme12.col(0xfff7eeff):KuroTheme12.col(0xffaa99b9));autoNextQuickPill155[0].setBackground(watchShape154(on?KuroTheme12.col(0xff522c7a):KuroTheme12.col(0xff21152b),on?KuroTheme12.col(0xffb478ee):KuroTheme12.col(0xff443253),9));};\n'
        '  LinearLayout epStrip155=new LinearLayout(this);epStrip155.setOrientation(LinearLayout.HORIZONTAL);epStrip155.setGravity(Gravity.CENTER_VERTICAL);epStrip155.setTag("watch-ep-strip-155");\n'
        '  TextView prevEpBtn155=text("‹ Prev",12,canPrev155?KuroTheme12.col(0xffeae2f2):KuroTheme12.col(0xff6b5b7a));prevEpBtn155.setTypeface(null,android.graphics.Typeface.BOLD);prevEpBtn155.setGravity(Gravity.CENTER);prevEpBtn155.setMinHeight(dp(36));prevEpBtn155.setPadding(dp(11),0,dp(11),0);prevEpBtn155.setBackground(watchShape154(canPrev155?KuroTheme12.col(0xff21152b):KuroTheme12.col(0xff160e1e),canPrev155?KuroTheme12.col(0xff443253):KuroTheme12.col(0xff2b1e36),9));prevEpBtn155.setEnabled(canPrev155);prevEpBtn155.setOnClickListener(v->{if(canPrev155)chooseEpisode581(prevPlayable155);});epStrip155.addView(prevEpBtn155,new LinearLayout.LayoutParams(-2,dp(36)));\n'
        '  JSONObject curEpObj155=eps.optJSONObject(current);String curEpNum155=info.optString("episode",curEpObj155!=null?curEpObj155.optString("number","?"):"?");String curEpName155=info.optString("episodeName",curEpObj155!=null?curEpObj155.optString("name",""):"");if(curEpName155!=null){curEpName155=curEpName155.trim();if(curEpName155.equalsIgnoreCase("Episode "+curEpNum155))curEpName155="";}\n'
        '  TextView curEpCenter155=text("Episode "+curEpNum155+(curEpName155==null||curEpName155.isEmpty()?"":"  •  "+curEpName155),12,Color.WHITE);curEpCenter155.setTypeface(null,android.graphics.Typeface.BOLD);curEpCenter155.setGravity(Gravity.CENTER);curEpCenter155.setSingleLine(true);curEpCenter155.setEllipsize(android.text.TextUtils.TruncateAt.END);curEpCenter155.setPadding(dp(8),0,dp(8),0);epStrip155.addView(curEpCenter155,new LinearLayout.LayoutParams(0,-2,1));\n'
        '  TextView nextEpBtn155=text("Next ›",12,canNext?KuroTheme12.col(0xffeae2f2):KuroTheme12.col(0xff6b5b7a));nextEpBtn155.setTypeface(null,android.graphics.Typeface.BOLD);nextEpBtn155.setGravity(Gravity.CENTER);nextEpBtn155.setMinHeight(dp(36));nextEpBtn155.setPadding(dp(11),0,dp(11),0);nextEpBtn155.setBackground(watchShape154(canNext?KuroTheme12.col(0xff21152b):KuroTheme12.col(0xff160e1e),canNext?KuroTheme12.col(0xff443253):KuroTheme12.col(0xff2b1e36),9));nextEpBtn155.setEnabled(canNext);nextEpBtn155.setOnClickListener(v->{if(canNext)chooseEpisode581(nextPlayable155);});epStrip155.addView(nextEpBtn155,new LinearLayout.LayoutParams(-2,dp(36)));\n'
        '  TextView autoNextPill155=text("Auto-Next",11,Color.WHITE);autoNextPill155.setTypeface(null,android.graphics.Typeface.BOLD);autoNextPill155.setGravity(Gravity.CENTER);autoNextPill155.setMinHeight(dp(36));autoNextPill155.setPadding(dp(11),0,dp(11),0);autoNextPill155.setTag("watch-autonext-pill-155");autoNextQuickPill155[0]=autoNextPill155;styleAutoNextPill155.accept(getPreferences(MODE_PRIVATE).getBoolean("auto_next",false));LinearLayout.LayoutParams anLp155=new LinearLayout.LayoutParams(-2,dp(36));anLp155.setMargins(dp(6),0,0,0);epStrip155.addView(autoNextPill155,anLp155);\n'
        '  final LinearLayout directHost155=new LinearLayout(this);directHost155.setOrientation(LinearLayout.VERTICAL);\n'
        '  content.addView(epStrip155,new LinearLayout.LayoutParams(-1,-2));addGap(content,10);content.addView(directHost155,new LinearLayout.LayoutParams(-1,-2));addDirectTools561(directHost155);\n'
        '  final LinearLayout toolsDrawer155=new LinearLayout(this);toolsDrawer155.setOrientation(LinearLayout.VERTICAL);toolsDrawer155.setVisibility(View.GONE);toolsDrawer155.setPadding(0,dp(10),0,0);\n'
        '  final PlaybackPanel.Listener pbListener155=new PlaybackPanel.Listener(){\n'
        '    @Override public void nextEpisode(){if(!canNext)return;chooseEpisode581(nextPlayable155);}\n'
        '    @Override public void autoplayChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("autoplay",on).apply();if(nativePlayer561!=null)nativePlayer561.setAutoplay(on);if(player!=null){player.getSettings().setMediaPlaybackRequiresUserGesture(!on);player.evaluateJavascript("window.__kuroAutoplay="+(on?"true":"false")+";if(window.__kuroSetAutoplay561)window.__kuroSetAutoplay561("+(on?"true":"false")+");"+(on?"document.querySelectorAll(\'video\').forEach(function(v){if(v.paused){try{var p=v.play();if(p&&p.catch)p.catch(function(){})}catch(e){}}});":""),null);}Toast.makeText(MainActivity.this,on?"Autoplay requested; some players require a tap.":"Autoplay off for new videos.",Toast.LENGTH_SHORT).show();}\n'
        '    @Override public void autoNextChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",on).apply();styleAutoNextPill155.accept(on);if(!syncingAutoNext155[0])Toast.makeText(MainActivity.this,on?"Auto-Next ON":"Auto-Next OFF",Toast.LENGTH_SHORT).show();}\n'
        '    @Override public void autoSkipChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_skip",on).apply();updateSkipFrames();Toast.makeText(MainActivity.this,nativePlayer561!=null?(on?"Auto skip uses available or saved markers.":"Auto skip off."):on?(skipFrameMode?"Auto skip will try visible Skip controls.":"Auto skip is limited on this WebView; try updating Android System WebView."):"Auto skip off.",Toast.LENGTH_SHORT).show();}\n'
        '  };\n'
        '  final PlaybackPanel[] controlsRef155={new PlaybackPanel(this,info.optString("episode",""),followingNumber,canNext,getPreferences(MODE_PRIVATE).getBoolean("autoplay",true),getPreferences(MODE_PRIVATE).getBoolean("auto_next",false),getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false),pbListener155)};\n'
        '  autoNextPill155.setOnClickListener(v->{boolean nxt=!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false);getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",nxt).apply();styleAutoNextPill155.accept(nxt);PlaybackPanel cp=controlsRef155[0];if(cp!=null&&cp.getAutoNext()!=null&&cp.getAutoNext().isChecked()!=nxt){syncingAutoNext155[0]=true;cp.getAutoNext().setChecked(nxt);syncingAutoNext155[0]=false;}Toast.makeText(MainActivity.this,nxt?"Auto-Next ON":"Auto-Next OFF",Toast.LENGTH_SHORT).show();});\n'
        '  toolsDrawer155.addView(controlsRef155[0],new LinearLayout.LayoutParams(-1,-2));addGap(toolsDrawer155,8);'
    )

    ma_text = patch_once(
        ma_text,
        "watch-ep-strip-155",
        old_top_controls,
        new_top_controls,
        "MainActivity Quick Episode Action Strip & Tools Drawer top",
    )

    old_mid_controls = (
        '  });content.addView(diagnostics573,new LinearLayout.LayoutParams(-1,-2));addGap(content,8);\n'
        '  LinearLayout fxActions=new LinearLayout(this);fxActions.setOrientation(LinearLayout.HORIZONTAL);\n'
        '  TextView optionsButton=chip("⚙   Advanced",false);optionsButton.setTag("player-options");optionsButton.setContentDescription("Advanced video enhancement and volume boost");optionsButton.setMinHeight(dp(48));optionsButton.setOnClickListener(v->showPlayerOptions());fxActions.addView(optionsButton,new LinearLayout.LayoutParams(0,-2,1));\n'
        '  TextView enhancedButton=chip("⛶  Enhanced fullscreen",false);enhancedButton.setTag("enhanced-fullscreen-enter");enhancedButton.setContentDescription("Enhanced fullscreen, retains supported video effects");enhancedButton.setMinHeight(dp(48));enhancedButton.setOnClickListener(v->enterEnhancedFullscreen());LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,-2,1);fp.setMargins(dp(8),0,0,0);fxActions.addView(enhancedButton,fp);content.addView(fxActions,new LinearLayout.LayoutParams(-1,-2));addGap(content,20);'
    )

    new_mid_controls = (
        '  });\n'
        '  LinearLayout fxActions=new LinearLayout(this);fxActions.setOrientation(LinearLayout.HORIZONTAL);\n'
        '  TextView optionsButton=chip("⚙   Advanced",false);optionsButton.setTag("player-options");optionsButton.setContentDescription("Advanced video enhancement and volume boost");optionsButton.setMinHeight(dp(44));optionsButton.setOnClickListener(v->showPlayerOptions());fxActions.addView(optionsButton,new LinearLayout.LayoutParams(0,-2,1));\n'
        '  TextView enhancedButton=chip("⛶  Enhanced fullscreen",false);enhancedButton.setTag("enhanced-fullscreen-enter");enhancedButton.setContentDescription("Enhanced fullscreen, retains supported video effects");enhancedButton.setMinHeight(dp(44));enhancedButton.setOnClickListener(v->enterEnhancedFullscreen());LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,-2,1);fp.setMargins(dp(8),0,0,0);fxActions.addView(enhancedButton,fp);toolsDrawer155.addView(fxActions,new LinearLayout.LayoutParams(-1,-2));addGap(toolsDrawer155,8);toolsDrawer155.addView(diagnostics573,new LinearLayout.LayoutParams(-1,-2));'
    )

    ma_text = patch_once(
        ma_text,
        "toolsDrawer155.addView(fxActions",
        old_mid_controls,
        new_mid_controls,
        "MainActivity move fxActions & diagnostics into toolsDrawer155",
    )

    old_watch_body = (
        '  TextView title=text(info.optString("title","Anime"),21,Color.WHITE);title.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(title);addGap(content,7);TextView epText=text("EPISODE "+info.optString("episode","")+"   •   SELECT A SERVER BELOW",11,KuroTheme12.col(0xffb37ee4));content.addView(epText);addGap(content,22);TextView serverTitle=text("Servers & audio",16,Color.WHITE);serverTitle.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(serverTitle);addGap(content,9);\n'
        '  LinearLayout groupRow=new LinearLayout(this);groupRow.setOrientation(LinearLayout.HORIZONTAL);content.addView(groupRow);LinearLayout subCol=new LinearLayout(this),dubCol=new LinearLayout(this),extraCol=new LinearLayout(this);subCol.setOrientation(LinearLayout.VERTICAL);dubCol.setOrientation(LinearLayout.VERTICAL);extraCol.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams subParams=new LinearLayout.LayoutParams(0,-2,1);subParams.setMargins(0,0,dp(6),0);LinearLayout.LayoutParams dubParams=new LinearLayout.LayoutParams(0,-2,1);dubParams.setMargins(dp(6),0,0,0);groupRow.addView(subCol,subParams);groupRow.addView(dubCol,dubParams);TextView subTitle=text("SUB",12,KuroTheme12.col(0xffb37ee4)),dubTitle=text("DUB",12,KuroTheme12.col(0xffb37ee4));subTitle.setTypeface(null,android.graphics.Typeface.BOLD);dubTitle.setTypeface(null,android.graphics.Typeface.BOLD);subCol.addView(subTitle);dubCol.addView(dubTitle);addGap(subCol,8);addGap(dubCol,8);final ArrayList<TextView> buttons=new ArrayList<>();final boolean[] tried=new boolean[50];final int[] active={chosen},countsByLang={0,0,0};final boolean[] waiting={false},lookupDone={info.optBoolean("lookupDone",false)},lookupBlocked={info.optBoolean("blocked",false)};tried[chosen]=true;\n'
        '  TextView noSub=text("Not available yet",12,KuroTheme12.col(0xff91809f)),noDub=text("Not available yet",12,KuroTheme12.col(0xff91809f));subCol.addView(noSub);dubCol.addView(noDub);\n'
        '  java.util.function.BiConsumer<Integer,JSONObject> addButton=(pos,opt)->{\n'
        '   String url=opt.optString("url"),serverRef=opt.optString("referer",ref),kind=opt.optString("type",""),lang=opt.optString("lang","").toLowerCase(Locale.ROOT),note=opt.optString("note","");\n'
        '   boolean isSub=lang.equals("sub"),isDub=lang.equals("dub");LinearLayout column=isSub?subCol:isDub?dubCol:extraCol;int index=isSub?0:isDub?1:2;int num=++countsByLang[index];if(isSub)noSub.setVisibility(View.GONE);if(isDub)noDub.setVisibility(View.GONE);\n'
        '   String displayName="Server "+num+(note.isEmpty()?"":" • "+(note.length()>35?note.substring(0,35):note));TextView btn=chip(displayName,pos==chosen);buttons.add(btn);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(7));column.addView(btn,lp);\n'
        '   btn.setOnClickListener(v->{verification[0]=false;if(loadPlayerOption561(opt,ref)){if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.GONE);Arrays.fill(tried,false);tried[pos]=true;active[0]=pos;waiting[0]=false;for(int j=0;j<buttons.size();j++){boolean on=j==pos;buttons.get(j).setTextColor(on?KuroTheme12.col(0xff12081b):KuroTheme12.col(0xffe3dce9));buttons.get(j).setBackground(rounded(on?KuroTheme12.col(0xffae71e7):KuroTheme12.col(0xff281b32),on?0:KuroTheme12.col(0xff4c3a59)));}}});\n'
        '  };\n'
        '  for(int i=0;i<options.length();i++)addButton.accept(i,options.getJSONObject(i));\n'
        '  retryServer[0]=()->{\n'
        '   if(player==null||playerLayer==null||verification[0])return;\n'
        '   try{\n'
        '    if(lookupBlocked[0]){waiting[0]=false;if(connectionText[0]!=null)connectionText[0].setText("Verification is required. Check that library manually.");return;}\n'
        '    String currentLang=options.getJSONObject(active[0]).optString("lang","");int next=RetrySelector.next(options,tried,currentLang,"dub".equals(settings560().getString("audio","dub"))?KuroPreferences560.preferredServers(MainActivity.this):null);\n'
        '    if(next<0){waiting[0]=!lookupDone[0];if(connectionText[0]!=null)connectionText[0].setText(waiting[0]?"Connecting to another source…":"No working exact-match source was detected. Try another server.");return;}\n'
        '    tried[next]=true;JSONObject target=options.getJSONObject(next);if(!loadPlayerOption561(target,ref)){main.post(retryServer[0]);return;}\n'
        '    active[0]=next;waiting[0]=false;if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.GONE);for(int j=0;j<buttons.size();j++){boolean on=j==next;buttons.get(j).setTextColor(on?KuroTheme12.col(0xff12081b):KuroTheme12.col(0xffe3dce9));buttons.get(j).setBackground(rounded(on?KuroTheme12.col(0xffae71e7):KuroTheme12.col(0xff281b32),on?0:KuroTheme12.col(0xff4c3a59)));}\n'
        '   }catch(Exception ignored){}\n'
        '  };\n'
        '  watchSink=(incomingSession,update)->{\n'
        '   if(!session.equals(incomingSession)||player==null||playerLayer==null)return;\n'
        '   JSONArray found=update.optJSONArray("servers");if(found!=null)for(int k=0;k<found.length()&&options.length()<50;k++){\n'
        '    JSONObject item=found.optJSONObject(k);if(item==null)continue;String url=item.optString("url"),lang=item.optString("lang","");Uri u=Uri.parse(url);if(!"https".equals(u.getScheme())||!playerHost(u.getHost())||u.getUserInfo()!=null||!("sub".equals(lang)||"dub".equals(lang)))continue;\n'
        '    boolean dup=false;for(int j=0;j<options.length();j++){JSONObject old=options.optJSONObject(j);if(old!=null&&url.equals(old.optString("url"))&&lang.equals(old.optString("lang"))){dup=true;break;}}if(dup)continue;\n'
        '    int index=options.length();options.put(item);addButton.accept(index,item);\n'
        '   }\n'
        '   if(update.optBoolean("blocked",false))lookupBlocked[0]=true;if(update.optBoolean("done",false))lookupDone[0]=true;\n'
        '   if(waiting[0])retryServer[0].run();\n'
        '  };\n'
        '  if(countsByLang[2]>0){addGap(content,8);TextView moreTitle=text("OTHER AUDIO",12,KuroTheme12.col(0xffb37ee4));moreTitle.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(moreTitle);addGap(content,8);content.addView(extraCol);}\n'
        '  addGap(content,22);TextView epTitle=text("Episodes",16,Color.WHITE);epTitle.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(epTitle);addGap(content,6);TextView epHint=text("Search by number or name • "+eps.length()+" available",12,KuroTheme12.col(0xffa79ab2));content.addView(epHint);addGap(content,10);\n'
        '  EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(KuroTheme12.col(0xff907f9d));search.setHint("Search episodes");search.setTextSize(14);search.setPadding(dp(12),0,dp(12),0);search.setBackground(rounded(KuroTheme12.col(0xff24172e),KuroTheme12.col(0xff4f3f5c)));content.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));addGap(content,12);\n'
        '  LinearLayout pagination=new LinearLayout(this);pagination.setGravity(Gravity.CENTER_VERTICAL);content.addView(pagination);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);final int[] page={current/100};final ArrayList<Integer> matches=new ArrayList<>();\n'
        '  Runnable refresh=()->{matches.clear();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(int i=0;i<eps.length();i++){JSONObject ep=eps.optJSONObject(i);String num=ep!=null?ep.optString("number",""):eps.optString(i,"");String name=ep!=null?ep.optString("name",""):"";if(query.isEmpty()||num.toLowerCase(Locale.ROOT).contains(query)||name.toLowerCase(Locale.ROOT).contains(query))matches.add(i);}int pages=Math.max(1,(matches.size()+99)/100);if(page[0]>=pages)page[0]=pages-1;pagination.removeAllViews();list.removeAllViews();if(matches.size()>100){TextView prev=chip("‹",false),indicator=text("   "+(page[0]+1)+" / "+pages+"   ",12,KuroTheme12.col(0xffb9aac6)),next=chip("›",false);pagination.addView(prev);pagination.addView(indicator);pagination.addView(next);prev.setOnClickListener(v->{if(page[0]>0){page[0]--;pagination.post(()->((Runnable)list.getTag()).run());}});next.setOnClickListener(v->{if(page[0]+1<pages){page[0]++;pagination.post(()->((Runnable)list.getTag()).run());}});addGap(list,10);}if(matches.isEmpty())list.addView(text("No matching episodes.",13,KuroTheme12.col(0xffa79ab2)));for(int k=page[0]*100;k<Math.min((page[0]+1)*100,matches.size());k++){int idx=matches.get(k);JSONObject ep=eps.optJSONObject(idx);String num=ep!=null?ep.optString("number","?"):eps.optString(idx,"?");String name=ep!=null?ep.optString("name","").trim():"";if(num.length()>20)num=num.substring(0,20);if(name.length()>160)name=name.substring(0,160);if(name.isEmpty())name="Episode "+num;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),0,dp(12),0);row.setBackgroundColor(idx==current?KuroTheme12.col(0xff39234b):(k%2==0?KuroTheme12.col(0xff24182d):KuroTheme12.col(0xff1c1124)));TextView number=text(num,14,idx==current?KuroTheme12.col(0xffbf88f2):KuroTheme12.col(0xffb2a6bc));number.setGravity(Gravity.CENTER);row.addView(number,new LinearLayout.LayoutParams(dp(36),-1));TextView titleLabel=text(name,14,idx==current?KuroTheme12.col(0xffcfa2fb):KuroTheme12.col(0xffe6e1eb));titleLabel.setPadding(dp(12),0,0,0);row.addView(titleLabel,new LinearLayout.LayoutParams(0,-2,1));if(idx==current)row.addView(text("▶",14,KuroTheme12.col(0xffae73e5)));list.addView(row,new LinearLayout.LayoutParams(-1,dp(54)));row.setOnClickListener(v->{if(idx==current)return;closePlayer();ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+idx+")",null);});}};\n'
        '  list.setTag(refresh);search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){page[0]=0;refresh.run();}public void afterTextChanged(android.text.Editable e){}});refresh.run();\n'
        '  addGap(content,24);JSONObject anime=info.optJSONObject("animeInfo");if(anime==null)anime=new JSONObject();\n'
        '  TextView detailHeading=text("Anime details",16,KuroTheme12.col(0xfff3eef7));detailHeading.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(detailHeading);addGap(content,10);\n'
        '  LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.HORIZONTAL);detail.setPadding(dp(12),dp(12),dp(12),dp(12));detail.setBackground(rounded(KuroTheme12.col(0xff21142a),KuroTheme12.col(0xff3f2f4c)));content.addView(detail,new LinearLayout.LayoutParams(-1,-2));\n'
        '  ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);poster.setBackgroundColor(KuroTheme12.col(0xff372447));LinearLayout.LayoutParams posterLP=new LinearLayout.LayoutParams(dp(76),dp(114));posterLP.setMargins(0,0,dp(14),0);detail.addView(poster,posterLP);loadPoster(poster,anime.optString("image",""));\n'
        '  LinearLayout detailText=new LinearLayout(this);detailText.setOrientation(LinearLayout.VERTICAL);detail.addView(detailText,new LinearLayout.LayoutParams(0,-2,1));TextView detailName=text(info.optString("title","Anime"),18,Color.WHITE);detailName.setTypeface(null,android.graphics.Typeface.BOLD);detailText.addView(detailName);addGap(detailText,7);\n'
        '  JSONObject counts=anime.optJSONObject("counts");StringBuilder facts=new StringBuilder();if(counts!=null){String sub=counts.optString("sub","");String dub=counts.optString("dub","");String total=counts.optString("total","");if(sub.matches("[0-9]{1,5}"))facts.append("CC ").append(sub);if(dub.matches("[0-9]{1,5}")){if(facts.length()>0)facts.append("  •  ");facts.append("DUB ").append(dub);}if(total.matches("[0-9]{1,5}")){if(facts.length()>0)facts.append("  •  ");facts.append(total).append(" EPS");}}String meta=anime.optString("meta","").trim();if(!meta.isEmpty()){if(facts.length()>0)facts.append("  •  ");facts.append(meta.length()>80?meta.substring(0,80):meta);}if(facts.length()>0){detailText.addView(text(facts.toString(),11,KuroTheme12.col(0xffb98ae5)));addGap(detailText,8);}String desc=anime.optString("description","").trim();if(desc.length()>3000)desc=desc.substring(0,3000);TextView synopsis=text(desc.isEmpty()?"Description not available from this source.":desc,13,KuroTheme12.col(0xffdcd5e2));synopsis.setLineSpacing(dp(3),1);detailText.addView(synopsis);addGap(detailText,10);TextView more=chip("View details",false);detailText.addView(more);more.setOnClickListener(v->{closePlayer();ui.evaluateJavascript("window.kuroOpenDetails&&window.kuroOpenDetails()",null);});\n'
        '  addGap(content,10);TextView note=text("Playback uses a third-party player. Autoplay is best effort; tap play if needed. Use its fullscreen control to expand.",12,KuroTheme12.col(0xffa08fae));content.addView(note);'
    )

    new_watch_body = (
        '  /* k155 Watch Screen v3: Compact SUB/DUB Server Switcher + Rich 3-Row Playlist + Anime Details + More Anime */\n'
        '  JSONObject anime=info.optJSONObject("animeInfo");if(anime==null)anime=new JSONObject();\n'
        '  final ConcurrentHashMap<String,String> epThumbs155=new ConcurrentHashMap<>();\n'
        '  final String[] epFallbackImg155={!info.optString("episodeImage","").isEmpty()?info.optString("episodeImage",""):anime.optString("image","")};\n'
        '  final int[] epDurMin155={24};\n'
        '  LinearLayout serverCard155=new LinearLayout(this);serverCard155.setOrientation(LinearLayout.VERTICAL);serverCard155.setTag("server-card-155");serverCard155.setPadding(dp(12),dp(11),dp(12),dp(12));serverCard155.setBackground(watchShape154(KuroTheme12.col(0xff1b1124),KuroTheme12.col(0xff3c2b4b),14));content.addView(serverCard155,new LinearLayout.LayoutParams(-1,-2));\n'
        '  LinearLayout srvHeadRow155=new LinearLayout(this);srvHeadRow155.setOrientation(LinearLayout.HORIZONTAL);srvHeadRow155.setGravity(Gravity.CENTER_VERTICAL);serverCard155.addView(srvHeadRow155,new LinearLayout.LayoutParams(-1,-2));\n'
        '  TextView serverTitle=text("Server Switcher",15,Color.WHITE);serverTitle.setTypeface(null,android.graphics.Typeface.BOLD);srvHeadRow155.addView(serverTitle,new LinearLayout.LayoutParams(0,-2,1));\n'
        '  TextView toolsToggle155=text("⚙ Tools",11,KuroTheme12.col(0xffd8b8f8));toolsToggle155.setTypeface(null,android.graphics.Typeface.BOLD);toolsToggle155.setGravity(Gravity.CENTER);toolsToggle155.setPadding(dp(10),dp(4),dp(10),dp(4));toolsToggle155.setBackground(watchShape154(KuroTheme12.col(0xff281a35),KuroTheme12.col(0xff4c385e),999));toolsToggle155.setOnClickListener(v->{boolean open=toolsDrawer155.getVisibility()!=View.VISIBLE;toolsDrawer155.setVisibility(open?View.VISIBLE:View.GONE);toolsToggle155.setBackground(watchShape154(open?KuroTheme12.col(0xff4b296d):KuroTheme12.col(0xff281a35),open?KuroTheme12.col(0xffa96de8):KuroTheme12.col(0xff4c385e),999));});srvHeadRow155.addView(toolsToggle155,new LinearLayout.LayoutParams(-2,-2));\n'
        '  TextView diagIcon155=text("ⓘ",13,KuroTheme12.col(0xffcbb4e3));diagIcon155.setTypeface(null,android.graphics.Typeface.BOLD);diagIcon155.setGravity(Gravity.CENTER);diagIcon155.setBackground(watchShape154(KuroTheme12.col(0xff281a35),KuroTheme12.col(0xff4c385e),999));diagIcon155.setContentDescription("Playback help and diagnostics");diagIcon155.setOnClickListener(v->diagnostics573.performClick());LinearLayout.LayoutParams diLp155=new LinearLayout.LayoutParams(dp(28),dp(28));diLp155.setMargins(dp(6),0,0,0);srvHeadRow155.addView(diagIcon155,diLp155);\n'
        '  addGap(serverCard155,10);\n'
        '  LinearLayout segBar155=new LinearLayout(this);segBar155.setOrientation(LinearLayout.HORIZONTAL);segBar155.setPadding(dp(3),dp(3),dp(3),dp(3));segBar155.setBackground(watchShape154(KuroTheme12.col(0xff120b19),KuroTheme12.col(0xff332341),10));serverCard155.addView(segBar155,new LinearLayout.LayoutParams(-1,dp(36)));\n'
        '  TextView subTab155=text("SUB",12,Color.WHITE),dubTab155=text("DUB",12,Color.WHITE);subTab155.setTypeface(null,android.graphics.Typeface.BOLD);dubTab155.setTypeface(null,android.graphics.Typeface.BOLD);subTab155.setGravity(Gravity.CENTER);dubTab155.setGravity(Gravity.CENTER);segBar155.addView(subTab155,new LinearLayout.LayoutParams(0,-1,1));segBar155.addView(dubTab155,new LinearLayout.LayoutParams(0,-1,1));\n'
        '  addGap(serverCard155,10);\n'
        '  final HorizontalScrollView srvHScroll155=new HorizontalScrollView(this){private float sx,sy;@Override public boolean dispatchTouchEvent(MotionEvent ev){int a=ev.getActionMasked();if(a==MotionEvent.ACTION_DOWN){sx=ev.getX();sy=ev.getY();}else if(a==MotionEvent.ACTION_MOVE){if(Math.abs(ev.getX()-sx)>Math.abs(ev.getY()-sy)&&(canScrollHorizontally(1)||canScrollHorizontally(-1))){ViewParent p=getParent();if(p!=null)p.requestDisallowInterceptTouchEvent(true);}}return super.dispatchTouchEvent(ev);}};\n'
        '  srvHScroll155.setHorizontalScrollBarEnabled(false);LinearLayout srvChipRow155=new LinearLayout(this);srvChipRow155.setOrientation(LinearLayout.HORIZONTAL);srvChipRow155.setGravity(Gravity.CENTER_VERTICAL);srvHScroll155.addView(srvChipRow155,new FrameLayout.LayoutParams(-2,-2));serverCard155.addView(srvHScroll155,new LinearLayout.LayoutParams(-1,-2));\n'
        '  LinearLayout extraCol=new LinearLayout(this);extraCol.setOrientation(LinearLayout.VERTICAL);content.addView(toolsDrawer155,new LinearLayout.LayoutParams(-1,-2));\n'
        '  String initChosenLang155=options.getJSONObject(chosen).optString("lang","sub").toLowerCase(Locale.ROOT);\n'
        '  final int[] selectedLang155={"dub".equals(initChosenLang155)?1:0},serverPage154={0},active={chosen},countsByLang={0,0,0};\n'
        '  final ArrayList<TextView> buttons=new ArrayList<>();final ArrayList<Integer> subIdx155=new ArrayList<>(),dubIdx155=new ArrayList<>();final String[] baseLabels155=new String[50];final int[] posLang154=new int[50],posSlot154=new int[50];\n'
        '  final boolean[] tried=new boolean[50],waiting={false},lookupDone={info.optBoolean("lookupDone",false)},lookupBlocked={info.optBoolean("blocked",false)};tried[chosen]=true;\n'
        '  final Runnable[] syncServers154={null};\n'
        '  syncServers154[0]=()->{\n'
        '   if(selectedLang155[0]==0&&subIdx155.isEmpty()&&!dubIdx155.isEmpty())selectedLang155[0]=1;\n'
        '   else if(selectedLang155[0]==1&&dubIdx155.isEmpty()&&!subIdx155.isEmpty())selectedLang155[0]=0;\n'
        '   boolean subSel=selectedLang155[0]==0;\n'
        '   subTab155.setText(subIdx155.isEmpty()?"SUB":("SUB  •  "+subIdx155.size()));dubTab155.setText(dubIdx155.isEmpty()?"DUB":("DUB  •  "+dubIdx155.size()));\n'
        '   subTab155.setTextColor(subSel?KuroTheme12.col(0xfff6edff):KuroTheme12.col(0xff9e8eb0));subTab155.setBackground(subSel?watchShape154(KuroTheme12.col(0xff4c2b6f),KuroTheme12.col(0xff9e65da),8):null);\n'
        '   dubTab155.setTextColor(!subSel?KuroTheme12.col(0xfff6edff):KuroTheme12.col(0xff9e8eb0));dubTab155.setBackground(!subSel?watchShape154(KuroTheme12.col(0xff4c2b6f),KuroTheme12.col(0xff9e65da),8):null);\n'
        '   ArrayList<Integer> poolIdx=subSel?subIdx155:dubIdx155;\n'
        '   int srvPages=Math.max(1,(poolIdx.size()+4)/5);\n'
        '   if(serverPage154[0]>=srvPages)serverPage154[0]=srvPages-1;if(serverPage154[0]<0)serverPage154[0]=0;\n'
        '   srvChipRow155.removeAllViews();\n'
        '   if(poolIdx.isEmpty()){TextView emptySrv=text(subSel?"No SUB servers available yet":"No DUB servers available yet",12,KuroTheme12.col(0xff9a8aa9));emptySrv.setPadding(dp(4),dp(6),dp(4),dp(6));srvChipRow155.addView(emptySrv);return;}\n'
        '   if(serverPage154[0]>0){\n'
        '    String prevLbl=serverPage154[0]==1?"‹ Servers 1–5":("‹ Servers "+((serverPage154[0]-1)*5+1)+"–"+(serverPage154[0]*5));\n'
        '    TextView prevSrv=text(prevLbl,12,KuroTheme12.col(0xffd8b8f8));prevSrv.setTag("server-prev-154");prevSrv.setTypeface(null,android.graphics.Typeface.BOLD);prevSrv.setGravity(Gravity.CENTER);prevSrv.setMinHeight(dp(36));prevSrv.setPadding(dp(12),0,dp(12),0);prevSrv.setBackground(watchShape154(KuroTheme12.col(0xff261933),KuroTheme12.col(0xff4c385e),9));\n'
        '    LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(-2,dp(36));plp.setMargins(0,0,dp(8),0);prevSrv.setOnClickListener(v->{serverPage154[0]--;syncServers154[0].run();srvHScroll155.scrollTo(0,0);});srvChipRow155.addView(prevSrv,plp);\n'
        '   }\n'
        '   int pStart=serverPage154[0]*5,pEnd=Math.min(poolIdx.size(),pStart+5);\n'
        '   for(int k=pStart;k<pEnd;k++){\n'
        '    int pos=poolIdx.get(k);TextView btn=buttons.get(pos);if(btn.getParent() instanceof ViewGroup)((ViewGroup)btn.getParent()).removeView(btn);\n'
        '    boolean on=pos==active[0];String base=baseLabels155[pos]==null?("Server "+(k+1)):baseLabels155[pos];\n'
        '    btn.setText(on?("●  "+base):base);btn.setTextSize(12);btn.setTypeface(null,on?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);\n'
        '    btn.setTextColor(on?KuroTheme12.col(0xfffaf4ff):KuroTheme12.col(0xffe5deec));btn.setMinHeight(dp(36));btn.setPadding(dp(13),0,dp(13),0);btn.setGravity(Gravity.CENTER);\n'
        '    btn.setBackground(watchShape154(on?KuroTheme12.col(0xff552e7e):KuroTheme12.col(0xff23172e),on?KuroTheme12.col(0xffb478ee):KuroTheme12.col(0xff443253),9));\n'
        '    LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-2,dp(36));if(k+1<pEnd||serverPage154[0]+1<srvPages)clp.setMargins(0,0,dp(8),0);srvChipRow155.addView(btn,clp);\n'
        '   }\n'
        '   if(serverPage154[0]+1<srvPages){\n'
        '    TextView otherBtn=text("Other servers ›",12,KuroTheme12.col(0xffd8b8f8));otherBtn.setTag("other-servers-154");otherBtn.setTypeface(null,android.graphics.Typeface.BOLD);otherBtn.setGravity(Gravity.CENTER);otherBtn.setMinHeight(dp(36));otherBtn.setPadding(dp(13),0,dp(13),0);otherBtn.setBackground(watchShape154(KuroTheme12.col(0xff261933),KuroTheme12.col(0xff4c385e),9));\n'
        '    otherBtn.setOnClickListener(v->{serverPage154[0]++;syncServers154[0].run();srvHScroll155.scrollTo(0,0);});srvChipRow155.addView(otherBtn,new LinearLayout.LayoutParams(-2,dp(36)));\n'
        '   }\n'
        '  };\n'
        '  subTab155.setOnClickListener(v->{if(selectedLang155[0]==0)return;selectedLang155[0]=0;serverPage154[0]=(active[0]>=0&&active[0]<50&&posLang154[active[0]]==0)?(posSlot154[active[0]]/5):0;syncServers154[0].run();srvHScroll155.scrollTo(0,0);});\n'
        '  dubTab155.setOnClickListener(v->{if(selectedLang155[0]==1)return;selectedLang155[0]=1;serverPage154[0]=(active[0]>=0&&active[0]<50&&posLang154[active[0]]==1)?(posSlot154[active[0]]/5):0;syncServers154[0].run();srvHScroll155.scrollTo(0,0);});\n'
        '  java.util.function.BiConsumer<Integer,JSONObject> addButton=(pos,opt)->{\n'
        '   String lang=opt.optString("lang","").toLowerCase(Locale.ROOT),note=opt.optString("note","").trim();\n'
        '   boolean isSub=lang.equals("sub"),isDub=lang.equals("dub");int index=isSub?0:isDub?1:2;int num=++countsByLang[index];\n'
        '   String shortNote=note.length()>22?note.substring(0,22):note;String base="Server "+num+(shortNote.isEmpty()?"":" • "+shortNote);\n'
        '   TextView btn=chip(base,pos==chosen);buttons.add(btn);\n'
        '   if(pos>=0&&pos<50){posLang154[pos]=index;posSlot154[pos]=num-1;baseLabels155[pos]=base;}\n'
        '   if(isSub)subIdx155.add(pos);else if(isDub)dubIdx155.add(pos);else{LinearLayout.LayoutParams elp=new LinearLayout.LayoutParams(-1,-2);elp.setMargins(0,0,0,dp(6));extraCol.addView(btn,elp);}\n'
        '   btn.setOnClickListener(v->{verification[0]=false;if(loadPlayerOption561(opt,ref)){if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.GONE);Arrays.fill(tried,false);tried[pos]=true;active[0]=pos;waiting[0]=false;if(index<2){selectedLang155[0]=index;serverPage154[0]=posSlot154[pos]/5;}syncServers154[0].run();}});\n'
        '   if(syncServers154[0]!=null)syncServers154[0].run();\n'
        '  };\n'
        '  for(int i=0;i<options.length();i++)addButton.accept(i,options.getJSONObject(i));\n'
        '  if(chosen>=0&&chosen<50&&posLang154[chosen]<2){selectedLang155[0]=posLang154[chosen];serverPage154[0]=posSlot154[chosen]/5;}\n'
        '  syncServers154[0].run();\n'
        '  retryServer[0]=()->{\n'
        '   if(player==null||playerLayer==null||verification[0])return;\n'
        '   try{\n'
        '    if(lookupBlocked[0]){waiting[0]=false;if(connectionText[0]!=null)connectionText[0].setText("Verification is required. Check that library manually.");return;}\n'
        '    String currentLang=options.getJSONObject(active[0]).optString("lang","");int next=RetrySelector.next(options,tried,currentLang,"dub".equals(settings560().getString("audio","dub"))?KuroPreferences560.preferredServers(MainActivity.this):null);\n'
        '    if(next<0){waiting[0]=!lookupDone[0];if(connectionText[0]!=null)connectionText[0].setText(waiting[0]?"Connecting to another source…":"No working exact-match source was detected. Try another server.");return;}\n'
        '    tried[next]=true;JSONObject target=options.getJSONObject(next);if(!loadPlayerOption561(target,ref)){main.post(retryServer[0]);return;}\n'
        '    active[0]=next;waiting[0]=false;if(connectionCover[0]!=null)connectionCover[0].setVisibility(View.GONE);\n'
        '    if(next>=0&&next<50&&posLang154[next]<2){selectedLang155[0]=posLang154[next];serverPage154[0]=posSlot154[next]/5;}\n'
        '    syncServers154[0].run();\n'
        '   }catch(Exception ignored){}\n'
        '  };\n'
        '  watchSink=(incomingSession,update)->{\n'
        '   if(!session.equals(incomingSession)||player==null||playerLayer==null)return;\n'
        '   JSONArray found=update.optJSONArray("servers");if(found!=null)for(int k=0;k<found.length()&&options.length()<50;k++){\n'
        '    JSONObject item=found.optJSONObject(k);if(item==null)continue;String url=item.optString("url"),lang=item.optString("lang","");Uri u=Uri.parse(url);if(!"https".equals(u.getScheme())||!playerHost(u.getHost())||u.getUserInfo()!=null||!("sub".equals(lang)||"dub".equals(lang)))continue;\n'
        '    boolean dup=false;for(int j=0;j<options.length();j++){JSONObject old=options.optJSONObject(j);if(old!=null&&url.equals(old.optString("url"))&&lang.equals(old.optString("lang"))){dup=true;break;}}if(dup)continue;\n'
        '    int index=options.length();options.put(item);addButton.accept(index,item);\n'
        '   }\n'
        '   if(update.optBoolean("blocked",false))lookupBlocked[0]=true;if(update.optBoolean("done",false))lookupDone[0]=true;\n'
        '   if(waiting[0])retryServer[0].run();\n'
        '  };\n'
        '  if(countsByLang[2]>0){addGap(content,8);TextView moreTitle=text("OTHER AUDIO",12,KuroTheme12.col(0xffb37ee4));moreTitle.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(moreTitle);addGap(content,6);content.addView(extraCol);}\n'
        '  addGap(content,14);\n'
        '  LinearLayout playlistCard154=new LinearLayout(this);playlistCard154.setOrientation(LinearLayout.VERTICAL);playlistCard154.setPadding(dp(12),dp(11),dp(12),dp(12));playlistCard154.setBackground(watchShape154(KuroTheme12.col(0xff1b1124),KuroTheme12.col(0xff3c2b4b),14));content.addView(playlistCard154,new LinearLayout.LayoutParams(-1,-2));\n'
        '  LinearLayout epTopRow154=new LinearLayout(this);epTopRow154.setOrientation(LinearLayout.HORIZONTAL);epTopRow154.setGravity(Gravity.CENTER_VERTICAL);playlistCard154.addView(epTopRow154,new LinearLayout.LayoutParams(-1,-2));\n'
        '  TextView epTitle=text("Episodes",15,Color.WHITE);epTitle.setTypeface(null,android.graphics.Typeface.BOLD);epTopRow154.addView(epTitle);\n'
        '  TextView epCountPill154=text((Math.min(current+1,Math.max(1,eps.length())))+" / "+Math.max(1,eps.length()),11,KuroTheme12.col(0xffd7b4fc));epCountPill154.setTypeface(null,android.graphics.Typeface.BOLD);epCountPill154.setPadding(dp(9),dp(3),dp(9),dp(3));epCountPill154.setBackground(watchShape154(KuroTheme12.col(0xff281a35),KuroTheme12.col(0xff4c385e),999));LinearLayout.LayoutParams cntLp155=new LinearLayout.LayoutParams(-2,-2);cntLp155.setMargins(dp(8),0,0,0);epTopRow154.addView(epCountPill154,cntLp155);\n'
        '  epTopRow154.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));\n'
        '  final boolean[] epExpanded155={false};final TextView seeAllEps155=text(eps.length()>3?"See all ›":"",12,KuroTheme12.col(0xffc993fc));seeAllEps155.setTypeface(null,android.graphics.Typeface.BOLD);if(eps.length()<=3)seeAllEps155.setVisibility(View.GONE);epTopRow154.addView(seeAllEps155);\n'
        '  addGap(playlistCard154,9);\n'
        '  EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(KuroTheme12.col(0xff8e7d9b));search.setHint("Search episodes");search.setTextSize(13);search.setPadding(dp(12),0,dp(12),0);search.setBackground(watchShape154(KuroTheme12.col(0xff23172e),KuroTheme12.col(0xff443253),9));playlistCard154.addView(search,new LinearLayout.LayoutParams(-1,dp(38)));addGap(playlistCard154,9);\n'
        '  LinearLayout pagination=new LinearLayout(this);pagination.setGravity(Gravity.CENTER_VERTICAL);playlistCard154.addView(pagination);\n'
        '  final ScrollView epScroll154=new ScrollView(this){@Override public boolean dispatchTouchEvent(MotionEvent ev){int act=ev.getActionMasked();if((act==MotionEvent.ACTION_DOWN||act==MotionEvent.ACTION_MOVE)&&(canScrollVertically(1)||canScrollVertically(-1))){ViewParent p=getParent();if(p!=null)p.requestDisallowInterceptTouchEvent(true);}return super.dispatchTouchEvent(ev);}};\n'
        '  epScroll154.setTag("ep-playlist-scroll-154");epScroll154.setVerticalScrollBarEnabled(true);epScroll154.setScrollbarFadingEnabled(false);\n'
        '  LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);epScroll154.addView(list,new FrameLayout.LayoutParams(-1,-2));playlistCard154.addView(epScroll154,new LinearLayout.LayoutParams(-1,-2));\n'
        '  final int[] page={current/500};final boolean[] firstEpScroll154={true};final ArrayList<Integer> matches=new ArrayList<>();final String showId155=info.optString("showIdentity","");\n'
        '  Runnable refresh=()->{matches.clear();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);for(int i=0;i<eps.length();i++){JSONObject ep=eps.optJSONObject(i);String num=ep!=null?ep.optString("number",""):eps.optString(i,"");String name=ep!=null?ep.optString("name",""):"";if(query.isEmpty()||num.toLowerCase(Locale.ROOT).contains(query)||name.toLowerCase(Locale.ROOT).contains(query))matches.add(i);}int pages=Math.max(1,(matches.size()+499)/500);if(page[0]>=pages)page[0]=pages-1;pagination.removeAllViews();list.removeAllViews();if(matches.size()>500){TextView prev=chip("‹",false),indicator=text("   "+(page[0]+1)+" / "+pages+"   ",12,KuroTheme12.col(0xffb9aac6)),next=chip("›",false);pagination.addView(prev);pagination.addView(indicator);pagination.addView(next);prev.setOnClickListener(v->{if(page[0]>0){page[0]--;pagination.post(()->((Runnable)list.getTag()).run());}});next.setOnClickListener(v->{if(page[0]+1<pages){page[0]++;pagination.post(()->((Runnable)list.getTag()).run());}});addGap(pagination,8);}if(matches.isEmpty()){TextView emptyEp=text("No matching episodes.",13,KuroTheme12.col(0xffa79ab2));emptyEp.setPadding(dp(12),dp(14),dp(12),dp(14));list.addView(emptyEp);}int activeInSlice=-1,sliceCount=0;\n'
        '   for(int k=page[0]*500;k<Math.min((page[0]+1)*500,matches.size());k++){int idx=matches.get(k);JSONObject ep=eps.optJSONObject(idx);String num=ep!=null?ep.optString("number","?"):eps.optString(idx,"?");String name=ep!=null?ep.optString("name","").trim():"";if(num.length()>20)num=num.substring(0,20);if(name.length()>160)name=name.substring(0,160);if(name.isEmpty())name="Episode "+num;\n'
        '    boolean isCur=idx==current;if(isCur)activeInSlice=sliceCount;\n'
        '    long resumeMs=(!showId155.isEmpty()&&!num.isEmpty())?KuroPlayback561.read(this,KuroPlayback561.key(ref,showId155,num)):0L;\n'
        '    long durMs=Math.max(5,epDurMin155[0])*60000L;\n'
        '    boolean isWatched=(ep!=null&&ep.optInt("w",0)==1)||(resumeMs>=durMs*85L/100L);\n'
        '    boolean isFiller=ep!=null&&(ep.optInt("f",0)==1||ep.optBoolean("filler154",false));\n'
        '    boolean isRecap=!isFiller&&ep!=null&&ep.optInt("r",0)==1;\n'
        '    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(6),dp(10),dp(6));\n'
        '    row.setBackground(watchShape154(isCur?KuroTheme12.col(0xff2c1a40):(sliceCount%2==0?KuroTheme12.col(0xff22162d):KuroTheme12.col(0xff1d1227)),isCur?KuroTheme12.col(0xffac70ea):KuroTheme12.col(0xff332341),10));\n'
        '    FrameLayout thumbBox=new FrameLayout(this);thumbBox.setBackground(watchShape154(KuroTheme12.col(0xff2b1c38),KuroTheme12.col(0xff422f52),7));thumbBox.setClipToOutline(true);thumbBox.setOutlineProvider(ViewOutlineProvider.BACKGROUND);\n'
        '    ImageView thumbIv=new ImageView(this);thumbIv.setScaleType(ImageView.ScaleType.CENTER_CROP);thumbBox.addView(thumbIv,new FrameLayout.LayoutParams(-1,-1));\n'
        '    String tUrl=epThumbs155.get(num);if(tUrl==null||tUrl.isEmpty())tUrl=epFallbackImg155[0];loadThumb154(thumbIv,tUrl,240,136);\n'
        '    TextView numBadge=text(num,11,Color.WHITE);numBadge.setTypeface(null,android.graphics.Typeface.BOLD);numBadge.setPadding(dp(5),dp(1),dp(5),dp(1));numBadge.setBackground(watchShape154(Color.argb(205,10,7,16),0,4));FrameLayout.LayoutParams nbLp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);nbLp.setMargins(dp(4),dp(4),dp(4),dp(4));thumbBox.addView(numBadge,nbLp);\n'
        '    row.addView(thumbBox,new LinearLayout.LayoutParams(dp(82),dp(46)));\n'
        '    LinearLayout midCol=new LinearLayout(this);midCol.setOrientation(LinearLayout.VERTICAL);midCol.setPadding(dp(10),0,dp(8),0);\n'
        '    TextView titleLabel=text(name,13,isCur?Color.WHITE:KuroTheme12.col(0xffeae4f0));titleLabel.setTypeface(null,isCur?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);titleLabel.setSingleLine(true);titleLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);midCol.addView(titleLabel);\n'
        '    addGap(midCol,5);\n'
        '    LinearLayout progRow=new LinearLayout(this);progRow.setOrientation(LinearLayout.HORIZONTAL);progRow.setGravity(Gravity.CENTER_VERTICAL);\n'
        '    float frac=isWatched?1f:(resumeMs>0?Math.max(0.10f,Math.min(0.95f,(float)resumeMs/(float)durMs)):(isCur?0.38f:0f));\n'
        '    FrameLayout progTrack=new FrameLayout(this);progTrack.setBackground(watchShape154(KuroTheme12.col(0xff352445),0,999));\n'
        '    if(frac>0f){View progFill=new View(this);progFill.setBackground(watchShape154(KuroTheme12.col(0xffb478ee),0,999));progTrack.addView(progFill,new FrameLayout.LayoutParams(dp(Math.max(4,Math.round(64f*frac))),dp(4)));}\n'
        '    LinearLayout.LayoutParams ptLp=new LinearLayout.LayoutParams(dp(64),dp(4));ptLp.setMargins(0,0,dp(8),0);progRow.addView(progTrack,ptLp);\n'
        '    String subMeta=isCur?"Now Playing":(resumeMs>0&&!isWatched?(Math.max(1L,(durMs-resumeMs)/60000L)+"m left"):(epDurMin155[0]+"m"));\n'
        '    TextView metaLbl=text(subMeta,10,isCur?KuroTheme12.col(0xffcb96fc):KuroTheme12.col(0xff9e8fb0));if(isCur)metaLbl.setTypeface(null,android.graphics.Typeface.BOLD);progRow.addView(metaLbl);\n'
        '    midCol.addView(progRow);row.addView(midCol,new LinearLayout.LayoutParams(0,-2,1));\n'
        '    if(isCur){TextView eq=text("▮▮▮",11,KuroTheme12.col(0xffc993fc));eq.setTypeface(null,android.graphics.Typeface.BOLD);row.addView(eq);}else if(isFiller){TextView fp2=text("FILLER",10,Color.rgb(251,191,36));fp2.setTypeface(null,android.graphics.Typeface.BOLD);fp2.setPadding(dp(8),dp(3),dp(8),dp(3));fp2.setBackground(watchShape154(Color.argb(40,245,158,11),Color.argb(145,251,191,36),999));row.addView(fp2);}else if(isRecap){TextView rp2=text("RECAP",10,KuroTheme12.col(0xffdfbfff));rp2.setTypeface(null,android.graphics.Typeface.BOLD);rp2.setPadding(dp(8),dp(3),dp(8),dp(3));rp2.setBackground(watchShape154(KuroTheme12.col(0x2ea855f7),KuroTheme12.col(0x88c084fc),999));row.addView(rp2);}else if(isWatched){TextView wp2=text("✓ Watched",10,KuroTheme12.col(0xffdcbaff));wp2.setTypeface(null,android.graphics.Typeface.BOLD);wp2.setPadding(dp(8),dp(3),dp(8),dp(3));wp2.setBackground(watchShape154(KuroTheme12.col(0x2ab478ee),KuroTheme12.col(0x88b478ee),999));row.addView(wp2);}\n'
        '    LinearLayout.LayoutParams rLp=new LinearLayout.LayoutParams(-1,dp(62));if(k+1<Math.min((page[0]+1)*500,matches.size()))rLp.setMargins(0,0,0,dp(5));list.addView(row,rLp);sliceCount++;\n'
        '    row.setOnClickListener(v->{if(idx==current)return;chooseEpisode581(idx);});\n'
        '   }\n'
        '   int visibleRows=epExpanded155[0]?7:3;int wantH=sliceCount>visibleRows?dp(visibleRows*67-5):-2;LinearLayout.LayoutParams curLp=(LinearLayout.LayoutParams)epScroll154.getLayoutParams();if(curLp!=null&&curLp.height!=wantH){curLp.height=wantH;epScroll154.setLayoutParams(curLp);}\n'
        '   if(firstEpScroll154[0]&&activeInSlice>=0){firstEpScroll154[0]=false;final int scrollTargetY=Math.max(0,activeInSlice*dp(67)-dp(67));epScroll154.post(()->epScroll154.scrollTo(0,scrollTargetY));}else if(!query.isEmpty()){epScroll154.post(()->epScroll154.scrollTo(0,0));}};\n'
        '  seeAllEps155.setOnClickListener(v->{epExpanded155[0]=!epExpanded155[0];seeAllEps155.setText(epExpanded155[0]?"Show less ▴":"See all ›");refresh.run();});\n'
        '  list.setTag(refresh);search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){page[0]=0;refresh.run();}public void afterTextChanged(android.text.Editable e){}});refresh.run();\n'
        '  addGap(content,14);\n'
        '  LinearLayout detailCard155=new LinearLayout(this);detailCard155.setOrientation(LinearLayout.VERTICAL);detailCard155.setPadding(dp(12),dp(11),dp(12),dp(12));detailCard155.setBackground(watchShape154(KuroTheme12.col(0xff1b1124),KuroTheme12.col(0xff3c2b4b),14));content.addView(detailCard155,new LinearLayout.LayoutParams(-1,-2));\n'
        '  LinearLayout detHdrRow155=new LinearLayout(this);detHdrRow155.setOrientation(LinearLayout.HORIZONTAL);detHdrRow155.setGravity(Gravity.CENTER_VERTICAL);detailCard155.addView(detHdrRow155,new LinearLayout.LayoutParams(-1,-2));\n'
        '  TextView detailHeading=text("Anime details",15,KuroTheme12.col(0xfff3eef7));detailHeading.setTypeface(null,android.graphics.Typeface.BOLD);detHdrRow155.addView(detailHeading,new LinearLayout.LayoutParams(0,-2,1));\n'
        '  final TextView detailScore155=text("",12,KuroTheme12.col(0xffd7a8ff));detailScore155.setTypeface(null,android.graphics.Typeface.BOLD);detailScore155.setVisibility(View.GONE);detHdrRow155.addView(detailScore155);\n'
        '  addGap(detailCard155,10);\n'
        '  LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.HORIZONTAL);detailCard155.addView(detail,new LinearLayout.LayoutParams(-1,-2));\n'
        '  ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);poster.setBackground(watchShape154(KuroTheme12.col(0xff372447),KuroTheme12.col(0xff4c385e),9));poster.setClipToOutline(true);poster.setOutlineProvider(ViewOutlineProvider.BACKGROUND);LinearLayout.LayoutParams posterLP=new LinearLayout.LayoutParams(dp(76),dp(110));posterLP.setMargins(0,0,dp(12),0);detail.addView(poster,posterLP);loadPoster(poster,anime.optString("image",""));\n'
        '  LinearLayout detailText=new LinearLayout(this);detailText.setOrientation(LinearLayout.VERTICAL);detail.addView(detailText,new LinearLayout.LayoutParams(0,-2,1));TextView detailName=text(info.optString("title","Anime"),16,Color.WHITE);detailName.setTypeface(null,android.graphics.Typeface.BOLD);detailText.addView(detailName);addGap(detailText,5);\n'
        '  JSONObject counts=anime.optJSONObject("counts");StringBuilder facts=new StringBuilder();if(counts!=null){String sub=counts.optString("sub","");String dub=counts.optString("dub","");String total=counts.optString("total","");if(sub.matches("[0-9]{1,5}"))facts.append("CC ").append(sub);if(dub.matches("[0-9]{1,5}")){if(facts.length()>0)facts.append("  •  ");facts.append("DUB ").append(dub);}if(total.matches("[0-9]{1,5}")){if(facts.length()>0)facts.append("  •  ");facts.append(total).append(" EPS");}}String meta=anime.optString("meta","").trim();if(!meta.isEmpty()){if(facts.length()>0)facts.append("  •  ");facts.append(meta.length()>80?meta.substring(0,80):meta);}final TextView detailFacts155=text(facts.length()>0?facts.toString():(eps.length()+" Episodes"),11,KuroTheme12.col(0xffb98ae5));detailText.addView(detailFacts155);addGap(detailText,6);\n'
        '  final LinearLayout detailGenres155=new LinearLayout(this);detailGenres155.setOrientation(LinearLayout.HORIZONTAL);detailGenres155.setVisibility(View.GONE);detailText.addView(detailGenres155,new LinearLayout.LayoutParams(-1,-2));\n'
        '  String desc=anime.optString("description","").trim();if(desc.length()>3000)desc=desc.substring(0,3000);TextView synopsis=text(desc.isEmpty()?"Description not available from this source.":desc,12,KuroTheme12.col(0xffdcd5e2));synopsis.setLineSpacing(dp(2),1);synopsis.setMaxLines(3);synopsis.setEllipsize(android.text.TextUtils.TruncateAt.END);synopsis.setOnClickListener(v->synopsis.setMaxLines(synopsis.getMaxLines()==3?40:3));detailText.addView(synopsis);addGap(detailText,8);TextView more=chip("View details",false);detailText.addView(more);more.setOnClickListener(v->{closePlayer();ui.evaluateJavascript("window.kuroOpenDetails&&window.kuroOpenDetails()",null);});\n'
        '  addGap(content,18);content.setClipToPadding(false);\n'
        '  LinearLayout recHdrRow154=new LinearLayout(this);recHdrRow154.setOrientation(LinearLayout.HORIZONTAL);recHdrRow154.setGravity(Gravity.CENTER_VERTICAL);\n'
        '  LinearLayout recTitleCol154=new LinearLayout(this);recTitleCol154.setOrientation(LinearLayout.VERTICAL);\n'
        '  TextView recHeading154=text("More anime",15,KuroTheme12.col(0xfff3eef7));recHeading154.setTypeface(null,android.graphics.Typeface.BOLD);recTitleCol154.addView(recHeading154);addGap(recTitleCol154,2);\n'
        '  TextView recSub154=text("Recommended • swipe left / right",11,KuroTheme12.col(0xffa79ab2));recTitleCol154.addView(recSub154);\n'
        '  recHdrRow154.addView(recTitleCol154,new LinearLayout.LayoutParams(0,-2,1));\n'
        '  TextView recLeftBtn154=text("‹",18,KuroTheme12.col(0xffe6e1eb));recLeftBtn154.setTypeface(null,android.graphics.Typeface.BOLD);recLeftBtn154.setGravity(Gravity.CENTER);recLeftBtn154.setBackground(watchShape154(KuroTheme12.col(0xff261830),KuroTheme12.col(0xff4c3a59),999));recLeftBtn154.setContentDescription("Scroll recommendations left");\n'
        '  LinearLayout.LayoutParams rlLp154=new LinearLayout.LayoutParams(dp(32),dp(32));rlLp154.setMargins(dp(8),0,dp(6),0);recHdrRow154.addView(recLeftBtn154,rlLp154);\n'
        '  TextView recRightBtn154=text("›",18,KuroTheme12.col(0xffe6e1eb));recRightBtn154.setTypeface(null,android.graphics.Typeface.BOLD);recRightBtn154.setGravity(Gravity.CENTER);recRightBtn154.setBackground(watchShape154(KuroTheme12.col(0xff261830),KuroTheme12.col(0xff4c3a59),999));recRightBtn154.setContentDescription("Scroll recommendations right");\n'
        '  recHdrRow154.addView(recRightBtn154,new LinearLayout.LayoutParams(dp(32),dp(32)));\n'
        '  content.addView(recHdrRow154,new LinearLayout.LayoutParams(-1,-2));addGap(content,10);\n'
        '  final HorizontalScrollView recHScroll154=new HorizontalScrollView(this){private float startX,startY;@Override public boolean dispatchTouchEvent(MotionEvent ev){int act=ev.getActionMasked();if(act==MotionEvent.ACTION_DOWN){startX=ev.getX();startY=ev.getY();}else if(act==MotionEvent.ACTION_MOVE){float dx=Math.abs(ev.getX()-startX),dy=Math.abs(ev.getY()-startY);if(dx>dy&&(canScrollHorizontally(1)||canScrollHorizontally(-1))){ViewParent p=getParent();if(p!=null)p.requestDisallowInterceptTouchEvent(true);}}return super.dispatchTouchEvent(ev);}};\n'
        '  recHScroll154.setTag("watch-rec-hscroll-154");recHScroll154.setHorizontalScrollBarEnabled(false);recHScroll154.setClipToPadding(false);recHScroll154.setPadding(dp(14),dp(2),dp(14),dp(4));\n'
        '  LinearLayout.LayoutParams hScrollLp154=new LinearLayout.LayoutParams(-1,-2);hScrollLp154.setMargins(-dp(14),0,-dp(14),0);\n'
        '  LinearLayout recFeed154=new LinearLayout(this);recFeed154.setOrientation(LinearLayout.HORIZONTAL);recFeed154.setTag("watch-rec-feed-154");\n'
        '  recHScroll154.addView(recFeed154,new FrameLayout.LayoutParams(-2,-2));content.addView(recHScroll154,hScrollLp154);\n'
        '  recLeftBtn154.setOnClickListener(v->recHScroll154.smoothScrollBy(-dp(268),0));\n'
        '  recRightBtn154.setOnClickListener(v->recHScroll154.smoothScrollBy(dp(268),0));\n'
        '  final ArrayList<JSONObject> lastRecCards155=new ArrayList<>();final ArrayList<String> lastGenres155=new ArrayList<>();\n'
        '  populateWatchRecommendations154(session,info,anime,recFeed154,recSub154,epThumbs155,epFallbackImg155,epDurMin155,refresh,detailScore155,detailGenres155,lastRecCards155,lastGenres155);\n'
        '  addGap(content,14);TextView note=text("Playback uses a third-party player. Autoplay is best effort; tap play if needed. Use its fullscreen control to expand.",11,KuroTheme12.col(0xff9887a6));content.addView(note);\n'
        '  final String followNum155=followingNumber;final JSONObject animeRef155=anime;\n'
        '  watchRetheme155=()->{\n'
        '   scroll.setBackgroundColor(KuroTheme12.col(0xff0d0914));\n'
        '   if(connectionText[0]!=null)connectionText[0].setTextColor(KuroTheme12.col(0xffd7c7e4));\n'
        '   prevEpBtn155.setTextColor(canPrev155?KuroTheme12.col(0xffeae2f2):KuroTheme12.col(0xff6b5b7a));\n'
        '   prevEpBtn155.setBackground(watchShape154(canPrev155?KuroTheme12.col(0xff21152b):KuroTheme12.col(0xff160e1e),canPrev155?KuroTheme12.col(0xff443253):KuroTheme12.col(0xff2b1e36),9));\n'
        '   nextEpBtn155.setTextColor(canNext?KuroTheme12.col(0xffeae2f2):KuroTheme12.col(0xff6b5b7a));\n'
        '   nextEpBtn155.setBackground(watchShape154(canNext?KuroTheme12.col(0xff21152b):KuroTheme12.col(0xff160e1e),canNext?KuroTheme12.col(0xff443253):KuroTheme12.col(0xff2b1e36),9));\n'
        '   styleAutoNextPill155.accept(getPreferences(MODE_PRIVATE).getBoolean("auto_next",false));\n'
        '   directHost155.removeAllViews();addDirectTools561(directHost155);refreshDirectTools561();\n'
        '   if(controlsRef155[0]!=null&&toolsDrawer155.indexOfChild(controlsRef155[0])>=0){int ci=toolsDrawer155.indexOfChild(controlsRef155[0]);toolsDrawer155.removeViewAt(ci);controlsRef155[0]=new PlaybackPanel(MainActivity.this,info.optString("episode",""),followNum155,canNext,getPreferences(MODE_PRIVATE).getBoolean("autoplay",true),getPreferences(MODE_PRIVATE).getBoolean("auto_next",false),getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false),pbListener155);toolsDrawer155.addView(controlsRef155[0],ci,new LinearLayout.LayoutParams(-1,-2));}\n'
        '   for(TextView tv:new TextView[]{optionsButton,enhancedButton,diagnostics573}){tv.setTextColor(KuroTheme12.col(0xffe3dce9));tv.setBackground(rounded(KuroTheme12.col(0xff281b32),KuroTheme12.col(0xff4c3a59)));}\n'
        '   boolean tOpen=toolsDrawer155.getVisibility()==View.VISIBLE;\n'
        '   serverCard155.setBackground(watchShape154(KuroTheme12.col(0xff1b1124),KuroTheme12.col(0xff3c2b4b),14));\n'
        '   toolsToggle155.setTextColor(KuroTheme12.col(0xffd8b8f8));toolsToggle155.setBackground(watchShape154(tOpen?KuroTheme12.col(0xff4b296d):KuroTheme12.col(0xff281a35),tOpen?KuroTheme12.col(0xffa96de8):KuroTheme12.col(0xff4c385e),999));\n'
        '   diagIcon155.setTextColor(KuroTheme12.col(0xffcbb4e3));diagIcon155.setBackground(watchShape154(KuroTheme12.col(0xff281a35),KuroTheme12.col(0xff4c385e),999));\n'
        '   segBar155.setBackground(watchShape154(KuroTheme12.col(0xff120b19),KuroTheme12.col(0xff332341),10));\n'
        '   syncServers154[0].run();\n'
        '   for(int ei=0;ei<extraCol.getChildCount();ei++){if(extraCol.getChildAt(ei) instanceof TextView){TextView eb=(TextView)extraCol.getChildAt(ei);eb.setTextColor(KuroTheme12.col(0xffe3dce9));eb.setBackground(rounded(KuroTheme12.col(0xff281b32),KuroTheme12.col(0xff4c3a59)));}}\n'
        '   playlistCard154.setBackground(watchShape154(KuroTheme12.col(0xff1b1124),KuroTheme12.col(0xff3c2b4b),14));\n'
        '   epCountPill154.setTextColor(KuroTheme12.col(0xffd7b4fc));epCountPill154.setBackground(watchShape154(KuroTheme12.col(0xff281a35),KuroTheme12.col(0xff4c385e),999));\n'
        '   seeAllEps155.setTextColor(KuroTheme12.col(0xffc993fc));\n'
        '   search.setHintTextColor(KuroTheme12.col(0xff8e7d9b));search.setBackground(watchShape154(KuroTheme12.col(0xff23172e),KuroTheme12.col(0xff443253),9));\n'
        '   final int savedEpY=epScroll154.getScrollY();refresh.run();epScroll154.post(()->epScroll154.scrollTo(0,savedEpY));\n'
        '   detailCard155.setBackground(watchShape154(KuroTheme12.col(0xff1b1124),KuroTheme12.col(0xff3c2b4b),14));\n'
        '   detailHeading.setTextColor(KuroTheme12.col(0xfff3eef7));detailScore155.setTextColor(KuroTheme12.col(0xffd7a8ff));\n'
        '   poster.setBackground(watchShape154(KuroTheme12.col(0xff372447),KuroTheme12.col(0xff4c385e),9));\n'
        '   detailFacts155.setTextColor(KuroTheme12.col(0xffb98ae5));synopsis.setTextColor(KuroTheme12.col(0xffdcd5e2));\n'
        '   more.setTextColor(KuroTheme12.col(0xffe3dce9));more.setBackground(rounded(KuroTheme12.col(0xff281b32),KuroTheme12.col(0xff4c3a59)));\n'
        '   if(!lastGenres155.isEmpty()){detailGenres155.removeAllViews();for(int gi=0;gi<lastGenres155.size();gi++){TextView gp=text(lastGenres155.get(gi),10,KuroTheme12.col(0xffdcd2e8));gp.setPadding(dp(8),dp(3),dp(8),dp(3));gp.setBackground(watchShape154(KuroTheme12.col(0xff251832),KuroTheme12.col(0xff453356),7));LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(-2,-2);glp.setMargins(0,0,dp(6),dp(7));detailGenres155.addView(gp,glp);}}\n'
        '   recHeading154.setTextColor(KuroTheme12.col(0xfff3eef7));recSub154.setTextColor(KuroTheme12.col(0xffa79ab2));\n'
        '   recLeftBtn154.setTextColor(KuroTheme12.col(0xffe6e1eb));recLeftBtn154.setBackground(watchShape154(KuroTheme12.col(0xff261830),KuroTheme12.col(0xff4c3a59),999));\n'
        '   recRightBtn154.setTextColor(KuroTheme12.col(0xffe6e1eb));recRightBtn154.setBackground(watchShape154(KuroTheme12.col(0xff261830),KuroTheme12.col(0xff4c3a59),999));\n'
        '   if(!lastRecCards155.isEmpty()){final int savedRecX=recHScroll154.getScrollX();recFeed154.removeAllViews();for(int ri=0;ri<lastRecCards155.size();ri++){View cv=buildYoutubeAnimeCard154(lastRecCards155.get(ri));LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(dp(256),-2);clp.setMargins(0,0,ri+1<lastRecCards155.size()?dp(12):0,0);recFeed154.addView(cv,clp);}recHScroll154.post(()->recHScroll154.scrollTo(savedRecX,0));}\n'
        '   note.setTextColor(KuroTheme12.col(0xff9887a6));\n'
        '  };'
    )

    ma_text = patch_once(
        ma_text,
        "/* k155 Watch Screen v3:",
        old_watch_body,
        new_watch_body,
        "MainActivity Watch Screen v3 compact Server Switcher + Rich Playlist + More Anime",
    )

    watch_helpers_154 = r""" private android.graphics.drawable.GradientDrawable watchShape154(int fill,int stroke,int radiusDp){
  android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
  d.setColor(fill);d.setCornerRadius(dp(radiusDp));
  if(stroke!=0)d.setStroke(dp(1),stroke);
  return d;
 }
 private static boolean allowedArtworkHost154(String h){
  if(h==null||h.isEmpty())return false;
  return h.endsWith(".anilist.co")||"anilist.co".equals(h)||h.endsWith(".myanimelist.net")||h.endsWith(".kitsu.app")||h.endsWith(".kitsu.io")||"hianime.ms".equals(h)||h.endsWith(".hianime.ms")||"hianime.dk".equals(h)||h.endsWith(".hianime.dk")||h.endsWith(".noitatnemucod.net")||h.endsWith(".flawlessfiles.com")||h.endsWith(".crunchyroll.com")||"image.tmdb.org".equals(h)||"artworks.thetvdb.com".equals(h);
 }
 private static String extractStreamEpNum155(String title){
  if(title==null||title.isEmpty())return "";
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)episode\\s+([0-9]{1,4})").matcher(title);
  if(m.find())return String.valueOf(Integer.parseInt(m.group(1)));
  java.util.regex.Matcher m2=java.util.regex.Pattern.compile("\\b([0-9]{1,4})\\b").matcher(title);
  return m2.find()?String.valueOf(Integer.parseInt(m2.group(1))):"";
 }
 private void loadThumb154(ImageView target,String address,int reqW,int reqH){
  if(target==null||address==null||address.isEmpty())return;
  final String cacheKey=reqW+"x"+reqH+"|"+address;
  Bitmap hit=posterCache154().get(cacheKey);
  if(hit!=null&&!hit.isRecycled()){target.setImageBitmap(hit);return;}
  target.setTag(cacheKey);
  pool.execute(()->{
   try{
    URI u=new URI(address);String h=u.getHost();
    if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||u.getPort()!=-1||h==null||!allowedArtworkHost154(h.toLowerCase(Locale.ROOT)))return;
    byte[] bytes=KuroFastNet154.fetchPoster(u,3200000);if(bytes==null||bytes.length==0)return;
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
    BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
    if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>8000||bounds.outHeight>8000)return;
    BitmapFactory.Options opts=new BitmapFactory.Options();
    opts.inSampleSize=Math.max(1,Math.min(bounds.outWidth/Math.max(64,reqW),bounds.outHeight/Math.max(64,reqH)));
    opts.inPreferredConfig=Bitmap.Config.RGB_565;
    Bitmap bmp=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
    if(bmp!=null){
     posterCache154().put(cacheKey,bmp);
     main.post(()->{if(cacheKey.equals(target.getTag()))target.setImageBitmap(bmp);});
    }
   }catch(Exception ignored){}
  });
 }
 private static String normWatchTitle154(String s){
  if(s==null)return "";
  String n=java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  StringBuilder b=new StringBuilder();
  for(int i=0;i<n.length();i++){char c=n.charAt(i);if(Character.isLetterOrDigit(c))b.append(c);}
  return b.toString();
 }
 private View buildYoutubeAnimeCard154(JSONObject m){
  JSONObject tObj=m.optJSONObject("title");
  String eng=tObj==null?"":tObj.optString("english","").trim();
  String rom=tObj==null?"":tObj.optString("romaji","").trim();
  String dispTitle=!eng.isEmpty()?eng:(!rom.isEmpty()?rom:"Anime");
  JSONObject cov=m.optJSONObject("coverImage");
  String coverXl=cov==null?"":cov.optString("extraLarge","").trim();
  String coverLg=cov==null?"":cov.optString("large","").trim();
  String coverUrl=!coverLg.isEmpty()?coverLg:coverXl;
  String bannerUrl=m.optString("bannerImage","").trim();
  String thumbUrl=!bannerUrl.isEmpty()?bannerUrl:(!coverXl.isEmpty()?coverXl:coverLg);
  int epsCount=m.optInt("episodes",0);
  int score=m.optInt("averageScore",0);
  int year=m.optInt("seasonYear",0);
  String fmt=m.optString("format","").replace('_',' ').trim();
  String studio="";
  JSONObject stObj=m.optJSONObject("studios");JSONArray stNodes=stObj==null?null:stObj.optJSONArray("nodes");
  if(stNodes!=null&&stNodes.length()>0){JSONObject s0=stNodes.optJSONObject(0);if(s0!=null)studio=s0.optString("name","").trim();}
  StringBuilder subLine=new StringBuilder();
  if(!studio.isEmpty())subLine.append(studio);
  if(!fmt.isEmpty()){if(subLine.length()>0)subLine.append("  •  ");subLine.append(fmt);}
  if(year>=1950){if(subLine.length()>0)subLine.append("  •  ");subLine.append(year);}
  JSONArray genres=m.optJSONArray("genres");
  if(genres!=null&&genres.length()>0){
   StringBuilder gb=new StringBuilder();
   for(int gi=0;gi<Math.min(2,genres.length());gi++){String g=genres.optString(gi,"").trim();if(!g.isEmpty()){if(gb.length()>0)gb.append(", ");gb.append(g);}}
   if(gb.length()>0){if(subLine.length()>0)subLine.append("  •  ");subLine.append(gb);}
  }
  String badgeRight=epsCount>0?(epsCount+(epsCount==1?" EP":" EPS")):(!fmt.isEmpty()?fmt:"ANIME");

  LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
  card.setBackground(watchShape154(KuroTheme12.col(0xff21142a),KuroTheme12.col(0xff3f2f4c),14));
  card.setClipToOutline(true);card.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
  card.setClickable(true);card.setFocusable(true);
  card.setForeground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(46,255,255,255)),null,null));

  FrameLayout videoThumbBox=new FrameLayout(this);
  videoThumbBox.setBackgroundColor(KuroTheme12.col(0xff24172e));
  ImageView thumbImg=new ImageView(this);thumbImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
  videoThumbBox.addView(thumbImg,new FrameLayout.LayoutParams(-1,-1));
  loadThumb154(thumbImg,thumbUrl,640,360);

  TextView durBadge=text(badgeRight,10,Color.WHITE);durBadge.setTypeface(null,android.graphics.Typeface.BOLD);
  durBadge.setPadding(dp(7),dp(2),dp(7),dp(2));durBadge.setBackground(watchShape154(Color.argb(224,8,8,12),0,6));
  FrameLayout.LayoutParams durLp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);
  durLp.setMargins(dp(8),dp(8),dp(8),dp(8));
  videoThumbBox.addView(durBadge,durLp);

  if(score>0){
   String scStr=String.format(Locale.ROOT,"★ %.1f",score/10.0);
   TextView scBadge=text(scStr,10,KuroTheme12.col(0xffcfa2fb));scBadge.setTypeface(null,android.graphics.Typeface.BOLD);
   scBadge.setPadding(dp(7),dp(2),dp(7),dp(2));scBadge.setBackground(watchShape154(Color.argb(220,12,10,18),KuroTheme12.col(0xff4c3a59),6));
   FrameLayout.LayoutParams scLp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);
   scLp.setMargins(dp(8),dp(8),dp(8),dp(8));
   videoThumbBox.addView(scBadge,scLp);
  }
  card.addView(videoThumbBox,new LinearLayout.LayoutParams(-1,dp(144)));

  LinearLayout metaRow=new LinearLayout(this);metaRow.setOrientation(LinearLayout.HORIZONTAL);
  metaRow.setGravity(Gravity.TOP);metaRow.setPadding(dp(11),dp(10),dp(11),dp(12));
  ImageView avatar=new ImageView(this);avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
  avatar.setBackground(watchShape154(KuroTheme12.col(0xff372447),KuroTheme12.col(0xff4c3a59),999));
  avatar.setClipToOutline(true);avatar.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
  LinearLayout.LayoutParams avLp=new LinearLayout.LayoutParams(dp(34),dp(34));avLp.setMargins(0,dp(1),dp(10),0);
  metaRow.addView(avatar,avLp);
  loadThumb154(avatar,coverUrl,128,128);

  LinearLayout textCol=new LinearLayout(this);textCol.setOrientation(LinearLayout.VERTICAL);
  TextView tView=text(dispTitle,14,Color.WHITE);tView.setTypeface(null,android.graphics.Typeface.BOLD);
  tView.setLines(2);tView.setEllipsize(android.text.TextUtils.TruncateAt.END);
  textCol.addView(tView);
  addGap(textCol,3);
  TextView sView=text(subLine.length()>0?subLine.toString():"Anime",12,KuroTheme12.col(0xffa79ab2));
  sView.setSingleLine(true);sView.setEllipsize(android.text.TextUtils.TruncateAt.END);
  textCol.addView(sView);
  metaRow.addView(textCol,new LinearLayout.LayoutParams(0,-2,1));
  card.addView(metaRow,new LinearLayout.LayoutParams(-1,-2));

  final long recAid=m.optLong("id",0),recMid=m.optLong("idMal",0);
  final String clickTitle=dispTitle,clickRom=rom,clickImg=coverUrl;
  card.setOnClickListener(v->{
   try{
    JSONObject p=new JSONObject().put("title",clickTitle).put("romaji",clickRom).put("anilistId",recAid).put("malId",recMid).put("img",clickImg);
    closePlayer();
    if(ui!=null)ui.evaluateJavascript("window.kuroOpenRecommended154&&window.kuroOpenRecommended154("+JSONObject.quote(p.toString())+")",null);
   }catch(Exception ignored){}
  });
  return card;
 }
 private void populateWatchRecommendations154(String session,JSONObject info,JSONObject anime,LinearLayout feed,TextView subLabel,ConcurrentHashMap<String,String> epThumbs155,String[] epFallbackImg155,int[] epDurMin155,Runnable refreshPlaylist155,TextView detailScore155,LinearLayout detailGenres155,ArrayList<JSONObject> lastRecCards155,ArrayList<String> lastGenres155){
  if(feed==null||info==null)return;
  TextView loading=text("Loading similar anime…",13,KuroTheme12.col(0xffa79ab2));
  loading.setPadding(0,dp(4),0,dp(8));
  feed.addView(loading);
  final String curTitle=info.optString("title","").trim();
  final String curNorm=normWatchTitle154(curTitle);
  final long curAid=anime==null?0L:anime.optLong("anilistId",0L);
  pool.execute(()->{
   try{
    JSONObject req=new JSONObject();
    if(curAid>0)req.put("id",curAid);
    if(!curTitle.isEmpty())req.put("search",curTitle.length()>120?curTitle.substring(0,120):curTitle);
    String raw=KuroCatalog573.lookup("watchFeed154",req.toString());
    JSONObject data=new JSONObject(raw).optJSONObject("data");
    final ArrayList<JSONObject> cards=new ArrayList<>();
    final HashSet<Long> seen=new HashSet<>();
    final int[] resolvedScore={0};
    final ArrayList<String> resolvedGenres=new ArrayList<>();
    boolean playlistDirty=false;
    if(curAid>0)seen.add(curAid);
    if(data!=null){
     JSONObject media=data.optJSONObject("Media");
     if(media!=null){
      long resolvedId=media.optLong("id",0);if(resolvedId>0)seen.add(resolvedId);
      resolvedScore[0]=media.optInt("averageScore",0);
      int dur=media.optInt("duration",0);
      if(dur>=5&&dur<=180&&epDurMin155!=null&&epDurMin155[0]!=dur){epDurMin155[0]=dur;playlistDirty=true;}
      String mBanner=media.optString("bannerImage","").trim();
      JSONObject mCov=media.optJSONObject("coverImage");
      String mCovUrl=mCov==null?"":(!mCov.optString("extraLarge","").trim().isEmpty()?mCov.optString("extraLarge","").trim():mCov.optString("large","").trim());
      String bestEpArt=!mBanner.isEmpty()?mBanner:mCovUrl;
      if(!bestEpArt.isEmpty()&&epFallbackImg155!=null&&!bestEpArt.equals(epFallbackImg155[0])){epFallbackImg155[0]=bestEpArt;playlistDirty=true;}
      JSONArray streamEps=media.optJSONArray("streamingEpisodes");
      if(streamEps!=null&&epThumbs155!=null){
       for(int si=0;si<streamEps.length();si++){
        JSONObject se=streamEps.optJSONObject(si);if(se==null)continue;
        String th=se.optString("thumbnail","").trim();
        String en=extractStreamEpNum155(se.optString("title",""));
        if(!th.isEmpty()&&!en.isEmpty()){epThumbs155.put(en,th);playlistDirty=true;}
       }
      }
      JSONArray gArr=media.optJSONArray("genres");
      if(gArr!=null){for(int gi=0;gi<Math.min(3,gArr.length());gi++){String g=gArr.optString(gi,"").trim();if(!g.isEmpty())resolvedGenres.add(g);}}
      JSONObject recs=media.optJSONObject("recommendations");
      JSONArray nodes=recs==null?null:recs.optJSONArray("nodes");
      if(nodes!=null){
       for(int i=0;i<nodes.length()&&cards.size()<12;i++){
        JSONObject n=nodes.optJSONObject(i);
        JSONObject mr=n==null?null:n.optJSONObject("mediaRecommendation");
        if(mr==null||mr.optBoolean("isAdult",false))continue;
        long id=mr.optLong("id",0);
        if(id<=0||seen.contains(id))continue;
        JSONObject tt=mr.optJSONObject("title");
        String te=tt==null?"":tt.optString("english","");
        String tr=tt==null?"":tt.optString("romaji","");
        if(!curNorm.isEmpty()&&(curNorm.equals(normWatchTitle154(te))||curNorm.equals(normWatchTitle154(tr))))continue;
        seen.add(id);cards.add(mr);
       }
      }
     }
     JSONObject fb=data.optJSONObject("fallback");
     JSONArray fbMedia=fb==null?null:fb.optJSONArray("media");
     if(fbMedia!=null){
      for(int j=0;j<fbMedia.length()&&cards.size()<12;j++){
       JSONObject mr=fbMedia.optJSONObject(j);
       if(mr==null||mr.optBoolean("isAdult",false))continue;
       long id=mr.optLong("id",0);
       if(id<=0||seen.contains(id))continue;
       JSONObject tt=mr.optJSONObject("title");
       String te=tt==null?"":tt.optString("english","");
       String tr=tt==null?"":tt.optString("romaji","");
       if(!curNorm.isEmpty()&&(curNorm.equals(normWatchTitle154(te))||curNorm.equals(normWatchTitle154(tr))))continue;
       seen.add(id);cards.add(mr);
      }
     }
    }
    final boolean doRefreshPlaylist=playlistDirty;
    main.post(()->{
     if(!session.equals(watchSession)||playerLayer==null)return;
     if(doRefreshPlaylist&&refreshPlaylist155!=null)refreshPlaylist155.run();
     if(detailScore155!=null&&resolvedScore[0]>0){
      detailScore155.setText(String.format(Locale.ROOT,"★ %.1f/10",resolvedScore[0]/10.0));
      detailScore155.setVisibility(View.VISIBLE);
     }
     if(lastGenres155!=null){lastGenres155.clear();lastGenres155.addAll(resolvedGenres);}
     if(lastRecCards155!=null){lastRecCards155.clear();lastRecCards155.addAll(cards);}
     if(detailGenres155!=null&&!resolvedGenres.isEmpty()){
      detailGenres155.removeAllViews();
      for(int gi=0;gi<resolvedGenres.size();gi++){
       TextView gp=text(resolvedGenres.get(gi),10,KuroTheme12.col(0xffdcd2e8));
       gp.setPadding(dp(8),dp(3),dp(8),dp(3));
       gp.setBackground(watchShape154(KuroTheme12.col(0xff251832),KuroTheme12.col(0xff453356),7));
       LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(-2,-2);
       glp.setMargins(0,0,dp(6),dp(7));
       detailGenres155.addView(gp,glp);
      }
      detailGenres155.setVisibility(View.VISIBLE);
     }
     if(!feed.isAttachedToWindow())return;
     feed.removeAllViews();
     if(cards.isEmpty()){
      feed.addView(text("No recommendations available right now.",13,KuroTheme12.col(0xffa79ab2)));
      return;
     }
     if(subLabel!=null&&!curTitle.isEmpty())subLabel.setText("Recommended for "+curTitle+" • swipe left / right");
     for(int i=0;i<cards.size();i++){
      View cardView=buildYoutubeAnimeCard154(cards.get(i));
      LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(dp(256),-2);
      clp.setMargins(0,0,i+1<cards.size()?dp(12):0,0);
      feed.addView(cardView,clp);
     }
    });
   }catch(Exception e){
    main.post(()->{
     if(!session.equals(watchSession)||playerLayer==null||!feed.isAttachedToWindow())return;
     feed.removeAllViews();
     feed.addView(text("Recommendations unavailable offline.",13,KuroTheme12.col(0xffa79ab2)));
    });
   }
  });
 }
"""
    ma_text = patch_once(
        ma_text,
        "private View buildYoutubeAnimeCard154(JSONObject m){",
        " void loadPoster(ImageView target,String address){",
        watch_helpers_154 + " void loadPoster(ImageView target,String address){",
        "MainActivity YouTube anime card & artwork helpers",
    )

    ma_path.write_text(ma_text, encoding="utf-8")


KURO_DISPLAY_155_JAVA = """package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import org.json.JSONObject;

/**
 * Kuro 1.5.5-beta8 — Controllable App DPI, Global UI Size & Auto-Next Timing Engine.
 * Preserves native hardware system resolution while letting users choose System DPI or
 * any custom App DPI (280..640, default 500), global App UI Size (75%..135%, default 100%),
 * Auto-Play Next countdown seconds (3..20s, default 8s), and Up Next card scale (80%..125%).
 */
public final class KuroDisplay155 {
    public static final String PREFS = "kuro-display-155";
    public static final int FORCE_DPI = 500;
    public static final float FORCE_DENSITY = 500f / 160f;
    private static final int SYSTEM_DPI = detectHardwareSystemDpi();

    private static volatile boolean loaded = false;
    private static volatile int cfgDpi = FORCE_DPI;       // 0 = System DPI, else 280..640
    private static volatile int cfgUiScale = 100;         // 75..135 (%)
    private static volatile int cfgAutoNextSec = 8;       // 3..20 (s)
    private static volatile int cfgUpNextScale = 100;     // 80..125 (%)

    private KuroDisplay155() {}

    private static int detectHardwareSystemDpi() {
        try {
            Resources sys = Resources.getSystem();
            if (sys != null) {
                DisplayMetrics dm = sys.getDisplayMetrics();
                if (dm != null && dm.densityDpi >= 120 && dm.densityDpi <= 960) {
                    return dm.densityDpi;
                }
            }
        } catch (Throwable ignored) {}
        return 420;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static void ensureLoaded(Context context) {
        if (loaded || context == null) return;
        synchronized (KuroDisplay155.class) {
            if (loaded) return;
            try {
                SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                int d = sp.getInt("dpi", FORCE_DPI);
                cfgDpi = d <= 0 ? 0 : clamp(d, 280, 640);
                cfgUiScale = clamp(sp.getInt("uiScale", 100), 75, 135);
                cfgAutoNextSec = clamp(sp.getInt("autoNextSec", 8), 3, 20);
                cfgUpNextScale = clamp(sp.getInt("upNextScale", 100), 80, 125);
                loaded = true;
            } catch (Throwable ignored) {}
        }
    }

    public static void saveConfig(Context context, int dpi, int uiScale, int autoNextSec, int upNextScale) {
        int cleanDpi = dpi <= 0 ? 0 : clamp(dpi, 280, 640);
        int cleanScale = clamp(uiScale, 75, 135);
        int cleanSec = clamp(autoNextSec, 3, 20);
        int cleanCard = clamp(upNextScale, 80, 125);
        synchronized (KuroDisplay155.class) {
            cfgDpi = cleanDpi;
            cfgUiScale = cleanScale;
            cfgAutoNextSec = cleanSec;
            cfgUpNextScale = cleanCard;
            loaded = true;
            if (context != null) {
                try {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putInt("dpi", cleanDpi)
                        .putInt("uiScale", cleanScale)
                        .putInt("autoNextSec", cleanSec)
                        .putInt("upNextScale", cleanCard)
                        .commit();
                } catch (Throwable ignored) {}
            }
        }
        if (context != null) {
            enforce500Dpi(context);
        }
    }

    public static int systemDpi() {
        return SYSTEM_DPI;
    }

    public static int configuredDpi(Context context) {
        ensureLoaded(context);
        return cfgDpi;
    }

    public static int baseDpi(Context context) {
        ensureLoaded(context);
        return cfgDpi <= 0 ? SYSTEM_DPI : cfgDpi;
    }

    public static int uiScale(Context context) {
        ensureLoaded(context);
        return cfgUiScale;
    }

    public static int effectiveDpi(Context context) {
        ensureLoaded(context);
        int base = cfgDpi <= 0 ? SYSTEM_DPI : cfgDpi;
        return clamp(Math.round(base * (cfgUiScale / 100f)), 220, 760);
    }

    public static float effectiveDensity(Context context) {
        return effectiveDpi(context) / 160f;
    }

    public static int autoNextSeconds(Context context) {
        ensureLoaded(context);
        return cfgAutoNextSec;
    }

    public static int upNextScale(Context context) {
        ensureLoaded(context);
        return cfgUpNextScale;
    }

    public static float upNextScaleFactor(Context context) {
        return upNextScale(context) / 100f;
    }

    public static Context wrapContext(Context base) {
        if (base == null) return null;
        try {
            ensureLoaded(base);
            int effDpi = effectiveDpi(base);
            float effDensity = effDpi / 160f;
            Resources res = base.getResources();
            if (res == null) return base;
            DisplayMetrics rawDm = res.getDisplayMetrics();
            Configuration cfg = new Configuration(res.getConfiguration());
            cfg.densityDpi = effDpi;
            if (rawDm != null && rawDm.widthPixels > 0 && rawDm.heightPixels > 0) {
                int wDp = Math.max(260, Math.round(rawDm.widthPixels / effDensity));
                int hDp = Math.max(260, Math.round(rawDm.heightPixels / effDensity));
                cfg.screenWidthDp = wDp;
                cfg.screenHeightDp = hDp;
                cfg.smallestScreenWidthDp = Math.min(wDp, hDp);
            }
            Context wrapped = base.createConfigurationContext(cfg);
            applyMetricsToResources(wrapped.getResources(), effDpi, effDensity);
            return wrapped;
        } catch (RuntimeException ignored) {
            return base;
        }
    }

    public static void enforce500Dpi(Context context) {
        if (context == null) return;
        try {
            ensureLoaded(context);
            int effDpi = effectiveDpi(context);
            float effDensity = effDpi / 160f;
            applyMetricsToResources(context.getResources(), effDpi, effDensity);
            Context app = context.getApplicationContext();
            if (app != null && app != context) {
                applyMetricsToResources(app.getResources(), effDpi, effDensity);
            }
        } catch (RuntimeException ignored) {}
    }

    private static void applyMetricsToResources(Resources res, int effDpi, float effDensity) {
        if (res == null) return;
        Configuration cfg = res.getConfiguration();
        float fontScale = (cfg != null && cfg.fontScale > 0f) ? cfg.fontScale : 1f;
        if (cfg != null) {
            cfg.densityDpi = effDpi;
        }
        DisplayMetrics dm = res.getDisplayMetrics();
        if (dm != null) {
            dm.density = effDensity;
            dm.densityDpi = effDpi;
            dm.xdpi = effDpi;
            dm.ydpi = effDpi;
            dm.scaledDensity = effDensity * fontScale;
            if (cfg != null && dm.widthPixels > 0 && dm.heightPixels > 0) {
                int wDp = Math.max(260, Math.round(dm.widthPixels / effDensity));
                int hDp = Math.max(260, Math.round(dm.heightPixels / effDensity));
                cfg.screenWidthDp = wDp;
                cfg.screenHeightDp = hDp;
                cfg.smallestScreenWidthDp = Math.min(wDp, hDp);
            }
        }
    }

    public static void applyActivity500Dpi(Activity activity) {
        if (activity == null) return;
        enforce500Dpi(activity);
    }

    public static String infoJson(Context context) {
        return infoJson(context, true);
    }

    public static String infoJson(Context context, boolean autoNextEnabled) {
        try {
            enforce500Dpi(context);
            DisplayMetrics dm = context != null && context.getResources() != null
                ? context.getResources().getDisplayMetrics() : null;
            int pw = dm != null ? dm.widthPixels : 0;
            int ph = dm != null ? dm.heightPixels : 0;
            int effDpi = effectiveDpi(context);
            float effDensity = effDpi / 160f;
            int wDp = pw > 0 ? Math.round(pw / effDensity) : 360;
            int hDp = ph > 0 ? Math.round(ph / effDensity) : 780;
            int rawCfg = configuredDpi(context);
            return new JSONObject()
                .put("dpi", rawCfg)
                .put("dpiMode", rawCfg <= 0 ? "system" : "custom")
                .put("systemDpi", SYSTEM_DPI)
                .put("baseDpi", baseDpi(context))
                .put("uiScale", uiScale(context))
                .put("effectiveDpi", effDpi)
                .put("density", (double) effDensity)
                .put("widthDp", wDp)
                .put("heightDp", hDp)
                .put("autoNextEnabled", autoNextEnabled)
                .put("autoNextSec", autoNextSeconds(context))
                .put("upNextScale", upNextScale(context))
                .put("resolutionMode", "system-default")
                .put("physicalWidth", pw)
                .put("physicalHeight", ph)
                .toString();
        } catch (Exception e) {
            return "{\\"dpi\\":500,\\"baseDpi\\":500,\\"uiScale\\":100,\\"effectiveDpi\\":500,\\"density\\":3.125,\\"resolutionMode\\":\\"system-default\\"}";
        }
    }

    public static String diagnosticsSummary(Context context) {
        enforce500Dpi(context);
        DisplayMetrics dm = context != null && context.getResources() != null
            ? context.getResources().getDisplayMetrics() : null;
        int pw = dm != null ? dm.widthPixels : 0;
        int ph = dm != null ? dm.heightPixels : 0;
        int rawCfg = configuredDpi(context);
        int base = baseDpi(context);
        int scale = uiScale(context);
        int eff = effectiveDpi(context);
        return "System Resolution (" + pw + "x" + ph + ") \\u00b7 "
            + (rawCfg <= 0 ? ("System " + base) : ("App " + base))
            + " DPI \\u00b7 UI " + scale + "% (" + eff + " eff DPI)";
    }
}
"""


def patch_display_2k_500dpi_155(root: pathlib.Path) -> None:
    disp_path = root / "src/app/lovelycrxshx/kuro/KuroDisplay155.java"
    disp_path.write_text(KURO_DISPLAY_155_JAVA, encoding="utf-8")

    # 1. MainActivity: System default resolution & viewport + controllable App DPI & UI Size
    ma_path = root / "src/app/lovelycrxshx/kuro/MainActivity.java"
    ma_text = ma_path.read_text(encoding="utf-8")
    ma_text = patch_once(
        ma_text,
        " @Override public void onCreate(Bundle state){super.onCreate(state);KuroDisplay155.applyActivity500Dpi(this);",
        " @Override public void onCreate(Bundle state){super.onCreate(state);",
        " @Override public void onCreate(Bundle state){super.onCreate(state);KuroDisplay155.applyActivity500Dpi(this);",
        "MainActivity.onCreate 500DPI",
    )
    ma_text = patch_once(
        ma_text,
        "ui=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "ui=new WebView(this);",
        "ui=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "MainActivity ui WebView 500DPI",
    )
    ma_text = patch_once(
        ma_text,
        "WebView check=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "WebView check=new WebView(this);",
        "WebView check=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "MainActivity verification WebView 500DPI",
    )
    ma_text = patch_once(
        ma_text,
        "player=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "player=new WebView(this);",
        "player=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "MainActivity player WebView 500DPI",
    )
    if "Math.round(zoom*KuroDisplay155.uiScale(this)/100f)" not in ma_text:
        for old_zoom_snippet in (
            'KuroDisplay155.enforce500Dpi(this);if(ui!=null){ui.getSettings().setTextZoom(zoom);ui.getSettings().setUseWideViewPort(desktopUi154);ui.getSettings().setLoadWithOverviewMode(desktopUi154);if(uiReady)ui.evaluateJavascript("window.kuroSyncDesktopSite154&&window.kuroSyncDesktopSite154("+desktopUi154+")",null);}',
            'if(ui!=null){ui.getSettings().setTextZoom(zoom);ui.getSettings().setUseWideViewPort(desktopUi154);ui.getSettings().setLoadWithOverviewMode(desktopUi154);if(uiReady)ui.evaluateJavascript("window.kuroSyncDesktopSite154&&window.kuroSyncDesktopSite154("+desktopUi154+")",null);}',
        ):
            if old_zoom_snippet in ma_text:
                ma_text = ma_text.replace(
                    old_zoom_snippet,
                    'KuroDisplay155.enforce500Dpi(this);int effZoom155=Math.max(60,Math.min(180,Math.round(zoom*KuroDisplay155.uiScale(this)/100f)));if(ui!=null){ui.getSettings().setTextZoom(effZoom155);ui.getSettings().setUseWideViewPort(desktopUi154);ui.getSettings().setLoadWithOverviewMode(desktopUi154);if(uiReady)ui.evaluateJavascript("window.kuroSyncDesktopSite154&&window.kuroSyncDesktopSite154("+desktopUi154+");window.kuroSyncDisplay155&&window.kuroSyncDisplay155()",null);}',
                    1,
                )
                break
        else:
            raise SystemExit("ui_1.5.4.py: missing applyWindowPreferences560 zoom anchor in MainActivity.java")
    ma_text = patch_once(
        ma_text,
        "KuroDisplay155.diagnosticsSummary(this)",
        '") \u00b7 WebView "+web+" \u00b7 Available device storage',
        '") \u00b7 WebView "+web+" \u00b7 "+KuroDisplay155.diagnosticsSummary(this)+" \u00b7 Available device storage',
        "MainActivity.diagnostics560 500DPI",
    )
    bridge_display_methods = (
        "  @JavascriptInterface public String diagnostics560(){return MainActivity.this.diagnostics560();}\n"
        "  @JavascriptInterface public String displayInfo155(){return KuroDisplay155.infoJson(MainActivity.this,isAutoNextEnabled155());}\n"
        "  @JavascriptInterface public void setAutoNextEnabled155(boolean on){main.post(()->{getPreferences(MODE_PRIVATE).edit().putBoolean(\"auto_next\",on).apply();if(!on)cancelAutoNext573();});}\n"
        "  @JavascriptInterface public String saveDisplay155(int dpi,int uiScale,int autoNextSec,int upNextScale,boolean recreateNow){KuroDisplay155.saveConfig(MainActivity.this,dpi,uiScale,autoNextSec,upNextScale);main.post(()->{applyWindowPreferences560();if(recreateNow&&!isFinishing()&&!isDestroyed()){try{recreate();}catch(RuntimeException ignored){}}});return KuroDisplay155.infoJson(MainActivity.this,isAutoNextEnabled155());}\n"
        "  @JavascriptInterface public void previewAutoNext155(){main.post(MainActivity.this::previewAutoNextDialog155);}\n"
        "  @JavascriptInterface public void openDisplayDialog155(){main.post(MainActivity.this::openDisplayControllerFromMain155);}"
    )
    if "saveDisplay155(" not in ma_text:
        for old_bridge_disp in (
            "  @JavascriptInterface public String diagnostics560(){return MainActivity.this.diagnostics560();}\n  @JavascriptInterface public String displayInfo155(){return KuroDisplay155.infoJson(MainActivity.this);}",
            "  @JavascriptInterface public String diagnostics560(){return MainActivity.this.diagnostics560();}",
        ):
            if old_bridge_disp in ma_text:
                ma_text = ma_text.replace(old_bridge_disp, bridge_display_methods, 1)
                break
        else:
            raise SystemExit("ui_1.5.4.py: missing Bridge diagnostics560 anchor in MainActivity.java")
    ma_text = patch_once(
        ma_text,
        "protected void attachBaseContext(Context newBase)",
        " @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);syncWindowSystemBars154();",
        " @Override protected void attachBaseContext(Context newBase){super.attachBaseContext(KuroDisplay155.wrapContext(newBase));}\n @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);syncWindowSystemBars154();KuroDisplay155.applyActivity500Dpi(this);",
        "MainActivity attachBaseContext & onConfigurationChanged 500DPI",
    )
    ma_text = patch_once(
        ma_text,
        "super.onResume();activityResumed=true;syncWindowSystemBars154();KuroDisplay155.enforce500Dpi(this);",
        "super.onResume();activityResumed=true;syncWindowSystemBars154();",
        "super.onResume();activityResumed=true;syncWindowSystemBars154();KuroDisplay155.enforce500Dpi(this);",
        "MainActivity.onResume 500DPI",
    )
    ma_path.write_text(ma_text, encoding="utf-8")

    # 2. KuroOfflineActivity561: Controllable App DPI & UI Size, system default resolution
    oa_path = root / "src/app/lovelycrxshx/kuro/KuroOfflineActivity561.java"
    oa_text = oa_path.read_text(encoding="utf-8")
    oa_text = patch_once(
        oa_text,
        "attachBaseContext(android.content.Context newBase)",
        "    @Override protected void onCreate(Bundle b){super.onCreate(b);",
        "    @Override protected void attachBaseContext(android.content.Context newBase){super.attachBaseContext(KuroDisplay155.wrapContext(newBase));}\n    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);KuroDisplay155.applyActivity500Dpi(this);}\n    @Override protected void onCreate(Bundle b){super.onCreate(b);KuroDisplay155.applyActivity500Dpi(this);",
        "KuroOfflineActivity561 500DPI lifecycle",
    )
    oa_text = patch_once(
        oa_text,
        "@Override protected void onResume(){super.onResume();KuroDisplay155.enforce500Dpi(this);",
        "@Override protected void onResume(){super.onResume();",
        "@Override protected void onResume(){super.onResume();KuroDisplay155.enforce500Dpi(this);",
        "KuroOfflineActivity561 onResume 500DPI",
    )
    oa_path.write_text(oa_text, encoding="utf-8")

    # 3. KuroDialogs581: preserve Activity window token + themed Up Next card + Display & DPI controller
    dg_path = root / "src/app/lovelycrxshx/kuro/KuroDialogs581.java"
    dg_text = dg_path.read_text(encoding="utf-8")
    dialog_helpers_155 = """    static Activity findActivity155(Context c) {
        Context cur = c;
        for (int i = 0; i < 12 && cur != null; i++) {
            if (cur instanceof Activity) return (Activity) cur;
            if (cur instanceof android.content.ContextWrapper) cur = ((android.content.ContextWrapper) cur).getBaseContext();
            else break;
        }
        return null;
    }

    static boolean canShow155(Context c) {
        Activity a = findActivity155(c);
        return a != null && !a.isFinishing() && !a.isDestroyed();
    }

    static boolean safeShow155(Dialog d, Context c) {
        if (d == null || !canShow155(c)) return false;
        try {
            d.show();
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static android.app.AlertDialog safeShowBuilder155(android.app.AlertDialog.Builder b, Context c) {
        if (b == null || !canShow155(c)) return null;
        try {
            android.app.AlertDialog d = b.create();
            d.show();
            return d;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Context for stock alert dialogs: keeps Activity IBinder token intact while enforcing App DPI. */
    public static Context themed(Context context) {
        KuroDisplay155.enforce500Dpi(context);
        return new ContextThemeWrapper(context, KuroTheme12.dialogTheme());
    }"""
    if "static boolean safeShow155(Dialog d, Context c)" not in dg_text:
        for old_themed in (
            """    /** Context for the stock alert dialogs we have not rebuilt yet: no more grey panels. */
    public static Context themed(Context context) {
        return new ContextThemeWrapper(KuroDisplay155.wrapContext(context), KuroTheme12.dialogTheme());
    }""",
            """    /** Context for the stock alert dialogs we have not rebuilt yet: no more grey panels. */
    public static Context themed(Context context) {
        return new ContextThemeWrapper(context, KuroTheme12.dialogTheme());
    }""",
            """    /** Context for the stock alert dialogs we have not rebuilt yet: no more grey panels. */
    public static Context themed(Context context) {
        return new ContextThemeWrapper(context, R.style.KuroDialogTheme);
    }""",
        ):
            if old_themed in dg_text:
                dg_text = dg_text.replace(old_themed, dialog_helpers_155, 1)
                break
        else:
            raise SystemExit("ui_1.5.4.py: missing themed(Context) anchor in KuroDialogs581.java")
    dg_text = patch_once(
        dg_text,
        "try { dialog.dismiss(); } catch (RuntimeException ignored) {}",
        "                    button.setOnClickListener(v -> {\n                        dialog.dismiss();",
        "                    button.setOnClickListener(v -> {\n                        try { dialog.dismiss(); } catch (RuntimeException ignored) {}",
        "KuroDialogs581 safe button dismiss",
    )
    dg_text = patch_once(
        dg_text,
        "safeShow155(dialog, activity);\n            return dialog;",
        "            dialog.show();\n            return dialog;",
        "            safeShow155(dialog, activity);\n            return dialog;",
        "KuroDialogs581 Builder.show safeShow155",
    )

    kuro_autonext_and_display_ui_155 = """
    public interface DisplayHost155 {
        boolean isAutoNextEnabled();
        void setAutoNextEnabled(boolean on);
        void onDisplayApplied(boolean recreateNow);
        void onPreviewUpNext();
    }

    public static final class AutoNextHandle155 {
        public final Dialog dialog;
        public final android.widget.ImageView posterView;
        private final TextView timerBadge;
        private final TextView statusLine;
        private final View progressFill;
        private int trackWidthPx = 0;
        public boolean paused = false;

        AutoNextHandle155(Dialog d, android.widget.ImageView pv, TextView tb, TextView sl, View pf) {
            dialog = d;
            posterView = pv;
            timerBadge = tb;
            statusLine = sl;
            progressFill = pf;
        }

        public void updateProgress(long remainingMs, long totalMs, String epTag) {
            if (paused) return;
            int sec = (int) Math.max(1L, (remainingMs + 999L) / 1000L);
            timerBadge.setText("\\u23f8 " + sec + "s");
            statusLine.setText("Playing " + epTag + " in " + sec + (sec == 1 ? " second" : " seconds") + " \\u00b7 Tap timer to hold");
            float frac = totalMs > 0 ? Math.max(0f, Math.min(1f, 1f - (remainingMs / (float) totalMs))) : 1f;
            View parent = (View) progressFill.getParent();
            int w = parent != null ? parent.getWidth() : trackWidthPx;
            if (w > 0) {
                trackWidthPx = w;
                ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
                int targetW = Math.max(1, Math.round(w * frac));
                if (lp.width != targetW) {
                    lp.width = targetW;
                    progressFill.setLayoutParams(lp);
                }
            }
        }
    }

    private static int sdp155(Context c, float dpVal, float scale) {
        return Math.max(1, Math.round(dpVal * scale * c.getResources().getDisplayMetrics().density));
    }

    private static GradientDrawable box155(int fill, int stroke, int strokePx, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radiusPx);
        if (strokePx > 0) g.setStroke(strokePx, stroke);
        return g;
    }

    private static StateListDrawable pillStates155(boolean primary, int strokePx, float radiusPx) {
        StateListDrawable s = new StateListDrawable();
        int focusFill = primary ? PURPLE_SOFT : SELECTED;
        int idleFill = primary ? PURPLE : CARD;
        int idleStroke = primary ? PURPLE : BORDER;
        s.addState(new int[]{android.R.attr.state_focused}, box155(focusFill, PURPLE_SOFT, strokePx, radiusPx));
        s.addState(new int[]{android.R.attr.state_pressed}, box155(focusFill, PURPLE_SOFT, strokePx, radiusPx));
        s.addState(new int[]{}, box155(idleFill, idleStroke, strokePx, radiusPx));
        return s;
    }

    private static void attachBounce155(View v) {
        v.setOnTouchListener((view, ev) -> {
            int a = ev.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start();
            } else if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(130).start();
            }
            return false;
        });
    }

    public static AutoNextHandle155 createAutoNext155(
            Activity activity,
            String showTitle,
            String epBadgeText,
            String epHeadline,
            String epMetaLine,
            int totalSeconds,
            Runnable onCancel,
            Runnable onPlayNow,
            Runnable onOpenTuning
    ) {
        KuroDisplay155.enforce500Dpi(activity);
        retint();
        final float sc = KuroDisplay155.upNextScaleFactor(activity);
        final int stroke1 = Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density));

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setTag("kuro-autonext-card-155");
        int padH = sdp155(activity, 18, sc);
        int padV = sdp155(activity, 16, sc);
        root.setPadding(padH, padV, padH, padV);
        root.setBackground(box155(PANEL, BORDER, stroke1, sdp155(activity, 22, sc)));

        // Top accent glow bar matching KuroDialogs581 & KuroUpdateDialog582
        View accentBar = new View(activity);
        GradientDrawable glow = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{PURPLE, PURPLE_SOFT, KuroTheme12.col(0x00a65be4)}
        );
        glow.setCornerRadius(sdp155(activity, 99, sc));
        accentBar.setBackground(glow);
        LinearLayout.LayoutParams glowLp = new LinearLayout.LayoutParams(sdp155(activity, 68, sc), Math.max(sdp155(activity, 3, sc), 3));
        glowLp.bottomMargin = sdp155(activity, 12, sc);
        root.addView(accentBar, glowLp);

        // Header Row: [▶ UP NEXT] ............ [⏸ 8s] [⚙]
        LinearLayout headRow = new LinearLayout(activity);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView upNextPill = new TextView(activity);
        upNextPill.setText("\\u25b6  UP NEXT");
        upNextPill.setTextColor(PURPLE_SOFT);
        upNextPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f * sc);
        upNextPill.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        upNextPill.setLetterSpacing(0.08f);
        upNextPill.setPadding(sdp155(activity, 10, sc), sdp155(activity, 4, sc), sdp155(activity, 10, sc), sdp155(activity, 4, sc));
        upNextPill.setBackground(box155(SELECTED, PURPLE, stroke1, sdp155(activity, 999, sc)));
        headRow.addView(upNextPill, new LinearLayout.LayoutParams(-2, -2));

        View headSpacer = new View(activity);
        headRow.addView(headSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        final TextView timerChip = new TextView(activity);
        timerChip.setText("\\u23f8 " + totalSeconds + "s");
        timerChip.setTextColor(INK);
        timerChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f * sc);
        timerChip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        timerChip.setGravity(Gravity.CENTER);
        timerChip.setFocusable(true);
        timerChip.setClickable(true);
        timerChip.setPadding(sdp155(activity, 10, sc), sdp155(activity, 4, sc), sdp155(activity, 10, sc), sdp155(activity, 4, sc));
        timerChip.setBackground(pillStates155(false, stroke1, sdp155(activity, 999, sc)));
        headRow.addView(timerChip, new LinearLayout.LayoutParams(-2, -2));

        if (onOpenTuning != null) {
            TextView tuneBtn = new TextView(activity);
            tuneBtn.setText("\\u2699");
            tuneBtn.setContentDescription("Display, DPI and Auto-Next settings");
            tuneBtn.setTextColor(MUTED);
            tuneBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * sc);
            tuneBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tuneBtn.setGravity(Gravity.CENTER);
            tuneBtn.setFocusable(true);
            tuneBtn.setClickable(true);
            tuneBtn.setBackground(pillStates155(false, stroke1, sdp155(activity, 999, sc)));
            int gearSz = sdp155(activity, 28, sc);
            LinearLayout.LayoutParams gearLp = new LinearLayout.LayoutParams(gearSz, gearSz);
            gearLp.leftMargin = sdp155(activity, 6, sc);
            tuneBtn.setOnClickListener(v -> {
                try { dialog.dismiss(); } catch (RuntimeException ignored) {}
                onOpenTuning.run();
            });
            headRow.addView(tuneBtn, gearLp);
        }

        root.addView(headRow, new LinearLayout.LayoutParams(-1, -2));

        // Episode Media Card: [Poster + EP Badge] + [Series Title / Episode Headline / Meta]
        LinearLayout mediaCard = new LinearLayout(activity);
        mediaCard.setOrientation(LinearLayout.HORIZONTAL);
        mediaCard.setGravity(Gravity.CENTER_VERTICAL);
        int mcPad = sdp155(activity, 11, sc);
        mediaCard.setPadding(mcPad, mcPad, mcPad, mcPad);
        mediaCard.setBackground(box155(CARD, DIVIDER, stroke1, sdp155(activity, 15, sc)));
        LinearLayout.LayoutParams mcLp = new LinearLayout.LayoutParams(-1, -2);
        mcLp.topMargin = sdp155(activity, 12, sc);
        root.addView(mediaCard, mcLp);

        int posterW = sdp155(activity, 54, sc);
        int posterH = sdp155(activity, 74, sc);
        final int posterRad = sdp155(activity, 10, sc);
        android.widget.FrameLayout posterWrap = new android.widget.FrameLayout(activity);
        posterWrap.setBackground(box155(SELECTED, BORDER, stroke1, posterRad));
        posterWrap.setClipToOutline(true);
        posterWrap.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), posterRad);
            }
        });

        TextView posterFallback = new TextView(activity);
        posterFallback.setText("\\u25b6");
        posterFallback.setTextColor(PURPLE_SOFT);
        posterFallback.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f * sc);
        posterFallback.setGravity(Gravity.CENTER);
        posterWrap.addView(posterFallback, new android.widget.FrameLayout.LayoutParams(-1, -1));

        android.widget.ImageView posterImg = new android.widget.ImageView(activity);
        posterImg.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        posterWrap.addView(posterImg, new android.widget.FrameLayout.LayoutParams(-1, -1));

        if (epBadgeText != null && !epBadgeText.isEmpty()) {
            TextView epCorner = new TextView(activity);
            epCorner.setText(epBadgeText);
            epCorner.setTextColor(Color.WHITE);
            epCorner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f * sc);
            epCorner.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            epCorner.setPadding(sdp155(activity, 5, sc), sdp155(activity, 2, sc), sdp155(activity, 5, sc), sdp155(activity, 2, sc));
            epCorner.setBackground(box155(KuroTheme12.col(0x99000000), PURPLE, stroke1, sdp155(activity, 6, sc)));
            android.widget.FrameLayout.LayoutParams badgeLp = new android.widget.FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
            int bm = sdp155(activity, 4, sc);
            badgeLp.setMargins(bm, bm, bm, bm);
            posterWrap.addView(epCorner, badgeLp);
        }

        mediaCard.addView(posterWrap, new LinearLayout.LayoutParams(posterW, posterH));

        LinearLayout metaCol = new LinearLayout(activity);
        metaCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(0, -2, 1f);
        metaLp.leftMargin = sdp155(activity, 12, sc);
        mediaCard.addView(metaCol, metaLp);

        if (showTitle != null && !showTitle.isEmpty()) {
            TextView showTv = new TextView(activity);
            showTv.setText(showTitle);
            showTv.setTextColor(MUTED);
            showTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f * sc);
            showTv.setSingleLine(true);
            showTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            metaCol.addView(showTv, new LinearLayout.LayoutParams(-1, -2));
        }

        TextView epHeadTv = new TextView(activity);
        epHeadTv.setText(epHeadline);
        epHeadTv.setTextColor(INK);
        epHeadTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f * sc);
        epHeadTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        epHeadTv.setMaxLines(2);
        epHeadTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams ehLp = new LinearLayout.LayoutParams(-1, -2);
        ehLp.topMargin = sdp155(activity, 2, sc);
        metaCol.addView(epHeadTv, ehLp);

        if (epMetaLine != null && !epMetaLine.isEmpty()) {
            TextView metaTv = new TextView(activity);
            metaTv.setText(epMetaLine);
            metaTv.setTextColor(PURPLE_SOFT);
            metaTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f * sc);
            metaTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            metaTv.setSingleLine(true);
            metaTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams mtLp = new LinearLayout.LayoutParams(-1, -2);
            mtLp.topMargin = sdp155(activity, 4, sc);
            metaCol.addView(metaTv, mtLp);
        }

        // Smooth Animated Countdown Bar
        int barH = Math.max(4, sdp155(activity, 6, sc));
        int barRad = sdp155(activity, 99, sc);
        android.widget.FrameLayout barTrack = new android.widget.FrameLayout(activity);
        barTrack.setBackground(box155(DIVIDER, 0, 0, barRad));
        LinearLayout.LayoutParams btLp = new LinearLayout.LayoutParams(-1, barH);
        btLp.topMargin = sdp155(activity, 13, sc);
        root.addView(barTrack, btLp);

        View barFill = new View(activity);
        GradientDrawable fillGrad = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{PURPLE, PURPLE_SOFT}
        );
        fillGrad.setCornerRadius(barRad);
        barFill.setBackground(fillGrad);
        barTrack.addView(barFill, new android.widget.FrameLayout.LayoutParams(1, -1));

        final TextView statusTv = new TextView(activity);
        String cleanEpTag = (epBadgeText != null && !epBadgeText.isEmpty()) ? epBadgeText : "next episode";
        statusTv.setText("Playing " + cleanEpTag + " in " + totalSeconds + " seconds \\u00b7 Tap timer to hold");
        statusTv.setTextColor(MUTED);
        statusTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f * sc);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-1, -2);
        stLp.topMargin = sdp155(activity, 7, sc);
        root.addView(statusTv, stLp);

        // Action Row: [Cancel]   [▶ Play Ep X]
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(-1, -2);
        actLp.topMargin = sdp155(activity, 14, sc);
        root.addView(actions, actLp);

        int btnH = sdp155(activity, 44, sc);
        int btnRad = sdp155(activity, 13, sc);

        TextView cancelBtn = new TextView(activity);
        cancelBtn.setText("Cancel");
        cancelBtn.setTextColor(INK);
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * sc);
        cancelBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        cancelBtn.setGravity(Gravity.CENTER);
        cancelBtn.setMinHeight(btnH);
        cancelBtn.setFocusable(true);
        cancelBtn.setClickable(true);
        cancelBtn.setBackground(pillStates155(false, stroke1, btnRad));
        attachBounce155(cancelBtn);
        cancelBtn.setOnClickListener(v -> {
            if (onCancel != null) onCancel.run();
        });
        actions.addView(cancelBtn, new LinearLayout.LayoutParams(0, btnH, 1f));

        TextView playBtn = new TextView(activity);
        String playLabel = (epBadgeText != null && !epBadgeText.isEmpty()) ? ("\\u25b6  Play " + epBadgeText) : "\\u25b6  Play now";
        playBtn.setText(playLabel);
        playBtn.setTextColor(KuroTheme12.col(0xff140b1a));
        playBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f * sc);
        playBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        playBtn.setGravity(Gravity.CENTER);
        playBtn.setMinHeight(btnH);
        playBtn.setFocusable(true);
        playBtn.setClickable(true);
        playBtn.setBackground(pillStates155(true, stroke1, btnRad));
        attachBounce155(playBtn);
        playBtn.setOnClickListener(v -> {
            if (onPlayNow != null) onPlayNow.run();
        });
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(0, btnH, 1.38f);
        playLp.leftMargin = sdp155(activity, 9, sc);
        actions.addView(playBtn, playLp);

        final AutoNextHandle155 handle = new AutoNextHandle155(dialog, posterImg, timerChip, statusTv, barFill);
        timerChip.setOnClickListener(v -> {
            handle.paused = !handle.paused;
            if (handle.paused) {
                timerChip.setText("\\u25b6 Hold");
                statusTv.setText("Countdown paused \\u00b7 Tap Play when ready");
            } else {
                timerChip.setText("\\u23f8 Resumed");
            }
        });

        dialog.setContentView(root);
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams attr = window.getAttributes();
            attr.dimAmount = 0.62f;
            android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            int maxW = sdp155(activity, 396, sc);
            attr.width = Math.max(sdp155(activity, 260, 1f), Math.min(maxW, Math.round(dm.widthPixels * 0.90f)));
            attr.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(attr);
            root.setAlpha(0f);
            root.setScaleX(0.97f);
            root.setScaleY(0.97f);
            root.setTranslationY(sdp155(activity, 14, 1f));
            root.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
            playBtn.requestFocus();
        });

        return handle;
    }

    public static Dialog openDisplayController155(Activity activity, DisplayHost155 host) {
        if (!canShow155(activity)) return null;
        KuroDisplay155.enforce500Dpi(activity);
        retint();
        final int stroke1 = Math.max(1, Math.round(activity.getResources().getDisplayMetrics().density));

        final int sysDpi = KuroDisplay155.systemDpi();
        final int[] draftDpi = {KuroDisplay155.configuredDpi(activity)};
        final int[] draftScale = {KuroDisplay155.uiScale(activity)};
        final int[] draftSec = {KuroDisplay155.autoNextSeconds(activity)};
        final int[] draftCard = {KuroDisplay155.upNextScale(activity)};
        final boolean[] draftAutoNext = {host == null || host.isAutoNextEnabled()};

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setBackground(box155(PANEL, BORDER, stroke1, sdp155(activity, 22, 1f)));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = sdp155(activity, 18, 1f);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root, new ViewGroup.LayoutParams(-1, -2));

        View accentBar = new View(activity);
        GradientDrawable glow = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{PURPLE, PURPLE_SOFT, KuroTheme12.col(0x00a65be4)}
        );
        glow.setCornerRadius(sdp155(activity, 99, 1f));
        accentBar.setBackground(glow);
        LinearLayout.LayoutParams glowLp = new LinearLayout.LayoutParams(sdp155(activity, 64, 1f), Math.max(3, sdp155(activity, 3, 1f)));
        glowLp.bottomMargin = sdp155(activity, 10, 1f);
        root.addView(accentBar, glowLp);

        TextView titleTv = new TextView(activity);
        titleTv.setText("Display, App DPI & Auto-Next");
        titleTv.setTextColor(INK);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(titleTv, new LinearLayout.LayoutParams(-1, -2));

        final TextView subTv = new TextView(activity);
        subTv.setTextColor(MUTED);
        subTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = sdp155(activity, 3, 1f);
        subLp.bottomMargin = sdp155(activity, 12, 1f);
        root.addView(subTv, subLp);

        // Section 1: App DPI
        LinearLayout dpiBox = new LinearLayout(activity);
        dpiBox.setOrientation(LinearLayout.VERTICAL);
        int boxPad = sdp155(activity, 12, 1f);
        dpiBox.setPadding(boxPad, boxPad, boxPad, boxPad);
        dpiBox.setBackground(box155(CARD, DIVIDER, stroke1, sdp155(activity, 14, 1f)));
        root.addView(dpiBox, new LinearLayout.LayoutParams(-1, -2));

        final TextView dpiReadout = new TextView(activity);
        dpiReadout.setTextColor(INK);
        dpiReadout.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        dpiReadout.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dpiBox.addView(dpiReadout, new LinearLayout.LayoutParams(-1, -2));

        final android.widget.SeekBar dpiSeek = new android.widget.SeekBar(activity);
        dpiSeek.setMax(36); // 280 + i*10 -> 280..640
        LinearLayout.LayoutParams skLp = new LinearLayout.LayoutParams(-1, -2);
        skLp.topMargin = sdp155(activity, 6, 1f);
        dpiBox.addView(dpiSeek, skLp);

        android.widget.HorizontalScrollView dpiHScroll = new android.widget.HorizontalScrollView(activity);
        dpiHScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout dpiChipRow = new LinearLayout(activity);
        dpiChipRow.setOrientation(LinearLayout.HORIZONTAL);
        dpiHScroll.addView(dpiChipRow, new ViewGroup.LayoutParams(-2, -2));
        LinearLayout.LayoutParams dchLp = new LinearLayout.LayoutParams(-1, -2);
        dchLp.topMargin = sdp155(activity, 6, 1f);
        dpiBox.addView(dpiHScroll, dchLp);

        final int[] dpiPresetVals = {0, 360, 400, 440, 480, 500, 540, 600};
        final List<TextView> dpiPresetViews = new ArrayList<>();

        // Section 2: App UI Size
        LinearLayout scaleBox = new LinearLayout(activity);
        scaleBox.setOrientation(LinearLayout.VERTICAL);
        scaleBox.setPadding(boxPad, boxPad, boxPad, boxPad);
        scaleBox.setBackground(box155(CARD, DIVIDER, stroke1, sdp155(activity, 14, 1f)));
        LinearLayout.LayoutParams scBoxLp = new LinearLayout.LayoutParams(-1, -2);
        scBoxLp.topMargin = sdp155(activity, 10, 1f);
        root.addView(scaleBox, scBoxLp);

        final TextView scaleReadout = new TextView(activity);
        scaleReadout.setTextColor(INK);
        scaleReadout.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        scaleReadout.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scaleBox.addView(scaleReadout, new LinearLayout.LayoutParams(-1, -2));

        final android.widget.SeekBar scaleSeek = new android.widget.SeekBar(activity);
        scaleSeek.setMax(12); // 75 + i*5 -> 75..135
        LinearLayout.LayoutParams sskLp = new LinearLayout.LayoutParams(-1, -2);
        sskLp.topMargin = sdp155(activity, 6, 1f);
        scaleBox.addView(scaleSeek, sskLp);

        android.widget.HorizontalScrollView scaleHScroll = new android.widget.HorizontalScrollView(activity);
        scaleHScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout scaleChipRow = new LinearLayout(activity);
        scaleChipRow.setOrientation(LinearLayout.HORIZONTAL);
        scaleHScroll.addView(scaleChipRow, new ViewGroup.LayoutParams(-2, -2));
        LinearLayout.LayoutParams schLp = new LinearLayout.LayoutParams(-1, -2);
        schLp.topMargin = sdp155(activity, 6, 1f);
        scaleBox.addView(scaleHScroll, schLp);

        final int[] scalePresetVals = {80, 90, 95, 100, 105, 110, 120};
        final List<TextView> scalePresetViews = new ArrayList<>();

        // Section 3: Auto-Next Countdown & Card Size
        LinearLayout anBox = new LinearLayout(activity);
        anBox.setOrientation(LinearLayout.VERTICAL);
        anBox.setPadding(boxPad, boxPad, boxPad, boxPad);
        anBox.setBackground(box155(CARD, DIVIDER, stroke1, sdp155(activity, 14, 1f)));
        LinearLayout.LayoutParams anBoxLp = new LinearLayout.LayoutParams(-1, -2);
        anBoxLp.topMargin = sdp155(activity, 10, 1f);
        root.addView(anBox, anBoxLp);

        LinearLayout anHead = new LinearLayout(activity);
        anHead.setOrientation(LinearLayout.HORIZONTAL);
        anHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView anTitle = new TextView(activity);
        anTitle.setText("Auto-Play Next & Card Size");
        anTitle.setTextColor(INK);
        anTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        anTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        anHead.addView(anTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        final TextView anToggle = new TextView(activity);
        anToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        anToggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        anToggle.setPadding(sdp155(activity, 11, 1f), sdp155(activity, 5, 1f), sdp155(activity, 11, 1f), sdp155(activity, 5, 1f));
        anToggle.setFocusable(true);
        anToggle.setClickable(true);
        anHead.addView(anToggle, new LinearLayout.LayoutParams(-2, -2));
        anBox.addView(anHead, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout secRow = new LinearLayout(activity);
        secRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams secRowLp = new LinearLayout.LayoutParams(-1, -2);
        secRowLp.topMargin = sdp155(activity, 8, 1f);
        anBox.addView(secRow, secRowLp);
        final int[] secVals = {5, 8, 10, 15};
        final List<TextView> secViews = new ArrayList<>();

        LinearLayout cardRow = new LinearLayout(activity);
        cardRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cardRowLp = new LinearLayout.LayoutParams(-1, -2);
        cardRowLp.topMargin = sdp155(activity, 7, 1f);
        anBox.addView(cardRow, cardRowLp);
        final int[] cardVals = {85, 100, 115};
        final String[] cardNames = {"Compact", "Default", "Large"};
        final List<TextView> cardViews = new ArrayList<>();

        final boolean[] syncingSeek = {false};
        final Runnable refreshUi = () -> {
            int base = draftDpi[0] <= 0 ? sysDpi : draftDpi[0];
            int eff = Math.max(220, Math.min(760, Math.round(base * (draftScale[0] / 100f))));
            String dLbl = draftDpi[0] <= 0 ? ("System (" + sysDpi + " DPI)") : (draftDpi[0] + " DPI");
            subTv.setText(dLbl + " \\u00b7 UI Size " + draftScale[0] + "% \\u00b7 Effective " + eff + " DPI");
            dpiReadout.setText("App DPI: " + dLbl);
            scaleReadout.setText("App UI Size: " + draftScale[0] + "% (" + eff + " eff DPI)");

            syncingSeek[0] = true;
            dpiSeek.setProgress(Math.max(0, Math.min(36, Math.round((base - 280) / 10f))));
            scaleSeek.setProgress(Math.max(0, Math.min(12, Math.round((draftScale[0] - 75) / 5f))));
            syncingSeek[0] = false;

            for (int i = 0; i < dpiPresetViews.size(); i++) {
                boolean on = draftDpi[0] == dpiPresetVals[i];
                TextView tv = dpiPresetViews.get(i);
                tv.setTextColor(on ? KuroTheme12.col(0xff140b1a) : INK);
                tv.setBackground(pillStates155(on, stroke1, sdp155(activity, 999, 1f)));
            }
            for (int i = 0; i < scalePresetViews.size(); i++) {
                boolean on = draftScale[0] == scalePresetVals[i];
                TextView tv = scalePresetViews.get(i);
                tv.setTextColor(on ? KuroTheme12.col(0xff140b1a) : INK);
                tv.setBackground(pillStates155(on, stroke1, sdp155(activity, 999, 1f)));
            }
            anToggle.setText(draftAutoNext[0] ? "Auto-Next ON" : "Auto-Next OFF");
            anToggle.setTextColor(draftAutoNext[0] ? KuroTheme12.col(0xff140b1a) : INK);
            anToggle.setBackground(pillStates155(draftAutoNext[0], stroke1, sdp155(activity, 999, 1f)));

            for (int i = 0; i < secViews.size(); i++) {
                boolean on = draftSec[0] == secVals[i];
                TextView tv = secViews.get(i);
                tv.setTextColor(on ? KuroTheme12.col(0xff140b1a) : INK);
                tv.setBackground(pillStates155(on, stroke1, sdp155(activity, 999, 1f)));
            }
            for (int i = 0; i < cardViews.size(); i++) {
                boolean on = Math.abs(draftCard[0] - cardVals[i]) <= 4;
                TextView tv = cardViews.get(i);
                tv.setTextColor(on ? KuroTheme12.col(0xff140b1a) : INK);
                tv.setBackground(pillStates155(on, stroke1, sdp155(activity, 999, 1f)));
            }
        };

        for (int i = 0; i < dpiPresetVals.length; i++) {
            final int val = dpiPresetVals[i];
            TextView chip = new TextView(activity);
            chip.setText(val == 0 ? ("System (" + sysDpi + ")") : String.valueOf(val));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setPadding(sdp155(activity, 11, 1f), sdp155(activity, 6, 1f), sdp155(activity, 11, 1f), sdp155(activity, 6, 1f));
            chip.setOnClickListener(v -> { draftDpi[0] = val; refreshUi.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (i > 0) lp.leftMargin = sdp155(activity, 6, 1f);
            dpiChipRow.addView(chip, lp);
            dpiPresetViews.add(chip);
        }

        for (int i = 0; i < scalePresetVals.length; i++) {
            final int val = scalePresetVals[i];
            TextView chip = new TextView(activity);
            chip.setText(val + "%");
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setPadding(sdp155(activity, 11, 1f), sdp155(activity, 6, 1f), sdp155(activity, 11, 1f), sdp155(activity, 6, 1f));
            chip.setOnClickListener(v -> { draftScale[0] = val; refreshUi.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            if (i > 0) lp.leftMargin = sdp155(activity, 6, 1f);
            scaleChipRow.addView(chip, lp);
            scalePresetViews.add(chip);
        }

        for (int i = 0; i < secVals.length; i++) {
            final int val = secVals[i];
            TextView chip = new TextView(activity);
            chip.setText(val + "s");
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setPadding(0, sdp155(activity, 6, 1f), 0, sdp155(activity, 6, 1f));
            chip.setOnClickListener(v -> { draftSec[0] = val; refreshUi.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i > 0) lp.leftMargin = sdp155(activity, 6, 1f);
            secRow.addView(chip, lp);
            secViews.add(chip);
        }

        for (int i = 0; i < cardVals.length; i++) {
            final int val = cardVals[i];
            TextView chip = new TextView(activity);
            chip.setText("Card: " + cardNames[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setPadding(0, sdp155(activity, 6, 1f), 0, sdp155(activity, 6, 1f));
            chip.setOnClickListener(v -> { draftCard[0] = val; refreshUi.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i > 0) lp.leftMargin = sdp155(activity, 6, 1f);
            cardRow.addView(chip, lp);
            cardViews.add(chip);
        }

        dpiSeek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int progress, boolean fromUser) {
                if (!fromUser || syncingSeek[0]) return;
                draftDpi[0] = 280 + progress * 10;
                refreshUi.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        });

        scaleSeek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar sb, int progress, boolean fromUser) {
                if (!fromUser || syncingSeek[0]) return;
                draftScale[0] = 75 + progress * 5;
                refreshUi.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar sb) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar sb) {}
        });

        anToggle.setOnClickListener(v -> { draftAutoNext[0] = !draftAutoNext[0]; refreshUi.run(); });

        // Footer Buttons: [Reset] [▶ Preview] [✓ Apply]
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(-1, -2);
        actLp.topMargin = sdp155(activity, 14, 1f);
        root.addView(actions, actLp);

        int btnH = sdp155(activity, 42, 1f);
        int btnRad = sdp155(activity, 12, 1f);

        TextView resetBtn = new TextView(activity);
        resetBtn.setText("Reset");
        resetBtn.setTextColor(INK);
        resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        resetBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setFocusable(true);
        resetBtn.setClickable(true);
        resetBtn.setBackground(pillStates155(false, stroke1, btnRad));
        resetBtn.setOnClickListener(v -> {
            draftDpi[0] = 500;
            draftScale[0] = 100;
            draftSec[0] = 8;
            draftCard[0] = 100;
            draftAutoNext[0] = true;
            refreshUi.run();
        });
        actions.addView(resetBtn, new LinearLayout.LayoutParams(0, btnH, 0.85f));

        TextView prevBtn = new TextView(activity);
        prevBtn.setText("\\u25b6 Preview");
        prevBtn.setTextColor(INK);
        prevBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        prevBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        prevBtn.setGravity(Gravity.CENTER);
        prevBtn.setFocusable(true);
        prevBtn.setClickable(true);
        prevBtn.setBackground(pillStates155(false, stroke1, btnRad));
        prevBtn.setOnClickListener(v -> {
            KuroDisplay155.saveConfig(activity, draftDpi[0], draftScale[0], draftSec[0], draftCard[0]);
            if (host != null) host.setAutoNextEnabled(draftAutoNext[0]);
            try { dialog.dismiss(); } catch (RuntimeException ignored) {}
            if (host != null) host.onPreviewUpNext();
        });
        LinearLayout.LayoutParams pvLp = new LinearLayout.LayoutParams(0, btnH, 1.05f);
        pvLp.leftMargin = sdp155(activity, 7, 1f);
        actions.addView(prevBtn, pvLp);

        TextView applyBtn = new TextView(activity);
        applyBtn.setText("\\u2713 Apply");
        applyBtn.setTextColor(KuroTheme12.col(0xff140b1a));
        applyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        applyBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        applyBtn.setGravity(Gravity.CENTER);
        applyBtn.setFocusable(true);
        applyBtn.setClickable(true);
        applyBtn.setBackground(pillStates155(true, stroke1, btnRad));
        applyBtn.setOnClickListener(v -> {
            boolean densityChanged = draftDpi[0] != KuroDisplay155.configuredDpi(activity)
                || draftScale[0] != KuroDisplay155.uiScale(activity);
            KuroDisplay155.saveConfig(activity, draftDpi[0], draftScale[0], draftSec[0], draftCard[0]);
            if (host != null) host.setAutoNextEnabled(draftAutoNext[0]);
            try { dialog.dismiss(); } catch (RuntimeException ignored) {}
            if (host != null) host.onDisplayApplied(densityChanged);
        });
        LinearLayout.LayoutParams apLp = new LinearLayout.LayoutParams(0, btnH, 1.15f);
        apLp.leftMargin = sdp155(activity, 7, 1f);
        actions.addView(applyBtn, apLp);

        refreshUi.run();
        dialog.setContentView(scroll);
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams attr = window.getAttributes();
            attr.dimAmount = 0.72f;
            android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            attr.width = Math.min(sdp155(activity, 430, 1f), Math.round(dm.widthPixels * 0.92f));
            attr.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(attr);
            applyBtn.requestFocus();
        });

        safeShow155(dialog, activity);
        return dialog;
    }
}
"""
    if "createAutoNext155(" not in dg_text:
        if not dg_text.rstrip().endswith("    }\n}"):
            raise SystemExit("ui_1.5.4.py: unexpected tail of KuroDialogs581.java")
        idx = dg_text.rfind("}")
        dg_text = dg_text[:idx] + kuro_autonext_and_display_ui_155
    dg_path.write_text(dg_text, encoding="utf-8")

    # 4. Keep system default responsive viewport in assets/index.html + allow 500DPI DPR in video-effects537.js
    ix_path = root / "assets/index.html"
    ix_text = ix_path.read_text(encoding="utf-8")
    if 'content="width=461,viewport-fit=cover"' in ix_text:
        ix_text = ix_text.replace(
            'content="width=461,viewport-fit=cover"',
            'content="width=device-width,initial-scale=1,viewport-fit=cover"',
            1,
        )
        ix_path.write_text(ix_text, encoding="utf-8")
    if 'content="width=device-width,initial-scale=1,viewport-fit=cover"' not in ix_text:
        raise SystemExit("ui_1.5.4.py: missing system-default viewport meta in assets/index.html")

    ve_path = root / "assets/video-effects537.js"
    ve_text = ve_path.read_text(encoding="utf-8")
    ve_text = patch_once(
        ve_text,
        "Math.min(devicePixelRatio||1,4)",
        "dpr=Math.max(1,Math.min(devicePixelRatio||1,3))",
        "dpr=Math.max(1,Math.min(devicePixelRatio||1,4))",
        "video-effects537.js 500DPI (3.125 DPR) WebGL2 support",
    )
    ve_path.write_text(ve_text, encoding="utf-8")


def patch_stability_and_autonext_155(root: pathlib.Path) -> None:
    """Fix remaining Android crash vectors, offline DownloadManager thread affinity,
    batch download ID compatibility, and end-to-end Auto-Play Next across all players."""

    # 1. KuroOffline561: main-looper DownloadManager init + validDownloadId155 + batch checkpoint/ID fix + themed/safe dialogs
    ko_path = root / "src/app/lovelycrxshx/kuro/KuroOffline561.java"
    ko_text = ko_path.read_text(encoding="utf-8")
    old_mgr = """    static synchronized DownloadManager manager(Context c){
        if(manager!=null)return manager;
        Context app=c.getApplicationContext();
        DatabaseProvider db=new StandaloneDatabaseProvider(app);
        cache=new SimpleCache(new File(app.getFilesDir(),"kuro-offline561"),new NoOpCacheEvictor(),db);
        androidx.media3.datasource.okhttp.OkHttpDataSource.Factory http=KuroHttp561.factory();
        manager=new DownloadManager(app,db,cache,http,DOWNLOAD_EXECUTOR);
        manager.setMaxParallelDownloads(1);manager.setMinRetryCount(0);
        return manager;
    }"""
    new_mgr = """    private static volatile DownloadManager volatileManager155;
    private static DownloadManager createManagerOnMain155(Context app){
        synchronized(KuroOffline561.class){
            if(manager!=null)return manager;
            DatabaseProvider db=new StandaloneDatabaseProvider(app);
            cache=new SimpleCache(new File(app.getFilesDir(),"kuro-offline561"),new NoOpCacheEvictor(),db);
            androidx.media3.datasource.okhttp.OkHttpDataSource.Factory http=KuroHttp561.factory();
            manager=new DownloadManager(app,db,cache,http,DOWNLOAD_EXECUTOR);
            manager.setMaxParallelDownloads(1);manager.setMinRetryCount(0);
            volatileManager155=manager;
            return manager;
        }
    }
    static DownloadManager manager(Context c){
        DownloadManager fast=volatileManager155;
        if(fast!=null)return fast;
        final Context app=c.getApplicationContext();
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper()){
            return createManagerOnMain155(app);
        }
        final DownloadManager[] box=new DownloadManager[1];
        final RuntimeException[] err=new RuntimeException[1];
        final java.util.concurrent.CountDownLatch latch=new java.util.concurrent.CountDownLatch(1);
        new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
            try{box[0]=createManagerOnMain155(app);}catch(RuntimeException e){err[0]=e;}finally{latch.countDown();}
        });
        try{
            if(!latch.await(8,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("DownloadManager main-looper init timed out");
        }catch(InterruptedException ie){
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DownloadManager init interrupted",ie);
        }
        if(err[0]!=null)throw err[0];
        if(box[0]==null)throw new IllegalStateException("DownloadManager unavailable");
        return box[0];
    }"""
    ko_text = patch_once(ko_text, "createManagerOnMain155", old_mgr, new_mgr, "KuroOffline561 main-looper DownloadManager")

    # Upgrade confirmBatchDownload154 even when pre-baked in 1.5.5-beta6 source
    old_baked_batch = """    static void confirmBatchDownload154(Activity activity, String rawJson) {
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
    }"""
    new_baked_batch = """    static boolean validDownloadId155(String id) {
        return KuroPlayback561.validKey(id) || (id != null && id.matches("kuro561:[0-9a-fA-F]{1,8}"));
    }

    private static String resolveBatchCheckpointKey155(String rawKey) {
        if (KuroPlayback561.validKey(rawKey)) return rawKey;
        if (rawKey != null && rawKey.startsWith("zenkai|")) {
            int first = rawKey.indexOf('|');
            int last = rawKey.lastIndexOf('|');
            if (first > 0 && last > first + 1 && last + 1 < rawKey.length()) {
                String showUrl = rawKey.substring(first + 1, last);
                String epNum = rawKey.substring(last + 1);
                String computed = KuroPlayback561.key("zenkai", showUrl, epNum);
                if (KuroPlayback561.validKey(computed)) return computed;
            }
        }
        return "";
    }

    static void confirmBatchDownload154(Activity activity, String rawJson) {
        if (!KuroDialogs581.canShow155(activity) || rawJson == null || rawJson.length() > 250000) return;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(KuroDialogs581.themed(activity))
                .setTitle("Batch download " + valid.size() + (valid.size() == 1 ? " episode?" : " episodes?"))
                .setMessage(showTitle + "\\n" + rangeLabel + "\\n\\nQueues verified direct HLS streams into Kuro offline storage. Only store content you have permission to keep.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Queue " + valid.size(), (d, w) -> {
                    int queued = 0;
                    try { manager(activity); } catch (RuntimeException ignored) {}
                    for (JSONObject row : valid) {
                        try {
                            String addr = row.optString("address", "");
                            String ckey = resolveBatchCheckpointKey155(row.optString("key", ""));
                            String t = limit(row.optString("title", "Video"), 120);
                            String ep = limit(row.optString("episode", ""), 20);
                            String id = KuroPlayback561.sha(ckey + "\\n" + addr);
                            JSONObject meta = new JSONObject().put("title", t).put("episode", ep).put("key", ckey);
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
                });
        KuroDialogs581.safeShowBuilder155(builder, activity);
    }"""
    if "resolveBatchCheckpointKey155" not in ko_text:
        ko_text = patch_once(ko_text, "resolveBatchCheckpointKey155", old_baked_batch, new_baked_batch, "KuroOffline561 upgrade confirmBatchDownload154")

    ko_text = patch_once(
        ko_text,
        "KuroDialogs581.safeShowBuilder155(new AlertDialog.Builder(KuroDialogs581.themed(activity)).setTitle(\"Download for offline viewing?\")",
        '        new AlertDialog.Builder(activity).setTitle("Download for offline viewing?")\n'
        '            .setMessage("Only continue if you own this video or the provider permits offline downloads. Kuro does not bypass DRM or access checks; protected media may not work offline. The private download index may contain the source URL; offline media is not included in Kuro\'s JSON backup. A download may include multiple qualities and use substantial storage or mobile data. The source may refuse downloading or expire.")\n'
        '            .setNegativeButton("Cancel",null).setPositiveButton("I have permission",(d,w)->{\n'
        '                try{\n'
        '                    JSONObject info=new JSONObject();info.put("title",limit(title,160));info.put("episode",limit(episode,20));info.put("key",KuroPlayback561.validKey(key)?key:"");\n'
        '                    String id=KuroPlayback561.sha((key==null?"":key)+"\\n"+address);\n'
        '                    DownloadRequest request=new DownloadRequest.Builder(id,android.net.Uri.parse(address)).setMimeType(MimeTypes.APPLICATION_M3U8).setData(info.toString().getBytes(StandardCharsets.UTF_8)).build();\n'
        '                    manager(activity);DownloadService.sendAddDownload(activity,KuroDownloadService561.class,request,false);DownloadService.sendResumeDownloads(activity,KuroDownloadService561.class,false);\n'
        '                    Toast.makeText(activity,"Download queued. Open Manage downloads to check its status.",Toast.LENGTH_LONG).show();\n'
        '                }catch(Exception e){Toast.makeText(activity,"Could not start the download on this device.",Toast.LENGTH_LONG).show();}\n'
        '            }).show();',
        '        KuroDialogs581.safeShowBuilder155(new AlertDialog.Builder(KuroDialogs581.themed(activity)).setTitle("Download for offline viewing?")\n'
        '            .setMessage("Only continue if you own this video or the provider permits offline downloads. Kuro does not bypass DRM or access checks; protected media may not work offline. The private download index may contain the source URL; offline media is not included in Kuro\'s JSON backup. A download may include multiple qualities and use substantial storage or mobile data. The source may refuse downloading or expire.")\n'
        '            .setNegativeButton("Cancel",null).setPositiveButton("I have permission",(d,w)->{\n'
        '                try{\n'
        '                    JSONObject info=new JSONObject();info.put("title",limit(title,160));info.put("episode",limit(episode,20));info.put("key",KuroPlayback561.validKey(key)?key:"");\n'
        '                    String id=KuroPlayback561.sha((key==null?"":key)+"\\n"+address);\n'
        '                    DownloadRequest request=new DownloadRequest.Builder(id,android.net.Uri.parse(address)).setMimeType(MimeTypes.APPLICATION_M3U8).setData(info.toString().getBytes(StandardCharsets.UTF_8)).build();\n'
        '                    manager(activity);DownloadService.sendAddDownload(activity,KuroDownloadService561.class,request,false);DownloadService.sendResumeDownloads(activity,KuroDownloadService561.class,false);\n'
        '                    Toast.makeText(activity,"Download queued. Open Manage downloads to check its status.",Toast.LENGTH_LONG).show();\n'
        '                }catch(Exception e){Toast.makeText(activity,"Could not start the download on this device.",Toast.LENGTH_LONG).show();}\n'
        '            }),activity);',
        "KuroOffline561 confirmDownload themed + safeShow",
    )
    ko_text = patch_once(
        ko_text,
        'AlertDialog.Builder b=new AlertDialog.Builder(KuroDialogs581.themed(a)).setTitle("Kuro downloads")',
        '    private static void showList(Activity a,List<Download> downloads){\n'
        '        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle("Kuro downloads").setNegativeButton("Close",null).setNeutralButton("Android downloads",(d,w)->{try{a.startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));}catch(Exception e){Toast.makeText(a,"Android Downloads is unavailable.",Toast.LENGTH_SHORT).show();}});\n'
        '        if(downloads.isEmpty())b.setMessage("No Kuro offline downloads yet. A Download button appears for a supported direct stream. Only download content you have permission to store.");\n'
        '        else{String[] labels=new String[downloads.size()];for(int i=0;i<labels.length;i++){Download x=downloads.get(i);JSONObject info=metadata(x);String title=info.optString("title","Video"),ep=info.optString("episode","");labels[i]=title+(ep.isEmpty()?"":" · Episode "+ep)+"\\n"+state(x);}b.setItems(labels,(d,which)->actions(a,downloads.get(which)));}b.show();\n'
        '    }',
        '    private static void showList(Activity a,List<Download> downloads){\n'
        '        if(!KuroDialogs581.canShow155(a))return;\n'
        '        AlertDialog.Builder b=new AlertDialog.Builder(KuroDialogs581.themed(a)).setTitle("Kuro downloads").setNegativeButton("Close",null).setNeutralButton("Android downloads",(d,w)->{try{a.startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));}catch(Exception e){Toast.makeText(a,"Android Downloads is unavailable.",Toast.LENGTH_SHORT).show();}});\n'
        '        if(downloads.isEmpty())b.setMessage("No Kuro offline downloads yet. A Download button appears for a supported direct stream. Only download content you have permission to store.");\n'
        '        else{String[] labels=new String[downloads.size()];for(int i=0;i<labels.length;i++){Download x=downloads.get(i);JSONObject info=metadata(x);String title=info.optString("title","Video"),ep=info.optString("episode","");labels[i]=title+(ep.isEmpty()?"":" · Episode "+ep)+"\\n"+state(x);}b.setItems(labels,(d,which)->actions(a,downloads.get(which)));}KuroDialogs581.safeShowBuilder155(b,a);\n'
        '    }',
        "KuroOffline561 showList themed + safeShow",
    )
    ko_text = patch_once(
        ko_text,
        "KuroDialogs581.safeShowBuilder155(new AlertDialog.Builder(KuroDialogs581.themed(a)).setTitle(metadata(download)",
        '    private static void actions(Activity a,Download download){\n'
        '        boolean ready=download.state==Download.STATE_COMPLETED,failed=download.state==Download.STATE_FAILED;String[] choices=ready?new String[]{"Play offline","Remove this download"}:failed?new String[]{"Retry this download","Remove this download"}:new String[]{"Remove this download"};\n'
        '        new AlertDialog.Builder(a).setTitle(metadata(download).optString("title","Download")).setItems(choices,(dialog,which)->{\n'
        '            if(ready&&which==0){Intent intent=new Intent(a,KuroOfflineActivity561.class);intent.putExtra("id",download.request.id);a.startActivity(intent);return;}\n'
        '            if(failed&&which==0){new AlertDialog.Builder(a).setTitle("Retry this download?").setMessage("Retry only when the provider permits it. If access or verification is required, stop and check it manually.").setNegativeButton("Cancel",null).setPositiveButton("Retry",(d,w)->{try{DownloadService.sendAddDownload(a,KuroDownloadService561.class,download.request,false);DownloadService.sendResumeDownloads(a,KuroDownloadService561.class,false);Toast.makeText(a,"Download queued again.",Toast.LENGTH_SHORT).show();}catch(RuntimeException e){Toast.makeText(a,"Could not retry this download.",Toast.LENGTH_SHORT).show();}}).show();return;}\n'
        '            new AlertDialog.Builder(a).setTitle("Remove this download?").setMessage("Only this offline copy is removed. Your lists and history stay.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{try{DownloadService.sendRemoveDownload(a,KuroDownloadService561.class,download.request.id,false);Toast.makeText(a,"Removing download.",Toast.LENGTH_SHORT).show();}catch(RuntimeException e){Toast.makeText(a,"Could not remove download.",Toast.LENGTH_SHORT).show();}}).show();\n'
        '        }).setNegativeButton("Back",(d,w)->showDownloads(a)).show();\n'
        '    }',
        '    private static void actions(Activity a,Download download){\n'
        '        if(!KuroDialogs581.canShow155(a))return;\n'
        '        boolean ready=download.state==Download.STATE_COMPLETED,failed=download.state==Download.STATE_FAILED;String[] choices=ready?new String[]{"Play offline","Remove this download"}:failed?new String[]{"Retry this download","Remove this download"}:new String[]{"Remove this download"};\n'
        '        KuroDialogs581.safeShowBuilder155(new AlertDialog.Builder(KuroDialogs581.themed(a)).setTitle(metadata(download).optString("title","Download")).setItems(choices,(dialog,which)->{\n'
        '            if(ready&&which==0){Intent intent=new Intent(a,KuroOfflineActivity561.class);intent.putExtra("id",download.request.id);a.startActivity(intent);return;}\n'
        '            if(failed&&which==0){KuroDialogs581.safeShowBuilder155(new AlertDialog.Builder(KuroDialogs581.themed(a)).setTitle("Retry this download?").setMessage("Retry only when the provider permits it. If access or verification is required, stop and check it manually.").setNegativeButton("Cancel",null).setPositiveButton("Retry",(d,w)->{try{DownloadService.sendAddDownload(a,KuroDownloadService561.class,download.request,false);DownloadService.sendResumeDownloads(a,KuroDownloadService561.class,false);Toast.makeText(a,"Download queued again.",Toast.LENGTH_SHORT).show();}catch(RuntimeException e){Toast.makeText(a,"Could not retry this download.",Toast.LENGTH_SHORT).show();}}),a);return;}\n'
        '            KuroDialogs581.safeShowBuilder155(new AlertDialog.Builder(KuroDialogs581.themed(a)).setTitle("Remove this download?").setMessage("Only this offline copy is removed. Your lists and history stay.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{try{DownloadService.sendRemoveDownload(a,KuroDownloadService561.class,download.request.id,false);Toast.makeText(a,"Removing download.",Toast.LENGTH_SHORT).show();}catch(RuntimeException e){Toast.makeText(a,"Could not remove download.",Toast.LENGTH_SHORT).show();}}),a);\n'
        '        }).setNegativeButton("Back",(d,w)->showDownloads(a)),a);\n'
        '    }',
        "KuroOffline561 actions themed + safeShow",
    )
    ko_path.write_text(ko_text, encoding="utf-8")

    # 2. KuroOfflineActivity561: accept both 64-hex SHA-256 and legacy kuro561:xxxx IDs
    oa_path = root / "src/app/lovelycrxshx/kuro/KuroOfflineActivity561.java"
    oa_text = oa_path.read_text(encoding="utf-8")
    oa_text = patch_once(
        oa_text,
        "if(!KuroOffline561.validDownloadId155(id)){unavailable();return;}",
        "if(!KuroPlayback561.validKey(id)){unavailable();return;}",
        "if(!KuroOffline561.validDownloadId155(id)){unavailable();return;}",
        "KuroOfflineActivity561 validDownloadId155",
    )
    oa_path.write_text(oa_text, encoding="utf-8")

    # 3. KuroNativePlayer561: theme & guard markersDialog + editMarker573
    np_path = root / "src/app/lovelycrxshx/kuro/KuroNativePlayer561.java"
    np_text = np_path.read_text(encoding="utf-8")
    np_text = patch_once(
        np_text,
        'KuroDialogs581.safeShowBuilder155(new AlertDialog.Builder(KuroDialogs581.themed(activity)).setTitle("Edit skip markers")',
        '        new AlertDialog.Builder(activity).setTitle("Edit skip markers")\n'
        '            .setItems(new String[]{"Intro", "Outro"},(d,which)->editMarker573(which==1))\n'
        '            .setNegativeButton("Cancel",null).show();',
        '        KuroDialogs581.safeShowBuilder155(new AlertDialog.Builder(KuroDialogs581.themed(activity)).setTitle("Edit skip markers")\n'
        '            .setItems(new String[]{"Intro", "Outro"},(d,which)->editMarker573(which==1))\n'
        '            .setNegativeButton("Cancel",null),activity);',
        "KuroNativePlayer561 markersDialog themed + safeShow",
    )
    np_text = patch_once(
        np_text,
        'AlertDialog dialog=new AlertDialog.Builder(KuroDialogs581.themed(activity)).setTitle(kind+" marker")',
        'AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(kind+" marker")',
        'AlertDialog dialog=new AlertDialog.Builder(KuroDialogs581.themed(activity)).setTitle(kind+" marker")',
        "KuroNativePlayer561 editMarker573 themed",
    )
    np_text = patch_once(
        np_text,
        'KuroDialogs581.safeShow155(dialog,activity);\n    }',
        'within the episode and 30 minutes.");}}));dialog.show();\n    }',
        'within the episode and 30 minutes.");}}));KuroDialogs581.safeShow155(dialog,activity);\n    }',
        "KuroNativePlayer561 editMarker573 safeShow155",
    )
    np_path.write_text(np_text, encoding="utf-8")

    # 4. PlayerOptionsDialog & KuroUpdateDialog582: lifecycle & BadTokenException guards
    po_path = root / "src/app/lovelycrxshx/kuro/PlayerOptionsDialog.java"
    po_text = po_path.read_text(encoding="utf-8")
    po_text = patch_once(
        po_text,
        "public void show(){if(!KuroDialogs581.canShow155(activity))return;",
        " public void show(){",
        " public void show(){if(!KuroDialogs581.canShow155(activity))return;",
        "PlayerOptionsDialog.show canShow155",
    )
    po_text = patch_once(
        po_text,
        "if(!KuroDialogs581.safeShow155(dialog,activity))return;",
        "if(changed)notifyChanged();});dialog.show();\n  Window window=dialog.getWindow();",
        "if(changed)notifyChanged();});if(!KuroDialogs581.safeShow155(dialog,activity))return;\n  Window window=dialog.getWindow();",
        "PlayerOptionsDialog.show safeShow155",
    )
    po_text = patch_once(
        po_text,
        "if(dialog!=null)try{dialog.dismiss();}catch(RuntimeException ignored){}}",
        "if(changed)notifyChanged();if(dialog!=null)dialog.dismiss();}",
        "if(changed)notifyChanged();if(dialog!=null)try{dialog.dismiss();}catch(RuntimeException ignored){}}",
        "PlayerOptionsDialog.dismiss safe",
    )
    po_path.write_text(po_text, encoding="utf-8")

    ud_path = root / "src/app/lovelycrxshx/kuro/KuroUpdateDialog582.java"
    ud_text = ud_path.read_text(encoding="utf-8")
    old_ud_open = """    public static void open(Activity activity, Host host) {
        if (activity == null || activity.isFinishing()) return;
        if (current != null && current.dialog.isShowing()) {
            current.refresh();
            return;
        }
        KuroUpdateDialog582 screen = new KuroUpdateDialog582(activity, host);
        current = screen;
        try {
            screen.dialog.show();
        } catch (RuntimeException e) {
            current = null;
            return;
        }
        screen.refresh();
        screen.schedule();
    }"""
    new_ud_open = """    public static void open(Activity activity, Host host) {
        if (!KuroDialogs581.canShow155(activity)) return;
        if (current != null && current.dialog != null && current.dialog.isShowing()) {
            if (current.activity == activity && KuroDialogs581.canShow155(current.activity)) {
                current.refresh();
                return;
            }
            try { current.dialog.dismiss(); } catch (RuntimeException ignored) {}
            current = null;
        }
        KuroUpdateDialog582 screen = new KuroUpdateDialog582(activity, host);
        current = screen;
        if (!KuroDialogs581.safeShow155(screen.dialog, activity)) {
            current = null;
            return;
        }
        screen.refresh();
        screen.schedule();
    }"""
    ud_text = patch_once(ud_text, "current.activity == activity && KuroDialogs581.canShow155", old_ud_open, new_ud_open, "KuroUpdateDialog582.open stale Activity guard")
    ud_path.write_text(ud_text, encoding="utf-8")

    # 5. NewsSync divide-by-zero fix & KuroPresenceNotification564 null NotificationManager guard
    ns_path = root / "src/app/lovelycrxshx/kuro/NewsSync.java"
    ns_text = ns_path.read_text(encoding="utf-8")
    ns_text = patch_once(
        ns_text,
        "BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;",
        "BitmapFactory.Options options=new BitmapFactory.Options();while(bounds.outWidth/options.inSampleSize>1024",
        "BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(bounds.outWidth/options.inSampleSize>1024",
        "NewsSync.articleArtwork inSampleSize=1 divide-by-zero fix",
    )
    ns_path.write_text(ns_text, encoding="utf-8")

    pn_path = root / "src/app/lovelycrxshx/kuro/KuroPresenceNotification564.java"
    pn_text = pn_path.read_text(encoding="utf-8")
    pn_text = patch_once(
        pn_text,
        "if(manager!=null)manager.createNotificationChannel(",
        '  manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Kuro playback and presence",NotificationManager.IMPORTANCE_LOW));',
        '  if(manager!=null)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Kuro playback and presence",NotificationManager.IMPORTANCE_LOW));',
        "KuroPresenceNotification564 null manager guard",
    )
    pn_path.write_text(pn_text, encoding="utf-8")

    # 6. MainActivity: Themed Auto-Play Next Card + Display/DPI Controller + end-to-end reliability
    ma_path = root / "src/app/lovelycrxshx/kuro/MainActivity.java"
    ma_text = ma_path.read_text(encoding="utf-8")

    ma_text = patch_once(
        ma_text,
        "private android.app.Dialog autoNextDialog573;private Runnable autoNextTick573;",
        " private AlertDialog autoNextDialog573;private Runnable autoNextTick573;",
        " private android.app.Dialog autoNextDialog573;private Runnable autoNextTick573;",
        "MainActivity autoNextDialog573 widen to Dialog",
    )

    old_autonext_block_orig = (
        ' private void cancelAutoNext573(){\n'
        '  if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;\n'
        '  AlertDialog d=autoNextDialog573;autoNextDialog573=null;if(d!=null&&d.isShowing())d.dismiss();\n'
        ' }\n'
        ' private void offerAutoNext573(String session,int rawNext154){\n'
        '  final int next=nextPlayableEpisode154(watchInfo561,rawNext154);\n'
        '  if(next<0||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!session.equals(watchSession)||!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false))return;\n'
        '  cancelAutoNext573();final long end=android.os.SystemClock.elapsedRealtime()+10000;\n'
        '  final Runnable advance=()->{if(!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)){cancelAutoNext573();return;}cancelAutoNext573();closePlayer();if(ui!=null)ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);};\n'
        '  AlertDialog d=new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle("Next episode")\n'
        '   .setMessage("Playing the next episode in 10 seconds.")\n'
        '   .setNegativeButton("Cancel",(dialog,which)->cancelAutoNext573())\n'
        '   .setPositiveButton("Play now",(dialog,which)->advance.run()).create();\n'
        '  autoNextDialog573=d;d.setOnCancelListener(dialog->cancelAutoNext573());d.setOnDismissListener(dialog->{if(autoNextDialog573==d){if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;autoNextDialog573=null;}});\n'
        '  d.show();autoNextTick573=new Runnable(){@Override public void run(){\n'
        '   if(autoNextDialog573!=d||!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)){cancelAutoNext573();return;}\n'
        '   long remaining=end-android.os.SystemClock.elapsedRealtime();if(remaining<=0){advance.run();return;}d.setMessage("Playing the next episode in "+((remaining+999)/1000)+" seconds.");main.postDelayed(this,250);\n'
        '  }};main.post(autoNextTick573);\n'
        ' }'
    )
    old_autonext_block_beta7 = (
        ' private boolean isAutoNextEnabled155(){return getPreferences(MODE_PRIVATE).getBoolean("auto_next",true);}\n'
        ' private void triggerAutoNext155(String session,int rawNext154){if(session==null||session.isEmpty())return;if(autoNextDialog573!=null&&autoNextDialog573.isShowing())return;offerAutoNext573(session,rawNext154);}\n'
        ' private void cancelAutoNext573(){\n'
        '  if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;\n'
        '  AlertDialog d=autoNextDialog573;autoNextDialog573=null;if(d!=null)try{if(d.isShowing())d.dismiss();}catch(RuntimeException ignored){}\n'
        ' }\n'
        ' private void offerAutoNext573(String session,int rawNext154){\n'
        '  final int next=nextPlayableEpisode154(watchInfo561,rawNext154);\n'
        '  if(next<0||!KuroDialogs581.canShow155(this)||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!session.equals(watchSession)||!isAutoNextEnabled155())return;\n'
        '  cancelAutoNext573();final long end=android.os.SystemClock.elapsedRealtime()+10000;\n'
        '  String nextEpLabel="";try{JSONArray epsArr=watchInfo561!=null?watchInfo561.optJSONArray("episodes"):null;JSONObject nextObj=epsArr!=null?epsArr.optJSONObject(next):null;if(nextObj!=null)nextEpLabel=nextObj.optString("number","").trim();}catch(Exception ignored){}\n'
        '  final String titleText=nextEpLabel.isEmpty()?"Next episode":("Next episode · Ep "+nextEpLabel);\n'
        '  final Runnable advance=()->{if(!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!isAutoNextEnabled155()){cancelAutoNext573();return;}cancelAutoNext573();closePlayer();if(ui!=null)ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);};\n'
        '  AlertDialog d=new AlertDialog.Builder(KuroDialogs581.themed(this)).setTitle(titleText)\n'
        '   .setMessage("Playing the next episode in 10 seconds.")\n'
        '   .setNegativeButton("Cancel",(dialog,which)->cancelAutoNext573())\n'
        '   .setPositiveButton("Play now",(dialog,which)->advance.run()).create();\n'
        '  autoNextDialog573=d;d.setOnCancelListener(dialog->cancelAutoNext573());d.setOnDismissListener(dialog->{if(autoNextDialog573==d){if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;autoNextDialog573=null;}});\n'
        '  if(!KuroDialogs581.safeShow155(d,this)){autoNextDialog573=null;return;}\n'
        '  autoNextTick573=new Runnable(){@Override public void run(){\n'
        '   if(autoNextDialog573!=d||!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||!isAutoNextEnabled155()){cancelAutoNext573();return;}\n'
        '   long remaining=end-android.os.SystemClock.elapsedRealtime();if(remaining<=0){advance.run();return;}try{d.setMessage("Playing the next episode in "+((remaining+999)/1000)+" seconds.");}catch(RuntimeException ignored){}main.postDelayed(this,250);\n'
        '  }};main.post(autoNextTick573);\n'
        ' }'
    )
    new_autonext_block = (
        ' private boolean isAutoNextEnabled155(){return getPreferences(MODE_PRIVATE).getBoolean("auto_next",true);}\n'
        ' private void triggerAutoNext155(String session,int rawNext154){if(session==null||session.isEmpty())return;if(autoNextDialog573!=null&&autoNextDialog573.isShowing())return;offerAutoNext573(session,rawNext154);}\n'
        ' private void cancelAutoNext573(){\n'
        '  if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;\n'
        '  android.app.Dialog d=autoNextDialog573;autoNextDialog573=null;if(d!=null)try{if(d.isShowing())d.dismiss();}catch(RuntimeException ignored){}\n'
        ' }\n'
        ' private String resolveWatchPoster155(JSONObject wi,JSONObject nextObj){\n'
        '  if(nextObj!=null){String t=nextObj.optString("image",nextObj.optString("thumb","")).trim();if(!t.isEmpty())return t;}\n'
        '  if(wi==null)return "";\n'
        '  String ei=wi.optString("episodeImage","").trim();if(!ei.isEmpty())return ei;\n'
        '  JSONObject ai=wi.optJSONObject("animeInfo");if(ai!=null){String u=ai.optString("image","").trim();if(!u.isEmpty())return u;}\n'
        '  JSONObject an=wi.optJSONObject("anime");if(an!=null){String u=an.optString("image","").trim();if(!u.isEmpty())return u;}\n'
        '  return wi.optString("image","").trim();\n'
        ' }\n'
        ' private void openDisplayControllerFromMain155(){\n'
        '  KuroDialogs581.themed(this);\n'
        '  KuroDialogs581.openDisplayController155(this,new KuroDialogs581.DisplayHost155(){\n'
        '   @Override public boolean isAutoNextEnabled(){return isAutoNextEnabled155();}\n'
        '   @Override public void setAutoNextEnabled(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",on).apply();if(!on)cancelAutoNext573();if(ui!=null&&uiReady)ui.evaluateJavascript("window.kuroSyncDisplay155&&window.kuroSyncDisplay155()",null);}\n'
        '   @Override public void onDisplayApplied(boolean recreateNow){\n'
        '    KuroDisplay155.enforce500Dpi(MainActivity.this);applyWindowPreferences560();\n'
        '    if(playerLayer!=null&&watchInfo561!=null){\n'
        '     if(recreateNow){String snap=watchInfo561.toString();openWatch(snap);Toast.makeText(MainActivity.this,"Applied · "+KuroDisplay155.effectiveDpi(MainActivity.this)+" effective DPI",Toast.LENGTH_SHORT).show();}\n'
        '     else Toast.makeText(MainActivity.this,"Auto-Next & Display updated",Toast.LENGTH_SHORT).show();\n'
        '    }else if(recreateNow&&!isFinishing()&&!isDestroyed()){\n'
        '     if(ui!=null&&uiReady)ui.evaluateJavascript("try{localStorage.setItem(\'kuro-skip-intro-once-155\',String(Date.now()));if(window.kuroSavePage)window.kuroSavePage();}catch(e){}",v->{if(!isFinishing()&&!isDestroyed())try{recreate();}catch(RuntimeException ignored){}});\n'
        '     else try{recreate();}catch(RuntimeException ignored){}\n'
        '    }else Toast.makeText(MainActivity.this,"Display & Auto-Next saved",Toast.LENGTH_SHORT).show();\n'
        '   }\n'
        '   @Override public void onPreviewUpNext(){previewAutoNextDialog155();}\n'
        '  });\n'
        ' }\n'
        ' private void previewAutoNextDialog155(){\n'
        '  if(!KuroDialogs581.canShow155(this))return;\n'
        '  cancelAutoNext573();\n'
        '  String showTitle=watchInfo561!=null?watchInfo561.optString("title","Kuro Anime"):"Kuro Anime";\n'
        '  String posterUrl=resolveWatchPoster155(watchInfo561,null);\n'
        '  final int totalSec=KuroDisplay155.autoNextSeconds(this);\n'
        '  final long totalMs=totalSec*1000L;\n'
        '  final String epBadge="EP 3";\n'
        '  KuroDialogs581.AutoNextHandle155 h=KuroDialogs581.createAutoNext155(\n'
        '   this,showTitle,epBadge,"Episode 3 · Preview Up Next Card","Episode 3 of 12 · Themed Auto-Play Next",totalSec,\n'
        '   this::cancelAutoNext573,\n'
        '   ()->{cancelAutoNext573();Toast.makeText(MainActivity.this,"Up Next preview: Play Episode 3",Toast.LENGTH_SHORT).show();},\n'
        '   this::openDisplayControllerFromMain155);\n'
        '  final android.app.Dialog d=h.dialog;\n'
        '  autoNextDialog573=d;\n'
        '  d.setOnCancelListener(dialog->cancelAutoNext573());\n'
        '  d.setOnDismissListener(dialog->{if(autoNextDialog573==d){if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;autoNextDialog573=null;}});\n'
        '  if(!KuroDialogs581.safeShow155(d,this)){autoNextDialog573=null;return;}\n'
        '  if(!posterUrl.isEmpty())loadPoster(h.posterView,posterUrl);\n'
        '  final long[] remMs={totalMs};final long[] lastAt={android.os.SystemClock.elapsedRealtime()};\n'
        '  autoNextTick573=new Runnable(){@Override public void run(){\n'
        '   if(autoNextDialog573!=d||!activityResumed){cancelAutoNext573();return;}\n'
        '   long now=android.os.SystemClock.elapsedRealtime();long dt=Math.max(0L,now-lastAt[0]);lastAt[0]=now;\n'
        '   if(!h.paused){remMs[0]-=dt;if(remMs[0]<=0L){cancelAutoNext573();Toast.makeText(MainActivity.this,"Up Next preview finished",Toast.LENGTH_SHORT).show();return;}try{h.updateProgress(remMs[0],totalMs,"Ep 3");}catch(RuntimeException ignored){}}\n'
        '   main.postDelayed(this,60);\n'
        '  }};main.post(autoNextTick573);\n'
        ' }\n'
        ' private void offerAutoNext573(String session,int rawNext154){\n'
        '  final int next=nextPlayableEpisode154(watchInfo561,rawNext154);\n'
        '  if(next<0||!KuroDialogs581.canShow155(this)||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!session.equals(watchSession)||!isAutoNextEnabled155())return;\n'
        '  cancelAutoNext573();\n'
        '  final int totalSec=KuroDisplay155.autoNextSeconds(this);\n'
        '  final long totalMs=totalSec*1000L;\n'
        '  String showTitle=watchInfo561!=null?watchInfo561.optString("title","Anime"):"Anime";\n'
        '  String nextEpNum="",nextEpName="";int totalEps=0;JSONObject nextObj=null;\n'
        '  try{JSONArray epsArr=watchInfo561!=null?watchInfo561.optJSONArray("episodes"):null;if(epsArr!=null){totalEps=epsArr.length();nextObj=epsArr.optJSONObject(next);if(nextObj!=null){nextEpNum=nextObj.optString("number","").trim();nextEpName=nextObj.optString("name",nextObj.optString("title","")).trim();}}}catch(Exception ignored){}\n'
        '  if(!nextEpName.isEmpty()&&!nextEpNum.isEmpty()&&nextEpName.equalsIgnoreCase("Episode "+nextEpNum))nextEpName="";\n'
        '  final String numFallback=nextEpNum.isEmpty()?String.valueOf(next+1):nextEpNum;\n'
        '  final String epBadge="EP "+numFallback;\n'
        '  final String epHeadline=nextEpName.isEmpty()?("Episode "+numFallback):("Ep "+numFallback+" \\u00b7 "+nextEpName);\n'
        '  int skippedFiller=Math.max(0,next-rawNext154);\n'
        '  final String epMeta=(totalEps>0?("Episode "+numFallback+" of "+totalEps):"Next episode in queue")+(skippedFiller>0?(" \\u00b7 Skipped "+skippedFiller+" filler"):"");\n'
        '  final String posterUrl=resolveWatchPoster155(watchInfo561,nextObj);\n'
        '  final Runnable advance=()->{if(!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||playerLayer==null||ui==null||!isAutoNextEnabled155()){cancelAutoNext573();return;}cancelAutoNext573();closePlayer();if(ui!=null)ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);};\n'
        '  final KuroDialogs581.AutoNextHandle155 h=KuroDialogs581.createAutoNext155(this,showTitle,epBadge,epHeadline,epMeta,totalSec,this::cancelAutoNext573,advance,this::openDisplayControllerFromMain155);\n'
        '  final android.app.Dialog d=h.dialog;\n'
        '  autoNextDialog573=d;d.setOnCancelListener(dialog->cancelAutoNext573());d.setOnDismissListener(dialog->{if(autoNextDialog573==d){if(autoNextTick573!=null)main.removeCallbacks(autoNextTick573);autoNextTick573=null;autoNextDialog573=null;}});\n'
        '  if(!KuroDialogs581.safeShow155(d,this)){autoNextDialog573=null;return;}\n'
        '  if(!posterUrl.isEmpty())loadPoster(h.posterView,posterUrl);\n'
        '  final long[] remMs={totalMs};final long[] lastAt={android.os.SystemClock.elapsedRealtime()};\n'
        '  final String tickLabel="Ep "+numFallback;\n'
        '  autoNextTick573=new Runnable(){@Override public void run(){\n'
        '   if(autoNextDialog573!=d||!session.equals(watchSession)||!activityResumed||isInPictureInPictureMode()||!isAutoNextEnabled155()){cancelAutoNext573();return;}\n'
        '   long now=android.os.SystemClock.elapsedRealtime();long dt=Math.max(0L,now-lastAt[0]);lastAt[0]=now;\n'
        '   if(!h.paused){remMs[0]-=dt;if(remMs[0]<=0L){advance.run();return;}try{h.updateProgress(remMs[0],totalMs,tickLabel);}catch(RuntimeException ignored){}}\n'
        '   main.postDelayed(this,60);\n'
        '  }};main.post(autoNextTick573);\n'
        ' }'
    )
    if "KuroDialogs581.createAutoNext155(" not in ma_text:
        if old_autonext_block_beta7 in ma_text:
            ma_text = ma_text.replace(old_autonext_block_beta7, new_autonext_block, 1)
        elif old_autonext_block_orig in ma_text:
            ma_text = ma_text.replace(old_autonext_block_orig, new_autonext_block, 1)
        else:
            raise SystemExit("ui_1.5.4.py: missing offerAutoNext573 anchor in MainActivity.java")

    # Add Display, DPI & Auto-Next button inside Watch Tools drawer + long-press on Auto-Next pill
    ma_text = patch_once(
        ma_text,
        'autoNextPill155.setOnLongClickListener(v->{openDisplayControllerFromMain155();return true;});',
        'styleAutoNextPill155.accept(isAutoNextEnabled155());LinearLayout.LayoutParams anLp155=new LinearLayout.LayoutParams(-2,dp(36));',
        'styleAutoNextPill155.accept(isAutoNextEnabled155());autoNextPill155.setOnLongClickListener(v->{openDisplayControllerFromMain155();return true;});LinearLayout.LayoutParams anLp155=new LinearLayout.LayoutParams(-2,dp(36));',
        "MainActivity autoNextPill155 long-press Display & Auto-Next Controller",
    ) if 'styleAutoNextPill155.accept(isAutoNextEnabled155());LinearLayout.LayoutParams' in ma_text or 'autoNextPill155.setOnLongClickListener' in ma_text else ma_text

    # Watch UI pill + PlaybackPanel sync default auto_next=true and cancelAutoNext573 on toggle off
    ma_text = patch_once(
        ma_text,
        "styleAutoNextPill155.accept(isAutoNextEnabled155());",
        'styleAutoNextPill155.accept(getPreferences(MODE_PRIVATE).getBoolean("auto_next",false));',
        "styleAutoNextPill155.accept(isAutoNextEnabled155());",
        "MainActivity styleAutoNextPill155 default true",
    )
    ma_text = patch_once(
        ma_text,
        'autoNextPill155.setOnLongClickListener(v->{openDisplayControllerFromMain155();return true;});',
        'styleAutoNextPill155.accept(isAutoNextEnabled155());LinearLayout.LayoutParams anLp155=new LinearLayout.LayoutParams(-2,dp(36));',
        'styleAutoNextPill155.accept(isAutoNextEnabled155());autoNextPill155.setOnLongClickListener(v->{openDisplayControllerFromMain155();return true;});LinearLayout.LayoutParams anLp155=new LinearLayout.LayoutParams(-2,dp(36));',
        "MainActivity autoNextPill155 long-press Display & Auto-Next Controller (post-sync)",
    )
    ma_text = patch_once(
        ma_text,
        'displayToolsBtn155.setTag("watch-display-dpi-155")',
        'toolsDrawer155.addView(fxActions,new LinearLayout.LayoutParams(-1,-2));addGap(toolsDrawer155,8);toolsDrawer155.addView(diagnostics573,new LinearLayout.LayoutParams(-1,-2));',
        'toolsDrawer155.addView(fxActions,new LinearLayout.LayoutParams(-1,-2));addGap(toolsDrawer155,8);TextView displayToolsBtn155=chip("\\u229e  Display, App DPI & Auto-Next",false);displayToolsBtn155.setTag("watch-display-dpi-155");displayToolsBtn155.setMinHeight(dp(44));displayToolsBtn155.setOnClickListener(v->openDisplayControllerFromMain155());toolsDrawer155.addView(displayToolsBtn155,new LinearLayout.LayoutParams(-1,-2));addGap(toolsDrawer155,8);toolsDrawer155.addView(diagnostics573,new LinearLayout.LayoutParams(-1,-2));',
        "MainActivity toolsDrawer155 Display, App DPI & Auto-Next button",
    )
    ma_text = patch_once(
        ma_text,
        '@Override public void autoNextChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",on).apply();if(!on)cancelAutoNext573();styleAutoNextPill155.accept(on);if(!syncingAutoNext155[0])Toast.makeText(MainActivity.this,on?"Auto-Next ON":"Auto-Next OFF",Toast.LENGTH_SHORT).show();}',
        '@Override public void autoNextChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",on).apply();styleAutoNextPill155.accept(on);if(!syncingAutoNext155[0])Toast.makeText(MainActivity.this,on?"Auto-Next ON":"Auto-Next OFF",Toast.LENGTH_SHORT).show();}',
        '@Override public void autoNextChanged(boolean on){getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",on).apply();if(!on)cancelAutoNext573();styleAutoNextPill155.accept(on);if(!syncingAutoNext155[0])Toast.makeText(MainActivity.this,on?"Auto-Next ON":"Auto-Next OFF",Toast.LENGTH_SHORT).show();}',
        "MainActivity pbListener155 autoNextChanged cancel on off",
    )
    ma_text = patch_once(
        ma_text,
        'getPreferences(MODE_PRIVATE).getBoolean("autoplay",true),isAutoNextEnabled155(),getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false),pbListener155)};',
        'getPreferences(MODE_PRIVATE).getBoolean("autoplay",true),getPreferences(MODE_PRIVATE).getBoolean("auto_next",false),getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false),pbListener155)};',
        'getPreferences(MODE_PRIVATE).getBoolean("autoplay",true),isAutoNextEnabled155(),getPreferences(MODE_PRIVATE).getBoolean("auto_skip",false),pbListener155)};',
        "MainActivity PlaybackPanel init isAutoNextEnabled155",
    )
    ma_text = patch_once(
        ma_text,
        'autoNextPill155.setOnClickListener(v->{boolean nxt=!isAutoNextEnabled155();getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",nxt).apply();if(!nxt)cancelAutoNext573();',
        'autoNextPill155.setOnClickListener(v->{boolean nxt=!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false);getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",nxt).apply();',
        'autoNextPill155.setOnClickListener(v->{boolean nxt=!isAutoNextEnabled155();getPreferences(MODE_PRIVATE).edit().putBoolean("auto_next",nxt).apply();if(!nxt)cancelAutoNext573();',
        "MainActivity autoNextPill155 default true + cancel on off",
    )

    # Lower duration thresholds & fire Auto-Next from Native, Embedded Web iframes, and stream-player.html
    ma_text = patch_once(
        ma_text,
        'nativeEnded561=()->{if(verification[0]||nativePlayer561==null)return;updatePipActions571();long duration=nativePlayer561.duration(),position=nativePlayer561.position();if(duration<30000||position<duration*.85)return;if(!completionReported[0]&&ui!=null){completionReported[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(info.optString("episode",""))+")",null);}if(isAutoNextEnabled155()&&current+1<eps.length())offerAutoNext573(session,current+1);};',
        'nativeEnded561=()->{if(verification[0]||nativePlayer561==null)return;updatePipActions571();long duration=nativePlayer561.duration(),position=nativePlayer561.position();if(duration<180000||position<duration*.85)return;if(!completionReported[0]&&ui!=null){completionReported[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(info.optString("episode",""))+")",null);}if(getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)&&current+1<eps.length())offerAutoNext573(session,current+1);};',
        'nativeEnded561=()->{if(verification[0]||nativePlayer561==null)return;updatePipActions571();long duration=nativePlayer561.duration(),position=nativePlayer561.position();if(duration<30000||position<duration*.85)return;if(!completionReported[0]&&ui!=null){completionReported[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(info.optString("episode",""))+")",null);}if(isAutoNextEnabled155()&&current+1<eps.length())offerAutoNext573(session,current+1);};',
        "MainActivity nativeEnded561 30s threshold + isAutoNextEnabled155",
    )
    ma_text = patch_once(
        ma_text,
        "m.ended&&Number.isFinite(m.duration)&&m.duration>=45",
        'final WebView watchedPlayer=player;final boolean[] advanced={false};final Runnable[] completionCheck={null};completionCheck[0]=()->{if(player!=watchedPlayer||playerLayer==null||advanced[0])return;if(nativePlayer561!=null||!getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)||current+1>=eps.length()){main.postDelayed(completionCheck[0],2000);return;}watchedPlayer.evaluateJavascript("(function(){try{var v=document.querySelectorAll(\'video\');if(v.length!==1)return false;var m=v[0];return !!(m.ended&&Number.isFinite(m.duration)&&m.duration>=180&&m.currentTime>=m.duration*0.98)}catch(e){return false}})()",result->{if(player!=watchedPlayer||playerLayer==null||advanced[0])return;if("true".equals(result)&&getPreferences(MODE_PRIVATE).getBoolean("auto_next",false)){advanced[0]=true;if(!completionReported[0]&&ui!=null){completionReported[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(info.optString("episode",""))+")",null);}offerAutoNext573(session,current+1);}else main.postDelayed(completionCheck[0],2000);});};main.postDelayed(completionCheck[0],2000);',
        'final WebView watchedPlayer=player;final boolean[] advanced={false};final Runnable[] completionCheck={null};completionCheck[0]=()->{if(player!=watchedPlayer||playerLayer==null||advanced[0])return;if(nativePlayer561!=null||!isAutoNextEnabled155()||current+1>=eps.length()){main.postDelayed(completionCheck[0],2000);return;}watchedPlayer.evaluateJavascript("(function(){try{var v=document.querySelectorAll(\'video\');if(v.length!==1)return false;var m=v[0];return !!(m.ended&&Number.isFinite(m.duration)&&m.duration>=45&&(m.currentTime>=m.duration*0.80||m.currentTime===0))}catch(e){return false}})()",result->{if(player!=watchedPlayer||playerLayer==null||advanced[0])return;if("true".equals(result)&&isAutoNextEnabled155()){advanced[0]=true;if(!completionReported[0]&&ui!=null&&info.optString("episode","").matches("[1-9][0-9]{0,3}")){completionReported[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(info.optString("episode",""))+")",null);}offerAutoNext573(session,current+1);}else main.postDelayed(completionCheck[0],2000);});};main.postDelayed(completionCheck[0],2000);',
        "MainActivity completionCheck 45s threshold + isAutoNextEnabled155",
    )
    ma_text = patch_once(
        ma_text,
        'setupCompletionFrames(player,session,current+1,info.optString("episode",""),verification,completionReported);',
        'setupCompletionFrames(player,session,info.optString("episode",""),verification,completionReported);',
        'setupCompletionFrames(player,session,current+1,info.optString("episode",""),verification,completionReported);',
        "MainActivity setupCompletionFrames pass nextRawIdx",
    )
    old_completion_frames = (
        ' private void setupCompletionFrames(WebView view,String session,String number,boolean[] verification,boolean[] sent){\n'
        '  completionFrameMode=false;completionStartMode=false;\n'
        '  if(!number.matches("[1-9][0-9]{0,3}"))return;\n'
        '  try{\n'
        '   if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;\n'
        '   WebViewCompat.addWebMessageListener(view,"KuroFinished553",Collections.singleton("*"),(sender,message,origin,isMainFrame,reply)->{\n'
        '    /* k154 iframe+85pct completion */\n'
        '    if(sender!=player||!activityResumed||sent[0]||verification[0]||message.getType()!=WebMessageCompat.TYPE_STRING||!"ended".equals(message.getData())||origin==null||!"https".equals(origin.getScheme())||!playerHost(origin.getHost())||!session.equals(watchSession))return;\n'
        '    if(ui==null)return;sent[0]=true;ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(number)+")",null);\n'
        '   });\n'
        '   completionFrameMode=true;\n'
        '   if(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)){WebViewCompat.addDocumentStartJavaScript(view,assetText("player-ended553.js"),Collections.singleton("*"));completionStartMode=true;}\n'
        '  }catch(IOException|RuntimeException|LinkageError ignored){completionStartMode=false;}\n'
        ' }'
    )
    new_completion_frames = (
        ' private void setupCompletionFrames(WebView view,String session,int nextRawIdx155,String number,boolean[] verification,boolean[] sent){\n'
        '  completionFrameMode=false;completionStartMode=false;\n'
        '  final boolean canMarkWatched155=number!=null&&number.matches("[1-9][0-9]{0,3}");\n'
        '  try{\n'
        '   if(!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))return;\n'
        '   WebViewCompat.addWebMessageListener(view,"KuroFinished553",Collections.singleton("*"),(sender,message,origin,isMainFrame,reply)->{\n'
        '    /* k154 iframe+85pct completion */ /* k155 Auto-Next */\n'
        '    if(sender!=player||!activityResumed||verification[0]||message.getType()!=WebMessageCompat.TYPE_STRING||origin==null||!"https".equals(origin.getScheme())||!session.equals(watchSession))return;\n'
        '    String host=origin.getHost();if(host==null||adBlocker().blocked("https",host,"/",false,false))return;\n'
        '    String msg=message.getData();\n'
        '    if(("progress85".equals(msg)||"ended".equals(msg))&&!sent[0]&&canMarkWatched155&&ui!=null){\n'
        '     sent[0]=true;\n'
        '     ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(session)+","+JSONObject.quote(number)+")",null);\n'
        '    }\n'
        '    if("ended".equals(msg))main.post(()->triggerAutoNext155(session,nextRawIdx155));\n'
        '   });\n'
        '   completionFrameMode=true;\n'
        '   if(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)){WebViewCompat.addDocumentStartJavaScript(view,assetText("player-ended553.js"),Collections.singleton("*"));completionStartMode=true;}\n'
        '  }catch(IOException|RuntimeException|LinkageError ignored){completionStartMode=false;}\n'
        ' }'
    )
    ma_text = patch_once(ma_text, "triggerAutoNext155(session,nextRawIdx155)", old_completion_frames, new_completion_frames, "MainActivity setupCompletionFrames Auto-Next + nested CDN origins")

    old_progress561_end = (
        '/* k154 stream-player 85pct */ if(duration>=180000&&position>=duration*.85&&watchInfo561!=null&&!watchSession.isEmpty()&&ui!=null){String ep154=watchInfo561.optString("episode","");if(ep154.matches("[1-9][0-9]{0,3}"))ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(watchSession)+","+JSONObject.quote(ep154)+")",null);}if(v.optBoolean("ended",false)&&duration>=5000&&position>=duration*.98)KuroPlayback561.completed(MainActivity.this,resumeKey561);else KuroPlayback561.save(MainActivity.this,resumeKey561,position,duration);'
    )
    new_progress561_end = (
        '/* k154 stream-player 85pct */ boolean spEnded155=v.optBoolean("ended",false);if(((spEnded155&&duration>=30000)||(duration>=45000&&position>=duration*.85))&&watchInfo561!=null&&!watchSession.isEmpty()&&ui!=null){String ep154=watchInfo561.optString("episode","");if(ep154.matches("[1-9][0-9]{0,3}"))ui.evaluateJavascript("window.kuroEpisodeEnded553&&window.kuroEpisodeEnded553("+JSONObject.quote(watchSession)+","+JSONObject.quote(ep154)+")",null);}if(spEnded155&&duration>=5000){KuroPlayback561.completed(MainActivity.this,resumeKey561);if(duration>=30000&&watchInfo561!=null&&!watchSession.isEmpty()){final String sSnap=watchSession;final int nxtSnap=watchInfo561.optInt("current",-1)+1;main.post(()->triggerAutoNext155(sSnap,nxtSnap));}}else KuroPlayback561.save(MainActivity.this,resumeKey561,position,duration);'
    )
    ma_text = patch_once(ma_text, "triggerAutoNext155(sSnap,nxtSnap)", old_progress561_end, new_progress561_end, "MainActivity setupProgress561 stream-player Auto-Next")

    # Sync session SUB/DUB preference when user chooses a server on the Watch Screen
    ma_text = patch_once(
        ma_text,
        "window.__kuroSessionAudio155=",
        'if(index<2){selectedLang155[0]=index;serverPage154[0]=posSlot154[pos]/5;}syncServers154[0].run();}}',
        'if(index<2){selectedLang155[0]=index;serverPage154[0]=posSlot154[pos]/5;if(ui!=null)ui.evaluateJavascript("window.__kuroSessionAudio155="+(index==1?"\\"dub\\"":"\\"sub\\"")+";",null);}syncServers154[0].run();}}',
        "MainActivity server pill sync __kuroSessionAudio155",
    )

    # Discord notification "Next" command uses nextPlayableEpisode154
    ma_text = patch_once(
        ma_text,
        'int next=nextPlayableEpisode154(watchInfo561,idx+1);if(next>=0){closePlayer();ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);}',
        'if("next".equals(action)){JSONArray eps=watchInfo561.optJSONArray("episodes");int idx=watchInfo561.optInt("current",-1);if(eps!=null&&idx>=0&&idx+1<eps.length()&&ui!=null){int next=idx+1;closePlayer();ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);}return;}',
        'if("next".equals(action)){JSONArray eps=watchInfo561.optJSONArray("episodes");int idx=watchInfo561.optInt("current",-1);if(eps!=null&&idx>=0&&ui!=null){int next=nextPlayableEpisode154(watchInfo561,idx+1);if(next>=0){closePlayer();ui.evaluateJavascript("window.kuroChooseEpisode&&window.kuroChooseEpisode("+next+")",null);}}return;}',
        "MainActivity discordNotificationCommand564 nextPlayableEpisode154",
    )

    # Guard remaining raw dialog.show() calls in MainActivity
    ma_text = patch_once(
        ma_text,
        "if(KuroDialogs581.safeShow155(dialog,this))check.loadUrl(MIRURO_ORIGIN+\"/\");else{try{check.destroy();}catch(RuntimeException ignored){}}",
        'dialog.show();check.loadUrl(MIRURO_ORIGIN+"/");',
        'if(KuroDialogs581.safeShow155(dialog,this))check.loadUrl(MIRURO_ORIGIN+"/");else{try{check.destroy();}catch(RuntimeException ignored){}}',
        "MainActivity Miruro verification dialog safeShow155",
    )
    ma_text = patch_once(
        ma_text,
        'if(KuroDialogs581.safeShow155(dialog573,this)&&dialog573.getWindow()!=null)',
        'dialog573.show();if(dialog573.getWindow()!=null)',
        'if(KuroDialogs581.safeShow155(dialog573,this)&&dialog573.getWindow()!=null)',
        "MainActivity diagnostics573 safeShow155",
    )
    ma_text = patch_once(
        ma_text,
        "KuroDialogs581.safeShow155(dialog,this);\n }",
        'Toast.makeText(this,"Marker saved for this episode.",Toast.LENGTH_SHORT).show();}catch(Exception e){end.setError("Enter an end after the start, within 30 minutes.");}}));dialog.show();\n }',
        'Toast.makeText(this,"Marker saved for this episode.",Toast.LENGTH_SHORT).show();}catch(Exception e){end.setError("Enter an end after the start, within 30 minutes.");}}));KuroDialogs581.safeShow155(dialog,this);\n }',
        "MainActivity editWebMarker574 safeShow155",
    )
    ma_path.write_text(ma_text, encoding="utf-8")

    # 7. Assets: failover531.js (__kuroSessionAudio155 + Episode 0/.5 support) & stream-player.js (reliable ended event)
    fo_path = root / "assets/failover531.js"
    fo_text = fo_path.read_text(encoding="utf-8")
    old_pref_srv = """function preferredServer(list){
 let global=window.kuroSettings560?.snapshot?.()||{},audio=global.audio||'dub';"""
    new_pref_srv = """function preferredServer(list){
 let global=window.kuroSettings560?.snapshot?.()||{};
 let sessAudio=(typeof window.__kuroSessionAudio155==='string'&&(window.__kuroSessionAudio155==='sub'||window.__kuroSessionAudio155==='dub'))?window.__kuroSessionAudio155:'';
 let audio=sessAudio||global.audio||'dub';"""
    fo_text = patch_once(fo_text, "__kuroSessionAudio155", old_pref_srv, new_pref_srv, "failover531.js preferredServer session audio")
    fo_text = patch_once(
        fo_text,
        "validOpenEpisode155",
        "async function openEpisode(ep){\n if(!selected||!validEpisodeNumber(ep.number))return;",
        "function validOpenEpisode155(x){return /^\\d+(?:\\.\\d+)?$/.test(String(x))&&Number(x)>=0&&Number(x)<=5000;}\nasync function openEpisode(ep){\n if(!selected||!ep||!validOpenEpisode155(ep.number))return;",
        "failover531.js openEpisode allow Episode 0 and .5",
    )
    fo_path.write_text(fo_text, encoding="utf-8")

    sp_path = root / "assets/stream-player.js"
    sp_text = sp_path.read_text(encoding="utf-8")
    sp_text = patch_once(
        sp_text,
        "if((!resumeApplied&&!ended)||!Number.isSafeInteger(attempt)",
        "function progress(force,ended){if(!resumeApplied||!Number.isSafeInteger(attempt)",
        "function progress(force,ended){if((!resumeApplied&&!ended)||!Number.isSafeInteger(attempt)",
        "stream-player.js always report ended even if loadedmetadata was skipped",
    )
    sp_path.write_text(sp_text, encoding="utf-8")


def patch_clean_icons_and_no_selection_155(root: pathlib.Path) -> None:
    """1.5.5-beta9: Remove all 4-point sparkle glyphs app-wide and suppress browser WebView text selection & ActionMode."""
    sparkle_4 = "\u2726"   # 4-point black star
    sparkle_4o = "\u2727"  # 4-point white star
    star_6 = "\u2736"      # 6-point star
    star_16 = "\u273a"     # 16-point asterisk
    star_8 = "\u2738"      # 8-point rectilinear star

    # 1. MainActivity.java: block ActionMode on root ViewGroup + disableWebSelection155 on all WebViews + strip pill sparkle
    ma_path = root / "src/app/lovelycrxshx/kuro/MainActivity.java"
    ma_text = ma_path.read_text(encoding="utf-8")
    if f'"{sparkle_4} Auto-Next"' in ma_text:
        ma_text = ma_text.replace(f'"{sparkle_4} Auto-Next"', '"Auto-Next"')

    ma_text = patch_once(
        ma_text,
        "private static void disableWebSelection155(WebView w)",
        "@Override public void onTrimMemory(int level){",
        "private static void disableWebSelection155(WebView w){if(w==null)return;w.setLongClickable(false);w.setHapticFeedbackEnabled(false);w.setOnLongClickListener(v->true);}\n"
        " @Override public void onActionModeStarted(android.view.ActionMode mode){super.onActionModeStarted(mode);if(mode!=null)try{mode.getMenu().clear();mode.finish();}catch(RuntimeException ignored){}}\n"
        " @Override public void onTrimMemory(int level){",
        "MainActivity disableWebSelection155 + onActionModeStarted",
    )
    ma_text = patch_once(
        ma_text,
        "startActionModeForChild(View v,android.view.ActionMode.Callback cb)",
        "root=new FrameLayout(this);setContentView(root);",
        "root=new FrameLayout(this){@Override public android.view.ActionMode startActionModeForChild(View v,android.view.ActionMode.Callback cb){return null;}@Override public android.view.ActionMode startActionModeForChild(View v,android.view.ActionMode.Callback cb,int t){return null;}};setContentView(root);",
        "MainActivity root suppress startActionModeForChild",
    )
    ma_text = patch_once(
        ma_text,
        "ui=new WebView(this);KuroDisplay155.enforce500Dpi(this);disableWebSelection155(ui);",
        "ui=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "ui=new WebView(this);KuroDisplay155.enforce500Dpi(this);disableWebSelection155(ui);",
        "MainActivity disableWebSelection155(ui)",
    )
    ma_text = patch_once(
        ma_text,
        "WebView check=new WebView(this);KuroDisplay155.enforce500Dpi(this);disableWebSelection155(check);",
        "WebView check=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "WebView check=new WebView(this);KuroDisplay155.enforce500Dpi(this);disableWebSelection155(check);",
        "MainActivity disableWebSelection155(check)",
    )
    ma_text = patch_once(
        ma_text,
        "player=new WebView(this);KuroDisplay155.enforce500Dpi(this);disableWebSelection155(player);",
        "player=new WebView(this);KuroDisplay155.enforce500Dpi(this);",
        "player=new WebView(this);KuroDisplay155.enforce500Dpi(this);disableWebSelection155(player);",
        "MainActivity disableWebSelection155(player)",
    )
    ma_path.write_text(ma_text, encoding="utf-8")

    # 2. KuroOfflineActivity561.java: suppress ActionMode
    oa_path = root / "src/app/lovelycrxshx/kuro/KuroOfflineActivity561.java"
    oa_text = oa_path.read_text(encoding="utf-8")
    oa_text = patch_once(
        oa_text,
        "onActionModeStarted(android.view.ActionMode mode)",
        "@Override protected void attachBaseContext(android.content.Context newBase){",
        "@Override public void onActionModeStarted(android.view.ActionMode mode){super.onActionModeStarted(mode);if(mode!=null)try{mode.getMenu().clear();mode.finish();}catch(RuntimeException ignored){}}\n"
        "    @Override protected void attachBaseContext(android.content.Context newBase){",
        "KuroOfflineActivity561 onActionModeStarted",
    )
    oa_path.write_text(oa_text, encoding="utf-8")

    # 3. PlaybackPanel.java: remove 4-point sparkle mark from Playback controls header
    pp_path = root / "src/app/lovelycrxshx/kuro/PlaybackPanel.java"
    pp_text = pp_path.read_text(encoding="utf-8")
    old_pp_mark = (
        f'  TextView mark=label("{sparkle_4}",14,KuroTheme12.col(0xffd7a6ff),true);'
        'mark.setTextSize(TypedValue.COMPLEX_UNIT_DIP,14);mark.setGravity(Gravity.CENTER);'
        'mark.setBackground(solid(KuroTheme12.col(0xff3d2452),0,11));'
        'mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);'
        'header.addView(mark,new LayoutParams(dp(22),dp(22)));space(header,7);\n'
    )
    old_pp_mark_raw = (
        f'  TextView mark=label("{sparkle_4}",14,0xffd7a6ff,true);'
        'mark.setTextSize(TypedValue.COMPLEX_UNIT_DIP,14);mark.setGravity(Gravity.CENTER);'
        'mark.setBackground(solid(0xff3d2452,0,11));'
        'mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);'
        'header.addView(mark,new LayoutParams(dp(22),dp(22)));space(header,7);\n'
    )
    if old_pp_mark in pp_text:
        pp_text = pp_text.replace(old_pp_mark, "", 1)
    elif old_pp_mark_raw in pp_text:
        pp_text = pp_text.replace(old_pp_mark_raw, "", 1)
    pp_path.write_text(pp_text, encoding="utf-8")

    # 4. res/drawable/ic_news.xml & res/drawable/ic_stat_kuro.xml: strip 4-point sparkle subpaths
    ic_news_path = root / "res/drawable/ic_news.xml"
    if ic_news_path.exists():
        ic_news = ic_news_path.read_text(encoding="utf-8")
        ic_news = ic_news.replace(
            "M20.5,1.5l0.8,1.8 1.7,0.8 -1.7,0.8 -0.8,1.8 -0.8,-1.8 -1.7,-0.8 1.7,-0.8z",
            "",
        )
        ic_news_path.write_text(ic_news, encoding="utf-8")

    ic_stat_path = root / "res/drawable/ic_stat_kuro.xml"
    if ic_stat_path.exists():
        ic_stat_path.write_text(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp"\n'
            '    android:height="24dp"\n'
            '    android:viewportWidth="24"\n'
            '    android:viewportHeight="24">\n'
            '    <!-- Monochrome status-bar glyph: Kuro K monogram on transparent canvas -->\n'
            '    <path\n'
            '        android:fillColor="#FFFFFFFF"\n'
            '        android:pathData="M5,4h2.8v6.9L14.2,4h3.4l-6.6,7.1L18,20h-3.5l-5.3,-6.9L7.8,14.6V20H5V4z"/>\n'
            '</vector>\n',
            encoding="utf-8",
        )

    # 5. assets/stream-player.html: disable text selection & callout inside HLS WebView player
    sph_path = root / "assets/stream-player.html"
    if sph_path.exists():
        sph_text = sph_path.read_text(encoding="utf-8")
        if "-webkit-user-select:none" not in sph_text:
            sph_text = sph_text.replace(
                "html,body{margin:0;background:#000;width:100%;height:100%;overflow:hidden}",
                "html,body,*{margin:0;-webkit-user-select:none;user-select:none;-webkit-touch-callout:none}html,body{background:#000;width:100%;height:100%;overflow:hidden}",
                1,
            )
            sph_path.write_text(sph_text, encoding="utf-8")

    # 6. Strip/replace all 4-point sparkle & star-flare glyphs across unpacked base WebView JS
    app_js_path = root / "assets/app.js"
    if app_js_path.exists():
        aj = app_js_path.read_text(encoding="utf-8")
        aj = aj.replace(f'{{id:"action",name:"Action",icon:"{sparkle_4}"', '{id:"action",name:"Action",icon:"\u25b6"')
        aj = aj.replace(f'{{id:"fantasy",name:"Fantasy",icon:"{sparkle_4o}"', '{id:"fantasy",name:"Fantasy",icon:"\u25c7"')
        aj = aj.replace(f'{{id:"isekai",name:"Isekai",icon:"{star_16}"', '{id:"isekai",name:"Isekai",icon:"\u25ce"')
        aj = aj.replace(f'{{id:"magic",name:"Magic",icon:"{star_6}"', '{id:"magic",name:"Magic",icon:"\u25c8"')
        aj = aj.replace(f'{{id:"seinen",name:"Seinen",icon:"{star_6}"', '{id:"seinen",name:"Seinen",icon:"\u25a3"')
        aj = aj.replace(f'{{id:"award-winning",name:"Award Winning",icon:"{star_8}"', '{id:"award-winning",name:"Award Winning",icon:"\u2605"')
        aj = aj.replace(f'<div class="overline">{sparkle_4} ${{escapeHTML(heroTag581(x))}}</div>', '<div class="overline">${escapeHTML(heroTag581(x))}</div>')
        aj = aj.replace(f"${{id==='source'?'{sparkle_4}':id==='az'?'A':'Z'}}", "${id==='source'?'\u2261':id==='az'?'A':'Z'}")
        aj = aj.replace(f'<span class="discover-mark" aria-hidden="true">{sparkle_4}</span>', '')
        aj = aj.replace(f"tabButton('all','All anime','{sparkle_4}')", "tabButton('all','All anime','\u25a6')")
        aj = aj.replace(f'<div class="news-heading-icon" aria-hidden="true">{sparkle_4}</div>', '')
        aj = aj.replace(sparkle_4, "K").replace(sparkle_4o, "")
        app_js_path.write_text(aj, encoding="utf-8")

    uni_js_path = root / "assets/unified533.js"
    if uni_js_path.exists():
        uj = uni_js_path.read_text(encoding="utf-8")
        uj = uj.replace(f'<span class="discover-mark" aria-hidden="true">{sparkle_4}</span>', '')
        uj = uj.replace(f'<div class="unified-banner"><span aria-hidden="true">{sparkle_4}</span>', '<div class="unified-banner">')
        uj = uj.replace(f"tabButton('all','All anime','{sparkle_4}')", "tabButton('all','All anime','\u25a6')")
        uj = uj.replace(sparkle_4, "K").replace(sparkle_4o, "")
        uni_js_path.write_text(uj, encoding="utf-8")

    sim_js_path = root / "assets/simulcasts532.js"
    if sim_js_path.exists():
        sj = sim_js_path.read_text(encoding="utf-8")
        sj = sj.replace(sparkle_4, "K").replace(sparkle_4o, "")
        sim_js_path.write_text(sj, encoding="utf-8")

    acc_js_path = root / "assets/accounts542.js"
    if acc_js_path.exists():
        ac = acc_js_path.read_text(encoding="utf-8")
        ac = ac.replace(f'<small>{sparkle_4o} &nbsp;Local profile</small>', '<small>Local profile</small>')
        ac = ac.replace(f'<div class="profile-streak571">{sparkle_4} ${{streakDays}}-day streak</div>', '<div class="profile-streak571">${streakDays}-day streak</div>')
        ac = ac.replace(sparkle_4, "K").replace(sparkle_4o, "")
        acc_js_path.write_text(ac, encoding="utf-8")

    idx_path = root / "assets/index.html"
    if idx_path.exists():
        ix = idx_path.read_text(encoding="utf-8")
        old_dock_svg = '<path d="M14 3.7 17 10.6l6.9 3.2-6.9 3.1-3 7.1-3-7.1-6.9-3.1 6.9-3.2z"/><circle cx="14" cy="13.9" r="1.55" class="dock-gem"/>'
        new_dock_svg = '<rect x="4.8" y="4.8" width="7.6" height="7.6" rx="2"/><rect x="15.6" y="4.8" width="7.6" height="7.6" rx="2"/><rect x="4.8" y="15.6" width="7.6" height="7.6" rx="2"/><rect x="15.6" y="15.6" width="7.6" height="7.6" rx="2"/>'
        if old_dock_svg in ix:
            idx_path.write_text(ix.replace(old_dock_svg, new_dock_svg, 1), encoding="utf-8")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: ui_1.5.4.py <kuro-source-dir>")
    root = pathlib.Path(sys.argv[1]).resolve()
    patch_manifest(root)
    patch_updater(root)
    patch_catalog(root)
    patch_offline(root)
    patch_main_activity(root)
    patch_offline_activity_insets(root)
    patch_player_ad_blocker(root)
    patch_js_hooks(root)
    patch_perf_and_compat_154(root)
    patch_accounts_and_sync_154(root)
    patch_watch_ui_154(root)
    patch_display_2k_500dpi_155(root)
    patch_stability_and_autonext_155(root)
    patch_clean_icons_and_no_selection_155(root)
    import importlib.util as _ilu
    _tn_spec = _ilu.spec_from_file_location("theme_native_1_2", pathlib.Path(__file__).with_name("theme_native_1.2.py"))
    assert _tn_spec is not None and _tn_spec.loader is not None
    _tn_mod = _ilu.module_from_spec(_tn_spec)
    _tn_spec.loader.exec_module(_tn_mod)
    _tn_mod.main(["theme_native_1.2.py", str(root)])
    print("Applied Kuro 1.5.4 Detail v2, Desktop-Site Left-Rail Leanback, WindowInsets, Ad-Guard, Performance & Compatibility, Tracker Sync & Discord OAuth, Watch UI v3 (20-theme + OLED dynamic), and Smart Browse/Voice patches.")


if __name__ == "__main__":
    main()
