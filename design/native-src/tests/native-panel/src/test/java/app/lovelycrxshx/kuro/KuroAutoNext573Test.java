package app.lovelycrxshx.kuro;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Looper;
import android.widget.LinearLayout;
import android.webkit.WebView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class KuroAutoNext573Test {
 private ActivityController<MainActivity> controller;private MainActivity activity;
 private void set(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
 private Object get(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
 private void call(String name,Class<?>[] types,Object...args)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}
 @Before public void start()throws Exception{controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();activity.getPreferences(Context.MODE_PRIVATE).edit().putBoolean("auto_next",true).commit();set("activityResumed",true);set("watchSession","session-1");set("playerLayer",new LinearLayout(activity));set("ui",new WebView(activity));}
 @After public void close()throws Exception{call("cancelAutoNext573",new Class<?>[]{});set("playerLayer",null);controller.pause().stop().destroy();}
 @Test public void countdownShowsAndCancelStopsIt()throws Exception{call("offerAutoNext573",new Class<?>[]{String.class,int.class},"session-1",1);AlertDialog d=(AlertDialog)get("autoNextDialog573");assertNotNull(d);assertTrue(d.isShowing());d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();shadowOf(Looper.getMainLooper()).idle();assertNull(get("autoNextDialog573"));assertNull(get("autoNextTick573"));shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(11));assertEquals("session-1",get("watchSession"));}
 @Test public void wrongSessionOrDisabledNeverOpens()throws Exception{call("offerAutoNext573",new Class<?>[]{String.class,int.class},"old",1);assertNull(get("autoNextDialog573"));activity.getPreferences(Context.MODE_PRIVATE).edit().putBoolean("auto_next",false).commit();call("offerAutoNext573",new Class<?>[]{String.class,int.class},"session-1",1);assertNull(get("autoNextDialog573"));}
 @Test public void leavingForegroundCancels()throws Exception{call("offerAutoNext573",new Class<?>[]{String.class,int.class},"session-1",1);assertNotNull(get("autoNextDialog573"));controller.pause();assertNull(get("autoNextDialog573"));}
}
