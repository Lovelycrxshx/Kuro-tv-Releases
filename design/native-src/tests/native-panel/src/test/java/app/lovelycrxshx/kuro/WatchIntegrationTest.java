package app.lovelycrxshx.kuro;

import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Uses the actual native watch-page builder with a shadow WebView; no streams are played. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE,qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WatchIntegrationTest {
 private ActivityController<MainActivity> controller; private MainActivity activity;
 private static final class NoNetworkExecutor extends AbstractExecutorService {
  int submitted=0;public void shutdown(){}public List<Runnable> shutdownNow(){return Collections.emptyList();}public boolean isShutdown(){return false;}public boolean isTerminated(){return false;}public boolean awaitTermination(long t,TimeUnit u){return true;}public void execute(Runnable r){submitted++;}
 }
 private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
 private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
 @Before public void create()throws Exception{
  controller=Robolectric.buildActivity(MainActivity.class);activity=controller.get();
  Field f=MainActivity.class.getDeclaredField("pool");f.setAccessible(true);f.set(activity,new NoNetworkExecutor());
  controller.create(new Bundle()).start().resume().visible();activity.getPreferences(Context.MODE_PRIVATE).edit().clear().commit();
 }
 @After public void destroy(){if(controller!=null)controller.pause().stop().destroy();}
 private String watchData(int index)throws Exception{
  JSONObject data=new JSONObject();data.put("session","test-session");data.put("title","Test title");data.put("episode",String.valueOf(index+1));data.put("source","ms");data.put("current",index);data.put("index",0);
  data.put("servers",new JSONArray().put(new JSONObject().put("url","https://megaplay.buzz/stream/test").put("type","").put("lang","sub")));
  data.put("episodes",new JSONArray().put(new JSONObject().put("number","1")).put(new JSONObject().put("number","2")));
  data.put("animeInfo",new JSONObject().put("counts",new JSONObject()).put("anilistId",21));return data.toString();
 }
 private PlaybackPanel open(int index)throws Exception{activity.openWatch(watchData(index));View root=(View)field("root");PlaybackPanel panel=root.findViewWithTag("playback-panel");assertNotNull("watch page did not construct",panel);return panel;}
 @Test public void settingsPersistAndUpdateThePlayer()throws Exception{
  PlaybackPanel panel=open(0);assertTrue(panel.getAutoplay().isChecked());assertFalse(panel.getAutoNext().isChecked());assertFalse(panel.getAutoSkip().isChecked());
  ViewParent parent=panel.getParent();while(parent instanceof View && !(parent instanceof ScrollView))parent=parent.getParent();assertTrue("panel must scroll with details",parent instanceof ScrollView);
  WebView player=(WebView)field("player");assertFalse(player.getSettings().getMediaPlaybackRequiresUserGesture());
  panel.getAutoplay().performClick();assertTrue(player.getSettings().getMediaPlaybackRequiresUserGesture());assertFalse(activity.getPreferences(Context.MODE_PRIVATE).getBoolean("autoplay",true));assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("__kuroAutoplay=false"));
  panel.getAutoplay().performClick();assertFalse(player.getSettings().getMediaPlaybackRequiresUserGesture());assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("__kuroAutoplay=true"));panel.getAutoplay().performClick();
  panel.getAutoNext().performClick();panel.getAutoSkip().performClick();assertTrue(activity.getPreferences(Context.MODE_PRIVATE).getBoolean("auto_next",false));assertTrue(activity.getPreferences(Context.MODE_PRIVATE).getBoolean("auto_skip",false));
  if(!(Boolean)field("skipFrameMode")){assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("__kuroSkip=true"));panel.getAutoSkip().performClick();assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("__kuroSkip=false"));panel.getAutoSkip().performClick();}
  activity.closePlayer();panel=open(1);assertFalse(panel.getAutoplay().isChecked());assertTrue(panel.getAutoNext().isChecked());assertTrue(panel.getAutoSkip().isChecked());assertFalse(panel.getAutoNext().isEnabled());assertFalse(panel.getNextButton().isEnabled());
  panel.findViewWithTag("playback-tile-1").performClick();assertTrue("disabled auto-next must not erase saved choice",activity.getPreferences(Context.MODE_PRIVATE).getBoolean("auto_next",false));
  panel.getNextButton().performClick();assertNotNull("last-episode next cannot close or advance",field("playerLayer"));
 }
 @Test public void headerLogoClosesPlayerAndReturnsHome()throws Exception{
  open(0);WebView ui=(WebView)field("ui");View root=(View)field("root"),home=root.findViewWithTag("watch-home");assertNotNull(home);assertTrue(home.isClickable());
  home.performClick();assertNull("player closes before navigation",field("playerLayer"));assertNull(field("player"));assertTrue(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("kuroWatchHeader(\"home\")"));
  assertTrue("autoplay preference remains saved",activity.getPreferences(Context.MODE_PRIVATE).getBoolean("autoplay",true));
 }
 @Test public void headerSearchAndNotificationsUseExistingUiAndLoginStaysHonest()throws Exception{
  WebView ui=(WebView)field("ui");open(0);View root=(View)field("root");root.findViewWithTag("watch-search").performClick();assertNotNull(field("playerLayer"));assertNotNull(field("player"));assertEquals("search",field("watchOverlay"));assertTrue(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("kuroWatchSearchOpen("));activity.onBackPressed();assertNotNull(field("playerLayer"));assertEquals("",field("watchOverlay"));
  root.findViewWithTag("watch-notifications").performClick();assertNotNull(field("playerLayer"));assertNotNull(field("player"));assertEquals("news",field("watchOverlay"));assertTrue(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("kuroWatchNewsOpen("));activity.onBackPressed();assertNotNull(field("playerLayer"));assertEquals("",field("watchOverlay"));activity.closePlayer();
  open(0);WebView keep=(WebView)field("player");root.findViewWithTag("watch-login").performClick();assertNotNull("account overlay does not interrupt playback",field("playerLayer"));assertSame(keep,field("player"));assertEquals("account",field("watchOverlay"));assertTrue(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("kuroWatchAccountOpen("));int token=(Integer)field("watchOverlayToken");activity.new Bridge().watchAccountClosed(token);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("",field("watchOverlay"));assertSame(keep,field("player"));
 }
 @Test public void androidBackStillClosesTheWatchPage()throws Exception{
  open(0);activity.onBackPressed();assertNull(field("playerLayer"));assertNull(field("player"));
 }
 @Test public void nextNavigatesToFollowingEpisode()throws Exception{
  PlaybackPanel panel=open(0);WebView ui=(WebView)field("ui");panel.getNextButton().performClick();assertNull(field("playerLayer"));
  assertTrue(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("kuroChooseEpisode(1)"));
 }
 @Test public void overlayCallbacksAreTokenizedAndKeepVideoAlive()throws Exception{
  open(0);WebView playerBefore=(WebView)field("player");Object playerLayer=field("playerLayer");View root=(View)field("root");
  root.findViewWithTag("watch-search").performClick();int first=(Integer)field("watchOverlayToken");activity.onBackPressed();assertSame(playerBefore,field("player"));assertSame(playerLayer,field("playerLayer"));
  root.findViewWithTag("watch-search").performClick();int second=(Integer)field("watchOverlayToken");assertTrue(second>first);
  MainActivity.Bridge bridge=activity.new Bridge();bridge.watchSearchClosed(first);bridge.watchSearchSelected("{\"title\":\"stale\"}",first);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("search",field("watchOverlay"));assertSame(playerBefore,field("player"));
  bridge.watchSearchSelected("not json",second);bridge.watchSearchSelected("{\"title\":\"bad\",\"source\":\"ms\",\"url\":\"https://evil.example/\"}",second);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertSame(playerBefore,field("player"));
  bridge.watchSearchSelected("{\"title\":\"New title\",\"source\":\"ms\",\"url\":\"https://hianime.ms/details/test\"}",second);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertNull(field("player"));assertNull(field("playerLayer"));WebView ui=(WebView)field("ui");assertTrue(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("kuroWatchSearchResult"));
 }
 @Test public void notificationIntentUsesSameNonDestructiveOverlay()throws Exception{
  open(0);WebView playerBefore=(WebView)field("player");View root=(View)field("root");set("uiReady",true);activity.onNewIntent(new android.content.Intent(NewsSync.ACTION_OPEN));assertSame(playerBefore,field("player"));assertEquals("news",field("watchOverlay"));int first=(Integer)field("watchOverlayToken");
  activity.onNewIntent(new android.content.Intent(NewsSync.ACTION_OPEN));assertSame(playerBefore,field("player"));assertEquals("news",field("watchOverlay"));assertEquals(first,field("watchOverlayToken"));
  activity.onBackPressed();assertSame(playerBefore,field("player"));assertEquals("",field("watchOverlay"));
  root.findViewWithTag("watch-search").performClick();int searchToken=(Integer)field("watchOverlayToken");activity.onNewIntent(new android.content.Intent(NewsSync.ACTION_OPEN));assertEquals("news",field("watchOverlay"));activity.new Bridge().watchSearchClosed(searchToken);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals("news",field("watchOverlay"));assertSame(playerBefore,field("player"));
 }
 @Test public void failedOverlayLeavesPlayerAndFullscreenIntactInsteadOfExposingHome()throws Exception{
  open(0);WebView before=(WebView)field("player");View custom=new View(activity);int[] hidden={0};before.getWebChromeClient().onShowCustomView(custom,()->hidden[0]++);View root=(View)field("root");
  root.findViewWithTag("watch-notifications").performClick();int token=(Integer)field("watchOverlayToken");assertEquals("news",field("watchOverlay"));WebView ui=(WebView)field("ui");String script=Shadows.shadowOf(ui).getLastEvaluatedJavascript();assertTrue(script.contains("return !!document.getElementById('news-overlay')"));
  java.lang.reflect.Method result=MainActivity.class.getDeclaredMethod("watchOverlayResult",String.class,int.class,String.class);result.setAccessible(true);result.invoke(activity,"news",token,"false");
  assertEquals("",field("watchOverlay"));assertSame(before,field("player"));assertSame(custom,field("customView"));assertEquals(0,hidden[0]);assertEquals("Overlay isn't ready. Please try again.",org.robolectric.shadows.ShadowToast.getTextOfLatestToast());assertFalse(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("nav('home')"));
  root.findViewWithTag("watch-search").performClick();int next=(Integer)field("watchOverlayToken");result.invoke(activity,"news",token,"false");assertEquals("search",field("watchOverlay"));assertSame(before,field("player"));result.invoke(activity,"search",next,"false");assertEquals("",field("watchOverlay"));assertSame(custom,field("customView"));
 }
 @Test public void experimentalSitePlayerRequiresAnExactEpisodeUrl()throws Exception{
  open(0);String base="https://www.animeparadise.moe/watch/11111111-2222-3333-4444-555555555555?origin=good";
  assertTrue(activity.loadPlayer(base,"paradise",""));WebView before=(WebView)field("player");assertEquals(base,before.getUrl());
  assertFalse(activity.loadPlayer(base+"&origin=evil","paradise",""));assertFalse(activity.loadPlayer(base+"#fragment","paradise",""));assertFalse(activity.loadPlayer(base.replace("www.animeparadise.moe","evil.example"),"paradise",""));assertFalse(activity.loadPlayer(base.replace("11111111-2222-3333-4444-555555555555","not-a-uuid"),"paradise",""));assertFalse(activity.loadPlayer(base.replace("origin=good","origin=../bad"),"paradise",""));
  assertSame(before,field("player"));
 }
 @Test public void optionsAreReachablePersistentAndDoNotReplacePlayer()throws Exception{
  open(0);WebView before=(WebView)field("player");View root=(View)field("root"),option=root.findViewWithTag("player-options");assertNotNull(option);option.performClick();PlayerOptionsDialog dialog=(PlayerOptionsDialog)field("playerOptions");assertTrue(dialog.isShowing());assertSame(before,field("player"));
  assertFalse(PlayerOptionsDialog.readConfig(activity).optBoolean("enabled",true));Field df=PlayerOptionsDialog.class.getDeclaredField("dialog");df.setAccessible(true);android.app.Dialog actual=(android.app.Dialog)df.get(dialog);savePreview(dialog,System.getProperty("kuro.options.preview"));android.widget.SeekBar gain=actual.getWindow().getDecorView().findViewWithTag("fx-gain");assertNotNull(gain);changeSeek(gain,150);assertEquals(250,PlayerOptionsDialog.readConfig(activity).optInt("gain"));View openVideo=actual.getWindow().getDecorView().findViewWithTag("fx-upscale-open");assertNotNull(openVideo);openVideo.performClick();android.widget.Switch toggle=actual.getWindow().getDecorView().findViewWithTag("fx-enabled");assertNotNull(toggle);toggle.performClick();assertTrue(PlayerOptionsDialog.readConfig(activity).optBoolean("enabled",false));
  for(String[] choice:new String[][]{{"Mode A+A, Stronger restore","preset","A+A"},{"Darken Lines, Make outlines bolder","line","dark"},{"32×32, 13 taps · up to 4K","detail","32"}}){View c=findDescription(actual.getWindow().getDecorView(),choice[0]);assertNotNull(choice[0],c);c.performClick();assertEquals(choice[2],String.valueOf(PlayerOptionsDialog.readConfig(activity).opt(choice[1])));}
  for(String key:new String[]{"sharpen","denoise","lineStrength","edge","saturation","contrast","brightness","deband"}){android.widget.SeekBar seek=actual.getWindow().getDecorView().findViewWithTag("fx-"+key);assertNotNull(key,seek);changeSeek(seek,75);assertEquals(key,75,PlayerOptionsDialog.readConfig(activity).optInt(key));}
  savePreview(dialog,System.getProperty("kuro.options.video.preview"));actual.getWindow().getDecorView().findViewWithTag("fx-reset").performClick();assertEquals(100,PlayerOptionsDialog.readConfig(activity).optInt("sharpen"));assertEquals(0,PlayerOptionsDialog.readConfig(activity).optInt("deband"));assertEquals(250,PlayerOptionsDialog.readConfig(activity).optInt("gain"));
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(100,TimeUnit.MILLISECONDS);assertTrue(Shadows.shadowOf(before).getLastEvaluatedJavascript().contains("__kuroFx537.configure"));
  dialog.dismiss();option.performClick();assertTrue(((PlayerOptionsDialog)field("playerOptions")).isShowing());assertTrue(PlayerOptionsDialog.readConfig(activity).optBoolean("enabled",false));assertSame(before,field("player"));activity.closePlayer();assertNull(field("player"));
 }
 @Test public void advancedOverlayIsPartialNonDimmingAndRetainsPlayerOnBack()throws Exception{
  open(0);WebView player=(WebView)field("player");View root=(View)field("root"),button=root.findViewWithTag("player-options");assertTrue(((android.widget.TextView)button).getText().toString().contains("Advanced"));
  button.performClick();PlayerOptionsDialog wrapper=(PlayerOptionsDialog)field("playerOptions");Field f=PlayerOptionsDialog.class.getDeclaredField("dialog");f.setAccessible(true);android.app.Dialog dialog=(android.app.Dialog)f.get(wrapper);android.view.WindowManager.LayoutParams p=dialog.getWindow().getAttributes();android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
  assertEquals(android.view.WindowManager.LayoutParams.MATCH_PARENT,p.width);assertEquals(android.view.Gravity.BOTTOM,p.gravity);assertTrue(p.height>0&&p.height<metrics.heightPixels*.6);assertEquals(0f,p.dimAmount,0f);assertEquals(0,p.flags&android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);assertNotEquals(0,p.flags&android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
  wrapper.setRuntimeStatus(new JSONObject().put("video","active").put("audio","off").put("ai","active"));android.widget.TextView status=dialog.getWindow().getDecorView().findViewWithTag("fx-runtime-status");assertTrue(status.getText().toString().contains("Video active"));assertTrue(status.getText().toString().contains("AI active"));
  wrapper.setRuntimeStatus(new JSONObject().put("video","active").put("ai","unavailable").put("aiReason","AI paused to protect playback speed"));assertTrue(status.getText().toString().contains("slow GPU"));
  activity.onBackPressed();assertFalse(wrapper.isShowing());assertSame("Back closes Advanced, not the video",player,field("player"));button.performClick();wrapper=(PlayerOptionsDialog)field("playerOptions");dialog=(android.app.Dialog)f.get(wrapper);dialog.getWindow().getDecorView().findViewWithTag("fx-close").performClick();assertFalse(wrapper.isShowing());assertSame(player,field("player"));
 }
 @Test public void advancedRelayoutsForLandscapeWithoutReloading()throws Exception{
  open(0);WebView before=(WebView)field("player");((View)field("root")).findViewWithTag("player-options").performClick();PlayerOptionsDialog wrapper=(PlayerOptionsDialog)field("playerOptions");Field f=PlayerOptionsDialog.class.getDeclaredField("dialog");f.setAccessible(true);android.app.Dialog dialog=(android.app.Dialog)f.get(wrapper);
  try{org.robolectric.RuntimeEnvironment.setQualifiers("w800dp-h360dp-land-xhdpi");activity.onConfigurationChanged(activity.getResources().getConfiguration());android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);android.view.WindowManager.LayoutParams p=dialog.getWindow().getAttributes();assertEquals(android.view.WindowManager.LayoutParams.MATCH_PARENT,p.height);assertTrue(p.width>0&&p.width<metrics.widthPixels*.5);assertEquals(android.view.Gravity.END|android.view.Gravity.TOP,p.gravity);assertSame(before,field("player"));assertTrue(wrapper.isShowing());}
  finally{org.robolectric.RuntimeEnvironment.setQualifiers("w360dp-h800dp-port-xhdpi");}
 }
 @Test public void advancedClosesForSearchAndNotificationWithoutDestroyingPlayer()throws Exception{
  open(0);WebView before=(WebView)field("player");View root=(View)field("root");root.findViewWithTag("player-options").performClick();PlayerOptionsDialog first=(PlayerOptionsDialog)field("playerOptions");root.findViewWithTag("watch-search").performClick();assertFalse(first.isShowing());assertSame(before,field("player"));assertEquals("search",field("watchOverlay"));activity.onBackPressed();assertSame(before,field("player"));
  root.findViewWithTag("player-options").performClick();PlayerOptionsDialog second=(PlayerOptionsDialog)field("playerOptions");set("uiReady",true);activity.onNewIntent(new android.content.Intent(NewsSync.ACTION_OPEN));assertFalse(second.isShowing());assertSame(before,field("player"));assertEquals("news",field("watchOverlay"));activity.onBackPressed();assertSame(before,field("player"));
 }
 @Test public void experimentalAiIsOptInPersistentAndAppliedLive()throws Exception{
  activity.getPreferences(Context.MODE_PRIVATE).edit().putString("kuro_fx_settings_537","{\"enabled\":true,\"preset\":\"B\",\"gain\":180}").commit();JSONObject migrated=PlayerOptionsDialog.readConfig(activity);assertTrue(migrated.optBoolean("enabled"));assertFalse(migrated.optBoolean("ai",true));assertEquals(100,migrated.optInt("aiStrength"));assertEquals("B",migrated.optString("preset"));assertEquals(180,migrated.optInt("gain"));
  activity.getPreferences(Context.MODE_PRIVATE).edit().clear().commit();open(0);WebView before=(WebView)field("player");View root=(View)field("root"),button=root.findViewWithTag("player-options");button.performClick();PlayerOptionsDialog wrapper=(PlayerOptionsDialog)field("playerOptions");Field f=PlayerOptionsDialog.class.getDeclaredField("dialog");f.setAccessible(true);android.app.Dialog dialog=(android.app.Dialog)f.get(wrapper);View decor=dialog.getWindow().getDecorView();decor.findViewWithTag("fx-upscale-open").performClick();android.widget.Switch ai=decor.findViewWithTag("fx-ai"),master=decor.findViewWithTag("fx-enabled");assertNotNull(ai);assertFalse(ai.isChecked());assertFalse(master.isChecked());ai.performClick();assertTrue(ai.isChecked());assertTrue("AI enables the parent filter",master.isChecked());assertTrue(PlayerOptionsDialog.readConfig(activity).optBoolean("ai"));android.widget.SeekBar strength=decor.findViewWithTag("fx-aiStrength");assertNotNull(strength);changeSeek(strength,47);assertEquals(47,PlayerOptionsDialog.readConfig(activity).optInt("aiStrength"));
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(100,TimeUnit.MILLISECONDS);String js=Shadows.shadowOf(before).getLastEvaluatedJavascript();assertTrue(js.contains("__kuroFx537.configure"));assertTrue(js.contains("\"ai\":true"));assertTrue(js.contains("\"aiStrength\":47"));assertSame(before,field("player"));wrapper.dismiss();button.performClick();wrapper=(PlayerOptionsDialog)field("playerOptions");dialog=(android.app.Dialog)f.get(wrapper);decor=dialog.getWindow().getDecorView();decor.findViewWithTag("fx-upscale-open").performClick();assertTrue(((android.widget.Switch)decor.findViewWithTag("fx-ai")).isChecked());assertEquals(47,((android.widget.SeekBar)decor.findViewWithTag("fx-aiStrength")).getProgress());
 }
 @Test public void choosingModesKeepsAdvancedScrollPosition()throws Exception{
  open(0);((View)field("root")).findViewWithTag("player-options").performClick();PlayerOptionsDialog wrapper=(PlayerOptionsDialog)field("playerOptions");Field f=PlayerOptionsDialog.class.getDeclaredField("dialog");f.setAccessible(true);android.app.Dialog dialog=(android.app.Dialog)f.get(wrapper);View decor=dialog.getWindow().getDecorView();decor.findViewWithTag("fx-upscale-open").performClick();Field sf=PlayerOptionsDialog.class.getDeclaredField("scroll");sf.setAccessible(true);ScrollView scroll=(ScrollView)sf.get(wrapper);int width=Math.round(360*activity.getResources().getDisplayMetrics().density),height=dialog.getWindow().getAttributes().height;decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));decor.layout(0,0,width,height);scroll.scrollTo(0,Math.round(400*activity.getResources().getDisplayMetrics().density));int y=scroll.getScrollY();assertTrue("test needs a scrollable panel",y>0);View choice=findDescription(decor,"Mode B, Soft restore");assertNotNull(choice);choice.performClick();decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));decor.layout(0,0,width,height);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(y,scroll.getScrollY());
 }
 @Test public void videoEffectsScriptIsServedOnlyFromLocalPlayerOrigin()throws Exception{
  java.io.File apk=java.io.File.createTempFile("kuro-fx-test", ".apk");apk.deleteOnExit();try(java.util.zip.ZipOutputStream out=new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(apk))){out.putNextEntry(new java.util.zip.ZipEntry("assets/video-effects537.js"));java.nio.file.Files.copy(java.nio.file.Paths.get("assets/video-effects537.js"),out);out.closeEntry();out.putNextEntry(new java.util.zip.ZipEntry("assets/ai-upscale540.js"));java.nio.file.Files.copy(java.nio.file.Paths.get("assets/ai-upscale540.js"),out);out.closeEntry();}
  android.content.res.AssetManager.class.getMethod("addAssetPath",String.class).invoke(activity.getAssets(),apk.getAbsolutePath());
  open(0);WebView v=(WebView)field("player");Object client=v.getWebViewClient();java.lang.reflect.Method method=client.getClass().getMethod("shouldInterceptRequest",WebView.class,android.webkit.WebResourceRequest.class);method.setAccessible(true);
  android.webkit.WebResourceRequest req=request("https://kuro.local/video-effects537.js");android.webkit.WebResourceResponse ok=(android.webkit.WebResourceResponse)method.invoke(client,v,req);assertNotNull(ok);assertEquals("application/javascript",ok.getMimeType());
  assertNull(method.invoke(client,v,request("https://foreign.example/video-effects537.js")));android.webkit.WebResourceResponse ai=(android.webkit.WebResourceResponse)method.invoke(client,v,request("https://kuro.local/ai-upscale540.js"));assertNotNull(ai);assertEquals("application/javascript",ai.getMimeType());assertNull(method.invoke(client,v,request("https://foreign.example/ai-upscale540.js")));
 }

 @Test public void adProtectionDoesNotBlockPlayerMediaOrVerification()throws Exception{
  open(0);WebView v=(WebView)field("player");Object client=v.getWebViewClient();java.lang.reflect.Method method=client.getClass().getMethod("shouldInterceptRequest",WebView.class,android.webkit.WebResourceRequest.class);method.setAccessible(true);
  android.webkit.WebResourceResponse ad=(android.webkit.WebResourceResponse)method.invoke(client,v,request("https://syndication.exdynsrv.com/ads.js"));assertNotNull(ad);assertEquals(204,ad.getStatusCode());
  assertNull("main navigation must not be intercepted",method.invoke(client,v,request("https://syndication.exdynsrv.com/ads.js",true)));
  assertNull("player origin is protected",method.invoke(client,v,request("https://megaplay.buzz/stream/test")));
  assertNull("human verification is protected",method.invoke(client,v,request("https://challenges.cloudflare.com/turnstile/v0/api.js")));
  assertNull("ordinary media segments are protected",method.invoke(client,v,request("https://media.example/video/part.m4s")));
  assertFalse(v.getSettings().getJavaScriptCanOpenWindowsAutomatically());assertFalse(v.getSettings().supportMultipleWindows());
 }
 @Test public void resumeChecksNewsOnlyWhenPhoneAlertsAreOn()throws Exception{
  NoNetworkExecutor executor=(NoNetworkExecutor)field("pool");activity.getSharedPreferences(NewsSync.PREFS,Context.MODE_PRIVATE).edit().putBoolean("alerts_enabled",false).commit();int old=executor.submitted;activity.onPause();activity.onResume();assertEquals("off must not queue automatic checks",old,executor.submitted);
  activity.getSharedPreferences(NewsSync.PREFS,Context.MODE_PRIVATE).edit().putBoolean("alerts_enabled",true).commit();activity.onPause();activity.onResume();assertEquals("on queues one bounded automatic check",old+1,executor.submitted);
 }
 private android.webkit.WebResourceRequest request(String url){return request(url,false);}
 private android.webkit.WebResourceRequest request(String url,boolean main){return new android.webkit.WebResourceRequest(){
  public android.net.Uri getUrl(){return android.net.Uri.parse(url);}public boolean isForMainFrame(){return main;}public boolean isRedirect(){return false;}public boolean hasGesture(){return false;}public String getMethod(){return "GET";}public java.util.Map<String,String> getRequestHeaders(){return Collections.emptyMap();}
 };}

 private void changeSeek(android.widget.SeekBar seek,int progress)throws Exception{Field f=android.widget.SeekBar.class.getDeclaredField("mOnSeekBarChangeListener");f.setAccessible(true);android.widget.SeekBar.OnSeekBarChangeListener listener=(android.widget.SeekBar.OnSeekBarChangeListener)f.get(seek);assertNotNull(listener);listener.onProgressChanged(seek,progress,true);}
 private View findDescription(View v,String prefix){CharSequence d=v.getContentDescription();if(d!=null&&d.toString().startsWith(prefix))return v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=findDescription(g.getChildAt(i),prefix);if(found!=null)return found;}}return null;}

 private void savePreview(PlayerOptionsDialog wrapper,String path)throws Exception{if(path==null)return;Field f=PlayerOptionsDialog.class.getDeclaredField("body");f.setAccessible(true);View body=(View)f.get(wrapper);View content=(View)body.getParent().getParent();float d=activity.getResources().getDisplayMetrics().density;Field df=PlayerOptionsDialog.class.getDeclaredField("dialog");df.setAccessible(true);android.app.Dialog actual=(android.app.Dialog)df.get(wrapper);int w=Math.round(360*d),h=actual.getWindow().getAttributes().height;if(h<1)h=Math.round(800*d);content.setAlpha(1);content.setTranslationY(0);content.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));content.layout(0,0,w,h);android.graphics.Bitmap bmp=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);content.draw(new android.graphics.Canvas(bmp));try(java.io.FileOutputStream o=new java.io.FileOutputStream(path)){bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);}}

 @Test public void coldNotificationWaitsForLocalUiThenRequestsRecovery()throws Exception{
  set("uiReady",false);activity.onNewIntent(new android.content.Intent(NewsSync.ACTION_OPEN));assertEquals(true,field("pendingNews"));assertEquals(true,field("pendingNewsCold"));set("uiReady",true);java.lang.reflect.Method run=MainActivity.class.getDeclaredMethod("openPendingNews");run.setAccessible(true);run.invoke(activity);assertEquals(false,field("pendingNews"));WebView ui=(WebView)field("ui");assertTrue(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("kuroRestoreForNews"));assertNull("consumed notification is not replayed on recreation",activity.getIntent().getAction());
 }
 @Test public void notificationCreatedActivityRestoresAfterItsWebViewFinishes()throws Exception{
  android.content.Intent intent=new android.content.Intent(activity,MainActivity.class).setAction(NewsSync.ACTION_OPEN);
  ActivityController<MainActivity> cold=Robolectric.buildActivity(MainActivity.class,intent);MainActivity fresh=cold.get();
  Field executor=MainActivity.class.getDeclaredField("pool");executor.setAccessible(true);executor.set(fresh,new NoNetworkExecutor());
  try{cold.create().start().resume().visible();Field p=MainActivity.class.getDeclaredField("pendingNewsCold");p.setAccessible(true);assertTrue(p.getBoolean(fresh));
   Field uf=MainActivity.class.getDeclaredField("ui");uf.setAccessible(true);WebView freshUi=(WebView)uf.get(fresh);
   Field rf=MainActivity.class.getDeclaredField("root");rf.setAccessible(true);android.view.ViewGroup root=(android.view.ViewGroup)rf.get(fresh);assertEquals("notification launch skips the entry splash",1,root.getChildCount());
   freshUi.getWebViewClient().onPageFinished(freshUi,"https://kuro.local/index.html");assertTrue(Shadows.shadowOf(freshUi).getLastEvaluatedJavascript().contains("kuroRestoreForNews"));assertFalse(p.getBoolean(fresh));assertNull(fresh.getIntent().getAction());
  }finally{cold.pause().stop().destroy();}
 }
 @Test public void warmNotificationDoesNotRequestCheckpointRestoration()throws Exception{
  set("uiReady",true);activity.onNewIntent(new android.content.Intent(NewsSync.ACTION_OPEN));WebView ui=(WebView)field("ui");String js=Shadows.shadowOf(ui).getLastEvaluatedJavascript();assertTrue(js.contains("kuroNewsOpen"));assertFalse(js.contains("kuroRestoreForNews"));assertNull(activity.getIntent().getAction());
 }
 @Test public void notificationAndSearchPreserveFullscreenUntilUserClosesIt()throws Exception{
  open(0);WebView player=(WebView)field("player");View custom=new View(activity);int[] hidden={0};android.webkit.WebChromeClient chrome=player.getWebChromeClient();
  chrome.onShowCustomView(custom,new android.webkit.WebChromeClient.CustomViewCallback(){public void onCustomViewHidden(){hidden[0]++;}});
  assertSame(custom,field("customView"));View full=(View)field("full");assertNotNull(full);
  set("uiReady",true);activity.onNewIntent(new android.content.Intent(NewsSync.ACTION_OPEN));assertEquals("news",field("watchOverlay"));assertSame(custom,field("customView"));assertEquals(0,hidden[0]);
  activity.onBackPressed();assertEquals("",field("watchOverlay"));assertSame(custom,field("customView"));assertEquals(0,hidden[0]);View root=(View)field("root");assertSame("fullscreen is brought forward after closing news",full,((android.view.ViewGroup)root).getChildAt(((android.view.ViewGroup)root).getChildCount()-1));
  root.findViewWithTag("watch-search").performClick();assertEquals("search",field("watchOverlay"));assertSame(custom,field("customView"));activity.onBackPressed();assertEquals("",field("watchOverlay"));assertSame(custom,field("customView"));assertEquals(0,hidden[0]);
  activity.onBackPressed();assertNull(field("customView"));assertEquals(1,hidden[0]);assertSame(player,field("player"));
 }
 @Test public void noHayasePromotionOrExternalLaunchInWatch()throws Exception{
  MainActivity.Bridge bridge=activity.new Bridge();
  for(java.lang.reflect.Method method:MainActivity.Bridge.class.getDeclaredMethods())
   assertFalse("no Hayase app launcher in the native bridge",method.getName().startsWith("openHayase"));
  bridge.openExternalSource("hayase");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
  assertNull("Hayase is not an external source handoff",Shadows.shadowOf(activity).getNextStartedActivity());
  open(0);View root=(View)field("root");assertNotNull(field("player"));
  assertNull("watch details do not promote another app",root.findViewWithTag("watch-hayase-title"));
 }

 @Test public void enhancedFullscreenKeepsSameWebViewAndRestoresWatchWithoutReload()throws Exception{
  open(0);View root=(View)field("root");WebView player=(WebView)field("player");View box=(View)field("watchPlayerBox");ViewParent originalParent=player.getParent();ViewParent boxParent=box.getParent();String url=player.getUrl();
  View enter=root.findViewWithTag("enhanced-fullscreen-enter");assertNotNull(enter);enter.performClick();assertEquals(Boolean.TRUE,field("enhancedFullscreen"));assertSame(player,field("player"));assertSame(originalParent,player.getParent());assertSame(boxParent,box.getParent());assertEquals(url,player.getUrl());
  assertEquals(View.GONE,((View)field("watchHeader")).getVisibility());assertEquals(View.GONE,((View)field("watchScroll")).getVisibility());assertEquals(-1,box.getLayoutParams().height);assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":true"));
  View fullOptions=root.findViewWithTag("enhanced-fullscreen-options"),exit=root.findViewWithTag("enhanced-fullscreen-exit");assertNull("no enhancement control panel shortcut over fullscreen",fullOptions);assertNotNull(exit);root.findViewWithTag("player-options").performClick();assertNull("Advanced must not open over fullscreen",field("playerOptions"));assertEquals(Boolean.TRUE,field("enhancedFullscreen"));assertSame(player,field("player"));
  activity.onConfigurationChanged(activity.getResources().getConfiguration());assertSame(player,field("player"));assertEquals(Boolean.TRUE,field("enhancedFullscreen"));
  // A site's own native video-only fullscreen is untouched and honestly pauses
  // enhancement; returning from it returns to enhanced fullscreen.
  View custom=new View(activity);int[] hidden={0};player.getWebChromeClient().onShowCustomView(custom,()->hidden[0]++);assertSame(custom,field("customView"));assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":false"));activity.onBackPressed();assertEquals(1,hidden[0]);assertNull(field("customView"));assertEquals(Boolean.TRUE,field("enhancedFullscreen"));assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":true"));
  activity.onBackPressed();assertEquals(Boolean.FALSE,field("enhancedFullscreen"));assertSame(player,field("player"));assertSame(originalParent,player.getParent());assertEquals(View.VISIBLE,((View)field("watchHeader")).getVisibility());assertEquals(View.VISIBLE,((View)field("watchScroll")).getVisibility());assertNull(root.findViewWithTag("enhanced-fullscreen-exit"));
  enter.performClick();assertEquals(Boolean.TRUE,field("enhancedFullscreen"));root.findViewWithTag("enhanced-fullscreen-exit").performClick();assertEquals(Boolean.FALSE,field("enhancedFullscreen"));activity.closePlayer();assertNull(field("player"));assertNull(field("watchPlayerBox"));
 }
 @Test public void playerFullscreenKeepsSupportedOverlayAndDoesNotShowAdvancedPanel()throws Exception{
  activity.getPreferences(Context.MODE_PRIVATE).edit().putString("kuro_fx_settings_537","{\"enabled\":true,\"ai\":true}").commit();
  open(0);View root=(View)field("root");WebView player=(WebView)field("player");ViewParent original=player.getParent();String url=player.getUrl();
  root.findViewWithTag("player-options").performClick();PlayerOptionsDialog options=(PlayerOptionsDialog)field("playerOptions");assertTrue(options.isShowing());
  int[] hidden={0};View custom=new View(activity);player.getWebChromeClient().onShowCustomView(custom,()->hidden[0]++);
  assertEquals(1,hidden[0]);assertNull("video-only view is not displayed when effects can be retained",field("customView"));assertEquals(Boolean.TRUE,field("enhancedFullscreen"));assertFalse(options.isShowing());assertNull(field("playerOptions"));assertNull(root.findViewWithTag("enhanced-fullscreen-options"));assertNotNull(root.findViewWithTag("enhanced-fullscreen-exit"));assertSame(player,field("player"));assertSame(original,player.getParent());assertEquals(url,player.getUrl());assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":true"));
  player.getWebChromeClient().onHideCustomView();assertEquals(Boolean.TRUE,field("enhancedFullscreen"));
  controller.pause();assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":false"));controller.resume();assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":true"));
  View exit=root.findViewWithTag("enhanced-fullscreen-exit");View toolbar=(View)field("enhancedToolbar");Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(3600,TimeUnit.MILLISECONDS);assertEquals(View.GONE,toolbar.getVisibility());
  activity.onBackPressed();assertEquals(Boolean.FALSE,field("enhancedFullscreen"));assertSame(player,field("player"));assertEquals(View.VISIBLE,((View)field("watchScroll")).getVisibility());root.findViewWithTag("player-options").performClick();assertTrue(((PlayerOptionsDialog)field("playerOptions")).isShowing());
 }
 @Test public void colorOnlyFallbackRemainsInSameFullscreenWebView()throws Exception{
  activity.getPreferences(Context.MODE_PRIVATE).edit().putString("kuro_fx_settings_537","{\"enabled\":true,\"ai\":true}").commit();open(0);WebView player=(WebView)field("player");set("fxTopStatus",new JSONObject().put("video","color").put("reason","Color only; frame access is restricted").put("ai","unavailable"));
  int[] hidden={0};player.getWebChromeClient().onShowCustomView(new View(activity),()->hidden[0]++);assertEquals(1,hidden[0]);assertNull(field("customView"));assertEquals(Boolean.TRUE,field("enhancedFullscreen"));assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":true"));activity.onBackPressed();assertSame(player,field("player"));
 }
 @Test public void restrictedVideoKeepsNormalFullscreenAndPlayback()throws Exception{
  activity.getPreferences(Context.MODE_PRIVATE).edit().putString("kuro_fx_settings_537","{\"enabled\":true}").commit();open(0);WebView player=(WebView)field("player");set("fxTopStatus",new JSONObject().put("video","unavailable").put("reason","Protected video"));
  int[] hidden={0};View custom=new View(activity);player.getWebChromeClient().onShowCustomView(custom,()->hidden[0]++);assertSame(custom,field("customView"));assertEquals(Boolean.FALSE,field("enhancedFullscreen"));assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":false"));
  activity.onBackPressed();assertEquals(1,hidden[0]);assertNull(field("customView"));assertSame(player,field("player"));assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":true"));
 }
 @Test public void originalComparisonIsTemporaryAndDismissRestoresEnhancement()throws Exception{
  activity.getPreferences(Context.MODE_PRIVATE).edit().putString("kuro_fx_settings_537","{\"enabled\":true,\"ai\":true,\"preset\":\"B\"}").commit();open(0);View root=(View)field("root");WebView player=(WebView)field("player");root.findViewWithTag("player-options").performClick();PlayerOptionsDialog wrapper=(PlayerOptionsDialog)field("playerOptions");Field f=PlayerOptionsDialog.class.getDeclaredField("dialog");f.setAccessible(true);android.app.Dialog d=(android.app.Dialog)f.get(wrapper);View decor=d.getWindow().getDecorView();decor.findViewWithTag("fx-upscale-open").performClick();
  decor.findViewWithTag("fx-compare").performClick();assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"compare\":true"));assertFalse(activity.getPreferences(Context.MODE_PRIVATE).getString("kuro_fx_settings_537","").contains("compare"));assertEquals("B",PlayerOptionsDialog.readConfig(activity).optString("preset"));
  controller.pause();assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":false"));controller.resume();assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"visible\":true"));assertTrue("comparison survives lifecycle changes",Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"compare\":true"));
  android.widget.TextView compare=decor.findViewWithTag("fx-compare");assertTrue(compare.getText().toString().contains("enhanced"));compare.performClick();assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"compare\":false"));
  decor.findViewWithTag("fx-compare").performClick();wrapper.dismiss();assertTrue(Shadows.shadowOf(player).getLastEvaluatedJavascript().contains("\"compare\":false"));assertTrue(PlayerOptionsDialog.readConfig(activity).optBoolean("enabled"));assertTrue(PlayerOptionsDialog.readConfig(activity).optBoolean("ai"));assertSame(player,field("player"));
 }
 private static final class FakeFxReply extends androidx.webkit.JavaScriptReplyProxy{
  final java.util.ArrayList<String> sent=new java.util.ArrayList<>();@Override public void postMessage(String value){sent.add(value);}@Override public void postMessage(byte[] value){throw new AssertionError("unexpected binary message");}String latest(){return sent.get(sent.size()-1);}
 }
 @Test public void scopedFrameRouterRejectsOtherOriginsAndStalePlayerAttempts()throws Exception{
  assertTrue(MainActivity.fxOriginAllowed(android.net.Uri.parse("https://megaplay.buzz")));assertFalse(MainActivity.fxOriginAllowed(android.net.Uri.parse("http://megaplay.buzz")));assertFalse(MainActivity.fxOriginAllowed(android.net.Uri.parse("https://evil.megaplay.buzz")));assertFalse(MainActivity.fxOriginAllowed(android.net.Uri.parse("https://megaplay.buzz:444")));
  open(0);WebView player=(WebView)field("player");long attempt=(Long)field("playerAttempt");android.net.Uri ok=android.net.Uri.parse("https://megaplay.buzz"),bad=android.net.Uri.parse("https://foreign.example");FakeFxReply reply=new FakeFxReply();
  activity.onFxFrameMessage(player,bad,reply,"ready:"+attempt);activity.onFxFrameMessage(player,ok,reply,"ready:"+(attempt+1));assertTrue(reply.sent.isEmpty());assertTrue(((java.util.List<?>)field("fxFrames")).isEmpty());
  activity.onFxFrameMessage(player,ok,reply,"ready:"+attempt);assertEquals(1,reply.sent.size());JSONObject envelope=new JSONObject(reply.latest());assertEquals(559,envelope.getInt("version"));assertTrue(envelope.getJSONObject("settings").has("enabled"));assertFalse(envelope.toString().contains("password"));
  activity.onFxFrameMessage(player,ok,reply,"ready:"+attempt);assertEquals(1,((java.util.List<?>)field("fxFrames")).size());
  View root=(View)field("root");root.findViewWithTag("player-options").performClick();PlayerOptionsDialog wrapper=(PlayerOptionsDialog)field("playerOptions");Field f=PlayerOptionsDialog.class.getDeclaredField("dialog");f.setAccessible(true);android.app.Dialog d=(android.app.Dialog)f.get(wrapper);View decor=d.getWindow().getDecorView();
  activity.onFxFrameMessage(player,ok,reply,"status:"+attempt+":{\"video\":\"active\",\"ai\":\"active\",\"audio\":\"off\"}");android.widget.TextView st=decor.findViewWithTag("fx-runtime-status");assertTrue(st.getText().toString().contains("Video active"));assertTrue(st.getText().toString().contains("AI active"));
  activity.onFxFrameMessage(player,ok,reply,"status:"+attempt+":{\"video\":\"fake\",\"ai\":\"active\"}");assertTrue(st.getText().toString().contains("Video active"));
  activity.onFxFrameMessage(player,ok,reply,"status:"+attempt+":{\"video\":\"color\",\"ai\":\"unavailable\",\"reason\":\"Color only; frame access is restricted\",\"audio\":\"off\"}");assertTrue(st.getText().toString().contains("Color only"));assertTrue(st.getText().toString().contains("AI unavailable"));
  decor.findViewWithTag("fx-upscale-open").performClick();decor.findViewWithTag("fx-compare").performClick();assertTrue(new JSONObject(reply.latest()).getJSONObject("settings").getBoolean("compare"));wrapper.dismiss();assertFalse(new JSONObject(reply.latest()).getJSONObject("settings").getBoolean("compare"));
  for(int i=0;i<12;i++)activity.onFxFrameMessage(player,ok,new FakeFxReply(),"ready:"+attempt);assertEquals(8,((java.util.List<?>)field("fxFrames")).size());
  assertTrue(activity.loadPlayer("https://megaplay.buzz/stream/other","ms",""));long next=(Long)field("playerAttempt");assertTrue(next>attempt);assertTrue(((java.util.List<?>)field("fxFrames")).isEmpty());assertFalse(new JSONObject(reply.latest()).getJSONObject("settings").getBoolean("enabled"));int sent=reply.sent.size();
  activity.onFxFrameMessage(player,ok,reply,"ready:"+attempt);assertEquals(sent,reply.sent.size());activity.onFxFrameMessage(player,ok,reply,"ready:"+next);assertEquals(sent+1,reply.sent.size());activity.onFxFrameMessage(player,ok,reply,"bye:"+next);assertTrue(((java.util.List<?>)field("fxFrames")).isEmpty());
 }

 @Test public void presenceFramesAreOptInOriginBoundAndDiscardStaleOrAmbiguousSamples()throws Exception{
  open(0);WebView target=(WebView)field("player");long attempt=(Long)field("playerAttempt");android.net.Uri ok=android.net.Uri.parse("https://megaplay.buzz"),bad=android.net.Uri.parse("https://evil.example");FakeFxReply one=new FakeFxReply();
  activity.onPresenceFrame565(target,bad,one,"ready:"+attempt);activity.onPresenceFrame565(target,ok,one,"ready:"+(attempt+1));assertTrue(one.sent.isEmpty());
  activity.onPresenceFrame565(target,ok,one,"ready:"+attempt);assertEquals("off",one.latest());assertEquals(1,((java.util.List<?>)field("presenceFrames565")).size());
  java.lang.reflect.Method update=MainActivity.class.getDeclaredMethod("updatePresenceFrames565");update.setAccessible(true);
  activity.getSharedPreferences("kuro-discord-rpc563",Context.MODE_PRIVATE).edit().putBoolean("enabled",true).commit();update.invoke(activity);assertEquals("on",one.latest());
  String data="{\"kind\":\"sample\",\"attempt\":"+attempt+",\"state\":\"playing\",\"position\":10000,\"duration\":1200000,\"rate\":1}";
  activity.onPresenceFrame565(target,ok,one,data);java.lang.reflect.Method select=MainActivity.class.getDeclaredMethod("presenceSelection565");select.setAccessible(true);Object result=select.invoke(activity);Field count=result.getClass().getDeclaredField("count");count.setAccessible(true);assertEquals(1,count.getInt(result));
  FakeFxReply two=new FakeFxReply();activity.onPresenceFrame565(target,ok,two,"ready:"+attempt);activity.onPresenceFrame565(target,ok,two,data);result=select.invoke(activity);assertEquals("two valid player frames are ambiguous",2,count.getInt(result));
  activity.onPresenceFrame565(target,ok,two,"bye:"+attempt);result=select.invoke(activity);assertEquals(1,count.getInt(result));
  activity.getSharedPreferences("kuro-discord-rpc563",Context.MODE_PRIVATE).edit().putBoolean("enabled",false).commit();update.invoke(activity);assertEquals("off",one.latest());result=select.invoke(activity);assertEquals("disabling presence discards timing",0,count.getInt(result));
  assertTrue(activity.loadPlayer("https://megaplay.buzz/stream/other","ms",""));long fresh=(Long)field("playerAttempt");assertTrue(fresh>attempt);assertTrue(((java.util.List<?>)field("presenceFrames565")).isEmpty());int old=one.sent.size();activity.onPresenceFrame565(target,ok,one,"ready:"+attempt);assertEquals(old,one.sent.size());activity.onPresenceFrame565(target,ok,one,"ready:"+fresh);assertEquals("off",one.latest());activity.closePlayer();assertEquals("off",one.latest());assertTrue(((java.util.List<?>)field("presenceFrames565")).isEmpty());
 }
 @Test public void pictureInPictureTransitionKeepsPlayerActiveAndRestoresLayout()throws Exception{
  open(0);WebView player=(WebView)field("player");View root=(View)field("root");root.findViewWithTag("enhanced-fullscreen-enter").performClick();assertTrue((Boolean)field("enhancedFullscreen"));View toolbar=(View)field("enhancedToolbar");assertNotNull(toolbar);
  set("pipExpanded560",true);set("pipRequest560",true);activity.onPause();assertFalse("PiP transition must not pause its WebView",Shadows.shadowOf(player).wasOnPauseCalled());activity.onPictureInPictureModeChanged(true,new android.content.res.Configuration());assertEquals(View.GONE,toolbar.getVisibility());assertTrue(Shadows.shadowOf(player).wasOnResumeCalled());activity.onResume();activity.onPictureInPictureModeChanged(false,new android.content.res.Configuration());assertFalse((Boolean)field("enhancedFullscreen"));assertNotNull("leaving PiP keeps Watch",field("playerLayer"));activity.onPause();assertTrue("ordinary background pause remains",Shadows.shadowOf(player).wasOnPauseCalled());activity.onResume();
 }

 @Test public void reloadActionRequiresConfirmationAndRejectsAStalePlayer()throws Exception{
  open(0);WebView player=(WebView)field("player");MainActivity.Bridge bridge=activity.new Bridge();int before=Shadows.shadowOf(player).getReloadInvocations();bridge.settingsAction560("reload");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(before,Shadows.shadowOf(player).getReloadInvocations());
  bridge.settingsAction560("reload");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(before+1,Shadows.shadowOf(player).getReloadInvocations());
  bridge.settingsAction560("reload");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();activity.closePlayer();dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(before+1,Shadows.shadowOf(player).getReloadInvocations());
 }

 @Test public void playbackDiagnosticsAreRedactedAndKeepPlayer()throws Exception{
  open(0);View root=(View)field("root");WebView before=(WebView)field("player");View button=root.findViewWithTag("player-diagnostics-573");assertNotNull(button);button.performClick();android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);TextView mode=dialog.getWindow().getDecorView().findViewWithTag("diagnostics-player-573"),guidance=dialog.getWindow().getDecorView().findViewWithTag("diagnostics-guidance-573"),issue=dialog.getWindow().getDecorView().findViewWithTag("diagnostics-issue-573");assertNotNull(mode);assertNotNull(guidance);assertNotNull(issue);String message=mode.getText()+" "+guidance.getText()+" "+issue.getText();assertTrue(message.contains("Embedded Web player"));assertTrue(message.contains("No URLs"));assertFalse(message.contains("megaplay.buzz"));assertFalse(message.contains("test-session"));assertNotNull(dialog.getWindow().getDecorView().findViewWithTag("diagnostics-close-573"));assertSame(before,field("player"));dialog.dismiss();
 }

 @Test public void updateSettingsAreReachableThroughTheBridge()throws Exception{
  KuroUpdater572.prefs(activity).edit().clear().commit();MainActivity.Bridge bridge=activity.new Bridge();bridge.settingsAction560("updates572");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();android.app.AlertDialog status=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(status);assertTrue(String.valueOf(Shadows.shadowOf(status).getMessage()).contains("Reminder spacing"));status.dismiss();
  bridge.settingsAction560("updatesNetwork573");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();android.app.AlertDialog network=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(network);network.getListView().performItemClick(network.getListView().getChildAt(1),1,1);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(KuroUpdater572.wifiOnly(activity));
  bridge.settingsAction560("updatesInterval573");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();android.app.AlertDialog interval=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();interval.getListView().performItemClick(interval.getListView().getChildAt(2),2,2);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(3,KuroUpdater572.reminderDays(KuroUpdater572.prefs(activity)));
 }

}
