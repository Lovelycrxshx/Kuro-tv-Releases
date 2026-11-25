package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.SubtitleView;

/** Optional appearance for Media3's subtitle view only, never arbitrary WebViews. */
@OptIn(markerClass = UnstableApi.class)
final class KuroCaptionStyle571 {
 private static final String PREFS="kuro_caption_style571", KEY="style";
 private static final String[] LABELS={"System default", "Large white", "Large yellow", "High contrast"};
 private KuroCaptionStyle571(){}
 static int choice(Context context){int n=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(KEY,0);return n>=0&&n<LABELS.length?n:0;}
 static void save(Context context,int value){if(value<0||value>=LABELS.length)throw new IllegalArgumentException("Invalid caption style");context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt(KEY,value).apply();}
 static void apply(Context context,SubtitleView view){
  if(view==null)return;int value=choice(context);int bottom=Math.max(2,Math.min(30,context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("bottom573",8)));view.setBottomPaddingFraction(bottom/100f);
  if(value==0){view.setApplyEmbeddedStyles(true);view.setApplyEmbeddedFontSizes(true);view.setUserDefaultStyle();view.setUserDefaultTextSize();return;}
  view.setApplyEmbeddedStyles(false);view.setApplyEmbeddedFontSizes(false);
  int foreground=value==2?Color.YELLOW:Color.WHITE;int alpha=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("opacity573",-1);int background=alpha<0?(value==3?Color.BLACK:0xb8000000):Color.argb(Math.max(0,Math.min(100,alpha))*255/100,0,0,0);
  view.setStyle(new CaptionStyleCompat(foreground,background,Color.TRANSPARENT,CaptionStyleCompat.EDGE_TYPE_OUTLINE,Color.BLACK,Typeface.DEFAULT_BOLD));
  view.setFractionalTextSize(value==3?0.067f:0.064f);
 }
 // A sample only: the system and embedded subtitle styles can render differently.
 static void paintPreview(TextView preview,int value,int opacity){
  preview.setText(value==0?"System default · sample subtitles":"Sample subtitles");
  preview.setContentDescription("Subtitle appearance preview. "+(value==0?"System default; actual rendering may differ.":LABELS[value]));
  preview.setTextColor(value==2?Color.YELLOW:Color.WHITE);preview.setTextSize(value==0?17:20);preview.setTypeface(Typeface.DEFAULT,value==0?Typeface.NORMAL:Typeface.BOLD);preview.setBackgroundColor(value==0?0xb8000000:Color.argb(Math.max(0,Math.min(100,opacity))*255/100,0,0,0));
 }
 static void show(Activity activity,SubtitleView view){
  if(view==null)return;final int[] draft={choice(activity),Math.max(0,Math.min(100,activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("opacity573",72))),Math.max(2,Math.min(30,activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("bottom573",8)))};
  LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*activity.getResources().getDisplayMetrics().density+.5f);panel.setPadding(pad,0,pad,pad);
  TextView preview=new TextView(activity);preview.setTag("subtitle-preview-573");preview.setGravity(Gravity.CENTER);preview.setPadding(18,24,18,24);paintPreview(preview,draft[0],draft[1]);panel.addView(preview);
  TextView alphaLabel=new TextView(activity),bottomLabel=new TextView(activity);SeekBar alpha=new SeekBar(activity),bottom=new SeekBar(activity);alpha.setMax(100);alpha.setProgress(draft[1]);alpha.setTag("subtitle-opacity-573");bottom.setMax(28);bottom.setProgress(draft[2]-2);bottom.setTag("subtitle-bottom-573");
  Runnable labels=()->{alphaLabel.setText("Background opacity: "+draft[1]+"% (custom styles only)");alpha.setContentDescription("Subtitle background opacity "+draft[1]+" percent");bottomLabel.setText("Distance from bottom: "+draft[2]+"%");bottom.setContentDescription("Subtitle distance from bottom "+draft[2]+" percent");};labels.run();
  alpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int progress,boolean user){draft[1]=progress;labels.run();paintPreview(preview,draft[0],draft[1]);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
  bottom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int progress,boolean user){draft[2]=progress+2;labels.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
  panel.addView(alphaLabel);panel.addView(alpha);panel.addView(bottomLabel);panel.addView(bottom);ScrollView scroll=new ScrollView(activity);scroll.addView(panel);
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Native subtitle appearance")
   .setSingleChoiceItems(LABELS,draft[0],(d,which)->{draft[0]=which;paintPreview(preview,which,draft[1]);})
   .setView(scroll)
   .setNegativeButton("Cancel",null).setPositiveButton("Apply",(d,w)->{activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt(KEY,draft[0]).putInt("opacity573",draft[1]).putInt("bottom573",draft[2]).apply();apply(activity,view);})
   .setNeutralButton("About",(d,w)->new AlertDialog.Builder(activity).setTitle("Native subtitles only")
    .setMessage("These controls apply only to selectable subtitles in Kuro's direct native player. Opacity applies to Kuro's custom styles, not the system style. Some embedded subtitle positioning may override the bottom setting. Embedded websites, protected video, and captions burned into the picture are not changed. Preferences stay on this phone and are not included in the local backup.")
    .setPositiveButton("Close",null).show()).create();dialog.show();
 }
}
