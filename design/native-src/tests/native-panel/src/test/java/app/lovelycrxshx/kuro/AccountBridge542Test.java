package app.lovelycrxshx.kuro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Shadow Android/bridge tests; no live browser, provider, image picker or handset. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE,qualifiers="w360dp-h800dp-xhdpi")
public class AccountBridge542Test {
 private ActivityController<MainActivity> controller;private MainActivity activity;private AccountFlows542Test.Vault vault;private AccountFlows542Test.Fake http;private KuroAccounts accounts;
 static class Immediate extends AbstractExecutorService {public void shutdown(){}public List<Runnable> shutdownNow(){return Collections.emptyList();}public boolean isShutdown(){return false;}public boolean isTerminated(){return false;}public boolean awaitTermination(long t,TimeUnit u){return true;}public void execute(Runnable r){r.run();}}
 static class NoNetwork extends Immediate {@Override public void execute(Runnable r){}}
 private Object field(String n)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(activity);}
 private void set(String n,Object x)throws Exception{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);f.set(activity,x);}
 private void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 @Before public void create()throws Exception{
  controller=Robolectric.buildActivity(MainActivity.class);activity=controller.get();set("pool",new NoNetwork());controller.create(new Bundle()).start().resume().visible();set("pool",new Immediate());set("uiReady",true);
  vault=new AccountFlows542Test.Vault();http=new AccountFlows542Test.Fake();accounts=AccountFlows542Test.make(vault,http);set("accounts",accounts);
 }
 @After public void close(){controller.pause().stop().destroy();}
 @Test public void bridgeReturnsOnlySanitizedStatusAndStartsExternalProvider()throws Exception{
  MainActivity.Bridge bridge=activity.new Bridge();bridge.account("account-1","status","{}");idle();Map<?,?> responses=(Map<?,?>)field("responses");String result=new String((byte[])responses.get("account-1"),StandardCharsets.UTF_8);assertTrue(result.contains("local"));assertFalse(result.contains("supabase"));assertFalse(result.contains("fake-access-token"));
  bridge.account("invalid!","begin","{\"provider\":\"discord\"}");idle();assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
  bridge.account("account-2","begin","{\"provider\":\"discord\"}");idle();Intent opened=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(opened);assertEquals(Intent.ACTION_VIEW,opened.getAction());assertEquals("discord.com",opened.getData().getHost());assertFalse(opened.getDataString().contains("client_secret"));
 }
 @Test public void unconfiguredAdminCannotChangeProviderIdsFromBridge()throws Exception {
  String before=accounts.status().getJSONObject("config").toString();MainActivity.Bridge bridge=activity.new Bridge();
  bridge.account("account-config","configure","{\"discord\":\"123456789012345678\"}");idle();
  assertEquals(before,accounts.status().getJSONObject("config").toString());
  WebView ui=(WebView)field("ui");assertTrue(Shadows.shadowOf(ui).getLastEvaluatedJavascript().contains("Owner verification expired or was not completed"));
 }
 @Test public void ownerVerificationGuardsNativeWritesAndKeepsTokensOutOfBridge()throws Exception {
  final long[] clock={10000};
  GitHubAdmin568.Transport transport=new GitHubAdmin568.Transport(){
   public JSONObject post(String url,String body)throws Exception{
    if(url.equals(GitHubAdmin568.DEVICE_URL))return new JSONObject().put("device_code","fake-device-0123456789").put("user_code","A1B2-C3D4").put("verification_uri",GitHubAdmin568.VERIFY_URL).put("expires_in",900).put("interval",5);
    assertEquals(GitHubAdmin568.TOKEN_URL,url);return new JSONObject().put("access_token","fake-owner-token-12345").put("token_type","bearer").put("scope","");
   }
   public JSONObject user(String token)throws Exception{assertEquals("fake-owner-token-12345",token);return new JSONObject().put("id",GitHubAdmin568.OWNER_ID).put("login","renamed-owner");}
  };
  GitHubAdmin568 admin=new GitHubAdmin568(transport,()->clock[0]);set("githubAdmin568",admin);MainActivity.Bridge bridge=activity.new Bridge();
  Map<?,?> replies=(Map<?,?>)field("responses");String before=accounts.status().getJSONObject("config").toString();
  bridge.account("pre-config","configure","{\"discord\":\"987654321012345678\"}");idle();assertNull(replies.get("pre-config"));assertEquals(before,accounts.status().getJSONObject("config").toString());
  bridge.account("device-begin","adminBegin","{}");idle();String start=new String((byte[])replies.get("device-begin"),StandardCharsets.UTF_8);assertTrue(start.contains("A1B2-C3D4"));assertFalse(start.contains("fake-device"));assertFalse(start.contains("fake-owner-token"));
  bridge.account("device-open","adminOpen","{}");idle();Intent open=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(open);assertEquals(GitHubAdmin568.VERIFY_URL,open.getDataString());
  bridge.account("device-copy","adminCopy","{}");idle();android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);assertEquals("A1B2-C3D4",clipboard.getPrimaryClip().getItemAt(0).getText().toString());
  bridge.account("too-early","adminPoll","{}");idle();assertTrue(new String((byte[])replies.get("too-early"),StandardCharsets.UTF_8).contains("waiting"));assertFalse(admin.verified());
  clock[0]+=5000;bridge.account("device-poll","adminPoll","{}");idle();String finish=new String((byte[])replies.get("device-poll"),StandardCharsets.UTF_8);assertTrue(finish.contains("verified"));assertFalse(finish.contains("fake-owner-token"));assertTrue(admin.verified());
  bridge.account("owner-save","configure","{\"discord\":\"987654321012345678\",\"mal\":\"37160cd5a180308c9009afb7faa4e341\",\"anilist\":\"56440\"}");idle();assertNotNull(replies.get("owner-save"));assertEquals("987654321012345678",accounts.status().getJSONObject("config").getString("discord"));
  clock[0]+=GitHubAdmin568.SESSION_MS;bridge.account("expired-save","configure","{\"discord\":\"111111111111111111\"}");idle();assertNull(replies.get("expired-save"));assertEquals("987654321012345678",accounts.status().getJSONObject("config").getString("discord"));
 }
 @Test public void otherGitHubAccountCannotUnlockNativeSetup()throws Exception {
  final long[] clock={10000};GitHubAdmin568.Transport fake=new GitHubAdmin568.Transport(){
   public JSONObject post(String url,String body)throws Exception{return url.equals(GitHubAdmin568.DEVICE_URL)?new JSONObject().put("device_code","fake-device-0123456789").put("user_code","A1B2-C3D4").put("verification_uri",GitHubAdmin568.VERIFY_URL).put("expires_in",900).put("interval",5):new JSONObject().put("access_token","fake-other-token-12345").put("token_type","bearer").put("scope","");}
   public JSONObject user(String token)throws Exception{return new JSONObject().put("id",1).put("login","ImAuraxSiva");}
  };
  GitHubAdmin568 admin=new GitHubAdmin568(fake,()->clock[0]);set("githubAdmin568",admin);MainActivity.Bridge bridge=activity.new Bridge();String before=accounts.status().getJSONObject("config").toString();
  bridge.account("wrong-begin","adminBegin","{}");idle();clock[0]+=5000;bridge.account("wrong-poll","adminPoll","{}");idle();assertFalse(admin.verified());
  bridge.account("wrong-save","configure","{\"discord\":\"111111111111111111\"}");idle();assertEquals(before,accounts.status().getJSONObject("config").toString());Map<?,?> replies=(Map<?,?>)field("responses");assertNull(replies.get("wrong-save"));
 }
 @Test public void importedDetailActionUsesExactNativeProviderReadOnly()throws Exception {
  ImportDetails556Test.Fake fake=new ImportDetails556Test.Fake();set("accounts",new KuroAccounts(vault,fake));
  MainActivity.Bridge bridge=activity.new Bridge();bridge.account("account-detail","importDetails","{\"provider\":\"anilist\",\"sourceId\":\"123\"}");idle();
  Map<?,?> replies=(Map<?,?>)field("responses");JSONObject result=new JSONObject(new String((byte[])replies.get("account-detail"),StandardCharsets.UTF_8));
  assertEquals("anilist",result.getString("provider"));assertEquals("123",result.getString("sourceId"));assertEquals("Exact AniList title",result.getString("title"));
  assertEquals("",fake.auth.get(0));assertFalse(fake.bodies.get(0).contains("mutation"));
 }
 @Test public void callbackIsConsumedAndDoesNotReplaceWatchPlayer()throws Exception{
  JSONObject data=new JSONObject().put("session","test").put("title","Title").put("episode","1").put("source","ms").put("current",0).put("index",0).put("servers",new JSONArray().put(new JSONObject().put("url","https://megaplay.buzz/stream/test").put("type","").put("lang","sub"))).put("episodes",new JSONArray().put(new JSONObject().put("number","1"))).put("animeInfo",new JSONObject().put("counts",new JSONObject()).put("anilistId",21));activity.openWatch(data.toString());WebView player=(WebView)field("player");assertNotNull(player);
  String auth=accounts.begin("discord"),state=AccountFlows542Test.query(auth).get("state");Intent incoming=new Intent(Intent.ACTION_VIEW,Uri.parse(AccountOAuth.DISCORD_REDIRECT+"?code=fake-code&state="+state));activity.onNewIntent(incoming);idle();
  assertNull("callback URI is consumed, not replayed",incoming.getData());assertSame("callback must not close or reload player",player,field("player"));assertTrue(accounts.status().getJSONObject("connections").getJSONObject("discord").getBoolean("connected"));
  WebView ui=(WebView)field("ui");String js=Shadows.shadowOf(ui).getLastEvaluatedJavascript();assertTrue(js.contains("kuroAccountEvent"));assertFalse(js.contains("fake-access-token"));assertFalse(js.contains("fake-code"));
 }
 @Test public void onlyKnownHelpLinksAndPublicRedirectsAreExposed()throws Exception {
  MainActivity.Bridge bridge=activity.new Bridge();bridge.openAccountHelp("https://evil.example");bridge.copyAccountRedirect("secret");idle();assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
  bridge.openAccountHelp("anilist");idle();Intent opened=Shadows.shadowOf(activity).getNextStartedActivity();assertEquals("https://anilist.co/settings/developer",opened.getDataString());
  bridge.copyAccountRedirect("mal");idle();android.content.ClipboardManager clip=(android.content.ClipboardManager)activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);assertEquals(AccountOAuth.MAL_REDIRECT,clip.getPrimaryClip().getItemAt(0).getText().toString());
  bridge.copyAccountRedirect("google");bridge.openAccountHelp("supabase");idle();assertEquals(AccountOAuth.MAL_REDIRECT,clip.getPrimaryClip().getItemAt(0).getText().toString());assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
 }
}
