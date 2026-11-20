package app.lovelycrxshx.kuro;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.webkit.WebView;
import android.widget.FrameLayout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Security and navigation tests only: the source and WebView engine are not contacted. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE,qualifiers="w360dp-h800dp-xhdpi")
public class MiruroBridgeTest {
 private ActivityController<MainActivity> controller;private MainActivity activity;private MainActivity.Bridge bridge;
 private static final class NoNetwork extends AbstractExecutorService {
  public void shutdown(){}public List<Runnable> shutdownNow(){return Collections.emptyList();}public boolean isShutdown(){return false;}public boolean isTerminated(){return false;}public boolean awaitTermination(long t,TimeUnit u){return true;}public void execute(Runnable r){}
 }
 @Before public void create()throws Exception{controller=Robolectric.buildActivity(MainActivity.class);activity=controller.get();Field f=MainActivity.class.getDeclaredField("pool");f.setAccessible(true);f.set(activity,new NoNetwork());controller.create(new Bundle()).start().resume().visible();bridge=activity.new Bridge();}
 @After public void destroy(){controller.pause().stop().destroy();}
 @Test public void sourceAndPlayerHostPoliciesRemainNarrow()throws Exception{
  Method source=MainActivity.class.getDeclaredMethod("sourceHost",String.class),player=MainActivity.class.getDeclaredMethod("playerHost",String.class);source.setAccessible(true);player.setAccessible(true);
  assertTrue((Boolean)source.invoke(null,"www.miruro.cx"));assertFalse((Boolean)source.invoke(null,"miruro.cx"));assertFalse((Boolean)source.invoke(null,"evil.www.miruro.cx"));assertFalse((Boolean)source.invoke(null,"www.miruro.cx.evil.example"));
  assertFalse((Boolean)player.invoke(null,"www.miruro.cx"));assertFalse((Boolean)player.invoke(null,"evil.example"));assertTrue((Boolean)player.invoke(null,"megaplay.buzz"));
 }
 @Test public void browserLinkUsesOnlyValidatedOriginalSiteEpisode(){
  bridge.openLibrary11Episode("one-piece-21","2");Shadows.shadowOf(Looper.getMainLooper()).idle();Intent intent=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(intent);assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals("https://www.miruro.cx/watch/one-piece-21?ep=2",intent.getDataString());
  for(String[] value:new String[][]{{"../evil","1"},{"https://evil.example","1"},{"name","0"},{"name","5001"},{"name","1&next=https://evil.example"},{"name","-1"}})bridge.openLibrary11Episode(value[0],value[1]);
  Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
 }
 @Test public void verificationWindowIsOpenedOnlyByTheUserAction(){
  bridge.verifyLibrary11();Shadows.shadowOf(Looper.getMainLooper()).idle();AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);assertTrue(dialog.isShowing());assertEquals("Library 11 verification",Shadows.shadowOf(dialog).getTitle());
  FrameLayout holder=(FrameLayout)Shadows.shadowOf(dialog).getView();assertNotNull(holder);assertTrue(holder.getChildAt(0) instanceof WebView);WebView check=(WebView)holder.getChildAt(0);assertEquals("https://www.miruro.cx/",Shadows.shadowOf(check).getLastLoadedUrl());assertFalse(check.getSettings().getAllowFileAccess());assertFalse(check.getSettings().getAllowContentAccess());dialog.dismiss();
 }
}
