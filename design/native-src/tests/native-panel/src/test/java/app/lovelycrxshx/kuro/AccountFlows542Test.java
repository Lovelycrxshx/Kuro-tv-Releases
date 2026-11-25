package app.lovelycrxshx.kuro;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

/** Fake-provider tests only. No live credentials, accounts or phone are involved. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class AccountFlows542Test {
 static class Vault implements AccountVault {
  final Map<String,String> values=new HashMap<>();public String get(String k){return values.getOrDefault(k,"");}public void put(String k,String v){values.put(k,v);}public void remove(String k){values.remove(k);}
 }
 static Map<String,String> query(String url)throws Exception {
  String raw=new URI(url).getRawQuery();Map<String,String> m=new LinkedHashMap<>();if(raw!=null)for(String p:raw.split("&")){String[] x=p.split("=",2);m.put(URLDecoder.decode(x[0],"UTF-8"),URLDecoder.decode(x.length>1?x[1]:"","UTF-8"));}return m;
 }
 static Map<String,String> form(String text)throws Exception {return query("https://valid.example/?"+text);}
 static JSONObject tokens()throws Exception{return new JSONObject().put("access_token","fake-access-token").put("refresh_token","fake-refresh-token").put("expires_in",3600).put("user",new JSONObject().put("id","user-123").put("email","test@example.invalid"));}
 static class Fake implements AccountTransport {
  final List<String> requests=new ArrayList<>();final List<String> bodies=new ArrayList<>();boolean failMalSecond=false;int malPages=0;
  public JSONObject request(String method,String url,Map<String,String> headers,String body)throws Exception {
   requests.add(method+" "+url);bodies.add(body==null?"":body);
   if(url.contains("discord.com/api/oauth2/token"))return tokens();
   if(url.contains("discord.com/api/users/@me"))return new JSONObject().put("id","11111").put("username","Fake Discord");
   if(url.contains("myanimelist.net/v1/oauth2/token"))return tokens();
   if(url.contains("api.myanimelist.net/v2/users/@me/animelist")){
    malPages++;int offset=Integer.parseInt(query(url).get("offset"));if(offset>0&&failMalSecond)throw new AccountError("Provider blocked or rate-limited the request (HTTP 429). Wait and try manually.");
    JSONObject node=new JSONObject().put("id",offset==0?111:222).put("title",offset==0?"Anime one":"Anime two").put("rating",offset==0?"pg_13":"rx").put("num_episodes",12).put("main_picture",new JSONObject().put("medium","https://images.example/cover.jpg"));
    JSONObject row=new JSONObject().put("node",node).put("list_status",new JSONObject().put("status",offset==0?"watching":"completed").put("num_episodes_watched",offset==0?3:12));
    return new JSONObject().put("data",new JSONArray().put(row)).put("paging",offset==0?new JSONObject().put("next","https://do-not-follow.example/ignored"):new JSONObject());
   }
   if(url.endsWith("api.myanimelist.net/v2/users/@me"))return new JSONObject().put("id",55).put("name","Fake MAL");
   if(url.equals("https://graphql.anilist.co")){
    JSONObject payload=new JSONObject(body);String q=payload.optString("query");if(q.contains("Viewer"))return new JSONObject().put("data",new JSONObject().put("Viewer",new JSONObject().put("id",900).put("name","Fake AniList")));
    int chunk=payload.getJSONObject("variables").getInt("chunk");JSONObject media=new JSONObject().put("id",chunk==1?301:302).put("idMal",chunk==1?111:222).put("isAdult",chunk==2).put("episodes",24).put("title",new JSONObject().put("english",chunk==1?"Ani title":"Adult title")).put("coverImage",new JSONObject().put("large","https://images.example/anilist.jpg"));
    JSONObject entry=new JSONObject().put("status",chunk==1?"CURRENT":"PLANNING").put("progress",chunk==1?7:0).put("media",media);JSONObject list=new JSONObject().put("entries",new JSONArray().put(entry));return new JSONObject().put("data",new JSONObject().put("MediaListCollection",new JSONObject().put("hasNextChunk",chunk==1).put("lists",new JSONArray().put(list))));
   }
   throw new AssertionError("Unexpected fake request: "+method+" "+url);
  }
 }
 static JSONObject config()throws Exception{return new JSONObject().put("mal","testmalclientid1234567890").put("discord","123456789012345678").put("anilist","12345");}
 static KuroAccounts make(Vault v,Fake f)throws Exception {KuroAccounts a=new KuroAccounts(v,f);a.saveConfig(config());return a;}
 static String callback(String redirect,String url)throws Exception{return redirect+"?code=fake-code&state="+query(url).get("state");}
 static void expectError(Throwing work)throws Exception {try{work.run();fail("expected safe failure");}catch(AccountError expected){}}
 interface Throwing{void run()throws Exception;}

 @Test public void publicDefaultsDoNotReplaceExistingIdentifiersOrSessions()throws Exception {
  Vault empty=new Vault();empty.put("session.mal","{\"access\":\"saved-private-session\",\"name\":\"Saved\"}");empty.put("config","{\"discord\":\"123456789012345678\"}");
  KuroAccounts existing=new KuroAccounts(empty,new Fake());existing.status();assertTrue(empty.get("session.mal").contains("saved-private-session"));assertEquals("{\"discord\":\"123456789012345678\"}",empty.get("config"));
  empty=new Vault();KuroAccounts fresh=new KuroAccounts(empty,new Fake());JSONObject defaults=fresh.status().getJSONObject("config");
  assertEquals("1569077670074122250",defaults.getString("discord"));assertEquals("37160cd5a180308c9009afb7faa4e341",defaults.getString("mal"));assertEquals("56440",defaults.getString("anilist"));
  fresh.saveConfig(new JSONObject().put("discord","123456789012345678").put("anilist","12345"));
  JSONObject kept=fresh.status().getJSONObject("config");assertEquals("123456789012345678",kept.getString("discord"));assertEquals("12345",kept.getString("anilist"));assertEquals("37160cd5a180308c9009afb7faa4e341",kept.getString("mal"));
 }
 @Test public void oauthUrlsUseExpectedPkceAndMinimalScopes()throws Exception {
  String state=AccountOAuth.random(),verifier=AccountOAuth.random();assertEquals(43,state.length());assertNotEquals(state,verifier);
  Map<String,String>d=query(AccountOAuth.authorize("discord","123456789012345678",state,verifier));assertEquals("identify",d.get("scope"));assertEquals("S256",d.get("code_challenge_method"));assertEquals(AccountOAuth.challenge(verifier),d.get("code_challenge"));assertNotEquals(verifier,d.get("code_challenge"));assertEquals(AccountOAuth.DISCORD_REDIRECT,d.get("redirect_uri"));
  Map<String,String>m=query(AccountOAuth.authorize("mal","testmalclientid1234567890",state,verifier));assertEquals("plain",m.get("code_challenge_method"));assertEquals(verifier,m.get("code_challenge"));assertEquals(AccountOAuth.MAL_REDIRECT,m.get("redirect_uri"));
  expectError(()->AccountOAuth.authorize("google","",state,verifier));
  Map<String,String>a=query(AccountOAuth.authorize("anilist","12345",state,verifier));assertEquals(2,a.size());assertEquals("12345",a.get("client_id"));assertEquals("token",a.get("response_type"));assertFalse(a.containsKey("redirect_uri"));assertFalse(a.containsKey("state"));assertEquals("https://anilist.co/api/v2/oauth/pin",AccountOAuth.ANILIST_PIN);
 }
 @Test public void callbackRejectsSpoofingFragmentsDuplicatesAndBadState()throws Exception {
  String state=AccountOAuth.random();AccountOAuth.Callback cb=AccountOAuth.parseCallback(AccountOAuth.MAL_REDIRECT+"?code=abc%2B%2F%3D&state="+state);assertEquals("abc+/=",cb.code);
  expectError(()->AccountOAuth.parseCallback("app.lovelycrxshx.kuro://evil/mal?code=x&state="+state));
  expectError(()->AccountOAuth.parseCallback(AccountOAuth.MAL_REDIRECT+"?code=x&state="+state+"#access_token=secret"));
  expectError(()->AccountOAuth.parseCallback(AccountOAuth.MAL_REDIRECT+"?code=x&state="+state+"&state="+state));
  assertTrue(AccountOAuth.sameState(state,state));assertFalse(AccountOAuth.sameState(state,AccountOAuth.random()));
 }
 @Test public void configurationAndHttpRejectUnsafeProviders()throws Exception {
  Vault v=new Vault();Fake f=new Fake();KuroAccounts a=make(v,f);
  expectError(()->a.saveConfig(new JSONObject().put("discord","bad-id")));
  expectError(()->new AccountHttp().request("GET","https://discord.com.evil.example/steal",Collections.emptyMap(),null));
  expectError(()->new AccountHttp().request("GET","http://discord.com/api/users/@me",Collections.emptyMap(),null));
  expectError(()->new AccountHttp().request("GET","https://abcdefghijklmnop.supabase.co/auth/v1/settings",Collections.emptyMap(),null));
  assertFalse(a.status().toString().contains("supabase"));
 }
 @Test public void discordValidatesStateThenExchangesOnceWithoutSecret()throws Exception {
  Vault v=new Vault();Fake f=new Fake();KuroAccounts a=make(v,f);String auth=a.begin("discord"),state=query(auth).get("state");
  expectError(()->a.callback(AccountOAuth.DISCORD_REDIRECT+"?code=fake-code&state="+AccountOAuth.random()));assertEquals(0,f.requests.size());
  assertEquals("discord",a.callback(AccountOAuth.DISCORD_REDIRECT+"?code=fake-code&state="+state).getString("kind"));assertEquals(2,f.requests.size());
  Map<String,String> token=form(f.bodies.get(0));assertEquals("authorization_code",token.get("grant_type"));assertTrue(token.containsKey("code_verifier"));assertFalse(token.containsKey("client_secret"));assertEquals(query(auth).get("code_challenge"),AccountOAuth.challenge(token.get("code_verifier")));
  assertTrue(a.status().getJSONObject("connections").getJSONObject("discord").getBoolean("connected"));assertFalse(a.status().toString().contains("fake-access-token"));expectError(()->a.callback(AccountOAuth.DISCORD_REDIRECT+"?code=fake-code&state="+state));assertEquals(2,f.requests.size());
 }
 @Test public void malImportPaginatesPreservesStatusAndStopsOnRateLimit()throws Exception {
  Vault v=new Vault();Fake f=new Fake();KuroAccounts a=make(v,f);String auth=a.begin("mal");a.callback(callback(AccountOAuth.MAL_REDIRECT,auth));
  JSONObject r=a.importList("mal");assertFalse(r.getBoolean("partial"));assertEquals(2,r.getJSONArray("items").length());JSONObject first=r.getJSONArray("items").getJSONObject(0);assertEquals("watching",first.getString("status"));assertEquals(3,first.getInt("progress"));assertFalse(first.getBoolean("adult"));assertTrue(r.getJSONArray("items").getJSONObject(1).getBoolean("adult"));
  assertEquals(2,f.malPages);assertFalse(f.requests.toString().contains("do-not-follow.example"));
  f.failMalSecond=true;f.malPages=0;JSONObject partial=a.importList("mal");assertTrue(partial.getBoolean("partial"));assertEquals(1,partial.getJSONArray("items").length());assertTrue(partial.getString("reason").contains("429"));assertEquals(2,f.malPages);
 }
 @Test public void anilistPinIdentityAndChunkedImportAreReadOnly()throws Exception {
  Vault v=new Vault();Fake f=new Fake();KuroAccounts a=make(v,f);a.connectAniList("fake-anilist-token-abcdefghijklmnop");JSONObject r=a.importList("anilist");assertEquals(2,r.getJSONArray("items").length());JSONObject row=r.getJSONArray("items").getJSONObject(0);assertEquals(301,row.getLong("anilistId"));assertEquals(111,row.getLong("malId"));assertEquals("CURRENT",row.getString("status"));assertEquals(7,row.getInt("progress"));assertFalse(row.getBoolean("adult"));assertFalse(f.bodies.toString().contains("mutation"));assertFalse(a.status().toString().contains("fake-anilist-token"));
 }
 @Test public void localEmailDoesNotUseNetworkAndOldSessionRemainsInactive()throws Exception {
  Vault v=new Vault();Fake f=new Fake();KuroAccounts a=make(v,f);v.put("session.supabase","legacy-encrypted-session");
  a.emailLogin("test@example.invalid","fake-password",true);assertEquals(0,f.requests.size());
  JSONObject c=a.status().getJSONObject("connections");assertTrue(c.getJSONObject("local").getBoolean("connected"));assertFalse(c.has("supabase"));
  a.disconnect("local");assertFalse(a.status().getJSONObject("connections").getJSONObject("local").getBoolean("connected"));
  a.emailLogin("test@example.invalid","fake-password",false);assertTrue(a.status().getJSONObject("connections").getJSONObject("local").getBoolean("connected"));
  assertEquals("legacy-encrypted-session",v.get("session.supabase"));assertEquals(0,f.requests.size());
 }
 @Test public void changingIdsDisconnectsBoundTokensAndRefreshStaysOnOriginalProvider()throws Exception {
  Vault v=new Vault();Fake f=new Fake();KuroAccounts a=make(v,f);String auth=a.begin("mal");a.callback(callback(AccountOAuth.MAL_REDIRECT,auth));JSONObject s=new JSONObject(v.get("session.mal")).put("expiresAt",1);v.put("session.mal",s.toString());a.importList("mal");assertTrue(f.bodies.toString().contains("grant_type=refresh_token"));
  a.saveConfig(new JSONObject().put("mal","anotherpublicclient12345"));assertFalse(a.status().getJSONObject("connections").getJSONObject("mal").getBoolean("connected"));expectError(()->a.importList("mal"));
 }
}
