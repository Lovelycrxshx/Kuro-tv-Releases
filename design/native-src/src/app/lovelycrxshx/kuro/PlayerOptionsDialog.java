package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import org.json.JSONObject;

/** Opt-in video/audio controls; actual processing is performed in the player. */
public final class PlayerOptionsDialog {
 public interface Listener{void changed(JSONObject settings);}
 private static final String KEY="kuro_fx_settings_537";
 private static final int INK=0xfff7f1fc,MUTED=0xffb7a9c1,PURPLE=0xffb58bf1,CARD=0xff221b2b,BORDER=0xff594369;
 private final Activity activity;private final Listener listener;private final Handler main=new Handler(Looper.getMainLooper());private final JSONObject values;
 private Dialog dialog;private LinearLayout body,header;private ScrollView scroll;private TextView statusText;private JSONObject runtime=new JSONObject();private Runnable pending;private boolean comparing=false;
 public PlayerOptionsDialog(Activity a,Listener listener){activity=a;this.listener=listener;values=readConfig(a);}
 private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
 private static JSONObject defaults(){JSONObject o=new JSONObject();try{o.put("enabled",false);o.put("ai",false);o.put("aiStrength",100);o.put("preset","A");o.put("line","off");o.put("detail",8);for(String k:new String[]{"sharpen","denoise","lineStrength","edge","saturation","contrast","brightness"})o.put(k,100);o.put("deband",0);o.put("gain",100);}catch(Exception ignored){}return o;}
 public static JSONObject readConfig(Activity a){
  JSONObject o=defaults();try{JSONObject raw=new JSONObject(a.getPreferences(Activity.MODE_PRIVATE).getString(KEY,"{}"));
   o.put("enabled",raw.optBoolean("enabled",false));o.put("ai",raw.optBoolean("ai",false));o.put("aiStrength",Math.max(0,Math.min(100,raw.optInt("aiStrength",100))));String p=raw.optString("preset","A");if(!p.matches("A|B|C|A\\+A|B\\+B|C\\+A"))p="A";o.put("preset",p);
   String l=raw.optString("line","off");if(!l.matches("off|dark|thin"))l="off";o.put("line",l);int d=raw.optInt("detail",8);o.put("detail",d==16||d==32?d:8);
   for(String k:new String[]{"sharpen","denoise","lineStrength","edge","saturation","contrast"})o.put(k,Math.max(0,Math.min(200,raw.optInt(k,100))));
   o.put("brightness",Math.max(0,Math.min(150,raw.optInt("brightness",100))));o.put("deband",Math.max(0,Math.min(100,raw.optInt("deband",0))));o.put("gain",Math.max(100,Math.min(300,raw.optInt("gain",100))));
  }catch(Exception ignored){}return o;
 }
 private void set(String key,Object value){try{values.put(key,value);}catch(Exception ignored){}activity.getPreferences(Activity.MODE_PRIVATE).edit().putString(KEY,values.toString()).apply();
  if(pending!=null)main.removeCallbacks(pending);pending=()->{pending=null;notifyChanged();};main.postDelayed(pending,80);
 }
 private void notifyChanged(){if(listener==null)return;try{JSONObject copy=new JSONObject(values.toString());copy.put("compare",comparing);listener.changed(copy);}catch(Exception ignored){}}
 private GradientDrawable shape(int color,int border,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
 private TextView text(String s,int sp,int color){TextView t=new TextView(activity);t.setText(s);t.setTextColor(color);t.setTextSize(sp);return t;}
 private TextView heading(String s,int size){TextView t=text(s,size,INK);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private void gap(LinearLayout host,int h){host.addView(new Space(activity),new LinearLayout.LayoutParams(1,dp(h)));}
 private void divider(){View v=new View(activity);v.setBackgroundColor(0xff49394f);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));p.setMargins(0,dp(18),0,dp(18));body.addView(v,p);}
 private LinearLayout row(){LinearLayout r=new LinearLayout(activity);r.setOrientation(LinearLayout.HORIZONTAL);return r;}
 private TextView button(String label,String tag,Runnable action){
  TextView t=heading(label,14);t.setTag(tag);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));t.setPadding(dp(12),dp(10),dp(12),dp(10));t.setBackground(shape(CARD,BORDER,12));t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->action.run());
  t.setOnTouchListener((v,e)->{int a=e.getActionMasked();if(a==android.view.MotionEvent.ACTION_DOWN)v.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start();else if(a==android.view.MotionEvent.ACTION_UP||a==android.view.MotionEvent.ACTION_CANCEL)v.animate().scaleX(1).scaleY(1).setDuration(170).start();return false;});return t;
 }
 private TextView choice(String label,String hint,boolean chosen,Runnable action){TextView t=button(label+(hint.isEmpty()?"":"\n"+hint),null,action);t.setTextSize(12);t.setLineSpacing(dp(3),1f);t.setBackground(shape(chosen?0xff654b78:CARD,chosen?PURPLE:BORDER,12));t.setContentDescription(label+(hint.isEmpty()?"":", "+hint)+(chosen?", selected":""));return t;}
 private void section(String s){TextView t=text(s,12,MUTED);t.setLetterSpacing(.04f);body.addView(t);gap(body,10);}
 private void showScreen(boolean video){
  body.removeAllViews();header.removeAllViews();
  if(video){TextView back=button("‹","fx-back",()->showScreen(false));back.setTextSize(21);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(48),dp(48));p.setMargins(0,0,dp(10),0);header.addView(back,p);}
  LinearLayout titles=new LinearLayout(activity);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(heading(video?"Video enhancement":"Advanced",18));statusText=text("",11,MUTED);statusText.setTag("fx-runtime-status");statusText.setMaxLines(2);titles.addView(statusText);header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));TextView close=button("×","fx-close",this::dismiss);close.setTextSize(23);close.setContentDescription("Close Advanced");header.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));
  if(!video){
   TextView more=button("Video enhancement     ›","fx-upscale-open",()->{showScreen(true);scroll.scrollTo(0,0);});more.setGravity(Gravity.CENTER_VERTICAL);body.addView(more,new LinearLayout.LayoutParams(-1,-2));divider();body.addView(heading("Volume Boost",17));gap(body,8);slider("Gain","gain",100,300);
   TextView note=text("Above 100% uses Web Audio and may distort. Available only in Kuro's compatible direct player; it cannot amplify restricted embeds.",12,MUTED);note.setLineSpacing(dp(2),1f);body.addView(note);gap(body,16);updateStatus();return;
  }
  LinearLayout enable=row();enable.setGravity(Gravity.CENTER_VERTICAL);enable.addView(heading("Enable enhancement",15),new LinearLayout.LayoutParams(0,-2,1));Switch toggle=new Switch(activity);toggle.setTag("fx-enabled");toggle.setContentDescription("Enable video enhancement");toggle.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xffe9d9ff,0xffb9afbf}));toggle.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{PURPLE,0xff504959}));toggle.setChecked(values.optBoolean("enabled",false));toggle.setOnCheckedChangeListener((b,on)->set("enabled",on));enable.addView(toggle);body.addView(enable);gap(body,12);
  TextView desc=text("The presets use real-time filters. AI adds a small on-device CNN plus a vivid color and clarity finish. Frame access is required.",12,MUTED);desc.setLineSpacing(dp(2),1f);body.addView(desc);
  gap(body,10);TextView compare=button(comparing?"Show enhanced":"Show original", "fx-compare",()->{comparing=!comparing;notifyChanged();refreshVideoScreen();});compare.setContentDescription(comparing?"Show enhanced video":"Show original video for comparison");body.addView(compare,new LinearLayout.LayoutParams(-1,-2));
  TextView compareNote=text("Compare without changing saved settings. Fullscreen keeps supported overlays; exit fullscreen to change these controls. Cross-origin video may use color only; protected video may be unchanged.",12,MUTED);body.addView(compareNote);
  gap(body,14);LinearLayout aiRow=row();aiRow.setGravity(Gravity.CENTER_VERTICAL);aiRow.addView(heading("AI detail · experimental",15),new LinearLayout.LayoutParams(0,-2,1));Switch aiSwitch=new Switch(activity);aiSwitch.setTag("fx-ai");aiSwitch.setContentDescription("Enable experimental on-device AI detail");aiSwitch.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xffe9d9ff,0xffb9afbf}));aiSwitch.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{PURPLE,0xff504959}));aiSwitch.setChecked(values.optBoolean("ai",false));aiSwitch.setOnCheckedChangeListener((b,on)->{set("ai",on);if(on&&!toggle.isChecked())toggle.setChecked(true);});aiRow.addView(aiSwitch);body.addView(aiRow);gap(body,8);slider("AI strength","aiStrength",0,100);
  TextView aiNote=text("Uses a small open-source Anime4K CNN plus a vivid finish, not Topaz. It may use extra GPU/battery, reduce AI resolution or pause AI to protect playback. Restricted or protected video may not work.",12,MUTED);aiNote.setLineSpacing(dp(2),1f);body.addView(aiNote);
  divider();section("PRESET");String[] codes={"A","B","C","A+A","B+B","C+A"},hints={"Balanced","Soft restore","Denoise","Stronger restore","Double soft","Denoise + A"};
  for(int i=0;i<codes.length;i+=2){LinearLayout r=row();for(int j=i;j<i+2;j++){final String code=codes[j];TextView c=choice("Mode "+code,hints[j],values.optString("preset").equals(code),()->{set("preset",code);refreshVideoScreen();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(j>i)p.setMargins(dp(6),0,0,0);r.addView(c,p);}body.addView(r);gap(body,6);}
  divider();section("LINE EFFECTS");for(String[] spec:new String[][]{{"off","Off","Preserve source edges"},{"dark","Darken Lines","Make outlines bolder"},{"thin","Thin Lines","Reduce heavy edges"}}){final String key=spec[0];TextView c=choice(spec[1],spec[2],values.optString("line").equals(key),()->{set("line",key);refreshVideoScreen();});c.setGravity(Gravity.CENTER_VERTICAL);body.addView(c,new LinearLayout.LayoutParams(-1,-2));gap(body,6);}
  divider();section("PERFORMANCE: RENDER DETAIL");LinearLayout quality=row();int[] levels={8,16,32};for(int i=0;i<levels.length;i++){final int level=levels[i];String caption=level==8?"5 taps · 720p cap":level==16?"9 taps · 1080p cap":"13 taps · up to 4K";TextView c=choice(level+"×"+level,caption,values.optInt("detail")==level,()->{set("detail",level);refreshVideoScreen();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(i>0)p.setMargins(dp(5),0,0,0);quality.addView(c,p);}body.addView(quality);
  divider();section("VIDEO ENHANCEMENTS");slider("Sharpen strength","sharpen",0,200);slider("Denoise strength","denoise",0,200);slider("Line effect strength","lineStrength",0,200);slider("Edge preservation","edge",0,200);slider("Color saturation","saturation",0,200);slider("Contrast","contrast",0,200);slider("Brightness","brightness",0,150);slider("Debanding dither","deband",0,100);
  body.addView(button("Reset video tuning","fx-reset",()->{for(String key:new String[]{"sharpen","denoise","lineStrength","edge","saturation","contrast","brightness"})set(key,100);set("deband",0);set("aiStrength",100);refreshVideoScreen();}));gap(body,14);
  TextView note=text("Presets change sharpening, smoothing and color; line effects act on dark edges. Detail uses 5, 9 or 13 GPU samples per pixel, capped by the display and device. Dither is subtle. Tap the video to reveal its controls.",12,MUTED);note.setLineSpacing(dp(2),1f);body.addView(note);gap(body,14);updateStatus();
 }
 // Updating a preset or mode must not throw the user back to the top.
 private void refreshVideoScreen(){int y=scroll==null?0:scroll.getScrollY();showScreen(true);if(scroll!=null)scroll.post(()->scroll.scrollTo(0,y));}
 private void slider(String label,String key,int min,int max){
  LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(12),dp(12),dp(6));card.setBackground(shape(CARD,BORDER,10));LinearLayout line=row();line.setGravity(Gravity.CENTER_VERTICAL);TextView name=heading(label,13),value=heading(values.optInt(key,min)+"%",13);line.addView(name,new LinearLayout.LayoutParams(0,-2,1));line.addView(value);card.addView(line);gap(card,4);
  SeekBar seek=new SeekBar(activity);seek.setTag("fx-"+key);seek.setContentDescription(label);seek.setMax(max-min);seek.setProgress(values.optInt(key,min)-min);seek.setProgressTintList(ColorStateList.valueOf(PURPLE));seek.setThumbTintList(ColorStateList.valueOf(PURPLE));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){int v=n+min;value.setText(v+"%");if(user)set(key,v);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
  card.addView(seek,new LinearLayout.LayoutParams(-1,dp(48)));
  if("gain".equals(key)){LinearLayout ticks=row();for(int i=0;i<3;i++){TextView tick=text((100+i*100)+"%",11,MUTED);tick.setGravity(i==0?Gravity.START:i==1?Gravity.CENTER:Gravity.END);ticks.addView(tick,new LinearLayout.LayoutParams(0,-2,1));}card.addView(ticks);gap(card,6);}
  body.addView(card,new LinearLayout.LayoutParams(-1,-2));gap(body,8);
 }
 private void updateStatus(){if(statusText==null)return;String v=runtime.optString("video","off"),a=runtime.optString("audio","off");
  String video="active".equals(v)?"Video active":"color".equals(v)?"Color only":"compare".equals(v)?"Original shown":"waiting".equals(v)?"Video waiting":"unavailable".equals(v)?"Video unavailable":"paused".equals(v)?"Video paused":"Video off";
  String audio="active".equals(a)?"Boost active":"tap".equals(a)?"Tap video for boost":"waiting".equals(a)?"Boost waiting":"unavailable".equals(a)?"Boost unavailable":"Boost off";
  String ai=runtime.optString("ai","off"),why=runtime.optString("aiReason","");String aiLabel="active".equals(ai)?(runtime.optInt("aiTier",0)>0?"AI active (lighter)":"AI active"):"waiting".equals(ai)?"AI preparing":"paused".equals(ai)?"AI paused":"unavailable".equals(ai)?(why.contains("speed")?"AI paused: slow GPU":"AI unavailable"):"AI off";
  String reason=runtime.optString("reason","");String secondary=("unavailable".equals(v)||"color".equals(v))&&!reason.isEmpty()?reason:"unavailable".equals(ai)&&!why.isEmpty()?why:audio;
  statusText.setText(video+" · "+aiLabel+"\n"+secondary);statusText.setContentDescription(video+", "+aiLabel+(why.isEmpty()?"":", "+why)+(reason.isEmpty()?"":", "+reason)+", "+audio);}

 public void setRuntimeStatus(JSONObject data){if(data!=null){runtime=data;updateStatus();}}
 public boolean isShowing(){return dialog!=null&&dialog.isShowing();}
 public void dismiss(){boolean changed=comparing||pending!=null;comparing=false;if(pending!=null){main.removeCallbacks(pending);pending=null;}if(changed)notifyChanged();if(dialog!=null)dialog.dismiss();}
 // The dialog window covers only its panel, leaving the live video visible and
 // touchable outside the panel. Rotation recomputes these bounds without reload.
 public void relayout(){
  if(dialog==null||dialog.getWindow()==null)return;android.util.DisplayMetrics m=new android.util.DisplayMetrics();activity.getWindowManager().getDefaultDisplay().getMetrics(m);
  WindowManager.LayoutParams p=dialog.getWindow().getAttributes();boolean landscape=m.widthPixels>m.heightPixels;
  if(landscape){p.width=Math.min(dp(380),Math.round(m.widthPixels*.46f));p.height=WindowManager.LayoutParams.MATCH_PARENT;p.gravity=Gravity.END|Gravity.TOP;}
  else{p.width=WindowManager.LayoutParams.MATCH_PARENT;p.height=Math.max(dp(220),Math.round(m.heightPixels*.48f));p.height=Math.min(p.height,Math.max(1,m.heightPixels-dp(140)));p.gravity=Gravity.BOTTOM;}
  p.dimAmount=0f;dialog.getWindow().setAttributes(p);
 }
 public void show(){
  dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setCanceledOnTouchOutside(false);LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(shape(0xf2100a16,BORDER,20));root.setPadding(dp(14),dp(10),dp(14),dp(10));header=row();header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header,new LinearLayout.LayoutParams(-1,dp(58)));View line=new View(activity);line.setBackgroundColor(0xff49394f);root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
  scroll=new ScrollView(activity);scroll.setFillViewport(false);body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(12),0,dp(20));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(root);showScreen(false);dialog.setOnDismissListener(d->{boolean changed=comparing||pending!=null;comparing=false;if(pending!=null){main.removeCallbacks(pending);pending=null;}if(changed)notifyChanged();});dialog.show();
  Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);relayout();}
  root.setAlpha(0f);root.setTranslationY(dp(18));root.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();notifyChanged();
 }
}
