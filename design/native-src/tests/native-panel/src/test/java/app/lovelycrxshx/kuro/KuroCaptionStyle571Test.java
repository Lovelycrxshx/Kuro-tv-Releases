package app.lovelycrxshx.kuro;

import android.app.Activity;
import androidx.media3.ui.SubtitleView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Simulated subtitle view only; no real phone, subtitle track or DRM. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class KuroCaptionStyle571Test {
 @Test public void stylesAreOptInLocalAndRejectInvalidChoices(){
  Activity a=Robolectric.buildActivity(Activity.class).create().start().resume().get();
  a.getSharedPreferences("kuro_caption_style571",0).edit().clear().commit();
  SubtitleView v=new SubtitleView(a);assertEquals(0,KuroCaptionStyle571.choice(a));KuroCaptionStyle571.apply(a,v);
  for(int n=1;n<=3;n++){KuroCaptionStyle571.save(a,n);assertEquals(n,KuroCaptionStyle571.choice(a));KuroCaptionStyle571.apply(a,v);}
  try{KuroCaptionStyle571.save(a,4);fail("invalid style accepted");}catch(IllegalArgumentException expected){}
  assertEquals(3,KuroCaptionStyle571.choice(a));KuroCaptionStyle571.save(a,0);KuroCaptionStyle571.apply(a,v);assertEquals(0,KuroCaptionStyle571.choice(a));
 }
 @Test public void previewChangesWithoutSavingUntilApply(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();a.getSharedPreferences("kuro_caption_style571",0).edit().clear().commit();SubtitleView v=new SubtitleView(a);KuroCaptionStyle571.show(a,v);
  android.app.AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.widget.TextView preview=d.getWindow().getDecorView().findViewWithTag("subtitle-preview-573");assertNotNull(preview);assertTrue(preview.getText().toString().contains("System"));d.getListView().performItemClick(d.getListView().getChildAt(2),2,2);assertEquals(android.graphics.Color.YELLOW,preview.getCurrentTextColor());assertEquals(0,KuroCaptionStyle571.choice(a));d.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(0,KuroCaptionStyle571.choice(a));
  KuroCaptionStyle571.show(a,v);d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();d.getListView().performItemClick(d.getListView().getChildAt(3),3,3);d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(3,KuroCaptionStyle571.choice(a));
 }

 @Test public void opacityAndPositionApplyOnlyAfterConfirmation(){
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();android.content.SharedPreferences prefs=a.getSharedPreferences("kuro_caption_style571",0);prefs.edit().clear().commit();SubtitleView v=new SubtitleView(a);KuroCaptionStyle571.show(a,v);android.app.AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();android.widget.SeekBar alpha=d.getWindow().getDecorView().findViewWithTag("subtitle-opacity-573"),bottom=d.getWindow().getDecorView().findViewWithTag("subtitle-bottom-573");assertNotNull(alpha);assertNotNull(bottom);alpha.setProgress(40);bottom.setProgress(18);assertFalse(prefs.contains("opacity573"));assertFalse(prefs.contains("bottom573"));d.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(prefs.contains("opacity573"));
  KuroCaptionStyle571.show(a,v);d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();alpha=d.getWindow().getDecorView().findViewWithTag("subtitle-opacity-573");bottom=d.getWindow().getDecorView().findViewWithTag("subtitle-bottom-573");alpha.setProgress(40);bottom.setProgress(18);d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(40,prefs.getInt("opacity573",-1));assertEquals(20,prefs.getInt("bottom573",-1));KuroCaptionStyle571.apply(a,v);
 }

}
