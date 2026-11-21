package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Robolectric checks do not exercise the real Android Keystore or Discord network. */
@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class KuroDiscord564Test {
 private Context context;
 private static final String STATE="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmn123",VERIFIER="abcdefghijklmn123ABCDEFGHIJKLMNOPQRSTUVWXYZ";
 private String pending(long created)throws Exception{return new JSONObject().put("state",STATE).put("verifier",VERIFIER).put("created",created).toString();}
 private Uri cb(String q){return Uri.parse(KuroDiscordOAuth564.REDIRECT+"?"+q);}
 @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences(KuroDiscordPreferences564.FILE,Context.MODE_PRIVATE).edit().clear().commit();}
 @Test public void allChoicesValidateAndMapToTheModel(){
  KuroDiscordPreferences564 p=new KuroDiscordPreferences564(context);
  assertEquals("anime",p.get("layout"));assertEquals("show",p.get("title"));assertEquals("show",p.get("poster"));assertEquals("on",p.get("current"));assertEquals("on",p.get("download"));
  String[][] changes={{"layout","episode"},{"separator","dash"},{"title","hide"},{"episode","hide"},{"poster","icon"},{"watch","hide"},{"server","on"},{"total","on"},{"current","off"},{"linked","off"},{"pause","noProgress"},{"buffering","off"},{"download","on"},{"notification","on"}};
  for(String[] a:changes){assertTrue(a[0],p.set(a[0],a[1]));assertEquals(a[1],p.get(a[0]));}
  assertFalse(p.set("title","on"));assertFalse(p.set("pause","hide"));assertFalse(p.set("unknown","on"));assertFalse(p.set("download",null));
  assertTrue(p.setDownloadUrl("https://example.org/install"));assertFalse(p.setDownloadUrl("https://example.org/install?token=secret"));assertEquals("https://example.org/install",p.downloadUrl());
  KuroPresence564.Options o=p.options();assertEquals("episode",o.layout);assertEquals("dash",o.separator);assertFalse(o.title);assertFalse(o.episode);assertFalse(o.poster);assertFalse(o.watch);assertTrue(o.server);assertTrue(o.total);assertEquals("noProgress",o.pause);assertFalse(o.buffering);assertTrue(o.download);
  assertEquals("off",p.json().optString("current"));assertEquals("https://example.org/install",p.json().optString("downloadUrl"));
 }
 @Test public void oldButtonChoicesNeverPublishPages(){
  KuroDiscordPreferences564 p=new KuroDiscordPreferences564(context);
  assertTrue(p.set("watch","show"));assertTrue(p.set("download","on"));assertTrue(p.setDownloadUrl("https://example.org/install"));
  KuroPresence563.Snapshot s=KuroPresence564.watching(p.options(),"Example","3","Real name","sub",
    "https://hianime.ms/img.jpg","https://hianime.ms/anime/test","https://hianime.ms/watch-test",
    "Server 1","12","playing",30000,600000,1000000,1.0);
  assertTrue(s.buttonUrl.isEmpty());assertEquals(KuroPresence564.DOWNLOAD_PAGE_URL,s.downloadUrl);assertTrue(s.image.isEmpty());
  assertEquals("show",p.get("watch"));assertEquals("on",p.get("download"));
 }
 @Test public void serverAndTotalUseOnlyAvailableReportedMetadata()throws Exception{
  JSONObject anime=new JSONObject().put("counts",new JSONObject().put("total","24"));
  assertEquals("",MainActivity.discordReportedTotal564(anime));anime.put("countsSource","kitsune");assertEquals("",MainActivity.discordReportedTotal564(anime));anime.put("countsSource","paradise");assertEquals("",MainActivity.discordReportedTotal564(anime));anime.put("countsSource","miruro");assertEquals("24",MainActivity.discordReportedTotal564(anime));
  JSONObject sub=new JSONObject().put("lang","sub").put("url","https://example.test/s");JSONObject dub1=new JSONObject().put("lang","dub").put("url","https://example.test/a");JSONObject dub2=new JSONObject().put("lang","dub").put("url","https://example.test/b").put("note","Volt");
  JSONObject info=new JSONObject().put("servers",new org.json.JSONArray().put(sub).put(dub1).put(dub2));
  assertEquals("Server 2 Volt",MainActivity.discordServerLabel564(info,dub2,"dub"));dub2.put("note","https://secret.test/token");assertEquals("Server 2",MainActivity.discordServerLabel564(info,dub2,"dub"));
 }
 @Test public void optionalNotificationHasOnlyAvailableControlsAndPrivateText(){
  java.util.List<String> commands=new java.util.ArrayList<>();KuroPresenceNotification564 n=new KuroPresenceNotification564(context,commands::add);
  KuroPresence564.Options opts=new KuroPresence564.Options();opts.title=false;opts.episode=false;
  KuroPresence563.Snapshot safe=KuroPresence564.watching(opts,"Secret title","4","dub","","","","","paused",5000,600000,1000000);
  n.update(safe,"paused",5000,600000,true,true);
  android.app.NotificationManager manager=(android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
  android.app.Notification notification=org.robolectric.Shadows.shadowOf(manager).getNotification(56401);assertNotNull(notification);assertEquals(3,notification.actions.length);
  assertFalse(notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString().contains("Secret title"));
  android.content.Intent play=org.robolectric.Shadows.shadowOf(notification.actions[0].actionIntent).getSavedIntent();new KuroPresenceNotification564.Receiver().onReceive(context,play);assertEquals("play",commands.get(0));
  android.content.Intent next=org.robolectric.Shadows.shadowOf(notification.actions[1].actionIntent).getSavedIntent();new KuroPresenceNotification564.Receiver().onReceive(context,next);assertEquals("next",commands.get(1));
  n.update(safe,"unknown",-1,-1,false,false);notification=org.robolectric.Shadows.shadowOf(manager).getNotification(56401);assertEquals(1,notification.actions.length);
  android.content.Intent stop=org.robolectric.Shadows.shadowOf(notification.actions[0].actionIntent).getSavedIntent();new KuroPresenceNotification564.Receiver().onReceive(context,stop);assertEquals("stop",commands.get(2));
  n.close();assertNull(org.robolectric.Shadows.shadowOf(manager).getNotification(56401));
 }
 @Test public void callbackRequiresExactStateTimePathAndSingleValues()throws Exception{
  long now=10_000_000;String pending=pending(now-1000);assertEquals(43,STATE.length());assertEquals(43,VERIFIER.length());
  KuroDiscordOAuth564.Result ok=KuroDiscordOAuth564.validate(cb("code=valid-code_123&state="+STATE),pending,now);assertEquals("valid-code_123",ok.code);assertEquals(VERIFIER,ok.verifier);assertTrue(ok.consume);assertEquals("",ok.error);
  KuroDiscordOAuth564.Result wrong=KuroDiscordOAuth564.validate(cb("code=valid&state=wrong"),pending,now);assertFalse(wrong.consume);assertTrue(wrong.error.contains("state"));
  assertFalse(KuroDiscordOAuth564.validate(cb("code=valid&state="+STATE+"&state="+STATE),pending,now).consume);
  KuroDiscordOAuth564.Result dup=KuroDiscordOAuth564.validate(cb("code=one&code=two&state="+STATE),pending,now);assertTrue(dup.consume);assertTrue(dup.code.isEmpty());
  KuroDiscordOAuth564.Result denied=KuroDiscordOAuth564.validate(cb("error=access_denied&state="+STATE),pending,now);assertTrue(denied.consume);assertTrue(denied.error.contains("denied"));
  assertFalse(KuroDiscordOAuth564.validate(Uri.parse("discord-1569077670074122250://authorize/callback?code=valid&state="+STATE),pending,now).consume);
  assertFalse(KuroDiscordOAuth564.validate(Uri.parse("discord-1569077670074122250:/authorize/wrong?code=valid&state="+STATE),pending,now).consume);
  assertFalse(KuroDiscordOAuth564.validate(Uri.parse("discord-1569077670074122250:/authorize/callback?code=valid&state="+STATE+"#fragment"),pending,now).consume);
  assertTrue(KuroDiscordOAuth564.validate(cb("code=valid&state="+STATE),pending(now-600001),now).consume);
  assertTrue(KuroDiscordOAuth564.validate(cb("code=valid&state="+STATE),pending(now+1),now).consume);
  assertTrue(KuroDiscordOAuth564.validate(cb("code=valid&state="+STATE),null,now).error.contains("No pending"));
  assertTrue(KuroDiscordOAuth564.isCallback(new Intent(Intent.ACTION_VIEW,cb("state="+STATE))));assertFalse(KuroDiscordOAuth564.isCallback(new Intent(Intent.ACTION_VIEW,Uri.parse("https://discord.com"))));
 }
}
