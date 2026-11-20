package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Production watch header layout and callbacks; this uses a shadow framework. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE,qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WatchHeaderTest {
 private static final class Calls implements WatchHeader.Listener{
  List<String> values=new ArrayList<>();public void home(){values.add("home");}public void search(){values.add("search");}public void notifications(){values.add("news");}public void episodes(){values.add("episodes");}public void login(){values.add("login");}
 }
 private WatchHeader measure(int widthDp,float fontScale,Calls calls){
  Context c=RuntimeEnvironment.getApplication();Configuration conf=new Configuration(c.getResources().getConfiguration());conf.fontScale=fontScale;c.getResources().updateConfiguration(conf,c.getResources().getDisplayMetrics());
  WatchHeader header=new WatchHeader(c,calls);float d=c.getResources().getDisplayMetrics().density;int w=Math.round(widthDp*d),h=Math.round(62*d);
  header.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));header.layout(0,0,w,h);return header;
 }
 @Test public void actionsAndAccessibleNames(){
  Calls calls=new Calls();WatchHeader h=measure(360,1,calls);
  for(String tag:new String[]{"watch-home","watch-search","watch-notifications","watch-login"}){View v=h.findViewWithTag(tag);assertNotNull(tag,v);assertNotNull(v.getContentDescription());assertTrue(v.isClickable());v.performClick();}
  assertEquals(java.util.Arrays.asList("home","search","news","login"),calls.values);assertTrue(h.findViewWithTag("watch-home").getContentDescription().toString().contains("Home"));
 }
 @Test public void fitsPhonesLandscapeAndLargerText(){
  for(int width:new int[]{280,320,360,414,600})for(float scale:new float[]{1f,1.5f,2f}){
   WatchHeader h=measure(width,scale,new Calls());float density=h.getResources().getDisplayMetrics().density;int prior=0;
   for(String tag:new String[]{"watch-home","watch-search","watch-notifications","watch-login"}){
    View v=h.findViewWithTag(tag);assertTrue(tag+" >=48dp wide at "+width,v.getWidth()>=Math.round(48*density));assertTrue(tag+" >=48dp tall",v.getHeight()>=Math.round(48*density));assertTrue(tag+" overlaps prior",v.getLeft()>=prior);assertTrue(tag+" overflows",v.getRight()<=h.getWidth());prior=v.getRight();
   }
   FrameLayout login=(FrameLayout)h.findViewWithTag("watch-login");TextView word=(TextView)login.getChildAt(0);assertTrue("single line login",word.getLayout().getLineCount()<=1);assertEquals("login must not be ellipsized",0,word.getLayout().getEllipsisCount(0));assertTrue("login text must fit its button",word.getPaint().measureText(word.getText().toString())<=word.getWidth()+1);assertTrue("login height must fit",word.getLayout().getHeight()<=word.getHeight());
  }
 }
 @Test public void actualTouchDispatchHitsEachVisibleTarget(){
  android.app.Activity activity=org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup().get();Calls calls=new Calls();WatchHeader h=new WatchHeader(activity,calls);activity.setContentView(h);
  float d=activity.getResources().getDisplayMetrics().density;int w=Math.round(360*d),height=Math.round(62*d);h.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));h.layout(0,0,w,height);long at=1000;
  for(String tag:new String[]{"watch-home","watch-search","watch-notifications","watch-login"}){View v=h.findViewWithTag(tag);float x=(v.getLeft()+v.getRight())/2f,y=(v.getTop()+v.getBottom())/2f;MotionEvent down=MotionEvent.obtain(at,at,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(at,at+50,MotionEvent.ACTION_UP,x,y,0);assertTrue("touch down "+tag,h.dispatchTouchEvent(down));h.dispatchTouchEvent(up);down.recycle();up.recycle();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();at+=500;}
  assertEquals(java.util.Arrays.asList("home","search","news","login"),calls.values);
 }
 @Test public void renderPreview()throws Exception{
  WatchHeader h=measure(360,1,new Calls());ImageView logo=h.findViewWithTag("watch-home-logo");Bitmap original=BitmapFactory.decodeFile("assets/kuro-logo.png");assertNotNull("bundled logo fixture",original);logo.setImageBitmap(original);
  // Robolectric without a manifest has no packaged assets; remove the test-only
  // text fallback so the preview reflects the APK, which packages this image.
  FrameLayout parent=(FrameLayout)logo.getParent();for(int i=parent.getChildCount()-1;i>=0;i--)if(parent.getChildAt(i) instanceof TextView)parent.removeViewAt(i);
  h.measure(View.MeasureSpec.makeMeasureSpec(h.getWidth(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h.getHeight(),View.MeasureSpec.EXACTLY));h.layout(0,0,h.getWidth(),h.getHeight());
  Bitmap bitmap=Bitmap.createBitmap(h.getWidth(),h.getHeight(),Bitmap.Config.ARGB_8888);h.draw(new Canvas(bitmap));String target=System.getProperty("kuro.header.preview");if(target!=null){try(FileOutputStream out=new FileOutputStream(new File(target))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}
 }
}
