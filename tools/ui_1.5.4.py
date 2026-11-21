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


KURO_TV_BANNER_XML = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="320dp"
    android:height="180dp"
    android:viewportWidth="320"
    android:viewportHeight="180">
    <path
        android:fillColor="#080A10"
        android:pathData="M0,0h320v180h-320z" />
    <path
        android:fillColor="#111726"
        android:pathData="M10,10h300v160h-300z" />
    <path
        android:strokeColor="#2B3A55"
        android:strokeWidth="2"
        android:pathData="M10,10h300v160h-300z" />
    <path
        android:fillColor="#7FD8D1"
        android:fillAlpha="0.16"
        android:pathData="M86,90m-44,0a44,44 0,1 1,88 0a44,44 0,1 1,-88 0" />
    <path
        android:fillColor="#7FD8D1"
        android:pathData="M72,62h10v24l20,-24h13l-21,25l23,31h-13l-17,-23l-5,6v17h-10z" />
    <path
        android:fillColor="#F4F6FB"
        android:pathData="M148,70h9v17l15,-17h11l-16,18l17,22h-11l-12,-16l-4,4v12h-9z" />
    <path
        android:fillColor="#F4F6FB"
        android:pathData="M189,70h9v24c0,5 3,8 8,8s8,-3 8,-8v-24h9v24c0,10 -7,17 -17,17s-17,-7 -17,-17z" />
    <path
        android:fillColor="#F4F6FB"
        android:pathData="M231,70h17c9,0 15,5 15,13c0,6 -3,10 -9,12l10,15h-10l-9,-14h-5v14h-9zM240,78v11h7c4,0 6,-2 6,-5.5s-2,-5.5 -6,-5.5z" />
    <path
        android:fillColor="#7FD8D1"
        android:pathData="M286,70c11,0 19,9 19,20s-8,20 -19,20s-19,-9 -19,-20s8,-20 19,-20zM286,78c-6,0 -10,5 -10,12s4,12 10,12s10,-5 10,-12s-4,-12 -10,-12z" />
    <path
        android:fillColor="#7FD8D1"
        android:fillAlpha="0.85"
        android:pathData="M148,118h96v4h-96z" />
</vector>
"""


KURO_FAST_NET_154_JAVA = r"""package app.lovelycrxshx.kuro;

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
                dnsCache.put(host, new DnsEntry(sys, now + DNS_TTL_MS));
                return sys;
            }

            boolean forceDoh = "always".equals(mode) || poisonedHosts.contains(host);
            if (!forceDoh) {
                try {
                    List<InetAddress> sys = Dns.SYSTEM.lookup(hostname);
                    List<InetAddress> clean = filterValidPublicAddresses(sys);
                    if (!clean.isEmpty()) {
                        dnsCache.put(host, new DnsEntry(clean, now + DNS_TTL_MS));
                        return clean;
                    }
                    poisonedHosts.add(host);
                } catch (UnknownHostException ignored) {
                    // Fall through to encrypted DoH when carrier DNS blocks or fails the domain.
                }
            }

            List<InetAddress> doh = resolveViaDoh(host);
            if (!doh.isEmpty()) {
                dnsCache.put(host, new DnsEntry(doh, now + DNS_TTL_MS));
                return doh;
            }

            // Final fallback to system DNS if DoH endpoints are blocked on a restricted firewall.
            List<InetAddress> fallback = Dns.SYSTEM.lookup(hostname);
            if (!fallback.isEmpty()) {
                dnsCache.put(host, new DnsEntry(fallback, now + 60000L));
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
            "https://1.0.0.1/dns-query?name=" + encoded + "&type=A"
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
                    poisonedHosts.add(host);
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
                    poisonedHosts.add(host);
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
                    poisonedHosts.add(host);
                    dnsCache.remove(host);
                    continue;
                }
                throw ioe;
            }
        }
        throw new IOException("Catalogue unavailable");
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

    # 6. MainActivity.java: 8-thread pool, 90/120Hz unlock, WebView crash shield, LruCache posters, DoH & Perf bridge
    main_path = root / "src/app/lovelycrxshx/kuro/MainActivity.java"
    text = main_path.read_text(encoding="utf-8")

    text = patch_once(
        text,
        "Executors.newFixedThreadPool(8)",
        "private final ExecutorService pool=Executors.newFixedThreadPool(3);",
        "private final ExecutorService pool=Executors.newFixedThreadPool(8);",
        "MainActivity 8-worker network pool",
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
  try{
   UiModeManager um=(UiModeManager)getSystemService(UI_MODE_SERVICE);
   if(um!=null&&um.getCurrentModeType()==android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)return true;
   android.content.pm.PackageManager pm=getPackageManager();
   return pm!=null&&(pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)||pm.hasSystemFeature("amazon.hardware.fire_tv"));
  }catch(RuntimeException e){return false;}
 }
 private int totalRamMb154(){
  try{
   ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
   if(am==null)return 4096;
   ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();
   am.getMemoryInfo(mi);
   return mi.totalMem>0?(int)(mi.totalMem/(1024L*1024L)):4096;
  }catch(RuntimeException e){return 4096;}
 }
 private boolean isLowRamDevice154(){
  try{
   ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
   if(am!=null&&am.isLowRamDevice())return true;
  }catch(RuntimeException ignored){}
  return totalRamMb154()<=3900;
 }
 private int maxDisplayHz154(){
  try{
   Window w=getWindow();if(w==null)return 60;
   Display d=w.getWindowManager().getDefaultDisplay();if(d==null)return 60;
   Display.Mode cur=d.getMode();
   float best=d.getRefreshRate();
   Display.Mode[] modes=d.getSupportedModes();
   if(modes!=null&&cur!=null){
    for(Display.Mode m:modes){
     if(m.getPhysicalWidth()==cur.getPhysicalWidth()&&m.getPhysicalHeight()==cur.getPhysicalHeight()&&m.getRefreshRate()>best){
      best=m.getRefreshRate();
     }
    }
   }
   return Math.max(60,Math.round(best));
  }catch(RuntimeException e){return 60;}
 }
 private void applyDisplayRefresh154(){
  try{
   Window w=getWindow();if(w==null)return;
   Display d=w.getWindowManager().getDefaultDisplay();if(d==null)return;
   Display.Mode cur=d.getMode();
   Display.Mode[] modes=d.getSupportedModes();
   if(cur==null||modes==null||modes.length<=1)return;
   String pref=perfPrefs154().getString("refreshMode","auto");
   boolean videoFs=playerLayer!=null&&(customView!=null||enhancedFullscreen||isInPictureInPictureMode()||nativePlayer561!=null);
   int wantModeId=0;
   if("60".equals(pref)){
    float bestDiff=999f;
    for(Display.Mode m:modes){
     if(m.getPhysicalWidth()==cur.getPhysicalWidth()&&m.getPhysicalHeight()==cur.getPhysicalHeight()){
      float diff=Math.abs(m.getRefreshRate()-60f);
      if(diff<bestDiff){bestDiff=diff;wantModeId=m.getModeId();}
     }
    }
   }else if("max".equals(pref)||("auto".equals(pref)&&!videoFs)){
    float bestHz=0f;
    for(Display.Mode m:modes){
     if(m.getPhysicalWidth()==cur.getPhysicalWidth()&&m.getPhysicalHeight()==cur.getPhysicalHeight()&&m.getRefreshRate()>bestHz){
      bestHz=m.getRefreshRate();wantModeId=m.getModeId();
     }
    }
   }
   WindowManager.LayoutParams lp=w.getAttributes();
   if(lp.preferredDisplayModeId!=wantModeId){
    lp.preferredDisplayModeId=wantModeId;
    w.setAttributes(lp);
   }
  }catch(RuntimeException ignored){}
 }
 private void initPerf154(){
  SharedPreferences sp=perfPrefs154();
  KuroFastNet154.setDohMode(sp.getString("dohMode","auto"));
  if(ui!=null){
   WebSettings ws=ui.getSettings();
   ws.setOffscreenPreRaster(false);
  }
  applyDisplayRefresh154();
 }
 private long folderBytes154(File f){
  if(f==null||!f.exists())return 0L;
  if(f.isFile())return Math.max(0L,f.length());
  File[] kids=f.listFiles();if(kids==null)return 0L;
  long sum=0L;for(File k:kids)sum+=folderBytes154(k);
  return sum;
 }
 private void deleteCacheChildren154(File dir){
  if(dir==null||!dir.isDirectory())return;
  File[] kids=dir.listFiles();if(kids==null)return;
  for(File k:kids){
   if(k.isDirectory())deleteCacheChildren154(k);
   try{k.delete();}catch(RuntimeException ignored){}
  }
 }
 private String buildPerfStatusJson154(){
  SharedPreferences sp=perfPrefs154();
  double cacheMb=Math.round((folderBytes154(getCacheDir())/(1024.0*1024.0))*10.0)/10.0;
  try{
   return new JSONObject()
    .put("refreshMode",sp.getString("refreshMode","auto"))
    .put("dohMode",sp.getString("dohMode","auto"))
    .put("gpuMode",sp.getString("gpuMode","auto"))
    .put("fastBoot",sp.getBoolean("fastBoot",false))
    .put("isTv",isTelevisionDevice154())
    .put("lowRam",isLowRamDevice154())
    .put("ramMb",totalRamMb154())
    .put("maxHz",maxDisplayHz154())
    .put("cacheMb",cacheMb)
    .toString();
  }catch(Exception e){return "{}";}
 }
"""

    text = patch_once(
        text,
        "posterMemCache154",
        " private androidx.core.view.WindowInsetsCompat lastWindowInsets154;",
        perf_helpers + " private androidx.core.view.WindowInsetsCompat lastWindowInsets154;",
        "MainActivity performance & compatibility helpers",
    )

    text = patch_once(
        text,
        "applyDisplayRefresh154();applyRootInsets154",
        "applyRootInsets154(lastWindowInsets154);",
        "applyDisplayRefresh154();applyRootInsets154(lastWindowInsets154);",
        "MainActivity syncWindowSystemBars154 display refresh sync",
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
        '    if(j.has("refreshMode")){String r=j.optString("refreshMode","auto");if("auto".equals(r)||"max".equals(r)||"60".equals(r))ed.putString("refreshMode",r);}\n'
        '    if(j.has("dohMode")){String d=j.optString("dohMode","auto");if("auto".equals(d)||"always".equals(d)||"off".equals(d)){ed.putString("dohMode",d);KuroFastNet154.setDohMode(d);}}\n'
        '    if(j.has("gpuMode")){String g=j.optString("gpuMode","auto");if("auto".equals(g)||"lite".equals(g)||"full".equals(g))ed.putString("gpuMode",g);}\n'
        '    if(j.has("fastBoot"))ed.putBoolean("fastBoot",j.optBoolean("fastBoot",false));\n'
        '    ed.apply();\n'
        '    main.post(()->{if(!isFinishing()&&!isDestroyed())applyDisplayRefresh154();});\n'
        '   }catch(Exception ignored){}\n'
        '   return buildPerfStatusJson154();\n'
        '  }\n'
        '  @JavascriptInterface public String clearTempCache154(){\n'
        '   long before=folderBytes154(getCacheDir());\n'
        '   responses.clear();KuroFastNet154.clearCaches();\n'
        '   try{posterCache154().evictAll();}catch(RuntimeException ignored){}\n'
        '   deleteCacheChildren154(getCacheDir());\n'
        '   main.post(()->{if(ui!=null&&!isFinishing()&&!isDestroyed())try{ui.clearCache(true);}catch(RuntimeException ignored){}});\n'
        '   long after=folderBytes154(getCacheDir());\n'
        '   double freedMb=Math.max(0.0,Math.round(((before-after)/(1024.0*1024.0))*10.0)/10.0);\n'
        '   double leftMb=Math.round((after/(1024.0*1024.0))*10.0)/10.0;\n'
        '   try{return new JSONObject().put("freedMb",freedMb).put("cacheMb",leftMb).toString();}catch(Exception e){return "{}";}\n'
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

    text = patch_once(
        text,
        "if(resumeFullscreenUntil153>android.os.SystemClock.elapsedRealtime()){resumeFullscreenUntil153=0L;enterEnhancedFullscreen();}applyDisplayRefresh154();",
        "if(resumeFullscreenUntil153>android.os.SystemClock.elapsedRealtime()){resumeFullscreenUntil153=0L;enterEnhancedFullscreen();}",
        "if(resumeFullscreenUntil153>android.os.SystemClock.elapsedRealtime()){resumeFullscreenUntil153=0L;enterEnhancedFullscreen();}applyDisplayRefresh154();",
        "MainActivity openWatch applyDisplayRefresh154",
    )

    text = patch_once(
        text,
        "fxComparing=false;applyDisplayRefresh154();}",
        "fxConfig=null;fxTopStatus=null;fxComparing=false;}",
        "fxConfig=null;fxTopStatus=null;fxComparing=false;applyDisplayRefresh154();}",
        "MainActivity closePlayer applyDisplayRefresh154",
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
    print("Applied Kuro 1.5.4-beta Detail v2, Desktop-Site Left-Rail Leanback, WindowInsets, Ad-Guard, Performance & Compatibility, and Smart Browse/Voice patches.")


if __name__ == "__main__":
    main()
