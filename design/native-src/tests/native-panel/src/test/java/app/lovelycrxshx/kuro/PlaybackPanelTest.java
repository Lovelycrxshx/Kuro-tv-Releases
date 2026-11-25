package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.LinearLayout;
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

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE,qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PlaybackPanelTest {
 static final class Calls implements PlaybackPanel.Listener {
  int next; final List<String> toggles=new ArrayList<>();
  public void nextEpisode(){next++;}public void autoplayChanged(boolean b){toggles.add("autoplay="+b);}public void autoNextChanged(boolean b){toggles.add("autoNext="+b);}public void autoSkipChanged(boolean b){toggles.add("skip="+b);}
 }
 PlaybackPanel create(Calls c,boolean canNext,boolean autoplay,boolean autoNext,boolean skip){return new PlaybackPanel(RuntimeEnvironment.getApplication(),"11","12",canNext,autoplay,autoNext,skip,c);}
 @Test public void preferencesAndActions(){
  Calls c=new Calls();PlaybackPanel p=create(c,true,true,false,false);
  assertTrue(p.getAutoplay().isChecked());assertFalse(p.getAutoNext().isChecked());assertFalse(p.getAutoSkip().isChecked());assertTrue(c.toggles.isEmpty());
  p.getNextButton().performClick();assertEquals(1,c.next);
  p.getAutoplay().performClick();p.getAutoNext().performClick();p.getAutoSkip().performClick();
  assertEquals(java.util.Arrays.asList("autoplay=false","autoNext=true","skip=true"),c.toggles);
  p.findViewWithTag("playback-tile-2").performClick();assertFalse(p.getAutoSkip().isChecked());assertEquals("skip=false",c.toggles.get(3));
  p.findViewWithTag("playback-tile-0").performClick();assertTrue(p.getAutoplay().isChecked());assertEquals("autoplay=true",c.toggles.get(4));
  p.findViewWithTag("playback-tile-1").performClick();assertFalse(p.getAutoNext().isChecked());assertEquals("autoNext=false",c.toggles.get(5));
  assertEquals("Autoplay video",p.getAutoplay().getContentDescription());assertEquals("Auto next",p.getAutoNext().getContentDescription());assertEquals("Skip intro/outro",p.getAutoSkip().getContentDescription());
 }
 @Test public void lastEpisodeDoesNotAdvanceOrChangeStoredAutoNext(){
  Calls c=new Calls();PlaybackPanel p=create(c,false,true,true,true);
  assertFalse(p.getNextButton().isEnabled());assertFalse(p.getAutoNext().isEnabled());assertTrue(p.getAutoNext().isChecked());
  p.getNextButton().performClick();p.findViewWithTag("playback-tile-1").performClick();
  assertEquals(0,c.next);assertTrue(c.toggles.isEmpty());assertTrue(p.getAutoNext().isChecked());
  assertTrue(p.getNextButton().getContentDescription().toString().contains("unavailable"));
 }
 private void checkInside(View v){
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View child=g.getChildAt(i);assertTrue("outside left "+child,child.getLeft()>=0);assertTrue("outside top "+child,child.getTop()>=0);assertTrue("outside right "+child+" "+child.getRight()+" > "+g.getWidth(),child.getRight()<=g.getWidth());assertTrue("outside bottom "+child+" "+child.getBottom()+" > "+g.getHeight(),child.getBottom()<=g.getHeight());checkInside(child);}}
  if(v instanceof TextView && !(v instanceof Switch)){TextView t=(TextView)v;if(t.getLayout()!=null){for(int i=0;i<t.getLayout().getLineCount();i++)assertEquals("text truncated "+t.getText(),0,t.getLayout().getEllipsisCount(i));}}
 }
 private PlaybackPanel measure(int widthDp,float scale){
  Context context=RuntimeEnvironment.getApplication();Configuration config=new Configuration(context.getResources().getConfiguration());config.fontScale=scale;context.getResources().updateConfiguration(config,context.getResources().getDisplayMetrics());
  PlaybackPanel p=create(new Calls(),true,true,false,true);float density=context.getResources().getDisplayMetrics().density;int width=Math.round(widthDp*density);
  p.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));p.layout(0,0,width,p.getMeasuredHeight());
  assertTrue(p.getMeasuredHeight()>0);checkInside(p);
  for(Switch sw:new Switch[]{p.getAutoplay(),p.getAutoNext(),p.getAutoSkip()}){assertTrue("switch width "+sw.getWidth(),sw.getWidth()>=Math.round(48*density));assertTrue("switch height "+sw.getHeight(),sw.getHeight()>=Math.round(48*density));}
  assertTrue(p.getNextButton().getWidth()>=Math.round(48*density));assertTrue(p.getNextButton().getHeight()>=Math.round(48*density));return p;
 }
 @Test public void buttonsAreCompactAtStandardText(){
  PlaybackPanel p=measure(324,1f);float density=p.getResources().getDisplayMetrics().density;
  assertTrue("next action is no taller than 52dp",p.getNextButton().getHeight()<=Math.round(52*density));
  for(int i=0;i<3;i++)assertTrue("toggle tile is no taller than 52dp",((View)p.findViewWithTag("playback-tile-"+i)).getHeight()<=Math.round(52*density));
 }
 @Test public void layoutFitsNarrowAndLargeText(){for(int w:new int[]{244,284,324,414,600})for(float s:new float[]{1f,1.4f,2f})measure(w,s);}
 private int[] position(View view){int x=0,y=0;while(view.getParent() instanceof View && !(view instanceof PlaybackPanel)){x+=view.getLeft();y+=view.getTop();view=(View)view.getParent();}return new int[]{x,y};}
 private void touch(View root,long at,int action,float x,float y){MotionEvent e=MotionEvent.obtain(at>=2000?2000:1000,at,action,x,y,0);root.dispatchTouchEvent(e);e.recycle();}
 @Test public void touchAndSlideSwitches(){
  PlaybackPanel p=measure(324,1f);android.app.Activity a=org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup().get();a.setContentView(p);int width=p.getMeasuredWidth();p.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));p.layout(0,0,width,p.getMeasuredHeight());Switch next=p.getAutoNext();int[] xy=position(next);float x=xy[0]+next.getWidth()/2f,y=xy[1]+next.getHeight()/2f;
  touch(p,1000,MotionEvent.ACTION_DOWN,x,y);touch(p,1050,MotionEvent.ACTION_UP,x,y);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue("tap should toggle auto next",next.isChecked());
  Switch skip=p.getAutoSkip();xy=position(skip);float sy=xy[1]+skip.getHeight()/2f;float right=xy[0]+skip.getWidth()-8,left=xy[0]+8;
  touch(p,2000,MotionEvent.ACTION_DOWN,right,sy);touch(p,2050,MotionEvent.ACTION_MOVE,left,sy);touch(p,2100,MotionEvent.ACTION_UP,left,sy);org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse("slide should toggle skip",skip.isChecked());
 }
 @Test public void reflowsBackAndForthWithoutLosingState(){
  Context context=RuntimeEnvironment.getApplication();Configuration config=new Configuration(context.getResources().getConfiguration());config.fontScale=1f;context.getResources().updateConfiguration(config,context.getResources().getDisplayMetrics());
  Calls c=new Calls();PlaybackPanel p=create(c,true,true,false,true);float d=context.getResources().getDisplayMetrics().density;
  LinearLayout row=(LinearLayout)((ViewGroup)((ViewGroup)p.getAutoplay().getParent()).getParent()).getParent();
  for(int width:new int[]{324,244,324}){int px=Math.round(width*d);p.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));p.layout(0,0,px,p.getMeasuredHeight());checkInside(p);assertEquals(width==244?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL,row.getOrientation());assertTrue(p.getAutoplay().isChecked());assertTrue(p.getAutoSkip().isChecked());}
  p.getAutoSkip().performClick();assertEquals(java.util.Arrays.asList("skip=false"),c.toggles);
 }
 @Test public void renderPreview()throws Exception {
  PlaybackPanel p=measure(324,1f);int margin=24;Bitmap bm=Bitmap.createBitmap(p.getWidth()+margin*2,p.getHeight()+margin*2,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bm);c.drawColor(0xff0c0710);c.translate(margin,margin);p.draw(c);
  String target=System.getProperty("kuro.panel.preview");if(target!=null){try(FileOutputStream out=new FileOutputStream(new File(target))){bm.compress(Bitmap.CompressFormat.PNG,100,out);}}
  String lastTarget=System.getProperty("kuro.panel.lastpreview");if(lastTarget!=null){PlaybackPanel last=create(new Calls(),false,true,true,false);int width=p.getWidth();last.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));last.layout(0,0,width,last.getMeasuredHeight());Bitmap l=Bitmap.createBitmap(last.getWidth()+margin*2,last.getHeight()+margin*2,Bitmap.Config.ARGB_8888);Canvas lc=new Canvas(l);lc.drawColor(0xff0c0710);lc.translate(margin,margin);last.draw(lc);try(FileOutputStream out=new FileOutputStream(new File(lastTarget))){l.compress(Bitmap.CompressFormat.PNG,100,out);}}
  String bigTarget=System.getProperty("kuro.panel.bigpreview");if(bigTarget!=null){PlaybackPanel big=measure(244,2f);Bitmap b=Bitmap.createBitmap(big.getWidth()+margin*2,big.getHeight()+margin*2,Bitmap.Config.ARGB_8888);Canvas large=new Canvas(b);large.drawColor(0xff0c0710);large.translate(margin,margin);big.draw(large);try(FileOutputStream out=new FileOutputStream(new File(bigTarget))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
 }
}
