package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import org.json.JSONObject;
import java.util.Collections;

/** Real native playback for the already-approved direct HLS proxy only. */
@OptIn(markerClass = UnstableApi.class)
final class KuroNativePlayer561 {
    interface Listener { void failure(boolean verification); void ended(); }
    private final Activity activity;
    private final FrameLayout parent;
    private final String address,key;
    private final Listener listener;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final FrameLayout surface;
    private final PlayerView view;
    private final TextView skip;
    private final TextView hud581;
    private final ExoPlayer engine;
    private KuroPlayback561.Range intro,outro,activeRange,sourceIntro,sourceOutro;
    // Suggested windows stand in when the source ships no markers; they are never auto-skipped.
    private boolean introSuggested=false,outroSuggested=false,activeSuggested=false;
    private long suggestedFor=-1;
    private String activeLabel="";
    private boolean released=false,failed=false,ended=false,resumeAfterPause=false;
    private long lastSaved=0,lastAutomatic=0;
    private final Runnable hideHud581=new Runnable(){@Override public void run(){if(!released&&hud581!=null)hud581.animate().alpha(0f).setDuration(220).start();}};
    private volatile Uri subtitle;
    private String subtitleMime;

    KuroNativePlayer561(Activity activity,FrameLayout parent,String address,String key,JSONObject option,Listener listener) {this(activity,parent,address,key,option,listener,null);}
    // The optional factory is used by no-network synthetic tests; production passes null.
    KuroNativePlayer561(Activity activity,FrameLayout parent,String address,String key,JSONObject option,Listener listener,androidx.media3.datasource.DataSource.Factory fixture) {
        if(!KuroPlayback561.allowedHls(address,"zenkai","hls"))throw new IllegalArgumentException("Unsupported stream");
        this.activity=activity;this.parent=parent;this.address=address;this.key=key;this.listener=listener;
        JSONObject marks=option==null?null:option.optJSONObject("skip");
        sourceIntro=marks==null?null:KuroPlayback561.fromJson(marks.optJSONObject("intro"));intro=KuroPlayback561.manualRange(activity,key);
        if(intro==null)intro=sourceIntro;
        sourceOutro=marks==null?null:KuroPlayback561.fromJson(marks.optJSONObject("outro"));outro=KuroPlayback561.manualOutro(activity,key);if(outro==null)outro=sourceOutro;
        androidx.media3.datasource.okhttp.OkHttpDataSource.Factory http=KuroHttp561.factory();
        DefaultDataSource.Factory data=new DefaultDataSource.Factory(activity,http);
        androidx.media3.datasource.DataSource.Factory restricted=new androidx.media3.datasource.ResolvingDataSource.Factory(data,spec->{if(!KuroHttp561.allowedMedia(spec.uri,subtitle))throw new java.io.IOException("Unsupported media resource");return spec;});
        DefaultMediaSourceFactory media=new DefaultMediaSourceFactory(activity).setDataSourceFactory(fixture==null?restricted:fixture).setLoadErrorHandlingPolicy(new KuroLoadPolicy561());
        engine=new ExoPlayer.Builder(activity).setMediaSourceFactory(media).setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true).setHandleAudioBecomingNoisy(true).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build();
        FrameLayout madeSurface;PlayerView madeView;TextView madeSkip,hud;
        try{
            madeSurface=new FrameLayout(activity);madeSurface.setBackgroundColor(Color.BLACK);madeSurface.setTag("native-player-561");
            madeView=new PlayerView(activity);madeView.setPlayer(engine);madeView.setUseController(true);madeView.setShowSubtitleButton(true);madeView.setShowNextButton(false);madeView.setShowPreviousButton(false);madeView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);madeView.setResizeMode(resizeMode572(activity.getPreferences(Context.MODE_PRIVATE).getString(KuroVideoFit572.PREF,KuroVideoFit572.FIT)));KuroCaptionStyle571.apply(activity,madeView.getSubtitleView());
            madeSurface.addView(madeView,new FrameLayout.LayoutParams(-1,-1));
            madeSkip=new TextView(activity);madeSkip.setTextColor(Color.WHITE);madeSkip.setTextSize(14);madeSkip.setPadding(dp(16),dp(10),dp(16),dp(10));madeSkip.setMinHeight(dp(48));madeSkip.setGravity(Gravity.CENTER);madeSkip.setBackgroundColor(0xdd573278);madeSkip.setVisibility(View.GONE);
            FrameLayout.LayoutParams slp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);slp.setMargins(dp(12),0,dp(12),dp(55));madeSurface.addView(madeSkip,slp);
            // 5.81: a fading heads-up readout for the gesture layer.
            hud=new TextView(activity);hud.setTextColor(Color.WHITE);hud.setTextSize(15);hud.setGravity(Gravity.CENTER);hud.setPadding(dp(16),dp(9),dp(16),dp(9));hud.setBackground(gradientHud581());
            hud.setAlpha(0f);hud.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);hud.setTag("native-hud-581");
            FrameLayout.LayoutParams hlp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);hlp.setMargins(0,dp(18),0,0);madeSurface.addView(hud,hlp);
        }catch(RuntimeException|LinkageError e){engine.release();throw e;}
        surface=madeSurface;view=madeView;skip=madeSkip;hud581=hud;skip.setOnClickListener(v->{if(activeRange!=null&&!released)engine.seekTo(activeRange.endMs);});
        attachGestures581();
        try{
        parent.addView(surface,Math.min(1,parent.getChildCount()),new FrameLayout.LayoutParams(-1,-1));
        engine.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException error){if(released||failed)return;failed=true;listener.failure(KuroLoadPolicy561.blocked(error));}
            @Override public void onPlaybackStateChanged(int state){
                if(state==Player.STATE_ENDED&&!released&&!ended){ended=true;KuroPlayback561.completed(activity,key);listener.ended();}
                else if(ended&&(state==Player.STATE_BUFFERING||state==Player.STATE_READY)&&engine.getDuration()>0&&engine.getCurrentPosition()<engine.getDuration()-1000)ended=false;
            }
            @Override public void onIsPlayingChanged(boolean playing){if(!playing&&!ended)save();}
        });
        setItem(KuroPlayback561.read(activity,key),activity.getPreferences(Context.MODE_PRIVATE).getBoolean("autoplay",true));
        handler.post(tick);
        }catch(RuntimeException|LinkageError e){parent.removeView(surface);view.setPlayer(null);engine.release();throw e;}
    }
    // 5.81: double-tap to seek, vertical drag for brightness (left half) and volume
    // (right half), each with a fading readout. A plain tap still reaches PlayerView's
    // own controls, so this layer only adds gestures.
    private void attachGestures581(){
        final GestureDetector detector=new GestureDetector(activity,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDoubleTap(MotionEvent e){
                if(released)return false;
                int width=Math.max(1,view.getWidth());
                long delta=e.getX()<width*0.35f?-10000L:e.getX()>width*0.65f?10000L:0L;
                if(delta==0){boolean playing=engine.isPlaying();if(playing)engine.pause();else engine.play();showHud581(playing?"Paused":"Playing");return true;}
                long duration=engine.getDuration();long position=engine.getCurrentPosition();
                long target=Math.max(0,Math.min(duration>0?duration:Long.MAX_VALUE,position+delta));
                engine.seekTo(target);
                showHud581(delta>0?"+10 seconds":"\u221210 seconds");
                return true;
            }
        });
        final float[] start={0f,0f};
        final float[] origin={0f,0f};
        final boolean[] dragging={false};
        view.setOnTouchListener((v,event)->{
            detector.onTouchEvent(event);
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    start[0]=event.getX();start[1]=event.getY();dragging[0]=false;break;
                case MotionEvent.ACTION_MOVE:{
                    float dx=event.getX()-start[0],dy=event.getY()-start[1];
                    if(!dragging[0]&&Math.abs(dy)>dp(22)&&Math.abs(dy)>Math.abs(dx)*1.4f){
                        dragging[0]=true;origin[0]=event.getY();
                        origin[1]=event.getX()<view.getWidth()/2f?brightness581():engine.getVolume();
                    }
                    if(dragging[0]){
                        float span=Math.max(1f,view.getHeight()*0.7f);
                        float value=Math.max(0f,Math.min(1f,origin[1]-(event.getY()-origin[0])/span));
                        if(event.getX()<view.getWidth()/2f){setBrightness581(value);showHud581("Brightness "+Math.round(value*100)+"%");}
                        else{engine.setVolume(value);showHud581("Volume "+Math.round(value*100)+"%");}
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:dragging[0]=false;break;
            }
            return false;
        });
    }
    private float brightness581(){
        WindowManager.LayoutParams params=activity.getWindow().getAttributes();
        return params.screenBrightness<0f?0.5f:Math.max(0.02f,params.screenBrightness);
    }
    private void setBrightness581(float value){
        WindowManager.LayoutParams params=activity.getWindow().getAttributes();
        params.screenBrightness=Math.max(0.02f,Math.min(1f,value));
        activity.getWindow().setAttributes(params);
    }
    private void showHud581(String text){
        if(released||hud581==null)return;
        hud581.setText(text);hud581.setAlpha(1f);
        handler.removeCallbacks(hideHud581);handler.postDelayed(hideHud581,900);
    }
    private GradientDrawable gradientHud581(){
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(0xd2100c18);drawable.setCornerRadius(dp(14));drawable.setStroke(dp(1),0x66665078);
        return drawable;
    }
    private int dp(int n){return (int)(n*activity.getResources().getDisplayMetrics().density+.5f);}
    private void setItem(long position,boolean play){
        MediaItem.Builder b=new MediaItem.Builder().setUri(address).setMimeType(MimeTypes.APPLICATION_M3U8);
        if(subtitle!=null){MediaItem.SubtitleConfiguration c=new MediaItem.SubtitleConfiguration.Builder(subtitle).setMimeType(subtitleMime).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).setLabel("Selected subtitle").build();b.setSubtitleConfigurations(Collections.singletonList(c));}
        engine.setMediaItem(b.build());if(position>0)engine.seekTo(position);engine.prepare();engine.setPlayWhenReady(play);
        suggestedFor=-1;introSuggested=false;outroSuggested=false;
    }
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(released)return;
        long pos=engine.getCurrentPosition();
        long duration=engine.getDuration();
        // The HLS route carries no chapter data, so an episode with no markers would
        // never offer Skip at all. Fill in a conservative window once the duration is
        // known, and say so instead of pretending the source told us.
        if(duration>0&&duration!=suggestedFor){
            suggestedFor=duration;
            if(intro==null||introSuggested){KuroPlayback561.Range e=KuroSkipSuggestions581.intro(duration);intro=e;introSuggested=e!=null;}
            if(outro==null||outroSuggested){KuroPlayback561.Range e=KuroSkipSuggestions581.outro(duration);outro=e;outroSuggested=e!=null;}
        }
        KuroPlayback561.Range r=null;String label="";
        if(intro!=null&&intro.contains(pos)){r=intro;label="Intro";activeSuggested=introSuggested;}else if(outro!=null&&outro.contains(pos)){r=outro;label="Outro";activeSuggested=outroSuggested;}
        if(r==null)activeSuggested=false;
        activeRange=r;activeLabel=label;
        if(r==null)skip.setVisibility(View.GONE);else{
            skip.setText("Skip "+label);
            skip.setContentDescription(activeSuggested?"Skip "+label.toLowerCase()+" (suggested window)":"Skip "+label.toLowerCase());
            skip.setVisibility(View.VISIBLE);
            // A guess may be offered but never applied on its own.
            boolean auto=!activeSuggested&&activity.getPreferences(Context.MODE_PRIVATE).getBoolean("auto_skip",false);
            long now=android.os.SystemClock.elapsedRealtime();if(auto&&engine.isPlaying()&&now-lastAutomatic>1500){lastAutomatic=now;engine.seekTo(r.endMs);}
        }
        long now=android.os.SystemClock.elapsedRealtime();if(engine.isPlaying()&&now-lastSaved>=5000){save();lastSaved=now;}
        handler.postDelayed(this,1000);
    }};
    void save(){if(!released&&!ended)KuroPlayback561.save(activity,key,engine.getCurrentPosition(),engine.getDuration());}
    private static int resizeMode572(String mode){return KuroVideoFit572.CROP.equals(KuroVideoFit572.normalize(mode))?AspectRatioFrameLayout.RESIZE_MODE_ZOOM:AspectRatioFrameLayout.RESIZE_MODE_FIT;}
    /** Fit or crop inside Kuro's own surface; window, cutout and fullscreen are untouched. */
    String fit572(){return KuroVideoFit572.normalize(activity.getPreferences(Context.MODE_PRIVATE).getString(KuroVideoFit572.PREF,KuroVideoFit572.FIT));}
    String cycleFit572(){String next=KuroVideoFit572.next(fit572());setFit572(next);return next;}
    void setFit572(String mode){
        String value=KuroVideoFit572.normalize(mode);
        activity.getPreferences(Context.MODE_PRIVATE).edit().putString(KuroVideoFit572.PREF,value).apply();
        if(!released)view.setResizeMode(resizeMode572(value));
    }
    /**
     * Restart only when this engine reports a real position and duration. Returns false
     * so the caller can keep the old behaviour instead of pretending.
     */
    boolean restart572(){
        if(released||ended)return false;
        try{
            long position=engine.getCurrentPosition(),duration=engine.getDuration();
            if(!KuroResume572.restartable(position,duration))return false;
            engine.seekTo(0);
            KuroPlayback561.completed(activity,key);
            return true;
        }catch(RuntimeException e){return false;}
    }
    long position(){return released?0:engine.getCurrentPosition();}

    /**
     * Kuro 5.82: never seeks into the last
     * second of the item (that reads as "ended" and would trigger autoplay-next).
     */
    boolean seekTo582(long ms){
        if(released||ms<=0)return false;
        try{
            long duration=engine.getDuration();
            long target=duration>0?Math.min(ms,Math.max(0,duration-1500)):ms;
            engine.seekTo(target);
            return true;
        }catch(RuntimeException e){return false;}
    }
    long duration(){return released?0:engine.getDuration();}
    double speed565(){return released?1.0:engine.getPlaybackParameters().speed;}
    String presenceState563(){
        if(released)return "selected";
        if(engine.getPlaybackState()==Player.STATE_ENDED)return "ended";
        if(engine.isPlaying())return "playing";
        if(engine.getPlaybackState()==Player.STATE_BUFFERING&&engine.getPlayWhenReady())return "buffering";
        if(!engine.getPlayWhenReady()&&engine.getPlaybackState()==Player.STATE_READY)return "paused";
        return "loading";
    }
    void command564(boolean play){if(released)return;resumeAfterPause=false;if(play)engine.play();else{save();engine.pause();}}
    void cancelAutoResume(){resumeAfterPause=false;if(!released)engine.pause();}
    String address(){return address;}
    void setAutoplay(boolean on){if(!released&&on)engine.play();}
    void pauseForBackground(){if(released)return;save();resumeAfterPause=engine.getPlayWhenReady();engine.pause();}
    void resumeForeground(){if(released)return;if(resumeAfterPause){resumeAfterPause=false;engine.play();}}
    void release(){if(released)return;save();released=true;handler.removeCallbacks(tick);view.setPlayer(null);engine.release();parent.removeView(surface);}
    void useSubtitle(Uri uri,String mime){
        if(released||uri==null||!"content".equals(uri.getScheme())||!(MimeTypes.TEXT_VTT.equals(mime)||MimeTypes.APPLICATION_SUBRIP.equals(mime)))return;
        long position=engine.getCurrentPosition();boolean play=engine.getPlayWhenReady();subtitle=uri;subtitleMime=mime;ended=false;setItem(position,play);
        Toast.makeText(activity,"Subtitle added. Use CC to select a track.",Toast.LENGTH_LONG).show();
    }
    void subtitleStyleDialog(){if(!released)KuroCaptionStyle571.show(activity,view.getSubtitleView());}
    void markersDialog(){
        if(!KuroPlayback561.validKey(key)){Toast.makeText(activity,"This episode does not have a stable identity for saving markers.",Toast.LENGTH_LONG).show();return;}
        new AlertDialog.Builder(activity).setTitle("Edit skip markers")
            .setItems(new String[]{"Intro", "Outro"},(d,which)->editMarker573(which==1))
            .setNegativeButton("Cancel",null).show();
    }
    private void editMarker573(boolean isOutro){
        if(released)return;String kind=isOutro?"Outro":"Intro";
        LinearLayout layout=new LinearLayout(activity);layout.setPadding(dp(20),dp(4),dp(20),0);layout.setOrientation(LinearLayout.VERTICAL);
        TextView hint=new TextView(activity);hint.setText("Set this episode's "+kind.toLowerCase()+" start and end in seconds. A saved marker overrides any source marker. Clear saved to return to the source marker, if available.");layout.addView(hint);
        EditText start=new EditText(activity),end=new EditText(activity);start.setSingleLine(true);end.setSingleLine(true);start.setHint(kind+" start, seconds");end.setHint(kind+" end, seconds");start.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);end.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        KuroPlayback561.Range old=isOutro?KuroPlayback561.manualOutro(activity,key):KuroPlayback561.manualRange(activity,key);if(old!=null){start.setText(String.valueOf(old.startMs/1000.0));end.setText(String.valueOf(old.endMs/1000.0));}layout.addView(start);layout.addView(end);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(kind+" marker").setView(layout).setNegativeButton("Cancel",null).setNeutralButton("Clear saved",(d,w)->{if(isOutro){KuroPlayback561.setManualOutro(activity,key,null);outro=sourceOutro;}else{KuroPlayback561.setManualRange(activity,key,null);intro=sourceIntro;}Toast.makeText(activity,"Saved "+kind.toLowerCase()+" marker removed.",Toast.LENGTH_SHORT).show();}).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{double a=Double.parseDouble(start.getText().toString()),b=Double.parseDouble(end.getText().toString());KuroPlayback561.Range r=KuroPlayback561.range(a,b);long duration=engine.getDuration();if(r==null||r.endMs-r.startMs>30*60*1000||(duration>0&&r.endMs>duration))throw new IllegalArgumentException();if(isOutro){KuroPlayback561.setManualOutro(activity,key,r);outro=r;}else{KuroPlayback561.setManualRange(activity,key,r);intro=r;}dialog.dismiss();Toast.makeText(activity,kind+" marker saved for this episode.",Toast.LENGTH_SHORT).show();}catch(Exception e){end.setError("Enter an end after the start, within the episode and 30 minutes.");}}));dialog.show();
    }

}
