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
    patch_offline_activity_insets(root)
    patch_player_ad_blocker(root)
    patch_js_hooks(root)
    print("Applied Kuro 1.5.4-beta Detail v2, Desktop-Site Left-Rail Leanback, WindowInsets, Ad-Guard, and Smart Browse/Voice patches.")


if __name__ == "__main__":
    main()
