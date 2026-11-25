package app.lovelycrxshx.kuro;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import static org.junit.Assert.*;

/** In-memory provider responses only; no real accounts or live network writes. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class TrackerSync553Test {
 static class Fake implements AccountTransport {
  final List<String> methods=new ArrayList<>(),urls=new ArrayList<>(),bodies=new ArrayList<>(),auth=new ArrayList<>();
  boolean exists=true,adultSearch=false,missingListId=false,graphError=false;int aid=101,mid=201,total=12,progress=3,writes=0;
  String aniStatus="CURRENT",malStatus="watching";JSONObject lastMutation;Map<String,String> lastForm;
  public JSONObject request(String method,String url,Map<String,String> headers,String body)throws Exception {
   methods.add(method);urls.add(url);bodies.add(body==null?"":body);auth.add(headers.getOrDefault("Authorization",""));
   if(url.equals("https://graphql.anilist.co")){
    JSONObject req=new JSONObject(body);String query=req.getString("query");
    if(graphError)return new JSONObject().put("errors",new JSONArray().put(new JSONObject().put("message","failed")));
    if(query.contains("Page(page:1")){
     JSONArray media=new JSONArray().put(new JSONObject().put("id",101).put("idMal",201).put("isAdult",false).put("format","TV").put("seasonYear",2025).put("episodes",12).put("title",new JSONObject().put("english","Alpha").put("romaji","Alpha Romaji")))
      .put(new JSONObject().put("id",102).put("isAdult",true).put("title",new JSONObject().put("english","Hidden")))
      .put(new JSONObject().put("id",103).put("title",new JSONObject().put("english","Unclassified")));
     return new JSONObject().put("data",new JSONObject().put("Page",new JSONObject().put("media",media)));
    }
    if(query.contains("SaveMediaListEntry")){
     writes++;lastMutation=req.getJSONObject("variables");
     if(lastMutation.has("status"))aniStatus=lastMutation.getString("status");if(lastMutation.has("progress"))progress=lastMutation.getInt("progress");exists=true;
     return new JSONObject().put("data",new JSONObject().put("SaveMediaListEntry",new JSONObject().put("id",901).put("status",aniStatus).put("progress",progress)));
    }
    JSONObject media=new JSONObject().put("id",aid).put("idMal",mid).put("type","ANIME").put("episodes",total).put("title",new JSONObject().put("english","Alpha").put("romaji","Alpha Romaji")).put("synonyms",new JSONArray().put("Alpha Alternate"));
    if(exists&&query.contains("mediaListEntry")){JSONObject entry=new JSONObject().put("status",aniStatus).put("progress",progress);if(!missingListId)entry.put("id",901);media.put("mediaListEntry",entry);}
    return new JSONObject().put("data",new JSONObject().put("Media",media));
   }
   if("GET".equals(method)&&url.startsWith("https://api.myanimelist.net/v2/anime/")){
    JSONObject m=new JSONObject().put("id",mid).put("title","Alpha").put("alternative_titles",new JSONObject().put("en","Alpha").put("synonyms",new JSONArray().put("Alpha Alternate"))).put("num_episodes",total);
    if(exists)m.put("my_list_status",new JSONObject().put("status",malStatus).put("num_episodes_watched",progress).put("score",9).put("comments","leave this alone"));return m;
   }
   if("PUT".equals(method)&&url.matches("https://api\\.myanimelist\\.net/v2/anime/[0-9]+/my_list_status")){
    writes++;lastForm=parse(body);if(lastForm.containsKey("status"))malStatus=lastForm.get("status");if(lastForm.containsKey("num_watched_episodes"))progress=Integer.parseInt(lastForm.get("num_watched_episodes"));exists=true;return new JSONObject().put("status",malStatus);
   }
   throw new AssertionError("Unexpected fake request: "+method+" "+url);
  }
 }
 static Map<String,String> parse(String body)throws Exception{Map<String,String> m=new LinkedHashMap<>();for(String x:body.split("&")){String[] v=x.split("=",2);m.put(URLDecoder.decode(v[0],"UTF-8"),URLDecoder.decode(v.length>1?v[1]:"","UTF-8"));}return m;}
 static JSONObject req(String status,String intent,int... seen)throws Exception{JSONArray a=new JSONArray();for(int n:seen)a.put(n);return new JSONObject().put("anilistId",101).put("malId",201).put("title","Alpha").put("titles",new JSONArray()).put("status",status).put("intent",intent).put("watched",a);}
 interface Action{void run()throws Exception;}
 static void rejected(Action action)throws Exception{try{action.run();fail("Expected safe rejection");}catch(AccountError expected){}}

 @Test public void allFiveStatusesMapCorrectlyForBothProviders()throws Exception{
  String[] local={"watching","planned","completed","on_hold","dropped"};String[] ani={"CURRENT","PLANNING","COMPLETED","PAUSED","DROPPED"};String[] mal={"watching","plan_to_watch","completed","on_hold","dropped"};
  for(int i=0;i<local.length;i++){
   Fake f=new Fake();f.aniStatus=ani[i].equals("CURRENT")?"DROPPED":"CURRENT";new TrackerSync553(f).update("anilist",req(local[i],"status"),"ani-token");
   assertEquals(ani[i],f.lastMutation.getString("status"));assertEquals(901,f.lastMutation.getInt("entry"));assertFalse(f.lastMutation.has("media"));assertFalse(f.lastMutation.has("score"));
   f=new Fake();f.malStatus=mal[i].equals("watching")?"dropped":"watching";new TrackerSync553(f).update("mal",req(local[i],"status"),"mal-token");
   assertEquals(mal[i],f.lastForm.get("status"));assertFalse(f.lastForm.containsKey("score"));assertFalse(f.lastForm.containsKey("comments"));assertTrue(f.methods.contains("PUT"));
  }
 }
 @Test public void onlyContiguousWatchedEpisodesAdvanceAndProgressNeverIntentionallyDrops()throws Exception{
  Fake f=new Fake();JSONObject r=new TrackerSync553(f).update("anilist",req("watching","watch",4,6),"ani-token");assertEquals(4,r.getInt("progress"));assertEquals(4,f.lastMutation.getInt("progress"));assertFalse(f.lastMutation.has("status"));
  f=new Fake();f.progress=8;r=new TrackerSync553(f).update("mal",req("watching","watch",1,2,3),"mal-token");assertEquals(8,r.getInt("progress"));assertEquals(0,f.writes);
  f=new Fake();f.progress=11;r=new TrackerSync553(f).update("mal",req("watching","watch",12),"mal-token");assertEquals(12,r.getInt("progress"));assertEquals("completed",f.lastForm.get("status"));assertEquals("12",f.lastForm.get("num_watched_episodes"));
 }
 @Test public void completedSetsKnownTotalButUnknownTotalIsNeverInvented()throws Exception{
  Fake f=new Fake();new TrackerSync553(f).update("mal",req("completed","status"),"mal-token");assertEquals("12",f.lastForm.get("num_watched_episodes"));
  f=new Fake();f.total=0;new TrackerSync553(f).update("anilist",req("completed","status"),"ani-token");assertFalse(f.lastMutation.has("progress"));
 }
 @Test public void backfillPreservesConflictingRemoteStatusAndOtherFields()throws Exception{
  Fake f=new Fake();f.aniStatus="DROPPED";JSONObject r=new TrackerSync553(f).update("anilist",req("watching","backfill",4),"ani-token");assertTrue(r.getBoolean("conflict"));assertEquals("DROPPED",r.getString("status"));assertEquals(4,f.lastMutation.getInt("progress"));assertFalse(f.lastMutation.has("status"));assertFalse(f.lastMutation.has("notes"));
  f=new Fake();f.malStatus="on_hold";r=new TrackerSync553(f).update("mal",req("planned","backfill"),"mal-token");assertTrue(r.getBoolean("conflict"));assertEquals(0,f.writes);
 }
 @Test public void newEntryUsesMediaIdWhileExistingEntryUsesListEntryId()throws Exception{
  Fake f=new Fake();f.exists=false;new TrackerSync553(f).update("anilist",req("planned","backfill"),"ani-token");assertEquals(101,f.lastMutation.getInt("media"));assertFalse(f.lastMutation.has("entry"));assertEquals("PLANNING",f.lastMutation.getString("status"));
  f=new Fake();f.missingListId=true;Fake bad=f;rejected(()->new TrackerSync553(bad).update("anilist",req("dropped","status"),"ani-token"));assertEquals(0,f.writes);
 }
 @Test public void incompatibleIdsAndUnverifiedTitlesNeverWrite()throws Exception{
  Fake f=new Fake();JSONObject wrong=req("watching","status").put("malId",202);rejected(()->new TrackerSync553(f).update("anilist",wrong,"ani-token"));assertEquals(0,f.writes);
  Fake f2=new Fake();JSONObject other=req("watching","status").put("title","Different Season");rejected(()->new TrackerSync553(f2).update("mal",other,"mal-token"));assertEquals(0,f2.writes);
  Fake f3=new Fake();JSONObject empty=req("watching","status").put("anilistId",0).put("malId",0);rejected(()->new TrackerSync553(f3).update("mal",empty,"mal-token"));assertEquals(0,f3.methods.size());
  Fake f4=new Fake();new TrackerSync553(f4).update("mal",req("dropped","status").put("title","An alternate user-confirmed title").put("userMatched",true),"mal-token");assertEquals(1,f4.writes);
 }
 @Test public void malTokensNeverGoToAnilistForMapping()throws Exception{
  Fake f=new Fake();new TrackerSync553(f).update("mal",req("dropped","status"),"secret-mal-token");
  for(int i=0;i<f.urls.size();i++)if(f.urls.get(i).equals("https://graphql.anilist.co"))assertEquals("",f.auth.get(i));
 }
 @Test public void manualSearchHidesAdultAndUnclassifiedAndKeepsDistinctIds()throws Exception{
  Fake f=new Fake();JSONArray list=new TrackerSync553(f).search("Alpha").getJSONArray("items");assertEquals(1,list.length());assertEquals(101,list.getJSONObject(0).getInt("anilistId"));assertEquals(201,list.getJSONObject(0).getInt("malId"));
  rejected(()->new TrackerSync553(f).search("x"));
 }
 @Test public void ownerBindingRejectsAccountChangesBeforeAnyProviderRequest()throws Exception{
  AccountFlows542Test.Vault v=new AccountFlows542Test.Vault();Fake f=new Fake();
  v.put("config",new JSONObject().put("anilist","12345").toString());
  v.put("session.anilist",new JSONObject().put("access","fake-token").put("id","98765").put("name","Test").put("method","anilist").put("clientId","12345").put("expiresAt",0).toString());
  KuroAccounts accounts=new KuroAccounts(v,f);String owner=accounts.status().getJSONObject("connections").getJSONObject("anilist").getString("syncOwner");
  JSONObject payload=req("dropped","status").put("provider","anilist").put("owner","another-owner");rejected(()->accounts.syncTracker(payload));assertEquals(0,f.methods.size());
  payload.put("owner",owner);accounts.syncTracker(payload);assertEquals(1,f.writes);
  JSONObject session=new JSONObject(v.get("session.anilist"));session.put("id","11111");v.put("session.anilist",session.toString());rejected(()->accounts.syncTracker(payload));assertEquals(1,f.writes);
  assertFalse(accounts.status().toString().contains("fake-token"));
 }
 @Test public void malformedOrFailedResponsesDoNotTriggerWrites()throws Exception{
  Fake f=new Fake();f.graphError=true;rejected(()->new TrackerSync553(f).update("anilist",req("completed","status"),"ani-token"));assertEquals(0,f.writes);
  Fake f2=new Fake();rejected(()->new TrackerSync553(f2).update("mal",req("watching","watch",5001),"mal-token"));assertEquals(0,f2.methods.size());
  Fake f3=new Fake();JSONObject fractional=req("watching","status").put("anilistId",101.5);rejected(()->new TrackerSync553(f3).update("anilist",fractional,"ani-token"));assertEquals(0,f3.methods.size());
  Fake f4=new Fake();JSONObject fractionalEp=req("watching","watch").put("watched",new JSONArray().put(3.5));rejected(()->new TrackerSync553(f4).update("anilist",fractionalEp,"ani-token"));assertEquals(0,f4.methods.size());
 }
}
