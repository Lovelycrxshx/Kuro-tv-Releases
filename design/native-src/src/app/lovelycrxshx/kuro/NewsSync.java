package app.lovelycrxshx.kuro;

import android.Manifest;
import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.net.Uri;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Local, bounded RSS cache and inexact Android jobs. There is no user-operated server. */
public final class NewsSync {
 public static final String PREFS="kuro_news_v1",CHANNEL="anime_updates",ACTION_OPEN="app.lovelycrxshx.kuro.OPEN_NEWS";
 public static final int JOB_ID=52601,NOTIFICATION_ID=52602;
 private static final int MAX_CACHE=150;
 private static final long INTERVAL=60*60_000L, FLEX=15*60_000L, THROTTLE=2*60_000L;
 // Each replacement has a NEW id, so the first successful check seeds it
 // independently rather than notifying for an old channel's entire history.
 private static final NewsParser.Feed[] FEEDS={
  new NewsParser.Feed("crunchyroll","Crunchyroll News","news","https://cr-news-api-service.prd.crunchyrollsvc.com/v1/en-US/rss"),
  new NewsParser.Feed("animecorner","Anime Corner","news","https://animecorner.me/feed/"),
  new NewsParser.Feed("trending541","Anime Trending","news","https://anitrendz.net/news/feed/"),
  new NewsParser.Feed("mal541","MyAnimeList News","news","https://myanimelist.net/rss/news.xml"),
  new NewsParser.Feed("tohostore541","TOHO animation Store · official blog","official","https://tohoanimationstore.us/blogs/news/tagged/news.atom"),
  new NewsParser.Feed("aniplexjp541","Aniplex official · Japan","official","https://www.aniplex.co.jp/news/","aniplex-html"),
  new NewsParser.Feed("kadopr541","KADOKAWA press · Japan","official","https://prtimes.jp/companyrdf.php?company_id=7006","anime-only")
 };
 private static SharedPreferences pref(Context c){return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
 public static boolean enabled(Context c){return pref(c).getBoolean("alerts_enabled",true);}
 public static void ensureChannel(Context c){
  if(Build.VERSION.SDK_INT>=26){NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(n!=null&&n.getNotificationChannel(CHANNEL)==null){NotificationChannel channel=new NotificationChannel(CHANNEL,"Anime updates",NotificationManager.IMPORTANCE_DEFAULT);channel.setDescription("Anime news reports and official channel posts");n.createNotificationChannel(channel);}}
 }
 public static boolean allowed(Context c){
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
  NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(n==null||!n.areNotificationsEnabled())return false;
  if(Build.VERSION.SDK_INT>=26){NotificationChannel channel=n.getNotificationChannel(CHANNEL);if(channel!=null&&channel.getImportance()==NotificationManager.IMPORTANCE_NONE)return false;}return true;
 }
 public static JSONObject status(Context c){JSONObject o=new JSONObject();try{o.put("enabled",enabled(c));o.put("allowed",allowed(c));o.put("checkedAt",pref(c).getLong("checkedAt",0));}catch(Exception ignored){}return o;}
 public static void setEnabled(Context c,boolean value){pref(c).edit().putBoolean("alerts_enabled",value).apply();if(!value){NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(n!=null)n.cancel(NOTIFICATION_ID);}schedule(c);}
 public static void schedule(Context c){
  try{ensureChannel(c);JobScheduler j=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(j==null)return;
   if(!enabled(c)||!allowed(c)){j.cancel(JOB_ID);return;}
   if(j.getPendingJob(JOB_ID)!=null)return;
   JobInfo info=new JobInfo.Builder(JOB_ID,new ComponentName(c,NewsJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(INTERVAL,FLEX).build();j.schedule(info);
  }catch(Exception ignored){}
 }
 public static JSONObject cached(Context c){
  try{JSONObject result=new JSONObject(pref(c).getString("cache","{}"));if(!result.has("items"))result.put("items",new JSONArray());if(!result.has("failed"))result.put("failed",new JSONArray());return result;}
  catch(Exception e){JSONObject empty=new JSONObject();try{empty.put("items",new JSONArray());empty.put("failed",new JSONArray());}catch(Exception ignored){}return empty;}
 }
 private static final class FeedHttp extends IOException {final int status;FeedHttp(int s){super("feed HTTP "+s);status=s;}}
 private static boolean safeFeedUrl(URL original,URL next){
  if(!"https".equalsIgnoreCase(next.getProtocol())||next.getUserInfo()!=null||next.getPort()!=-1||next.getHost()==null)return false;
  String a=original.getHost().toLowerCase(Locale.ROOT),b=next.getHost().toLowerCase(Locale.ROOT);
  // Only same publisher, with an optional www prefix; no cross-site redirects.
  if(a.startsWith("www."))a=a.substring(4);if(b.startsWith("www."))b=b.substring(4);return a.equals(b);
 }
 private static byte[] download(String url)throws Exception{
  URL original=new URL(url),current=original;if(!safeFeedUrl(original,current))throw new IOException("unsafe feed URL");
  for(int redirects=0;redirects<=2;redirects++){
   if(Thread.currentThread().isInterrupted())throw new IOException("interrupted");
   HttpURLConnection c=(HttpURLConnection)current.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(7500);c.setReadTimeout(10000);
   c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36");
   c.setRequestProperty("Accept","application/rss+xml,application/atom+xml,application/xml,text/xml,text/html;q=0.8,*/*;q=0.2");
   try{int status=c.getResponseCode();
    if(status>=300&&status<400){String location=c.getHeaderField("Location");if(location==null||redirects==2)throw new IOException("feed redirect unavailable");URL next=new URL(current,location);if(!safeFeedUrl(original,next))throw new IOException("unsafe feed redirect");current=next;continue;}
    if(status!=200)throw new FeedHttp(status);
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("interrupted");if(out.size()+n>1_500_000)throw new IOException("feed too large");out.write(b,0,n);}return out.toByteArray();}
   }finally{c.disconnect();}
  }throw new IOException("feed redirects exceeded");
 }
 private static JSONObject json(NewsParser.Item x)throws JSONException{
  JSONObject j=new JSONObject();j.put("id",x.id);j.put("title",x.title);j.put("summary",x.summary);j.put("url",x.url);j.put("image",x.image);j.put("source",x.source);j.put("kind",x.kind);j.put("published",x.published);return j;
 }
 private static final class Download {final NewsParser.Feed feed;final List<NewsParser.Item> items;final int status;Download(NewsParser.Feed f,List<NewsParser.Item> i,int s){feed=f;items=i;status=s;}}
 /** Automatic startup/resume work is disabled with phone alerts; News stays manual. */
 public static JSONObject refreshIfEnabled(Context c){return enabled(c)?refresh(c,false):cached(c);}
 /** Serialize writes, and deduplicate checks from the activity and JobScheduler. */
 public static synchronized JSONObject refresh(Context context,boolean force){
  Context c=context.getApplicationContext();SharedPreferences prefs=pref(c);long now=System.currentTimeMillis();
  long age=now-prefs.getLong("checkedAt",0);
  if(!force&&prefs.getLong("feed_schema",0)==541&&age>=0&&age<THROTTLE)return cached(c);
  ArrayList<Future<Download>> tasks=new ArrayList<>();ExecutorService workers=Executors.newFixedThreadPool(4);
  for(final NewsParser.Feed feed:FEEDS)tasks.add(workers.submit(()->{
   // 403/429 can indicate verification or rate limiting. Pause automatic
   // requests for that source until the user explicitly taps Refresh.
   if(!force&&prefs.getBoolean("hold_"+feed.id,false))return new Download(feed,null,-1);
   try{return new Download(feed,NewsParser.parse(download(feed.url),feed),200);}
   catch(FeedHttp e){return new Download(feed,null,e.status);}
   catch(Exception e){return new Download(feed,null,0);}
  }));
  LinkedHashMap<String,JSONObject> merged=new LinkedHashMap<>();JSONArray old=cached(c).optJSONArray("items");if(old!=null)for(int i=0;i<old.length();i++){JSONObject x=old.optJSONObject(i);if(x!=null&&!x.optString("id").isEmpty())merged.put(x.optString("id"),x);}
  JSONArray failed=new JSONArray();ArrayList<NewsParser.Item> fresh=new ArrayList<>();SharedPreferences.Editor edit=prefs.edit();
  try{
   for(int i=0;i<FEEDS.length;i++){
    if(Thread.currentThread().isInterrupted())return cached(c);
    Download d;try{d=tasks.get(i).get(22,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();return cached(c);}catch(Exception e){tasks.get(i).cancel(true);d=new Download(FEEDS[i],null,0);}
    if(d.items==null){boolean restricted=d.status==403||d.status==429||d.status==-1;if(restricted)edit.putBoolean("hold_"+d.feed.id,true);failed.put(d.feed.label+(restricted?" (restricted)":""));continue;}
    edit.putBoolean("hold_"+d.feed.id,false);
    LinkedHashSet<String> seen=new LinkedHashSet<>();try{JSONArray a=new JSONArray(prefs.getString("seen_"+d.feed.id,"[]"));for(int z=0;z<a.length();z++)if(a.optString(z).length()<100)seen.add(a.optString(z));}catch(Exception ignored){}
    NewsLogic.Result r=NewsLogic.update(d.items,seen,prefs.getBoolean("seeded_"+d.feed.id,false),prefs.getLong("seededAt_"+d.feed.id,0),now);
    JSONArray saved=new JSONArray();for(String id:r.seen)saved.put(id);edit.putString("seen_"+d.feed.id,saved.toString()).putBoolean("seeded_"+d.feed.id,true).putLong("seededAt_"+d.feed.id,r.seededAt);
    fresh.addAll(r.newlyPublished);for(NewsParser.Item item:d.items)try{merged.put(item.id,json(item));}catch(Exception ignored){}
   }
  }finally{workers.shutdownNow();}
  if(Thread.currentThread().isInterrupted())return cached(c);
  ArrayList<JSONObject> items=new ArrayList<>(merged.values());Collections.sort(items,(a,b)->Long.compare(b.optLong("published",0),a.optLong("published",0)));
  JSONArray resultItems=new JSONArray();for(int i=0;i<Math.min(MAX_CACHE,items.size());i++)resultItems.put(items.get(i));
  JSONObject result=new JSONObject();try{result.put("items",resultItems);result.put("failed",failed);result.put("checkedAt",now);}catch(Exception ignored){}
  edit.putString("cache",result.toString()).putLong("checkedAt",now).putLong("feed_schema",541).apply();
  if(!fresh.isEmpty()&&enabled(c)&&allowed(c)){Collections.sort(fresh,(a,b)->Long.compare(b.published,a.published));notifyUpdates(c,fresh);}
  return result;
 }
 // Only explicitly approved image hosts; no redirects, cookies or unbounded downloads.
 private static Bitmap articleArtwork(String address){
  try{String safe=NewsParser.safeImage(address==null?"":address);if(safe.isEmpty())return null;
   HttpURLConnection c=(HttpURLConnection)new URL(safe).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(3000);c.setReadTimeout(3000);c.setRequestProperty("Accept","image/*");
   try{if(c.getResponseCode()!=200)return null;int size=c.getContentLength();if(size>1_500_000)return null;
    ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>1_500_000)return null;out.write(buf,0,n);}}
    byte[] data=out.toByteArray();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>12000||bounds.outHeight>12000)return null;
    BitmapFactory.Options options=new BitmapFactory.Options();while(bounds.outWidth/options.inSampleSize>1024||bounds.outHeight/options.inSampleSize>1024)options.inSampleSize*=2;return BitmapFactory.decodeByteArray(data,0,data.length,options);
   }finally{c.disconnect();}
  }catch(Exception ignored){return null;}
 }
 private static void notifyUpdates(Context c,List<NewsParser.Item> fresh){
  try{ensureChannel(c);NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(manager==null)return;
   Intent open=new Intent(c,MainActivity.class).setAction(ACTION_OPEN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
   int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
   PendingIntent tap=PendingIntent.getActivity(c,NOTIFICATION_ID,open,flags);
   int icon=c.getResources().getIdentifier("ic_news","drawable",c.getPackageName());if(icon==0)icon=c.getApplicationInfo().icon;
   NewsParser.Item first=fresh.get(0);String firstLine=first.source+" · "+first.title;
   Notification.Builder b=new Notification.Builder(c,CHANNEL).setSmallIcon(icon).setColor(Color.rgb(174,103,230)).setContentIntent(tap).setAutoCancel(true).setWhen(System.currentTimeMillis()).setShowWhen(true)
     .setCategory(Notification.CATEGORY_RECOMMENDATION).setSubText("Kuro.tv · Anime news")
     .addAction(new Notification.Action.Builder(icon,"Open news",tap).build());
   // A bundled image is used rather than loading third-party images in the
   // background; the system can still apply its own notification layout.
   int brand=c.getResources().getIdentifier("icon","drawable",c.getPackageName());
   if(brand!=0){Bitmap artwork=BitmapFactory.decodeResource(c.getResources(),brand);if(artwork!=null)b.setLargeIcon(artwork);}
   if(fresh.size()==1){
    String link=NewsParser.safeLink(first.url==null?"":first.url);
    if(!link.isEmpty()){Intent article=new Intent(Intent.ACTION_VIEW,Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);PendingIntent read=PendingIntent.getActivity(c,NOTIFICATION_ID+1,article,flags);b.addAction(new Notification.Action.Builder(icon,"Read article",read).build());}
    Bitmap articleImage=articleArtwork(first.image);if(articleImage!=null)b.setLargeIcon(articleImage);
    String details=first.source+" · "+first.title;
    if(first.summary!=null&&!first.summary.trim().isEmpty())details+="\n\n"+first.summary.trim();
    b.setContentTitle(first.title).setContentText(first.source+" · Anime news")
      .setStyle(new Notification.BigTextStyle().setBigContentTitle(first.title).bigText(details).setSummaryText("Open Kuro to read"));
   }else{
    b.setContentTitle(fresh.size()+" new anime updates").setContentText(firstLine);
    Notification.InboxStyle inbox=new Notification.InboxStyle().setBigContentTitle(fresh.size()+" new anime updates");
    for(int i=0;i<Math.min(5,fresh.size());i++)inbox.addLine(fresh.get(i).source+" · "+fresh.get(i).title);
    inbox.setSummaryText(fresh.size()>5?"+"+(fresh.size()-5)+" more · Open news":"Open news in Kuro");b.setStyle(inbox);
   }
   manager.notify(NOTIFICATION_ID,b.build());
  }catch(SecurityException ignored){}catch(RuntimeException ignored){}
 }
}
