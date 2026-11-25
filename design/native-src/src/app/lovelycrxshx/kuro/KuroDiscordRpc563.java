package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/** Two opt-in Social SDK clients: local Discord RPC and a separately authorized account. */
final class KuroDiscordRpc563 {
 static final long APPLICATION_ID=1569077670074122250L;
 private static boolean loadTried,loaded;
 private static final java.util.concurrent.atomic.AtomicLong OWNERS=new java.util.concurrent.atomic.AtomicLong();
 private final long owner=OWNERS.incrementAndGet();
 private final Activity activity;private final Context context;private final KuroDiscordPreferences564 preferences;
 private final Handler worker;
 private final KuroPresence563.Gate gate=new KuroPresence563.Gate();private final KuroPresence563.Retry retry567=new KuroPresence563.Retry();
 private volatile boolean active,closed,vaultFailed;
 private volatile int generation;
 private volatile String state="Off",currentState="Off",linkedState="Not linked";
 private volatile long lastCurrent,lastLinked,hotUntil;
 private volatile boolean failing;
 private volatile int directState,directError;
 private volatile String appProbe;
 private KuroPresence563.Snapshot latest;private long lastSend=-1,testUntil=0,linkOperation=0;private boolean inTest=false,initialized,draining;
 private int previousCurrent=Integer.MIN_VALUE,previousLink=Integer.MIN_VALUE,previousLinkedResult=Integer.MIN_VALUE;
 private final Object historyLock=new Object();

 KuroDiscordRpc563(Activity a){activity=a;context=a.getApplicationContext();preferences=new KuroDiscordPreferences564(context);worker=new Handler(context.getMainLooper());}
 private static synchronized boolean load(){
  if(!loadTried){loadTried=true;try{System.loadLibrary("discord_partner_sdk");System.loadLibrary("kuro_discord_rpc563");loaded=true;}catch(LinkageError|SecurityException ignored){loaded=false;}}
  return loaded;
 }
 // Presence rides the Discord app's own RPC service, so the one thing worth
 // reporting is whether this phone exposes it at all.
 private String appProbe(){
  if(appProbe!=null)return appProbe;
  String who="not installed",service="?";
  try{
   android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo("com.discord",0);
   who="installed"+(info.versionName==null?"":" "+info.versionName);
   android.content.Intent probe=new android.content.Intent("com.discord.socialsdk.rpc.IDiscordRpcService").setPackage("com.discord");
   service=context.getPackageManager().resolveService(probe,0)!=null?"presence service reachable":"presence service NOT exposed by this Discord build";
  }catch(android.content.pm.PackageManager.NameNotFoundException missing){}
  catch(Throwable ignored){service="probe failed";}
  appProbe="\nDiscord app: "+who+" · "+service;
  return appProbe;
 }
 private boolean currentTarget(){return "on".equals(preferences.get("current"));}
 private boolean linkedTarget(){return "on".equals(preferences.get("linked"));}
 String status(){
  String a=currentTarget()?currentState:"Not sharing";String b=linkedTarget()?linkedState:"Not sharing";
  String conn=currentTarget()?"\nPresence transport: "+(directState==2?"accepted by the Discord app":directState==1?"update in flight":directState<0?(directError!=0?"refused (SDK code "+directError+")":"refused by the Discord app"):"idle, nothing published yet"):"";
  String trace="";
  try{
   int count=nativeTraceCount(owner);StringBuilder t=new StringBuilder();
   for(int i=0;i<count;i++){
    int status=nativeTraceStatus(owner,i),error=nativeTraceError(owner,i);
    String name=status==3?"Ready":status==2?"Connected":status==1?"Connecting":status==4?"Reconnecting":status==5?"Disconnecting":status==6?"HttpWait":status==0?"Disconnected":"Status "+status;
    if(t.length()>0)t.append(" · ");
    t.append(name).append(error!=0?"(error "+error+")":"");
   }
   if(t.length()>0)trace="\nSDK events: "+t;
  }catch(RuntimeException|LinkageError ignored){}
  String stuck="";
  if(currentTarget()){
   String logs="";
   try{byte[] raw=nativeLogs(owner);if(raw!=null&&raw.length>0)logs="\nSDK log: "+new String(raw,StandardCharsets.UTF_8);}catch(RuntimeException|LinkageError ignored){}
   stuck=appProbe()+logs;
  }
  String retry=failing?"\nNext retry in "+Math.max(1,(retry567.waitFor(lastSend,SystemClock.elapsedRealtime())+999)/1000)+" s.":"";
  return state+"\nCurrent Discord app: "+a+"\nLinked Discord: "+b+"\nLast accepted update — current: "+ago(lastCurrent)+"; linked: "+ago(lastLinked)+retry+conn+stuck+trace
   +(vaultFailed?"\nSecure storage failed; relink may be needed.":"")+"\nAcceptance is not proof of profile visibility. Discord activity sharing must be enabled.";
 }
 private static String when(long t){return t>0?DateFormat.getDateTimeInstance().format(new Date(t)):"none";}
 private static String ago(long t){if(t<=0)return "never";long d=Math.max(0,System.currentTimeMillis()-t);if(d<10000)return "just now";if(d<60000)return (d/1000)+" s ago";if(d<3600000)return (d/60000)+" min ago";if(d<86400000)return (d/3600000)+" h ago";return new java.text.SimpleDateFormat("d MMM HH:mm",Locale.getDefault()).format(new Date(t));}
 private void event(String message){
  synchronized(historyLock){SharedPreferences p=context.getSharedPreferences("kuro-discord-history564",Context.MODE_PRIVATE);String previous=p.getString("events","");String line=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.MEDIUM).format(new Date())+" — "+message;
   String[] all=(previous+"\n"+line).trim().split("\n");int from=Math.max(0,all.length-20);StringBuilder out=new StringBuilder();for(int i=from;i<all.length;i++){if(out.length()>0)out.append('\n');out.append(all[i]);}p.edit().putString("events",out.toString()).apply();}
 }
 String history(){synchronized(historyLock){String s=context.getSharedPreferences("kuro-discord-history564",Context.MODE_PRIVATE).getString("events","");return s.isEmpty()?"No connection events yet.":s;}}
 void start(){
  if(closed||active)return;
  try{com.discord.socialsdk.DiscordSocialSdkInit.setEngineActivity(activity);}
  catch(RuntimeException|LinkageError e){state="Discord SDK could not initialize on this device.";return;}
  active=true;final int mine=++generation;state="Starting Discord Rich Presence…";event("Presence started");
  worker.post(()->initialize(mine));
 }
 private void initialize(int mine){
  if(!active||closed||mine!=generation)return;
  if(draining){worker.postDelayed(()->initialize(mine),150);return;}
  try{
   if(!load()){state="Discord SDK unavailable on this device.";active=false;event("SDK initialization failed");return;}
   if(nativeBusy(owner)){state="Finishing previous Discord authorization…";worker.postDelayed(()->initialize(mine),150);return;}
   if(!nativeInit(APPLICATION_ID,owner)){
    if(nativeBusy(owner)){worker.postDelayed(()->initialize(mine),150);return;}
    state="Discord SDK unavailable on this device.";active=false;event("SDK initialization failed");return;
   }
   initialized=true;gate.reset();retry567.reset();lastSend=-1;testUntil=0;inTest=false;previousCurrent=previousLink=previousLinkedResult=Integer.MIN_VALUE;
   nativeTargets(owner,currentTarget(),linkedTarget());state="Presence is on";currentState="Presence is armed; play something to publish";appProbe=null;linkedState="Not linked";
   if(linkedTarget())restoreSaved();worker.post(new Pump(mine));
  }catch(RuntimeException|LinkageError e){initialized=false;active=false;state="Discord SDK unavailable on this device.";event("SDK initialization failed");}
 }

 private void restoreSaved(){
  byte[] saved=KuroDiscordVault564.load(context,"refresh");
  if(saved==null){if(KuroDiscordVault564.hasStored(context,"refresh")){vaultFailed=true;linkedState="Saved authorization unavailable; relink";}else linkedState="Not linked";return;}
  try{nativeRestore(owner,saved);linkedState="Reconnecting saved authorization";}finally{Arrays.fill(saved,(byte)0);}
 }
 private boolean persistPending(){
  if(!initialized)return true;byte[] b=nativePendingRefresh(owner);if(b==null||b.length==0)return true;
  try{if(KuroDiscordVault564.store(context,"refresh",b)){nativeAckRefresh(owner,b);vaultFailed=false;return true;}vaultFailed=true;return false;}finally{Arrays.fill(b,(byte)0);}
 }
 /** Do not discard a client while its one-time refresh token is being rotated. */
 private void whenLinkSettled(long operation,long deadline,Runnable task){
  if(!active||closed||!initialized||operation!=linkOperation)return;
  try{
   if(nativeRefreshing(owner)||!persistPending()){
    if(SystemClock.elapsedRealtime()<deadline)worker.postDelayed(()->whenLinkSettled(operation,deadline,task),150);
    else{linkedState="Authorization is still pending; try again later";event("Linked account operation deferred");}
   }else task.run();
  }catch(RuntimeException|LinkageError ignored){linkedState="Linked account operation failed";}
 }

 void publish(KuroPresence563.Snapshot s){if(!active||closed||s==null)return;final int mine=generation;worker.post(()->{if(active&&mine==generation)latest=s;});}
 /** Immediately remove an old snapshot before publishing the new privacy settings. */
 void settingsChanged(){
  if(!active||closed)return;final int mine=generation;
  worker.post(()->{if(!active||mine!=generation||!initialized)return;try{
   persistPending();nativeClear(owner);nativeTargets(owner,currentTarget(),linkedTarget());
   if(linkedTarget()&&nativeLinkedState(owner)==0)restoreSaved();
   latest=null;gate.reset();lastSend=-1;
  }catch(RuntimeException|LinkageError ignored){state="Discord settings could not be applied.";}});
 }
 void clearForPause(){
  if(!active||closed)return;final int mine=generation;
  worker.post(()->{if(!active||mine!=generation||!initialized)return;latest=null;gate.reset();lastSend=-1;if(SystemClock.elapsedRealtime()>=testUntil)nativeClear(owner);});
 }
 boolean isActive(){return active&&!closed;}
 private void whenReady(int mine,long deadline,Runnable task){
  if(!active||closed||mine!=generation)return;
  if(initialized&&!draining)task.run();else if(SystemClock.elapsedRealtime()<deadline)worker.postDelayed(()->whenReady(mine,deadline,task),150);else state="Discord SDK is not ready; try again.";
 }
 void reconnectCurrent(){
  if(!isActive()){state="Turn on presence first.";return;}final int mine=generation;
  worker.post(()->whenReady(mine,SystemClock.elapsedRealtime()+20000,()->{nativeReconnectCurrent(owner);nativeTargets(owner,currentTarget(),linkedTarget());gate.reset();lastSend=-1;event("Current Discord reconnect requested");}));
 }
 void reconnectLinked(){
  if(!isActive()){state="Turn on presence first.";return;}if(!linkedTarget()){linkedState="Enable linked-account sharing first";return;}final int mine=generation;
  worker.post(()->whenReady(mine,SystemClock.elapsedRealtime()+20000,()->{long op=++linkOperation;whenLinkSettled(op,SystemClock.elapsedRealtime()+20000,()->{nativeUnlink(owner);restoreSaved();gate.reset();lastSend=-1;event("Linked Discord reconnect requested");});}));
 }
 void exchange(String code,String verifier){
  if(!isActive()||code==null||verifier==null)return;final int mine=generation;
  worker.post(()->{if(!active||mine!=generation)return;long op=++linkOperation;waitToExchange(mine,op,code,verifier,SystemClock.elapsedRealtime()+20000);});
 }
 private void waitToExchange(int mine,long op,String code,String verifier,long deadline){
  if(!active||closed||mine!=generation||op!=linkOperation)return;
  if(!initialized){if(SystemClock.elapsedRealtime()<deadline)worker.postDelayed(()->waitToExchange(mine,op,code,verifier,deadline),150);else linkedState="SDK initialization timed out";return;}
  whenLinkSettled(op,deadline,()->{nativeExchange(owner,utf8(code),utf8(verifier));gate.reset();lastSend=-1;linkedState="Authorizing linked account";event("Linked Discord authorization submitted");});
 }
 void unlink(){
  if(closed)return;final int mine=generation;
  worker.post(()->{++linkOperation;KuroDiscordVault564.unlink(context);if(initialized&&mine==generation)nativeUnlink(owner);linkedState="Disconnected on this phone";vaultFailed=false;event("Linked Discord disconnected locally");});
 }
 boolean test(){
  if(!isActive()){state="Turn on presence first.";return false;}if(!currentTarget()&&!linkedTarget()){state="Enable at least one account target first.";return false;}final int mine=generation;
  worker.post(()->whenReady(mine,SystemClock.elapsedRealtime()+20000,()->{testUntil=SystemClock.elapsedRealtime()+15000;inTest=true;gate.reset();lastSend=-1;state="Sending a 15-second test";event("15-second test requested");}));return true;
 }
 void stop(){
  if(closed)return;boolean was=active;active=false;++generation;state="Off or paused in background";
  worker.post(()->{++linkOperation;latest=null;testUntil=0;inTest=false;gate.reset();lastSend=-1;if(was)event("Presence stopped; clear requested");
   if(initialized&&!draining){try{nativeQuiesce(owner);}catch(RuntimeException|LinkageError ignored){}draining=true;finishStop(SystemClock.elapsedRealtime()+20000);}
   else if(!draining){currentState="Off";linkedState="Disconnected";}
  });
 }
 private void finishStop(long deadline){
  if(!draining)return;
  try{
   nativePump(owner);boolean saved=persistPending();
   if((nativeRefreshing(owner)||!saved)&&SystemClock.elapsedRealtime()<deadline){worker.postDelayed(()->finishStop(deadline),150);return;}
   if(nativeRefreshing(owner)||!saved){vaultFailed=true;event("Authorization did not finish before shutdown; relink may be needed");}
   nativeStop(owner);
  }catch(RuntimeException|LinkageError ignored){}initialized=false;draining=false;currentState="Off";linkedState="Disconnected";
 }
 void close(){if(closed)return;stop();closed=true;}

 private static final byte[] NO_PRESENCE_LINK=new byte[0];
 private static byte[] utf8(String s){return s.getBytes(StandardCharsets.UTF_8);}
 private final class Pump implements Runnable {
  private final int mine;Pump(int generation){mine=generation;}
  @Override public void run(){
   if(!active||closed||mine!=generation||!initialized)return;
   try{
    nativePump(owner);persistPending();int result=nativeResult(owner),link=nativeLinkedState(owner),linkResult=nativeLinkedResult(owner);
    if(result!=previousCurrent){previousCurrent=result;currentState=result>0?"Update accepted by SDK":result<0?"Update failed (SDK code "+result+"); open and sign in to Discord":"Waiting for Discord";if(currentTarget()&&result!=0)event(result>0?"Current account update accepted":"Current account update failed (SDK code "+result+")");}
    if(link!=previousLink){previousLink=link;linkedState=link==2?"Connected":link==1?"Connecting":link<0?"Connection or authorization failed":"Not linked";if(link!=0)event(link==2?"Linked account connected":link<0?"Linked account connection failed (SDK code "+link+")":"Linked account connecting");}
    if(linkResult!=previousLinkedResult){previousLinkedResult=linkResult;if(linkResult!=0)event(linkResult>0?"Linked account update accepted":"Linked account update failed (SDK code "+linkResult+")");}
    if(link==2&&linkResult>0)linkedState="Connected; update accepted by SDK";else if(link==2&&linkResult<0)linkedState="Connected; update failed";
    lastCurrent=nativeLastAccepted(owner,false);lastLinked=nativeLastAccepted(owner,true);
    long now=SystemClock.elapsedRealtime();
    if(inTest&&now>=testUntil){inTest=false;testUntil=0;gate.reset();lastSend=-1;state="Presence is on";event("15-second test finished");if(latest==null)nativeClear(owner);}
    directState=nativeDirectState(owner);directError=nativeDirectError(owner);
    if(currentTarget()&&result==0)currentState=directState==2?"Publishing to the Discord app":directState<0?"The Discord app refused the presence update":"Presence is armed; play something to publish";
    boolean failed=(currentTarget()&&result<0)||(linkedTarget()&&linkResult<0);
    boolean ready=retry567.ready(failed,lastSend,now);failing=failed;
    if(failed&&ready&&lastSend>=0)gate.reset();
    KuroPresence563.Snapshot s=inTest?KuroPresence564.test(preferences.options()):latest;
    if(s!=null&&ready&&gate.due(s,now)){
     nativePublish(owner,utf8(s.details),utf8(s.state),utf8(s.image),NO_PRESENCE_LINK,utf8(s.downloadUrl),s.startMs,s.endMs);
     gate.sent(s,now);retry567.sent(failed);lastSend=now;hotUntil=now+4000;
    }
    // Poll faster only while there is work: a handshake, an update in flight or
    // playback. A connected, idle session settles to 1.5 s.
    long delay;
    if(currentTarget()&&directState!=2)delay=50;   // handshake needs frequent callbacks
    else if(now<hotUntil)delay=100;
    else if(s!=null&&s.watching&&s.endMs>s.startMs)delay=500;
    else delay=1500;
    worker.postDelayed(this,delay);
   }catch(RuntimeException|LinkageError e){state="Discord SDK stopped unexpectedly.";active=false;try{nativeStop(owner);}catch(RuntimeException|LinkageError ignored){}initialized=false;event("SDK stopped unexpectedly");}
  }
 }
 private static native boolean nativeBusy(long owner);
 private static native boolean nativeRefreshing(long owner);
 private static native boolean nativeInit(long id,long owner);
 private static native void nativeTargets(long owner,boolean current,boolean linked);
 private static native void nativePublish(long owner,byte[] details,byte[] state,byte[] image,byte[] url,byte[] download,long startMs,long endMs);
 private static native void nativePump(long owner);
 private static native int nativeResult(long owner);
 private static native int nativeDirectState(long owner);
 private static native int nativeDirectError(long owner);
 private static native byte[] nativeLogs(long owner);
 private static native int nativeTraceCount(long owner);
 private static native int nativeTraceStatus(long owner,int index);
 private static native int nativeTraceError(long owner,int index);
 private static native int nativeLinkedState(long owner);
 private static native int nativeLinkedResult(long owner);
 private static native long nativeLastAccepted(long owner,boolean linked);
 private static native void nativeQuiesce(long owner);
 private static native void nativeClear(long owner);
 private static native void nativeReconnectCurrent(long owner);
 private static native void nativeUnlink(long owner);
 private static native void nativeRestore(long owner,byte[] refresh);
 private static native void nativeExchange(long owner,byte[] code,byte[] verifier);
 private static native byte[] nativePendingRefresh(long owner);
 private static native void nativeAckRefresh(long owner,byte[] refresh);
 private static native void nativeStop(long owner);
}
