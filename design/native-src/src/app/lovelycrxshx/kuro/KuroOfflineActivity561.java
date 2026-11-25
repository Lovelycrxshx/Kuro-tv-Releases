package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import org.json.JSONObject;

/** Playback of completed downloads is strictly cache-only; it cannot silently use network. */
@OptIn(markerClass = UnstableApi.class)
public final class KuroOfflineActivity561 extends Activity {
    private ExoPlayer engine;
    private PlayerView view;
    private LinearLayout top;
    private String key="";
    private boolean pipRequest=false,resumeAfterPause=false,ended=false,resumed=false;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable saveTick=new Runnable(){@Override public void run(){if(engine==null)return;if(engine.isPlaying())save();main.postDelayed(this,5000);}};
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private TextView button(String value){TextView t=new TextView(this);t.setText(value);t.setTextSize(14);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setPadding(dp(14),dp(10),dp(14),dp(10));t.setMinHeight(dp(48));return t;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);
        final String id=getIntent().getStringExtra("id");
        if(!KuroPlayback561.validKey(id)){unavailable();return;}
        final androidx.media3.exoplayer.offline.DownloadIndex index;
        try{index=KuroOffline561.manager(this).getDownloadIndex();}catch(RuntimeException e){unavailable();return;}
        TextView loading=button("Opening offline download…");loading.setBackgroundColor(Color.BLACK);setContentView(loading);
        java.util.concurrent.ExecutorService io=java.util.concurrent.Executors.newSingleThreadExecutor();
        io.execute(()->{Download item=null;try{item=index.getDownload(id);}catch(Exception ignored){}final Download result=item;
            runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())openOffline(result);});
        });io.shutdown();
    }
    private void unavailable(){Toast.makeText(this,"This offline download is not available.",Toast.LENGTH_LONG).show();finish();}
    private void openOffline(Download download){
        try{
            if(download==null||download.state!=Download.STATE_COMPLETED||!KuroPlayback561.allowedHls(download.request.uri.toString(),"zenkai","hls"))throw new IllegalArgumentException();
            JSONObject meta=KuroOffline561.metadata(download);key=meta.optString("key","");
            LinearLayout layout=new LinearLayout(this);layout.setBackgroundColor(Color.BLACK);layout.setOrientation(LinearLayout.VERTICAL);setContentView(layout);
            top=new LinearLayout(this);top.setBackgroundColor(0xff1b1029);top.setGravity(Gravity.CENTER_VERTICAL);
            TextView back=button("‹  Back");back.setContentDescription("Close offline player");back.setOnClickListener(v->finish());top.addView(back);
            TextView title=button(meta.optString("title","Offline video"));title.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            TextView pip=button("▣");pip.setContentDescription("Picture in picture");pip.setOnClickListener(v->enterPip());top.addView(pip);layout.addView(top,new LinearLayout.LayoutParams(-1,-2));
            view=new PlayerView(this);view.setUseController(true);view.setShowSubtitleButton(true);view.setShowNextButton(false);view.setShowPreviousButton(false);layout.addView(view,new LinearLayout.LayoutParams(-1,0,1));
            androidx.media3.datasource.DataSource.Factory cachedOnly=KuroOffline561.offlineFactory(this);
            DefaultMediaSourceFactory factory=new DefaultMediaSourceFactory(this).setDataSourceFactory(cachedOnly);
            engine=new ExoPlayer.Builder(this).setMediaSourceFactory(factory).setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(),true).setHandleAudioBecomingNoisy(true).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).build();view.setPlayer(engine);
            engine.addListener(new Player.Listener(){@Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED){ended=true;KuroPlayback561.completed(KuroOfflineActivity561.this,key);}else if(ended&&(state==Player.STATE_BUFFERING||state==Player.STATE_READY)&&engine.getDuration()>0&&engine.getCurrentPosition()<engine.getDuration()-1000)ended=false;}@Override public void onPlayerError(androidx.media3.common.PlaybackException e){Toast.makeText(KuroOfflineActivity561.this,"This offline copy is incomplete or no longer playable.",Toast.LENGTH_LONG).show();}});
            engine.setMediaItem(download.request.toMediaItem());long saved=KuroPlayback561.read(this,key);if(saved>0)engine.seekTo(saved);engine.prepare();engine.setPlayWhenReady(resumed);if(!resumed)resumeAfterPause=true;main.postDelayed(saveTick,5000);
        }catch(Exception e){unavailable();}
    }
    private void save(){if(engine!=null&&!ended)KuroPlayback561.save(this,key,engine.getCurrentPosition(),engine.getDuration());}
    private void enterPip(){
        if(engine==null||!getPackageManager().hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)){Toast.makeText(this,"Picture-in-picture is unavailable.",Toast.LENGTH_SHORT).show();return;}
        try{pipRequest=true;if(!enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9)).build()))pipRequest=false;}catch(RuntimeException e){pipRequest=false;Toast.makeText(this,"Picture-in-picture is unavailable.",Toast.LENGTH_SHORT).show();}
    }
    @Override public void onPictureInPictureModeChanged(boolean inPip,android.content.res.Configuration config){super.onPictureInPictureModeChanged(inPip,config);pipRequest=false;if(top!=null)top.setVisibility(inPip?View.GONE:View.VISIBLE);if(!inPip&&!resumed&&engine!=null){resumeAfterPause=engine.getPlayWhenReady();save();engine.pause();}}
    @Override protected void onResume(){super.onResume();resumed=true;if(engine!=null&&resumeAfterPause){resumeAfterPause=false;engine.play();}}
    @Override protected void onPause(){resumed=false;save();if(engine!=null&&!pipRequest&&!isInPictureInPictureMode()){resumeAfterPause=engine.getPlayWhenReady();engine.pause();}super.onPause();}
    @Override protected void onDestroy(){main.removeCallbacks(saveTick);save();if(view!=null)view.setPlayer(null);if(engine!=null){engine.release();engine=null;}super.onDestroy();}
}
