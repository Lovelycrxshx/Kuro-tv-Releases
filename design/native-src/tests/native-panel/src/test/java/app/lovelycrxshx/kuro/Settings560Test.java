package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.webkit.WebView;
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
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/** Local preference validation and view wiring. Framework and network are shadows. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE,qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class Settings560Test {
 private ActivityController<MainActivity> controller;private MainActivity activity;
 private static JSONObject config()throws Exception{return new JSONObject().put("audio","dub").put("server1","Volt").put("server2","Ryu").put("server3","").put("keepOn",true).put("errorAlerts",false).put("siteView","mobile").put("orientation","landscape").put("startup","last").put("theme","dark").put("accent","blue").put("pull",false).put("handleSize",48).put("handleOpacity",80).put("textZoom",115).put("brightness",70).put("adBlock",true).put("thirdCookies",false).put("external","ask");}
 @Before public void setup(){activity=Robolectric.buildActivity(MainActivity.class).get();KuroPreferences560.prefs(activity).edit().clear().commit();controller=Robolectric.buildActivity(MainActivity.class);activity=controller.setup().get();}
 @After public void close(){if(controller!=null)controller.pause().stop().destroy();}
 private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
 @Test public void validatedSettingsApplyAndPersist()throws Exception{
  MainActivity.Bridge bridge=activity.new Bridge();bridge.applySettings560(config().toString());Shadows.shadowOf(Looper.getMainLooper()).idle();
  assertEquals("landscape",KuroPreferences560.prefs(activity).getString("orientation",""));assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,activity.getRequestedOrientation());assertEquals(115,((WebView)field("ui")).getSettings().getTextZoom());assertEquals(.70f,activity.getWindow().getAttributes().screenBrightness,.001f);
  JSONObject bad=config().put("textZoom",10000);assertFalse(KuroPreferences560.store(activity,bad.toString()));assertEquals(115,KuroPreferences560.prefs(activity).getInt("textZoom",0));assertFalse(KuroPreferences560.store(activity,config().put("extra","bad").toString()));
 }
 @Test public void watchHeaderSwitchesFromLoginToMonogramAndBack(){
 // 5.81: no profile pictures — the signed-in chip shows the account initials.
  WatchHeader h=new WatchHeader(activity,null);View target=h.findViewWithTag("watch-login");TextView badge=(TextView)h.findViewWithTag("watch-mono571");assertNotNull(badge);assertEquals(View.GONE,badge.getVisibility());h.setProfile(true,"ka");assertEquals(View.VISIBLE,badge.getVisibility());assertEquals("KA",badge.getText().toString());assertEquals("Open profile menu",target.getContentDescription());h.setProfile(false,"");assertEquals(View.GONE,badge.getVisibility());assertEquals("Log in",target.getContentDescription());
 }
 @Test public void dubRetryHonorsPreferredNamesWithoutRepeating()throws Exception{
  JSONArray options=new JSONArray().put(new JSONObject().put("lang","dub").put("name","Ryu")).put(new JSONObject().put("lang","dub").put("name","Volt")).put(new JSONObject().put("lang","sub").put("name","Volt"));boolean[] tried={false,false,false};String[] pref={"Volt","Ryu",""};assertEquals(1,RetrySelector.next(options,tried,"dub",pref));tried[1]=true;assertEquals(0,RetrySelector.next(options,tried,"dub",pref));tried[0]=true;assertEquals(2,RetrySelector.next(options,tried,"dub",pref));
 }
 @Test public void badBridgeSettingsCannotChangeStoredData()throws Exception{assertTrue(KuroPreferences560.store(activity,config().toString()));MainActivity.Bridge bridge=activity.new Bridge();bridge.applySettings560(config().put("orientation","nonsense").toString());Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("landscape",KuroPreferences560.prefs(activity).getString("orientation",""));}
 @Test public void externalLinkPolicyBlocksAsksAndOpensOnlyWhenAllowed()throws Exception{
  MainActivity.Bridge bridge=activity.new Bridge();assertTrue(KuroPreferences560.store(activity,config().put("external","blocked").toString()));bridge.openExternalSource("anicult");Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(Shadows.shadowOf(activity).getNextStartedActivity());assertTrue(ShadowToast.getTextOfLatestToast().contains("blocked"));
  assertTrue(KuroPreferences560.store(activity,config().put("external","ask").toString()));bridge.openExternalSource("anicult");Shadows.shadowOf(Looper.getMainLooper()).idle();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
  assertTrue(KuroPreferences560.store(activity,config().put("external","allow").toString()));bridge.openExternalSource("anicult");Shadows.shadowOf(Looper.getMainLooper()).idle();Intent opened=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(opened);assertEquals(Intent.ACTION_VIEW,opened.getAction());assertEquals("https://anicult.vercel.app/",opened.getDataString());
 }
 @Test public void backupPickersAndUnavailablePipFailSafely(){
  MainActivity.Bridge bridge=activity.new Bridge();bridge.backupFile560("export","{\"format\":\"kuro-local-backup-1\"}");Shadows.shadowOf(Looper.getMainLooper()).idle();ShadowActivity.IntentForResult created=Shadows.shadowOf(activity).getNextStartedActivityForResult();assertNotNull(created);assertEquals(Intent.ACTION_CREATE_DOCUMENT,created.intent.getAction());assertEquals("application/json",created.intent.getType());assertEquals("Kuro-local-backup.json",created.intent.getStringExtra(Intent.EXTRA_TITLE));
  bridge.backupFile560("import","");Shadows.shadowOf(Looper.getMainLooper()).idle();ShadowActivity.IntentForResult selected=Shadows.shadowOf(activity).getNextStartedActivityForResult();assertNotNull(selected);assertEquals(Intent.ACTION_OPEN_DOCUMENT,selected.intent.getAction());
  bridge.settingsAction560("pip");Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(ShadowToast.getTextOfLatestToast().contains("Open a supported player"));
 }

}
