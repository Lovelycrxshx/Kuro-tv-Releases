package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** PiP actions and intent wiring under Android shadows, not a physical device. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class KuroPipControls571Test {
 @Test public void controlsExistOnlyForValidNativeSessions(){
  Activity a=Robolectric.buildActivity(Activity.class).create().start().resume().get();
  assertTrue(KuroPipControls571.params(a,"watch-1",false,true).getActions().isEmpty());
  assertTrue(KuroPipControls571.params(a,"not valid!",true,true).getActions().isEmpty());
  PictureInPictureParams playing=KuroPipControls571.params(a,"watch-1",true,true);
  assertEquals(1,playing.getActions().size());assertEquals("Pause",playing.getActions().get(0).getTitle());
  Intent intent=Shadows.shadowOf(playing.getActions().get(0).getActionIntent()).getSavedIntent();
  assertEquals("watch-1",intent.getStringExtra("session"));assertEquals(Uri.parse("kuro-pip://control/watch-1/pause"),intent.getData());
  assertEquals("Play",KuroPipControls571.params(a,"watch-1",true,false).getActions().get(0).getTitle());
 }
}
