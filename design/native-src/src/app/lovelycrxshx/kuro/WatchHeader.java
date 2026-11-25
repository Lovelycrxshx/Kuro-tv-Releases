package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.InputStream;

/** The watch-page equivalent of the app's compact Kuro.tv header. */
public final class WatchHeader extends LinearLayout {
 public interface Listener {void home();void search();void notifications();void episodes();void login();}
 private static final int BACKGROUND=0xff100c17,CHIP=0xff251730,BORDER=0xff4c3558,ICON=0xffe3d7ee;
 private final Listener listener; private FrameLayout accountTarget; private TextView accountWord; private TextView accountMono; private ImageView accountArt;

 public WatchHeader(Context context,Listener listener){
  super(context);this.listener=listener;setTag("watch-header");setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setBackgroundColor(BACKGROUND);setPadding(dp(15),0,dp(15),0);setMinimumHeight(dp(62));
  FrameLayout home=target("watch-home","Kuro.tv, go to Home",()->{if(this.listener!=null)this.listener.home();});
  ImageView logo=new ImageView(context);logo.setTag("watch-home-logo");logo.setScaleType(ImageView.ScaleType.FIT_START);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  Bitmap bitmap=null;try(InputStream in=context.getAssets().open("kuro-logo.png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;bitmap=BitmapFactory.decodeStream(in,null,options);}catch(Exception ignored){}
  if(bitmap!=null)logo.setImageBitmap(bitmap);
  home.addView(logo,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,dp(48),Gravity.CENTER_VERTICAL));
  if(bitmap==null){TextView fallback=new TextView(context);fallback.setText("kuro.tv");fallback.setTextSize(18);fallback.setTypeface(Typeface.DEFAULT,Typeface.BOLD_ITALIC);fallback.setTextColor(ICON);fallback.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);home.addView(fallback,new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT,Gravity.CENTER_VERTICAL|Gravity.START));}
  addView(home,new LinearLayout.LayoutParams(0,dp(48),1));
  addView(icon("watch-search","Search",0,()->{if(this.listener!=null)this.listener.search();}),new LinearLayout.LayoutParams(dp(48),dp(48)));
  addView(icon("watch-episodes","Episodes",2,()->{if(this.listener!=null)this.listener.episodes();}),new LinearLayout.LayoutParams(dp(48),dp(48)));
  addView(icon("watch-notifications","Notifications",1,()->{if(this.listener!=null)this.listener.notifications();}),new LinearLayout.LayoutParams(dp(48),dp(48)));
  FrameLayout login=target("watch-login","Log in",()->{if(this.listener!=null)this.listener.login();});
  TextView word=new TextView(context);word.setText("Log in");word.setGravity(Gravity.CENTER);word.setTextColor(ICON);word.setTextSize(TypedValue.COMPLEX_UNIT_SP,11);word.setTypeface(Typeface.DEFAULT,Typeface.BOLD);word.setMaxLines(1);word.setAutoSizeTextTypeUniformWithConfiguration(8,11,1,TypedValue.COMPLEX_UNIT_SP);word.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);word.setBackground(shape(CHIP,BORDER,17));
  login.addView(word,new FrameLayout.LayoutParams(dp(58),dp(34),Gravity.CENTER));accountTarget=login;accountWord=word;accountMono=new TextView(context);accountMono.setTag("watch-mono571");accountMono.setTextColor(ICON);accountMono.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);accountMono.setTypeface(Typeface.DEFAULT,Typeface.BOLD);accountMono.setGravity(Gravity.CENTER);accountMono.setMaxLines(1);accountMono.setBackground(shape(CHIP,BORDER,30));accountMono.setVisibility(View.GONE);accountMono.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);login.addView(accountMono,new FrameLayout.LayoutParams(dp(38),dp(38),Gravity.CENTER));
  accountArt=new ImageView(context);accountArt.setTag("watch-art581");accountArt.setScaleType(ImageView.ScaleType.CENTER_CROP);accountArt.setClipToOutline(true);accountArt.setOutlineProvider(ViewOutlineProvider.BACKGROUND);accountArt.setBackground(shape(CHIP,BORDER,30));accountArt.setVisibility(View.GONE);accountArt.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);login.addView(accountArt,new FrameLayout.LayoutParams(dp(38),dp(38),Gravity.CENTER));
  addView(login,new LinearLayout.LayoutParams(dp(60),dp(48)));
 }
 public void setProfile(boolean signedIn,String mono){setProfile(signedIn,mono,null);}
 // 5.82: the signed-in chip shows the same profile art the Web UI paints, and
 // falls back to initials when the user chose no avatar.
 public void setProfile(boolean signedIn,String mono,String artId){
  if(accountTarget==null)return;accountTarget.setContentDescription(signedIn?"Open profile menu":"Log in");accountWord.setVisibility(signedIn?View.GONE:View.VISIBLE);
  boolean art=signedIn&&showArt(artId);
  accountMono.setVisibility(signedIn&&!art?View.VISIBLE:View.GONE);
  accountMono.setText(mono==null||mono.trim().isEmpty()?"K":mono.trim().toUpperCase(java.util.Locale.ROOT));
 }
 private boolean showArt(String artId){
  if(accountArt==null)return false;
  if(artId==null||!artId.matches("art-[0-9]{2}")){accountArt.setImageDrawable(null);accountArt.setVisibility(View.GONE);return false;}
  try(java.io.InputStream in=getContext().getAssets().open("profile-art/pfp/"+artId+".webp")){
   Bitmap b=BitmapFactory.decodeStream(in);
   if(b!=null){accountArt.setImageBitmap(b);accountArt.setVisibility(View.VISIBLE);return true;}
  }catch(Exception ignored){}
  accountArt.setImageDrawable(null);accountArt.setVisibility(View.GONE);return false;
 }
 private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
 private GradientDrawable shape(int color,int border,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
 private FrameLayout target(String tag,String description,Runnable click){
  FrameLayout frame=new FrameLayout(getContext());frame.setTag(tag);frame.setContentDescription(description);frame.setFocusable(true);frame.setClickable(true);frame.setOnClickListener(v->click.run());
  frame.setForeground(new RippleDrawable(ColorStateList.valueOf(0x447f55a0),null,null));return frame;
 }
 private FrameLayout icon(String tag,String description,int which,Runnable click){
  FrameLayout frame=target(tag,description,click);Icon glyph=new Icon(getContext(),which);glyph.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);glyph.setBackground(shape(CHIP,BORDER,17));frame.addView(glyph,new FrameLayout.LayoutParams(dp(34),dp(34),Gravity.CENTER));return frame;
 }
 private static final class Icon extends View {
  final int which;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  Icon(Context context,int which){super(context);this.which=which;paint.setColor(ICON);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float size=18f*getResources().getDisplayMetrics().density,scale=size/24f;c.save();c.translate((getWidth()-size)/2f,(getHeight()-size)/2f);c.scale(scale,scale);
   if(which==0){c.drawCircle(10.5f,10.5f,6.5f,paint);c.drawLine(15.5f,15.5f,20.5f,20.5f,paint);}
   else if(which==2){c.drawLine(4,6,20,6,paint);c.drawLine(4,12,20,12,paint);c.drawLine(4,18,14,18,paint);}
   else{Path bell=new Path();bell.moveTo(18,8);bell.cubicTo(18,4.7f,15.3f,2,12,2);bell.cubicTo(8.7f,2,6,4.7f,6,8);bell.cubicTo(6,15,4,15,4,17);bell.lineTo(20,17);bell.cubicTo(20,15,18,15,18,8);c.drawPath(bell,paint);c.drawLine(10,20,14,20,paint);}
   c.restore();
  }
 }
}
