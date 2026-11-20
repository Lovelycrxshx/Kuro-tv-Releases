package app.lovelycrxshx.kuro;

import android.content.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Preference/JS bridge test with no real Discord client or SDK native library. */
@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class KuroDiscordPreference563Test {
 private ActivityController<MainActivity> controller;
 @Before public void reset(){RuntimeEnvironment.getApplication().getSharedPreferences("kuro-discord-rpc563",Context.MODE_PRIVATE).edit().clear().commit();}
 @After public void finish(){if(controller!=null)controller.pause().stop().destroy();reset();}
 @Test public void sdkNoticesOpenFromSettings(){
  controller=Robolectric.buildActivity(MainActivity.class).create().start().resume();MainActivity a=controller.get();
  a.new Bridge().settingsAction560("licenses");Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
  android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());
  android.widget.TextView text=dialog.findViewById(android.R.id.message);assertNotNull(text);assertTrue(text.getText().toString().contains("Open Source Software Disclosure"));
 }
 @Test public void defaultOffAndIndependentOfExistingSettings(){
  controller=Robolectric.buildActivity(MainActivity.class).create().start().resume();MainActivity a=controller.get();MainActivity.Bridge bridge=a.new Bridge();
  a.getPreferences(Context.MODE_PRIVATE).edit().putBoolean("autoplay",true).commit();
  assertFalse(bridge.discordRpcEnabled563());bridge.setDiscordRpcEnabled563(true);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
  assertTrue(bridge.discordRpcEnabled563());assertTrue(a.getPreferences(Context.MODE_PRIVATE).getBoolean("autoplay",false));
  bridge.setDiscordRpcEnabled563(false);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(bridge.discordRpcEnabled563());
  assertTrue(a.getPreferences(Context.MODE_PRIVATE).getBoolean("autoplay",false));
 }
}
