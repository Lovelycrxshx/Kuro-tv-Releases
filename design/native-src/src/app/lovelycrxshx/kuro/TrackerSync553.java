package app.lovelycrxshx.kuro;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;

/** Outbound-only, ID-checked MAL/AniList synchronization. Tokens stay in native code. */
final class TrackerSync553 {
 private final AccountTransport http;
 private static final int MAX_EPISODE=5000;
 TrackerSync553(AccountTransport transport){http=transport;}
 static String ownerKey(String provider,JSONObject session) throws Exception {
  String id=session.optString("id","");if(!id.matches("[0-9]{1,30}"))return "";
  String value=provider+"\n"+id+"\n"+session.optString("clientId","");
  return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))).substring(0,24);
 }
 private static Map<String,String> headers(String token,boolean json){
  Map<String,String> h=new LinkedHashMap<>();if(token!=null&&!token.isEmpty())h.put("Authorization","Bearer "+token);
  if(json)h.put("Content-Type","application/json");return h;
 }
 private JSONObject graph(String token,String query,JSONObject variables) throws Exception {
  JSONObject body=new JSONObject().put("query",query).put("variables",variables);
  JSONObject result=http.request("POST","https://graphql.anilist.co",headers(token,true),body.toString());
  if(result.has("errors")&&!result.isNull("errors"))throw new AccountError("AniList could not verify the anime or complete the update. Check the exact match and try again.");
  JSONObject data=result.optJSONObject("data");if(data==null)throw new AccountError("AniList returned an incomplete response.");return data;
 }
 private static int id(JSONObject obj,String key) throws Exception {
  Object raw=obj.opt(key);if(raw==null||raw==JSONObject.NULL)return 0;if(!(raw instanceof Number))throw new AccountError("This catalogue ID is invalid. Choose the exact anime again.");double value=((Number)raw).doubleValue();if(!Double.isFinite(value)||value<0||value>Integer.MAX_VALUE||value!=Math.rint(value))throw new AccountError("This catalogue ID is invalid. Choose the exact anime again.");return (int)value;
 }
 private static String normalized(String input){
  if(input==null)return "";
  return Normalizer.normalize(input,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
   .replaceAll("(?i)\\b(?:dub|sub|dubbed|subbed)\\b","")
   .replaceAll("[^\\p{L}\\p{N}]","");
 }
 private static void add(Set<String> names,String s){String x=normalized(s);if(x.length()>1)names.add(x);}
 private static Set<String> requestedNames(JSONObject request){
  Set<String> out=new HashSet<>();add(out,request.optString("title",""));JSONArray titles=request.optJSONArray("titles");
  if(titles!=null)for(int i=0;i<Math.min(12,titles.length());i++)add(out,titles.optString(i,""));return out;
 }
 private static void metadataNames(Set<String> out,JSONObject metadata){
  if(metadata==null)return;Object title=metadata.opt("title");if(title instanceof String)add(out,(String)title);
  if(title instanceof JSONObject){JSONObject t=(JSONObject)title;for(String k:new String[]{"english","romaji","native"})add(out,t.optString(k,""));}
  JSONObject alt=metadata.optJSONObject("alternative_titles");if(alt!=null){add(out,alt.optString("en",""));add(out,alt.optString("ja",""));JSONArray a=alt.optJSONArray("synonyms");if(a!=null)for(int i=0;i<Math.min(a.length(),80);i++)add(out,a.optString(i,""));}
  JSONArray aliases=metadata.optJSONArray("synonyms");if(aliases!=null)for(int i=0;i<Math.min(aliases.length(),80);i++)add(out,aliases.optString(i,""));
 }
 private static void verifyNames(JSONObject request,JSONObject a,JSONObject m) throws Exception {
  if(request.optBoolean("userMatched",false))return;
  Set<String> requested=requestedNames(request),provided=new HashSet<>();metadataNames(provided,a);metadataNames(provided,m);
  for(String s:requested)if(provided.contains(s))return;
  throw new AccountError("MATCH_REQUIRED: The title and catalogue ID could not be verified together. Choose the exact season in Tracker sync.");
 }
 private static String remoteStatus(String local,String provider) throws Exception {
  switch(local){
   case "watching":return "anilist".equals(provider)?"CURRENT":"watching";
   case "planned":return "anilist".equals(provider)?"PLANNING":"plan_to_watch";
   case "completed":return "anilist".equals(provider)?"COMPLETED":"completed";
   case "on_hold":return "anilist".equals(provider)?"PAUSED":"on_hold";
   case "dropped":return "anilist".equals(provider)?"DROPPED":"dropped";
   default:throw new AccountError("Choose a supported Kuro list status.");
  }
 }
 private static int total(JSONObject meta,String key){int n=meta==null?0:meta.optInt(key,0);return n>0&&n<=MAX_EPISODE?n:0;}
 private static BitSet watched(JSONObject request) throws Exception {
  JSONArray a=request.optJSONArray("watched");BitSet seen=new BitSet(MAX_EPISODE+1);if(a==null)return seen;
  if(a.length()>MAX_EPISODE)throw new AccountError("Too many episode numbers in one update.");
  for(int i=0;i<a.length();i++){Object raw=a.opt(i);if(!(raw instanceof Number))throw new AccountError("Invalid watched episode number.");double value=((Number)raw).doubleValue();if(!Double.isFinite(value)||value<1||value>MAX_EPISODE||value!=Math.rint(value))throw new AccountError("Invalid watched episode number.");seen.set((int)value);}return seen;
 }
 private static int nextProgress(int existing,BitSet episodes){int n=existing;while(n<MAX_EPISODE&&episodes.get(n+1))n++;return n;}
 private static JSONObject aniMedia(JSONObject data) throws Exception {
  JSONObject media=data.optJSONObject("Media");if(media==null||media.optInt("id",0)<=0||!"ANIME".equals(media.optString("type","")))throw new AccountError("MATCH_REQUIRED: AniList did not confirm this anime ID. Choose its exact season.");return media;
 }
 JSONObject update(String provider,JSONObject request,String access) throws Exception {
  if(!"mal".equals(provider)&&!"anilist".equals(provider))throw new AccountError("Choose MyAnimeList or AniList to sync.");
  int aid=id(request,"anilistId"),mid=id(request,"malId");if(aid==0&&mid==0)throw new AccountError("MATCH_REQUIRED: Choose the exact anime before syncing.");
  String local=request.optString("status","");String desired=remoteStatus(local,provider);
  String mode=request.optString("intent","");if(!Arrays.asList("backfill","status","watch").contains(mode))throw new AccountError("Invalid tracker update.");
  BitSet episodes=watched(request);JSONObject ani=null,mal=null,entry=null;
  // AniList supplies the authoritative cross-provider mapping; authenticated requests
  // additionally return the current user's own list entry (not another user's entry).
  if("anilist".equals(provider)||aid>0){
   String q="query($aid:Int,$mid:Int){Media(id:$aid,idMal:$mid,type:ANIME){id idMal type episodes title{english romaji native} synonyms"+("anilist".equals(provider)?" mediaListEntry{id status progress}":"")+"}}";
   JSONObject vars=new JSONObject();if(aid>0)vars.put("aid",aid);else vars.put("mid",mid);
   ani=aniMedia(graph("anilist".equals(provider)?access:"",q,vars));
   int foundAid=ani.optInt("id",0),foundMid=ani.optInt("idMal",0);
   if(aid>0&&foundAid!=aid)throw new AccountError("MATCH_REQUIRED: AniList ID does not match this anime.");
   if(mid>0&&foundMid!=mid)throw new AccountError("MATCH_REQUIRED: The MyAnimeList and AniList IDs disagree. Choose the exact season again.");
   aid=foundAid;if(foundMid>0)mid=foundMid;
  }
  if("mal".equals(provider)){
   if(mid<=0)throw new AccountError("MATCH_REQUIRED: AniList has no confirmed MyAnimeList ID for this title.");
   mal=http.request("GET","https://api.myanimelist.net/v2/anime/"+mid+"?fields=title,alternative_titles,num_episodes,my_list_status",headers(access,false),null);
   if(mal.optInt("id",0)!=mid)throw new AccountError("MATCH_REQUIRED: MyAnimeList returned a different anime. Choose the exact season.");
   entry=mal.optJSONObject("my_list_status");
  }else entry=ani.optJSONObject("mediaListEntry");
  verifyNames(request,ani,mal);
  boolean exists=entry!=null;String current=exists?entry.optString("status",""):"";
  if(exists&&current.isEmpty())throw new AccountError("The provider returned an incomplete list entry. Nothing was changed.");
  if(exists&&"anilist".equals(provider)&&entry.optInt("id",0)<=0)throw new AccountError("AniList did not return the existing list-entry ID. Nothing was changed.");
  String progressKey="anilist".equals(provider)?"progress":"num_episodes_watched";int old=0;
  if(exists){Object raw=entry.opt(progressKey);if(!(raw instanceof Number))throw new AccountError("The provider did not return the existing watched progress. Nothing was changed.");double n=((Number)raw).doubleValue();if(!Double.isFinite(n)||n<0||n>Integer.MAX_VALUE||n!=Math.rint(n))throw new AccountError("The provider returned invalid watched progress. Nothing was changed.");old=(int)n;}
  int watched=nextProgress(old,episodes);int total="mal".equals(provider)?total(mal,"num_episodes"):total(ani,"episodes");
  if(total>0&&watched>total&&old<=total)watched=total;
  // Choosing Completed is an explicit statement that all known episodes were watched.
  if("status".equals(mode)&&"completed".equals(local)&&total>0)watched=Math.max(watched,total);
  String next=exists?current:desired;boolean conflict="backfill".equals(mode)&&exists&&!current.equals(desired);
  if("status".equals(mode))next=desired;
  if("watch".equals(mode)){
   String planning="anilist".equals(provider)?"PLANNING":"plan_to_watch",watching="anilist".equals(provider)?"CURRENT":"watching",complete="anilist".equals(provider)?"COMPLETED":"completed";
   if(exists&&current.equals(planning)&&"watching".equals(local)&&watched>old)next=watching;
   if(next.equals(watching)&&"watching".equals(local)&&total>0&&watched>=total&&watched>old)next=complete;
  }
  boolean changeStatus=!exists||!next.equals(current),changeProgress=watched>old;
  if(changeStatus||changeProgress){
   if("mal".equals(provider)){
    Map<String,String> fields=new LinkedHashMap<>();if(changeStatus)fields.put("status",next);if(changeProgress)fields.put("num_watched_episodes",Integer.toString(watched));
    Map<String,String> h=headers(access,false);h.put("Content-Type","application/x-www-form-urlencoded");
    // MAL's official curl example uses PUT. HttpURLConnection supports actual PUT;
    // PATCH is not reliably supported on Android's HttpURLConnection.
    http.request("PUT","https://api.myanimelist.net/v2/anime/"+mid+"/my_list_status",h,AccountOAuth.form(fields));
   }else{
    JSONObject vars=new JSONObject();StringBuilder defs=new StringBuilder(),args=new StringBuilder();
    if(exists){defs.append("$entry:Int!");args.append("id:$entry");vars.put("entry",entry.optInt("id"));}
    else{defs.append("$media:Int!");args.append("mediaId:$media");vars.put("media",aid);}
    if(changeStatus){defs.append(",$status:MediaListStatus");args.append(",status:$status");vars.put("status",next);}
    if(changeProgress){defs.append(",$progress:Int");args.append(",progress:$progress");vars.put("progress",watched);}
    JSONObject data=graph(access,"mutation("+defs+"){SaveMediaListEntry("+args+"){id status progress}}",vars);
    JSONObject result=data.optJSONObject("SaveMediaListEntry");if(result==null||result.optInt("id",0)<=0)throw new AccountError("AniList did not confirm this list update. It remains pending.");
   }
  }
  return new JSONObject().put("updated",changeStatus||changeProgress).put("conflict",conflict).put("status",next).put("progress",watched).put("anilistId",aid).put("malId",mid);
 }
 JSONObject search(String supplied) throws Exception {
  String q=supplied==null?"":supplied.trim().replaceAll("[\\p{Cntrl}]"," ");if(q.length()<2||q.length()>160)throw new AccountError("Enter an anime title of 2–160 characters.");
  String query="query($search:String!){Page(page:1,perPage:12){media(search:$search,type:ANIME,sort:SEARCH_MATCH){id idMal isAdult format episodes seasonYear title{english romaji native}}}}";
  JSONObject page=graph("",query,new JSONObject().put("search",q)).optJSONObject("Page");if(page==null)throw new AccountError("AniList search returned an incomplete response.");JSONArray results=page.optJSONArray("media"),out=new JSONArray();
  if(results!=null)for(int i=0;i<results.length();i++){
   JSONObject m=results.optJSONObject(i);if(m==null||m.optInt("id",0)<=0||!Boolean.FALSE.equals(m.opt("isAdult")))continue;
   JSONObject title=m.optJSONObject("title");if(title==null)continue;String name=title.optString("english","");if(name.isEmpty())name=title.optString("romaji","");if(name.isEmpty())continue;
   if(name.length()>200)name=name.substring(0,200);
   out.put(new JSONObject().put("anilistId",m.optInt("id")).put("malId",Math.max(0,m.optInt("idMal",0))).put("title",name).put("romaji",title.optString("romaji","")).put("year",m.optInt("seasonYear",0)).put("format",m.optString("format","")).put("episodes",total(m,"episodes")));
  }
  return new JSONObject().put("items",out);
 }
}
