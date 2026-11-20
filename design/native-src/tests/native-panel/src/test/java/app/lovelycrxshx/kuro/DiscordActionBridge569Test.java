package app.lovelycrxshx.kuro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;
import static org.junit.Assert.*;

/** Exercises the actual JS-interface to native-action dispatch, with no Discord
 * app/network. Keystore and native SDK behavior require a device. */
@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class DiscordActionBridge569Test {
 private ActivityController<MainActivity> controller;private MainActivity activity;private MainActivity.Bridge bridge;private SharedPreferences prefs;
 private void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 private String toast(){CharSequence v=ShadowToast.getTextOfLatestToast();return v==null?"":v.toString();}
 private void action(String name){bridge.discordAction564(name);idle();}
 @Before public void create(){
  Context app=org.robolectric.RuntimeEnvironment.getApplication();app.getSharedPreferences(KuroDiscordPreferences564.FILE,Context.MODE_PRIVATE).edit().clear().commit();app.getSharedPreferences("kuro-discord-presence-vault-v1",Context.MODE_PRIVATE).edit().clear().commit();
  controller=Robolectric.buildActivity(MainActivity.class).create().start().resume();activity=controller.get();bridge=activity.new Bridge();prefs=activity.getSharedPreferences(KuroDiscordPreferences564.FILE,Context.MODE_PRIVATE);
 }
 @After public void destroy(){if(controller!=null)controller.pause().stop().destroy();}
 @Test public void clearReachesOuterActivityAndTurnsSharingOff(){
  prefs.edit().putBoolean("enabled",true).putString("option-title","hide").commit();action("clear");
  assertFalse(prefs.getBoolean("enabled",true));assertEquals("hide",prefs.getString("option-title",""));assertTrue(toast().contains("clearing was requested"));
 }
 @Test public void offActionsGiveUsefulFeedbackAndDoNotChangeData(){
  prefs.edit().putBoolean("enabled",false).commit();
  for(String name:new String[]{"current","linked","link","test"}){action(name);assertTrue(name+": "+toast(),toast().contains("Turn on Discord Rich Presence"));assertFalse(prefs.getBoolean("enabled",true));}
  action("invalid");assertFalse(prefs.getBoolean("enabled",true));
 }
 @Test public void reconnectExplainsTargetAndLinkPrerequisites(){
  prefs.edit().putBoolean("enabled",true).putString("option-current","off").putString("option-linked","off").commit();
  action("current");assertTrue(toast().contains("current account sharing"));action("linked");assertTrue(toast().contains("linked account sharing"));
  prefs.edit().putString("option-linked","on").commit();action("linked");assertTrue(toast().contains("Link another Discord account first"));
  assertEquals("off",prefs.getString("option-current",""));assertEquals("on",prefs.getString("option-linked",""));
 }
 @Test public void linkAndUnlinkShowRealConfirmationAndCancelKeepsAuthorization(){
  prefs.edit().putBoolean("enabled",true).commit();activity.getSharedPreferences("kuro-discord-presence-vault-v1",Context.MODE_PRIVATE).edit().putString("refresh","fake-encrypted-fixture").commit();
  action("link");AlertDialog link=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(link);assertTrue(link.isShowing());assertTrue(((android.widget.TextView)link.findViewById(android.R.id.message)).getText().toString().contains("browser"));link.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
  action("unlink");AlertDialog unlink=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(unlink);assertTrue(unlink.isShowing());assertTrue(((android.widget.TextView)unlink.findViewById(android.R.id.message)).getText().toString().contains("signed-in Discord app"));unlink.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
  assertTrue(activity.getSharedPreferences("kuro-discord-presence-vault-v1",Context.MODE_PRIVATE).contains("refresh"));assertTrue(prefs.getBoolean("enabled",false));
 }
 @Test public void unlinkIsAvailableEvenWhenPresenceIsOff(){
  prefs.edit().putBoolean("enabled",false).commit();action("unlink");AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(prefs.getBoolean("enabled",true));
 }
 @Test public void confirmedUnlinkActuallyRemovesOnlyTheLinkedCredential()throws Exception{
  prefs.edit().putBoolean("enabled",false).putString("option-title","hide").commit();SharedPreferences vault=activity.getSharedPreferences("kuro-discord-presence-vault-v1",Context.MODE_PRIVATE);vault.edit().putString("refresh","fake-encrypted-fixture").putString("pending","fake-encrypted-pending").commit();
  action("unlink");AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();
  java.lang.reflect.Field rpcField=MainActivity.class.getDeclaredField("discordRpc563");rpcField.setAccessible(true);Object rpc=rpcField.get(activity);java.lang.reflect.Field workerField=KuroDiscordRpc563.class.getDeclaredField("worker");workerField.setAccessible(true);android.os.Handler worker=(android.os.Handler)workerField.get(rpc);java.util.concurrent.CountDownLatch drained=new java.util.concurrent.CountDownLatch(1);worker.post(drained::countDown);assertTrue("linked-account worker did not drain",drained.await(8,java.util.concurrent.TimeUnit.SECONDS));
  assertFalse("refresh retained",vault.contains("refresh"));assertFalse("pending retained",vault.contains("pending"));assertEquals("hide",prefs.getString("option-title",""));assertFalse("sharing enabled",prefs.getBoolean("enabled",true));
 }
 @Test public void enabledActionsReachTheirRequestPathsAndTestRejectsNoTargets()throws Exception{
  java.lang.reflect.Field rf=MainActivity.class.getDeclaredField("discordRpc563");rf.setAccessible(true);Object rpc=rf.get(activity);java.lang.reflect.Field af=KuroDiscordRpc563.class.getDeclaredField("active");af.setAccessible(true);af.setBoolean(rpc,true);
  prefs.edit().putBoolean("enabled",true).putString("option-current","on").putString("option-linked","on").commit();activity.getSharedPreferences("kuro-discord-presence-vault-v1",Context.MODE_PRIVATE).edit().putString("refresh","fake-encrypted-fixture").commit();
  action("current");assertTrue(toast().contains("Reconnecting current"));action("linked");assertTrue(toast().contains("Reconnecting linked"));action("test");assertTrue(toast().contains("15-second test was requested"));
  prefs.edit().putString("option-current","off").putString("option-linked","off").commit();action("test");assertTrue(toast().contains("Enable an account target"));
 }
 @Test public void queuedActionIsIgnoredAfterActivityDestroyed(){
  prefs.edit().putBoolean("enabled",true).commit();bridge.discordAction564("clear");controller.pause().stop().destroy();controller=null;idle();assertTrue(activity.isDestroyed());assertTrue("destroyed activity must not perform queued UI action",prefs.getBoolean("enabled",false));
 }
}
