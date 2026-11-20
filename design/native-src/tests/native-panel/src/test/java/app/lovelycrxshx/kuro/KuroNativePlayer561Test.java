package app.lovelycrxshx.kuro;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import androidx.media3.datasource.ByteArrayDataSource;
import androidx.media3.datasource.DataSource;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
/** Wires the real PlayerView with an in-memory HLS manifest. No server or codec is used. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class KuroNativePlayer561Test {
 @Test public void nativeSurfaceAndControlsAreAddedAndRemovedWithoutTouchingTheWebPlayer()throws Exception{
  ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).create().start().resume().visible();Activity a=controller.get();a.getPreferences(Context.MODE_PRIVATE).edit().putBoolean("autoplay",false).commit();
  FrameLayout box=new FrameLayout(a);View original=new View(a),cover=new View(a);box.addView(original);box.addView(cover);a.setContentView(box);
  String url="https://www.zenkaitv.com/api/source?url=https%3A%2F%2Fplayer.zilla-networks.com%2Fm3u8%2Ffixture",key=KuroPlayback561.key("zenkai","show","1");
  byte[] manifest="#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:10\n#EXTINF:10,\nhttps://www.zenkaitv.com/test.ts\n#EXT-X-ENDLIST\n".getBytes(StandardCharsets.UTF_8);
  DataSource.Factory fixture=()->new ByteArrayDataSource(manifest);
  KuroNativePlayer561 p=new KuroNativePlayer561(a,box,url,key,new JSONObject().put("skip",new JSONObject().put("intro",new JSONObject().put("start",5).put("end",30))),new KuroNativePlayer561.Listener(){public void failure(boolean blocked){}public void ended(){}},fixture);
  assertNotNull(box.findViewWithTag("native-player-561"));assertSame(original,box.getChildAt(0));assertSame(cover,box.getChildAt(box.getChildCount()-1));assertEquals(3,box.getChildCount());
  p.pauseForBackground();p.resumeForeground();p.release();assertNull(box.findViewWithTag("native-player-561"));assertEquals(2,box.getChildCount());assertSame(original,box.getChildAt(0));controller.pause().stop().destroy();
 }
}
