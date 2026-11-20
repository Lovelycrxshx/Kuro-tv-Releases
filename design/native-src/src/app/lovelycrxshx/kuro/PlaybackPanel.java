package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;

/** Compact watch controls; the activity owns playback and saved state. */
public final class PlaybackPanel extends LinearLayout {
 public interface Listener {
  void nextEpisode();
  void autoplayChanged(boolean enabled);
  void autoNextChanged(boolean enabled);
  void autoSkipChanged(boolean enabled);
 }
 private final Listener listener;
 private final View nextButton;
 private final Switch autoplay,autoNext,autoSkip;
 private final LinearLayout firstRow,secondRow;
 private final Space rowGap;
 private final TextView episodeBadge;
 private boolean singleColumn;
 private static final int INK=0xfff5edf9,MUTED=0xffb7a4c3;

 public PlaybackPanel(Context context,String episode,String nextEpisode,boolean canNext,boolean autoplayOn,boolean autoNextOn,boolean autoSkipOn,Listener listener){
  super(context);this.listener=listener;setTag("playback-panel");setOrientation(VERTICAL);setPadding(dp(9),dp(9),dp(9),dp(9));
  setBackground(gradient(0xff281632,0xff1a1021,0xff563870,12));
  LinearLayout header=new LinearLayout(context);header.setOrientation(HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
  TextView mark=label("✦",14,0xffd7a6ff,true);mark.setTextSize(TypedValue.COMPLEX_UNIT_DIP,14);mark.setGravity(Gravity.CENTER);mark.setBackground(solid(0xff3d2452,0,11));mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);header.addView(mark,new LayoutParams(dp(22),dp(22)));space(header,7);
  TextView title=label("Playback controls",13,INK,true);header.addView(title,new LayoutParams(0,-2,1));
  String current=safeEpisode(episode);if(!current.isEmpty()){
   episodeBadge=label("EP "+current,9,0xffd9b4f8,true);episodeBadge.setGravity(Gravity.CENTER);episodeBadge.setPadding(dp(7),dp(4),dp(7),dp(4));episodeBadge.setBackground(solid(0xff3b244d,0xff674481,9));header.addView(episodeBadge);
  }else episodeBadge=null;
  addView(header,new LayoutParams(-1,-2));verticalSpace(this,7);
  firstRow=row(context);secondRow=row(context);addView(firstRow,new LayoutParams(-1,-2));rowGap=new Space(context);rowGap.setVisibility(View.GONE);addView(rowGap,new LayoutParams(1,dp(6)));addView(secondRow,new LayoutParams(-1,-2));
  nextButton=buildNext(context,nextEpisode,canNext);addTile(firstRow,nextButton,false);
  autoplay=buildToggle(context,firstRow,"Autoplay video",autoplayOn,true,0);
  autoNext=buildToggle(context,secondRow,"Auto next",autoNextOn,canNext,1);
  autoSkip=buildToggle(context,secondRow,"Skip intro/outro",autoSkipOn,true,2);
  verticalSpace(this,7);
  TextView note=label("ⓘ  Auto-next & skip depend on player support. Autoplay may need a tap.",9,MUTED,false);note.setLineSpacing(dp(1),1f);addView(note,new LayoutParams(-1,-2));
 }
 /** Reflow to one column instead of clipping labels at narrow widths or large system text. */
 @Override protected void onMeasure(int widthSpec,int heightSpec){
  if(firstRow!=null&&secondRow!=null&&View.MeasureSpec.getMode(widthSpec)!=View.MeasureSpec.UNSPECIFIED){
   float widthDp=View.MeasureSpec.getSize(widthSpec)/getResources().getDisplayMetrics().density;
   float fontScale=Math.max(1f,getResources().getConfiguration().fontScale);
   float titleRoom=(widthDp-18f-8f)/2f-14f-48f;
   boolean stack=titleRoom<66f*fontScale;
   if(stack!=singleColumn){singleColumn=stack;reflow(firstRow,stack);reflow(secondRow,stack);rowGap.setVisibility(stack?View.VISIBLE:View.GONE);}
   if(episodeBadge!=null){int visible=stack&&fontScale>=1.3f?View.GONE:View.VISIBLE;if(episodeBadge.getVisibility()!=visible)episodeBadge.setVisibility(visible);}
  }
  super.onMeasure(widthSpec,heightSpec);
 }
 private void reflow(LinearLayout row,boolean stack){
  row.setOrientation(stack?VERTICAL:HORIZONTAL);
  for(int i=0;i<row.getChildCount();i++){
   View tile=row.getChildAt(i);LayoutParams p=(LayoutParams)tile.getLayoutParams();p.width=stack?LayoutParams.MATCH_PARENT:0;p.height=stack?LayoutParams.WRAP_CONTENT:LayoutParams.MATCH_PARENT;p.weight=stack?0:1;
   if(stack)p.setMargins(0,0,0,i==0?dp(6):0);else p.setMargins(i==1?dp(4):0,0,i==0?dp(4):0,0);tile.setLayoutParams(p);
  }
 }
 private int dp(int value){return (int)(getResources().getDisplayMetrics().density*value+.5f);}
 private static String safeEpisode(String value){String s=value==null?"":value.trim();return s.matches("[0-9]{1,5}")?s:"";}
 private TextView label(String value,int size,int color,boolean bold){
  TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setIncludeFontPadding(false);return v;
 }
 private GradientDrawable solid(int color,int border,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
 private GradientDrawable gradient(int top,int bottom,int border,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
 private RippleDrawable ripple(GradientDrawable content,int color){return new RippleDrawable(ColorStateList.valueOf(color),content,null);}
 // Keep a 48dp hit target while giving the visible tiles the smaller height of the server buttons.
 private InsetDrawable compactSurface(GradientDrawable content,int color){int inset=getResources().getConfiguration().fontScale>=1.3f?0:dp(5);return new InsetDrawable(ripple(content,color),0,inset,0,inset){@Override public boolean getPadding(Rect out){out.set(0,0,0,0);return false;}};}
 private void space(LinearLayout root,int width){root.addView(new Space(getContext()),new LayoutParams(dp(width),1));}
 private void verticalSpace(LinearLayout root,int height){root.addView(new Space(getContext()),new LayoutParams(1,dp(height)));}
 private LinearLayout row(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(HORIZONTAL);return r;}
 private void addTile(LinearLayout r,View tile,boolean right){LayoutParams p=new LayoutParams(0,-1,1);if(right)p.setMargins(dp(4),0,0,0);else p.setMargins(0,0,dp(4),0);r.addView(tile,p);}
 private View buildNext(Context context,String episode,boolean enabled){
  String next=safeEpisode(episode);LinearLayout card=row(context);card.setTag("playback-next");card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(10),dp(3),dp(7),dp(3));card.setMinimumHeight(dp(48));
  card.setBackground(compactSurface(enabled?gradient(0xffc294f2,0xff9e65d7,0xffd9b8f8,7):gradient(0xff332440,0xff291d34,0xff5c486b,7),0x44ffffff));card.setEnabled(enabled);card.setFocusable(true);card.setClickable(enabled);
  card.setContentDescription(enabled?(next.isEmpty()?"Play next episode":"Play next episode, episode "+next):"Next episode unavailable");
  LinearLayout words=new LinearLayout(context);words.setOrientation(VERTICAL);card.addView(words,new LayoutParams(0,-2,1));
  TextView title=label("Next episode",11,enabled?0xff25102f:0xffe1d4e8,true);title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);words.addView(title);
  TextView sub=label(enabled?(next.isEmpty()?"Continue watching":"Episode "+next):"Last episode",9,enabled?0xff533661:0xffc0adc9,false);sub.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);words.addView(sub);
  TextView arrow=label("›",22,enabled?0xff4b2a61:0xffbba6c6,true);arrow.setGravity(Gravity.CENTER);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(arrow,new LayoutParams(dp(20),-2));
  card.setOnClickListener(v->{if(card.isEnabled()&&this.listener!=null)this.listener.nextEpisode();});return card;
 }
 private Switch buildToggle(Context context,LinearLayout target,String title,boolean checked,boolean enabled,int which){
  LinearLayout card=row(context);card.setTag("playback-tile-"+which);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(10),0,dp(4),0);card.setMinimumHeight(dp(48));card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  LinearLayout words=new LinearLayout(context);words.setOrientation(VERTICAL);card.addView(words,new LayoutParams(0,-2,1));
  TextView name=label(title,11,enabled?INK:0xffc9b8d1,true);name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);words.addView(name);
  TextView state=label(enabled?(checked?"ON":"OFF"):"N/A",9,enabled?0xffbe9ece:0xffb5a3c0,true);state.setLetterSpacing(.06f);state.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);words.addView(state);
  LinearLayout touch=row(context);touch.setGravity(Gravity.CENTER);card.addView(touch,new LayoutParams(dp(48),dp(48)));
  Switch toggle=new Switch(context);toggle.setTag(which==0?"playback-autoplay":which==1?"playback-auto-next":"playback-skip");toggle.setText(null);toggle.setShowText(false);toggle.setContentDescription(enabled?title:title+", unavailable on the last episode");toggle.setPadding(0,0,0,0);toggle.setSwitchMinWidth(dp(38));toggle.setMinWidth(dp(48));toggle.setMinHeight(dp(48));
  GradientDrawable thumb=new GradientDrawable();thumb.setShape(GradientDrawable.OVAL);thumb.setSize(dp(21),dp(21));thumb.setColor(Color.WHITE);toggle.setThumbDrawable(thumb);GradientDrawable track=solid(Color.WHITE,0,12);track.setSize(dp(39),dp(22));toggle.setTrackDrawable(track);
  int[][] states=new int[][]{new int[]{-android.R.attr.state_enabled,android.R.attr.state_checked},new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}};
  toggle.setThumbTintList(new ColorStateList(states,new int[]{0xffb79aca,0xff907d9d,0xffe3c7ff,0xffc3accf}));toggle.setTrackTintList(new ColorStateList(states,new int[]{0xff604276,0xff39283f,0xff8652b4,0xff4d365c}));
  toggle.setChecked(checked);toggle.setEnabled(enabled);touch.addView(toggle,new LayoutParams(dp(48),dp(48)));
  card.setBackground(compactSurface(solid(!enabled?0xff21162a:checked?0xff3b2550:0xff211529,!enabled?0xff503d5a:checked?0xff79579b:0xff4f385d,7),0x33c997f2));
  card.setOnClickListener(v->{if(toggle.isEnabled())toggle.toggle();});
  toggle.setOnCheckedChangeListener((button,on)->{
   state.setText(enabled?(on?"ON":"OFF"):"N/A");card.setBackground(compactSurface(solid(on?0xff3b2550:0xff211529,on?0xff79579b:0xff4f385d,7),0x33c997f2));
   if(this.listener==null)return;if(which==0)this.listener.autoplayChanged(on);else if(which==1)this.listener.autoNextChanged(on);else this.listener.autoSkipChanged(on);
  });
  addTile(target,card,which!=1);return toggle;
 }
 public View getNextButton(){return nextButton;}
 public Switch getAutoplay(){return autoplay;}
 public Switch getAutoNext(){return autoNext;}
 public Switch getAutoSkip(){return autoSkip;}
}
