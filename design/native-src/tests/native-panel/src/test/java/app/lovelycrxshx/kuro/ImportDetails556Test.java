package app.lovelycrxshx.kuro;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

/** Native read-only import details with fake HTTP/vault; not a phone or live provider. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class ImportDetails556Test {
 static class Fake implements AccountTransport {
  final List<String> methods=new ArrayList<>(),urls=new ArrayList<>(),auth=new ArrayList<>(),bodies=new ArrayList<>();
  int returnId=123;String rating="pg_13";Object adult=Boolean.FALSE;boolean graphError=false;String image="https://images.example/cover.jpg";
  public JSONObject request(String method,String url,Map<String,String> headers,String body)throws Exception{
   methods.add(method);urls.add(url);auth.add(headers.getOrDefault("Authorization",""));bodies.add(body==null?"":body);
   if(url.startsWith("https://api.myanimelist.net/v2/anime/")){
    JSONArray genres=new JSONArray(),studios=new JSONArray();for(int i=0;i<15;i++){genres.put(new JSONObject().put("name","Genre "+i));studios.put(new JSONObject().put("name","Studio "+i));}
    return new JSONObject().put("id",returnId).put("title","Exact MAL title").put("main_picture",new JSONObject().put("large",image)).put("synopsis","A brief synopsis").put("media_type","tv").put("status","finished_airing").put("num_episodes",12).put("start_date","2020-01-01").put("rating",rating).put("genres",genres).put("studios",studios);
   }
   if(url.equals("https://graphql.anilist.co")){
    if(graphError)return new JSONObject().put("errors",new JSONArray().put(new JSONObject().put("message","fail")));
    JSONObject media=new JSONObject().put("id",returnId).put("title",new JSONObject().put("romaji","Exact AniList title")).put("description","A synopsis").put("format","TV").put("status","FINISHED").put("episodes",12).put("startDate",new JSONObject().put("year",2022)).put("coverImage",new JSONObject().put("large",image)).put("genres",new JSONArray().put("Action")).put("studios",new JSONObject().put("nodes",new JSONArray().put(new JSONObject().put("name","Studio"))));
    if(adult!=null)media.put("isAdult",adult);
    return new JSONObject().put("data",new JSONObject().put("Media",media));
   }
   throw new AssertionError("Unexpected fake request "+method+" "+url);
  }
 }
 static AccountFlows542Test.Vault vault()throws Exception{
  AccountFlows542Test.Vault v=new AccountFlows542Test.Vault();v.put("config",new JSONObject().put("mal","testmalclientid1234567890").toString());v.put("session.mal",new JSONObject().put("access","fake-mal-token").put("refresh","").put("clientId","testmalclientid1234567890").put("expiresAt",System.currentTimeMillis()+3600000).put("id","42").toString());return v;
 }
 interface Work{void run()throws Exception;}
 static void rejected(Work work)throws Exception{try{work.run();fail("expected safe rejection");}catch(AccountError expected){}}
 @Test public void malDetailsUseAuthenticatedExactReadOnlyIdAndBoundFields()throws Exception{
  Fake f=new Fake();AccountFlows542Test.Vault v=vault();Map<String,String> before=new HashMap<>(v.values);JSONObject result=new KuroAccounts(v,f).importDetails("mal","123");
  assertEquals("mal",result.getString("provider"));assertEquals("123",result.getString("sourceId"));assertEquals("Exact MAL title",result.getString("title"));assertEquals(12,result.getInt("episodes"));assertFalse(result.getBoolean("adult"));assertEquals(10,result.getJSONArray("genres").length());assertEquals(10,result.getJSONArray("studios").length());assertEquals("GET",f.methods.get(0));assertTrue(f.urls.get(0).startsWith("https://api.myanimelist.net/v2/anime/123?"));assertEquals("Bearer fake-mal-token",f.auth.get(0));assertFalse(result.toString().contains("fake-mal-token"));assertEquals(before,v.values);
 }
 @Test public void malformedIdsAndUnknownProvidersFailBeforeAnyRequest()throws Exception{
  Fake f=new Fake();KuroAccounts a=new KuroAccounts(vault(),f);for(String id:new String[]{"","0","-1","01","1/2","1000000000","123?x=1","1e2"})rejected(()->a.importDetails("mal",id));rejected(()->a.importDetails("other","123"));assertTrue(f.urls.isEmpty());
 }
 @Test public void mismatchedProviderIdNeverReturnsDetails()throws Exception{
  Fake f=new Fake();f.returnId=124;KuroAccounts a=new KuroAccounts(vault(),f);rejected(()->a.importDetails("mal","123"));rejected(()->a.importDetails("anilist","123"));assertEquals(2,f.urls.size());
 }
 @Test public void malNeedsSessionAndNeverFallsBackToUnauthenticatedOrOtherProvider()throws Exception{
  Fake f=new Fake();KuroAccounts a=new KuroAccounts(new AccountFlows542Test.Vault(),f);rejected(()->a.importDetails("mal","123"));assertTrue(f.urls.isEmpty());
 }
 @Test public void anilistDetailsArePublicExactQueryWithoutTokenOrMutation()throws Exception{
  Fake f=new Fake();AccountFlows542Test.Vault v=vault();Map<String,String> before=new HashMap<>(v.values);JSONObject r=new KuroAccounts(v,f).importDetails("anilist","123");assertEquals("Exact AniList title",r.getString("title"));assertEquals("2022",r.getString("date"));assertEquals("Action",r.getJSONArray("genres").getString(0));assertEquals("Studio",r.getJSONArray("studios").getString(0));assertEquals("",f.auth.get(0));JSONObject request=new JSONObject(f.bodies.get(0));assertEquals(123,request.getJSONObject("variables").getInt("id"));assertTrue(request.getString("query").startsWith("query("));assertFalse(request.getString("query").contains("mutation"));assertEquals(before,v.values);
 }
 @Test public void missingOrMalformedClassificationStaysUnknown()throws Exception{
  Fake f=new Fake();KuroAccounts a=new KuroAccounts(vault(),f);f.adult=null;assertTrue(a.importDetails("anilist","123").isNull("adult"));f.adult="unexpected";assertTrue(a.importDetails("anilist","123").isNull("adult"));f.adult=Boolean.TRUE;assertTrue(a.importDetails("anilist","123").getBoolean("adult"));f.rating="";assertTrue(a.importDetails("mal","123").isNull("adult"));f.rating="rx";assertTrue(a.importDetails("mal","123").getBoolean("adult"));
 }
 @Test public void unsafePicturesAreDiscardedAndGraphErrorsFailClosed()throws Exception{
  Fake f=new Fake();KuroAccounts a=new KuroAccounts(vault(),f);for(String image:new String[]{"http://images.example/pic.jpg","https://user:pass@images.example/pic.jpg","https://images.example:444/pic.jpg","javascript:alert(1)"}){f.image=image;assertEquals("",a.importDetails("anilist","123").getString("image"));assertEquals("",a.importDetails("mal","123").getString("image"));}f.graphError=true;rejected(()->a.importDetails("anilist","123"));
 }
}
