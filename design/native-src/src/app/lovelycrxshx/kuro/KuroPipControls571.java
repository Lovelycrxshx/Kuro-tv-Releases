package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.util.Rational;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/** Explicit PiP controls for the app-owned native player, not unobservable embeds. */
public final class KuroPipControls571 {
 private static final String PREFIX="app.lovelycrxshx.kuro.PIP571_";
 private static WeakReference<MainActivity> current=new WeakReference<>(null);
 private KuroPipControls571(){}
 static synchronized void attach(MainActivity activity){current=new WeakReference<>(activity);}
 static synchronized void detach(MainActivity activity){if(current.get()==activity)current.clear();}
 static PictureInPictureParams params(Activity activity,String session,boolean nativePlayer,boolean playing){
  PictureInPictureParams.Builder builder=new PictureInPictureParams.Builder().setAspectRatio(new Rational(16,9));
  ArrayList<RemoteAction> actions=new ArrayList<>();
  if(nativePlayer&&session!=null&&session.matches("[A-Za-z0-9_-]{1,40}")){
   String action=playing?"pause":"play";Intent i=new Intent(activity,Receiver.class).setAction(PREFIX+action).setData(Uri.parse("kuro-pip://control/"+session+"/"+action)).putExtra("session",session);
   PendingIntent pending=PendingIntent.getBroadcast(activity,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   int icon=playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play;String label=playing?"Pause":"Play";
   actions.add(new RemoteAction(Icon.createWithResource(activity,icon),label,label+" native video",pending));
  }
  return builder.setActions(actions).build();
 }
 /** Non-exported, explicit, immutable intents; stale watch sessions are ignored. */
 public static final class Receiver extends BroadcastReceiver {
  @Override public void onReceive(Context context,Intent intent){
   if(intent==null||intent.getAction()==null||!intent.getAction().startsWith(PREFIX))return;
   String action=intent.getAction().substring(PREFIX.length()),session=intent.getStringExtra("session");
   if(!("play".equals(action)||"pause".equals(action))||session==null||!session.matches("[A-Za-z0-9_-]{1,40}")||!Uri.parse("kuro-pip://control/"+session+"/"+action).equals(intent.getData()))return;
   MainActivity activity;synchronized(KuroPipControls571.class){activity=current.get();}
   if(activity!=null)activity.runOnUiThread(()->activity.pipCommand571(session,action));
  }
 }
}
