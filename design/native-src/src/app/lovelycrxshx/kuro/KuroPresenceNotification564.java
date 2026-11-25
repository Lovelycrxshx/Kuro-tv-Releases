package app.lovelycrxshx.kuro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import java.lang.ref.WeakReference;

/** Optional media notification for the active Kuro activity; not a background playback service. */
public final class KuroPresenceNotification564 {
 interface Listener{void command(String action);}
 private static final String CHANNEL="kuro-presence-playback564",PREFIX="app.lovelycrxshx.kuro.RPC_MEDIA_";
 private static final int ID=56401;
 private static WeakReference<KuroPresenceNotification564> current=new WeakReference<>(null);
 private final Context context;private final NotificationManager manager;private final MediaSession session;private final Listener listener;
 KuroPresenceNotification564(Context c,Listener l){
  context=c.getApplicationContext();listener=l;manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Kuro playback and presence",NotificationManager.IMPORTANCE_LOW));
  session=new MediaSession(context,"Kuro playback");session.setCallback(new MediaSession.Callback(){
   @Override public void onPlay(){dispatch("play");}@Override public void onPause(){dispatch("pause");}@Override public void onSkipToNext(){dispatch("next");}@Override public void onStop(){dispatch("stop");}
  });session.setActive(true);synchronized(KuroPresenceNotification564.class){current=new WeakReference<>(this);}
 }
 private void dispatch(String action){listener.command(action);}
 private PendingIntent action(String a,int request){Intent i=new Intent(context,Receiver.class).setAction(PREFIX+a);return PendingIntent.getBroadcast(context,request,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 void update(KuroPresence563.Snapshot s,String status,long pos,long duration,boolean control,boolean next){
  if(manager==null)return;long actions=PlaybackState.ACTION_STOP;
  if(control)actions|=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE;
  if(next)actions|=PlaybackState.ACTION_SKIP_TO_NEXT;
  int state="playing".equals(status)?PlaybackState.STATE_PLAYING:"paused".equals(status)?PlaybackState.STATE_PAUSED:"buffering".equals(status)?PlaybackState.STATE_BUFFERING:PlaybackState.STATE_NONE;
  PlaybackState.Builder pb=new PlaybackState.Builder().setActions(actions).setState(state,Math.max(0,pos),state==PlaybackState.STATE_PLAYING?1f:0f,SystemClock.elapsedRealtime());session.setPlaybackState(pb.build());
  MediaMetadata.Builder mb=new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,s.details).putString(MediaMetadata.METADATA_KEY_ARTIST,s.state);
  if(duration>0&&duration<=KuroPresence563.MAX_MEDIA_MS)mb.putLong(MediaMetadata.METADATA_KEY_DURATION,duration);session.setMetadata(mb.build());
  Intent launch=context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
  PendingIntent open=launch==null?null:PendingIntent.getActivity(context,56410,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=new Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(s.details).setContentText(s.state).setOnlyAlertOnce(true).setOngoing(false).setVisibility(Notification.VISIBILITY_PRIVATE);
  if(open!=null)b.setContentIntent(open);int compact=0;
  if(control){boolean playing="playing".equals(status);b.addAction(new Notification.Action.Builder(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing?"Pause":"Play",action(playing?"pause":"play",56411)).build());compact++;}
  if(next){b.addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next,"Next",action("next",56412)).build());compact++;}
  b.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,"Stop presence",action("stop",56413)).build());
  Notification.MediaStyle style=new Notification.MediaStyle().setMediaSession(session.getSessionToken());if(compact==1)style.setShowActionsInCompactView(0);else if(compact>=2)style.setShowActionsInCompactView(0,1);b.setStyle(style);
  try{manager.notify(ID,b.build());}catch(SecurityException ignored){}
 }
 void cancel(){if(manager!=null)manager.cancel(ID);}
 void close(){cancel();session.setActive(false);session.release();synchronized(KuroPresenceNotification564.class){if(current.get()==this)current.clear();}}
 /** Explicit and non-exported: only PendingIntents created by Kuro invoke it. */
 public static final class Receiver extends BroadcastReceiver{
  @Override public void onReceive(Context context,Intent intent){
   if(intent==null||intent.getAction()==null||!intent.getAction().startsWith(PREFIX))return;
   String a=intent.getAction().substring(PREFIX.length());if(!("play".equals(a)||"pause".equals(a)||"next".equals(a)||"stop".equals(a)))return;
   KuroPresenceNotification564 x;synchronized(KuroPresenceNotification564.class){x=current.get();}if(x!=null)x.dispatch(a);
  }
 }
}
