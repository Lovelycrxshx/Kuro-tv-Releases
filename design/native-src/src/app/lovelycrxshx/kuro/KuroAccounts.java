package app.lovelycrxshx.kuro;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local-only email/password, public-client imports and opt-controlled tracker updates.
 */
final class KuroAccounts {
 private final AccountVault vault;
 private final AccountTransport http;
 private final LocalEmail551 localEmail;
 private static final int MAX_IMPORT=5000,PAGE_SIZE=100;
 KuroAccounts(Context c){this(new AndroidAccountVault(c),new AccountHttp());}
 KuroAccounts(AccountVault v,AccountTransport t){vault=v;http=t;localEmail=new LocalEmail551(v);}
 private JSONObject read(String name) throws Exception {String s=vault.get(name);return s.isEmpty()?new JSONObject():new JSONObject(s);}
 private void write(String name,JSONObject value) throws Exception {vault.put(name,value.toString());}
 private static String str(JSONObject obj,String key){Object o=obj.opt(key);return o instanceof String?(String)o:"";}
 private static String clean(String s,int max){if(s==null)return "";String x=s.replaceAll("[\\p{Cntrl}]"," ").trim();return x.length()>max?x.substring(0,max):x;}
 private static String url(String s){try{URI u=new URI(s);return "https".equals(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null&&u.getPort()==-1&&s.length()<=1600?s:"";}catch(Exception e){return "";}}
 private static Map<String,String> headers(String bearer){Map<String,String> m=new LinkedHashMap<>();if(bearer!=null&&!bearer.isEmpty())m.put("Authorization","Bearer "+bearer);return m;}
 private static Map<String,String> jsonHeaders(String bearer){Map<String,String> m=headers(bearer);m.put("Content-Type","application/json");return m;}
 private static Map<String,String> formHeaders(String bearer){Map<String,String> m=headers(bearer);m.put("Content-Type","application/x-www-form-urlencoded");return m;}
 // These are public client identifiers, not secrets. Existing nonempty
 // per-device identifiers and encrypted sessions are never overwritten.
 private JSONObject conf() throws Exception {
  JSONObject c=read("config");
  if(str(c,"discord").isEmpty())c.put("discord","1569077670074122250");
  if(str(c,"mal").isEmpty())c.put("mal","37160cd5a180308c9009afb7faa4e341");
  if(str(c,"anilist").isEmpty())c.put("anilist","56440");
  return c;
 }
 private JSONObject session(String p) throws Exception {return read("session."+p);}
 private void saveSession(String p,JSONObject s) throws Exception {write("session."+p,s);}
 private static String cleanName(String name){String n=clean(name,80);return n.isEmpty()?"Account":n;}
 private static String required(String s,String message) throws Exception {if(s==null||s.trim().isEmpty())throw new AccountError(message);return s.trim();}
 private static JSONObject checkedGraph(JSONObject result) throws Exception {if(result.has("errors")&&!result.isNull("errors"))throw new AccountError("AniList could not complete that request. Check the account and try again.");JSONObject data=result.optJSONObject("data");if(data==null)throw new AccountError("AniList returned an incomplete response.");return data;}
 private JSONObject graph(String token,String query,JSONObject variables) throws Exception {
  JSONObject body=new JSONObject().put("query",query);if(variables!=null)body.put("variables",variables);
  return checkedGraph(http.request("POST","https://graphql.anilist.co",jsonHeaders(token),body.toString()));
 }
 synchronized JSONObject status() throws Exception {
  JSONObject c=conf(),visible=new JSONObject(),connections=new JSONObject();
  for(String k:new String[]{"mal","discord","anilist"})visible.put(k,str(c,k));
  connections.put("local",localEmail.status());
  for(String provider:new String[]{"discord","mal","anilist"}){
   JSONObject session=session(provider);boolean connected=!str(session,"access").isEmpty();
   JSONObject v=new JSONObject().put("connected",connected);
   if(connected){v.put("name",cleanName(str(session,"name"))).put("method",clean(str(session,"method"),24));if("mal".equals(provider)||"anilist".equals(provider))v.put("syncOwner",TrackerSync553.ownerKey(provider,session));}
   connections.put(provider,v);
  }
  return new JSONObject().put("config",visible).put("connections",connections).put("redirectMal",AccountOAuth.MAL_REDIRECT).put("redirectDiscord",AccountOAuth.DISCORD_REDIRECT).put("redirectAniList",AccountOAuth.ANILIST_PIN);
 }

 synchronized JSONObject saveConfig(JSONObject patch) throws Exception {
  JSONObject old=conf(),next=new JSONObject(old.toString());
  for(String provider:new String[]{"mal","discord","anilist"})if(patch.has(provider)){
   String value=clean(str(patch,provider),200);
   if(!value.isEmpty()&&!AccountOAuth.publicId(provider,value))throw new AccountError("Check the "+("mal".equals(provider)?"MyAnimeList":("anilist".equals(provider)?"AniList":"Discord"))+" public client ID.");
   next.put(provider,value);
  }
  write("config",next);
  for(String provider:new String[]{"mal","discord","anilist"})if(!str(old,provider).equals(str(next,provider))){vault.remove("session."+provider);vault.remove("pending."+provider);}
  return new JSONObject().put("message","Setup saved on this device. Changed providers were disconnected for safety.");
 }
 private JSONObject pending(String kind,String id) throws Exception {
  JSONObject pending=new JSONObject().put("state",AccountOAuth.random()).put("verifier",AccountOAuth.random()).put("at",System.currentTimeMillis()).put("id",id);write("pending."+kind,pending);return pending;
 }
 synchronized String begin(String provider) throws Exception {
  JSONObject c=conf();String id=str(c,provider);
  if("anilist".equals(provider))return AccountOAuth.authorize(provider,id,"","");
  if(!("mal".equals(provider)||"discord".equals(provider)))throw new AccountError("Unknown account provider.");
  if(!AccountOAuth.publicId(provider,id))throw new AccountError("Add the provider's public client ID in Account setup first.");
  JSONObject pending=pending(provider,id);
  return AccountOAuth.authorize(provider,id,str(pending,"state"),str(pending,"verifier"));
 }
 private JSONObject fromTokens(String provider,JSONObject response,JSONObject binding) throws Exception {
  String access=str(response,"access_token"),refresh=str(response,"refresh_token");
  if(access.isEmpty()||access.length()>10000||refresh.length()>10000)throw new AccountError("The provider did not return a valid session.");
  JSONObject identity;
  if("mal".equals(provider))identity=http.request("GET","https://api.myanimelist.net/v2/users/@me",headers(access),null);
  else if("discord".equals(provider))identity=http.request("GET","https://discord.com/api/users/@me",headers(access),null);
  else throw new AccountError("Unknown session type.");
  String userId=String.valueOf(identity.opt("id"));if(userId.equals("null")||userId.trim().isEmpty()||userId.length()>100)throw new AccountError("The provider did not confirm the account identity.");
  String name;if("mal".equals(provider))name=str(identity,"name");else{name=str(identity,"global_name");if(name.isEmpty())name=str(identity,"username");}
  long seconds=response.optLong("expires_in",3600);if(seconds<1||seconds>31536000)seconds=3600;
  JSONObject session=new JSONObject().put("access",access).put("refresh",refresh).put("expiresAt",System.currentTimeMillis()+seconds*1000L).put("id",clean(userId,100)).put("name",cleanName(name)).put("method",provider).put("clientId",str(binding,"id"));
  saveSession(provider,session);return session;
 }
 private JSONObject token(String url,Map<String,String> h,Map<String,String> body) throws Exception {return http.request("POST",url,h,AccountOAuth.form(body));}
 synchronized JSONObject callback(String raw) throws Exception {
  AccountOAuth.Callback cb=AccountOAuth.parseCallback(raw);JSONObject pending=read("pending."+cb.kind);
  if(!AccountOAuth.sameState(cb.state,str(pending,"state")))throw new AccountError("This sign-in return does not match the request. Start again in Kuro.");
  long age=System.currentTimeMillis()-pending.optLong("at",0L);if(age<0||age>1200000L){vault.remove("pending."+cb.kind);throw new AccountError("That sign-in request expired. Start again in Kuro.");}
  JSONObject config=conf();String provider=cb.kind;
  if(!str(pending,"id").equals(str(config,provider)))throw new AccountError("The client ID changed. Start again in Kuro.");
  vault.remove("pending."+cb.kind);
  if(!cb.error.isEmpty())throw new AccountError("Sign-in was cancelled or denied. No account was connected.");
  JSONObject response;
  if("mal".equals(provider)){
   Map<String,String> h=formHeaders("");h.put("Authorization","Basic "+Base64.getEncoder().encodeToString((str(pending,"id")+":").getBytes(StandardCharsets.UTF_8)));
   response=token("https://myanimelist.net/v1/oauth2/token",h,AccountOAuth.map("client_id",str(pending,"id"),"grant_type","authorization_code","code",cb.code,"redirect_uri",AccountOAuth.MAL_REDIRECT,"code_verifier",str(pending,"verifier")));
  }else if("discord".equals(provider))response=token("https://discord.com/api/oauth2/token",formHeaders(""),AccountOAuth.map("client_id",str(pending,"id"),"grant_type","authorization_code","code",cb.code,"redirect_uri",AccountOAuth.DISCORD_REDIRECT,"code_verifier",str(pending,"verifier")));
  else throw new AccountError("Unknown account provider.");
  JSONObject session=fromTokens(provider,response,pending);
  return new JSONObject().put("message","Connected as "+str(session,"name")+".").put("kind",provider);
 }
 synchronized JSONObject emailLogin(String mail,String pass,boolean signup) throws Exception {
  return signup?localEmail.signup(mail,pass):localEmail.login(mail,pass);
 }
 synchronized JSONObject connectAniList(String supplied) throws Exception {
  String access=required(supplied,"Paste the token shown by AniList into Kuro on your phone.");
  if(access.length()<16||access.length()>10000||!access.matches("[\\x21-\\x7E]+"))throw new AccountError("That AniList token does not look valid.");
  JSONObject data=graph(access,"query { Viewer { id name } }",null),viewer=data.optJSONObject("Viewer");
  if(viewer==null||viewer.optLong("id",0)<=0)throw new AccountError("AniList could not confirm this account.");
  JSONObject s=new JSONObject().put("access",access).put("refresh","").put("expiresAt",0).put("id",Long.toString(viewer.optLong("id"))).put("name",cleanName(str(viewer,"name"))).put("method","anilist").put("clientId",str(conf(),"anilist"));
  saveSession("anilist",s);return new JSONObject().put("message","Connected to AniList as "+str(s,"name")+".");
 }
 synchronized JSONObject disconnect(String provider) throws Exception {
  if("local".equals(provider))return localEmail.signOut();
  if(!("mal".equals(provider)||"anilist".equals(provider)||"discord".equals(provider)))throw new AccountError("Unknown account provider.");
  vault.remove("session."+provider);vault.remove("pending."+provider);
  return new JSONObject().put("message","Disconnected on this phone. Revoke access in the provider's settings if you also want to invalidate its authorization.");
 }
 private JSONObject validSession(String provider) throws Exception {
  JSONObject s=session(provider),c=conf();String access=str(s,"access");if(access.isEmpty())throw new AccountError("Connect "+("mal".equals(provider)?"MyAnimeList":"anilist".equals(provider)?"AniList":"your account")+" first.");
  if(!str(c,provider).equals(str(s,"clientId")))throw new AccountError("The provider client ID changed. Sign in again.");
  long expiry=s.optLong("expiresAt",0);if(expiry==0||expiry>System.currentTimeMillis()+60000)return s;
  String refresh=str(s,"refresh");if(refresh.isEmpty())throw new AccountError("The session expired. Sign in again.");JSONObject r;
  if("mal".equals(provider)){
   Map<String,String> h=formHeaders("");h.put("Authorization","Basic "+Base64.getEncoder().encodeToString((str(s,"clientId")+":").getBytes(StandardCharsets.UTF_8)));
   r=token("https://myanimelist.net/v1/oauth2/token",h,AccountOAuth.map("client_id",str(s,"clientId"),"grant_type","refresh_token","refresh_token",refresh));
  }else if("discord".equals(provider))r=token("https://discord.com/api/oauth2/token",formHeaders(""),AccountOAuth.map("client_id",str(s,"clientId"),"grant_type","refresh_token","refresh_token",refresh));
  else throw new AccountError("The session expired. Sign in again.");
  String newAccess=str(r,"access_token"),newRefresh=str(r,"refresh_token");if(newAccess.isEmpty()||newAccess.length()>10000||newRefresh.length()>10000)throw new AccountError("The provider did not refresh this session. Sign in again.");
  long seconds=r.optLong("expires_in",3600);if(seconds<1||seconds>31536000)seconds=3600;s.put("access",newAccess).put("refresh",newRefresh.isEmpty()?refresh:newRefresh).put("expiresAt",System.currentTimeMillis()+seconds*1000L);saveSession(provider,s);return s;
 }

 synchronized JSONObject syncTracker(JSONObject request) throws Exception {
  String provider=str(request,"provider");if(!("mal".equals(provider)||"anilist".equals(provider)))throw new AccountError("Choose MyAnimeList or AniList to sync.");
  JSONObject saved=session(provider);String expected=TrackerSync553.ownerKey(provider,saved),supplied=str(request,"owner");
  if(expected.isEmpty()||!expected.equals(supplied))throw new AccountError("The connected account changed. Pending updates were not sent to another account.");
  JSONObject s=validSession(provider);return new TrackerSync553(http).update(provider,request,str(s,"access"));
 }
 synchronized JSONObject searchTracker(String title) throws Exception {return new TrackerSync553(http).search(title);}

 /** Read-only, on-demand details for an exact imported provider ID. */
 private static JSONArray detailNames(JSONArray values,boolean objects) throws Exception {
  JSONArray out=new JSONArray();if(values==null)return out;
  for(int i=0;i<values.length()&&out.length()<10;i++){
   String name=objects?(values.optJSONObject(i)==null?"":str(values.optJSONObject(i),"name")):values.optString(i,"");
   name=clean(name,100);if(!name.isEmpty()&&!"null".equals(name))out.put(name);
  }
  return out;
 }
 private static Object malAdult(String rating){
  if("rx".equalsIgnoreCase(rating))return Boolean.TRUE;
  return rating.matches("(?i)g|pg|pg_13|r|r_plus|r\\+")?Boolean.FALSE:JSONObject.NULL;
 }
 synchronized JSONObject importDetails(String provider,String sourceId) throws Exception {
  if(!("mal".equals(provider)||"anilist".equals(provider))||sourceId==null||!sourceId.matches("[1-9][0-9]{0,8}"))throw new AccountError("Invalid imported anime identifier.");
  int id=Integer.parseInt(sourceId);JSONObject media;String title,image,description,format,airing,date;int total;JSONArray genres,studios;Object adult;
  if("mal".equals(provider)){
   JSONObject session=validSession("mal");
   String address="https://api.myanimelist.net/v2/anime/"+id+"?"+AccountOAuth.form(AccountOAuth.map("fields","synopsis,genres,media_type,status,num_episodes,start_date,studios,rating"));
   media=http.request("GET",address,headers(str(session,"access")),null);
   if(media.optLong("id",0)!=id)throw new AccountError("MyAnimeList did not confirm the requested anime ID.");
   title=clean(str(media,"title"),300);JSONObject picture=media.optJSONObject("main_picture");image=picture==null?"":url(str(picture,"large"));if(image.isEmpty()&&picture!=null)image=url(str(picture,"medium"));
   description=clean(str(media,"synopsis"),4000);format=clean(str(media,"media_type"),60);airing=clean(str(media,"status"),60);date=clean(str(media,"start_date"),30);total=Math.max(0,media.optInt("num_episodes",0));
   genres=detailNames(media.optJSONArray("genres"),true);studios=detailNames(media.optJSONArray("studios"),true);adult=malAdult(str(media,"rating"));
  }else{
   String q="query($id:Int!){Media(id:$id,type:ANIME){id isAdult title{english romaji} description(asHtml:false) format status episodes startDate{year month day} genres studios(isMain:true){nodes{name}} coverImage{large}}}";
   media=graph("",q,new JSONObject().put("id",id)).optJSONObject("Media");
   if(media==null||media.optLong("id",0)!=id)throw new AccountError("AniList did not confirm the requested anime ID.");
   JSONObject titles=media.optJSONObject("title"),picture=media.optJSONObject("coverImage"),start=media.optJSONObject("startDate"),studio=media.optJSONObject("studios");
   title=titles==null?"":str(titles,"english");if(title.isEmpty()&&titles!=null)title=str(titles,"romaji");title=clean(title,300);
   image=picture==null?"":url(str(picture,"large"));description=clean(str(media,"description"),4000);format=clean(str(media,"format"),60);airing=clean(str(media,"status"),60);
   int year=start==null?0:start.optInt("year",0);date=year>0?Integer.toString(year):"";total=Math.max(0,media.optInt("episodes",0));
   genres=detailNames(media.optJSONArray("genres"),false);studios=detailNames(studio==null?null:studio.optJSONArray("nodes"),true);
   Object classification=media.opt("isAdult");adult=classification instanceof Boolean?classification:JSONObject.NULL;
  }
  return new JSONObject().put("provider",provider).put("sourceId",sourceId).put("title",title).put("image",image).put("synopsis",description).put("format",format).put("airing",airing).put("date",date).put("episodes",total).put("genres",genres).put("studios",studios).put("adult",adult);
 }
 private static JSONObject malItem(JSONObject row) throws Exception {
  JSONObject n=row.optJSONObject("node"),s=row.optJSONObject("list_status");if(n==null||s==null||n.optLong("id",0)<=0)return null;
  JSONObject pic=n.optJSONObject("main_picture");String image=pic==null?"":url(str(pic,"medium"));String rating=str(n,"rating");Object adult=JSONObject.NULL;
  if("rx".equalsIgnoreCase(rating))adult=Boolean.TRUE;else if(rating.matches("(?i)g|pg|pg_13|r|r_plus|r\\+"))adult=Boolean.FALSE;
  return new JSONObject().put("provider","mal").put("sourceId",Long.toString(n.optLong("id"))).put("malId",n.optLong("id")).put("anilistId",0).put("title",clean(str(n,"title"),300)).put("image",image).put("status",clean(str(s,"status"),40)).put("progress",Math.max(0,s.optInt("num_episodes_watched",0))).put("total",Math.max(0,n.optInt("num_episodes",0))).put("adult",adult);
 }
 private static JSONObject aniItem(JSONObject row) throws Exception {
  JSONObject m=row.optJSONObject("media");if(m==null||m.optLong("id",0)<=0)return null;JSONObject titles=m.optJSONObject("title"),picture=m.optJSONObject("coverImage");
  String title=titles==null?"":str(titles,"english");if(title.isEmpty()&&titles!=null)title=str(titles,"romaji");
  Object adult=m.has("isAdult")&&!m.isNull("isAdult")?Boolean.valueOf(m.optBoolean("isAdult")):JSONObject.NULL;
  return new JSONObject().put("provider","anilist").put("sourceId",Long.toString(m.optLong("id"))).put("malId",Math.max(0,m.optLong("idMal",0))).put("anilistId",m.optLong("id")).put("title",clean(title,300)).put("image",picture==null?"":url(str(picture,"large"))).put("status",clean(str(row,"status"),40)).put("progress",Math.max(0,row.optInt("progress",0))).put("total",Math.max(0,m.optInt("episodes",0))).put("adult",adult);
 }
 synchronized JSONObject importList(String provider) throws Exception {
  if(!("mal".equals(provider)||"anilist".equals(provider)))throw new AccountError("Choose MyAnimeList or AniList to import.");
  JSONObject s=validSession(provider);String access=str(s,"access");JSONArray items=new JSONArray();boolean partial=false,more=false;String reason="";
  for(int page=0;page<MAX_IMPORT/PAGE_SIZE;page++){
   try{
    if("mal".equals(provider)){
     String address="https://api.myanimelist.net/v2/users/@me/animelist?"+AccountOAuth.form(AccountOAuth.map("limit",Integer.toString(PAGE_SIZE),"offset",Integer.toString(page*PAGE_SIZE),"fields","list_status,rating,num_episodes"));
     JSONObject response=http.request("GET",address,headers(access),null);JSONArray data=response.optJSONArray("data");if(data==null)throw new AccountError("MyAnimeList returned an incomplete list.");
     for(int i=0;i<data.length()&&items.length()<MAX_IMPORT;i++){JSONObject row=data.optJSONObject(i);JSONObject item=row==null?null:malItem(row);if(item!=null&&!str(item,"title").isEmpty())items.put(item);}
     JSONObject paging=response.optJSONObject("paging");more=paging==null?data.length()==PAGE_SIZE:!str(paging,"next").isEmpty();
    }else{
     String query="query($userId:Int!,$chunk:Int!){MediaListCollection(userId:$userId,type:ANIME,chunk:$chunk,perChunk:100,forceSingleCompletedList:true){hasNextChunk lists{entries{status progress media{id idMal isAdult episodes title{english romaji} coverImage{large}}}}}}";
     long id;try{id=Long.parseLong(str(s,"id"));}catch(Exception e){throw new AccountError("Reconnect AniList before importing.");}if(id<=0||id>Integer.MAX_VALUE)throw new AccountError("Reconnect AniList before importing.");
     JSONObject response=graph(access,query,new JSONObject().put("userId",id).put("chunk",page+1)),collection=response.optJSONObject("MediaListCollection");if(collection==null)throw new AccountError("AniList returned an incomplete list.");
     JSONArray lists=collection.optJSONArray("lists");if(lists!=null)for(int j=0;j<lists.length();j++){JSONObject group=lists.optJSONObject(j);JSONArray entries=group==null?null:group.optJSONArray("entries");if(entries!=null)for(int i=0;i<entries.length()&&items.length()<MAX_IMPORT;i++){JSONObject row=entries.optJSONObject(i);JSONObject item=row==null?null:aniItem(row);if(item!=null&&!str(item,"title").isEmpty())items.put(item);}}
     more=collection.optBoolean("hasNextChunk",false);
    }
    if(more&&page==MAX_IMPORT/PAGE_SIZE-1){partial=true;reason="This version read up to 50 pages; more entries may remain.";}
    if(!more||items.length()>=MAX_IMPORT)break;
   }catch(Exception ex){if(items.length()==0)throw ex;partial=true;reason=AccountError.display(ex);break;}
  }
  if(more&&items.length()>=MAX_IMPORT){partial=true;reason="The first "+MAX_IMPORT+" entries were read; this version limits each import to "+MAX_IMPORT+".";}
  return new JSONObject().put("items",items).put("partial",partial).put("reason",reason).put("provider",provider);
 }
}
