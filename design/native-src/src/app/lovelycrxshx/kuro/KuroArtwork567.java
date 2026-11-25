package app.lovelycrxshx.kuro;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import javax.net.ssl.HttpsURLConnection;

/** Opt-in anime covers only: no episode images, video capture, source URLs or credentials. */
final class KuroArtwork567 {
 interface Search { JSONObject find(String title) throws Exception; }
 interface Work { String run() throws Exception; }
 static final long POSITIVE_TTL=6*60*60*1000L, NEGATIVE_TTL=10*60*1000L, LOOKUP_INTERVAL=5000L;
 private static final int MAX_CACHE=32,MAX_PENDING=2,MAX_RESPONSE=196608;
 private static final String QUERY="query ($search: String) { Page(page: 1, perPage: 5) { media(type: ANIME, search: $search, sort: SEARCH_MATCH) { id title { romaji english native } synonyms coverImage { extraLarge large } } } }";
 private static final class Entry {final String value;final long stored,expiry;Entry(String value,long now){this.value=value;stored=now;expiry=now+(value.isEmpty()?NEGATIVE_TTL:POSITIVE_TTL);}}
 private final Executor executor;private final Search search,kitsu;private final LongSupplier clock;
 private final Map<String,Entry> cache=bounded();
 private final Set<String> pending=new HashSet<>();private long nextLookup=0;
 private static Map<String,Entry> bounded(){return new LinkedHashMap<String,KuroArtwork567.Entry>(16,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<String,KuroArtwork567.Entry> e){return size()>MAX_CACHE;}};}
 KuroArtwork567(Executor e){this(e,KuroArtwork567::request,KuroArtwork567::requestKitsu,()->TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));}
 KuroArtwork567(Executor e,Search s,LongSupplier c){this(e,s,t->null,c);}
 KuroArtwork567(Executor e,Search s,Search k,LongSupplier c){executor=e;search=s;kitsu=k;clock=c;}
 private Entry fresh(String key,long now){Entry e=cache.get(key);if(e!=null&&(now<e.stored||now>=e.expiry)){cache.remove(key);return null;}return e;}
 private void schedule(String key,Work work,Runnable ready){
  long now=clock.getAsLong();synchronized(this){
   if(fresh(key,now)!=null||pending.contains(key)||pending.size()>=MAX_PENDING||now<nextLookup)return;
   pending.add(key);nextLookup=now+LOOKUP_INTERVAL;
  }
  try{executor.execute(()->{
   String image="";try{image=KuroPresence563.safeUrl(work.run(),true);}catch(Exception ignored){}
   synchronized(this){pending.remove(key);cache.put(key,new Entry(image,clock.getAsLong()));}
   if(ready!=null)try{ready.run();}catch(RuntimeException ignored){}
  });}catch(RuntimeException rejected){synchronized(this){pending.remove(key);cache.put(key,new Entry("",clock.getAsLong()));}}
 }
 /** Prefer the supplied anime cover, then a unique title match, otherwise no asset. */
 String resolve(String title,String animeImage,Runnable ready){
  String supplied=posterUrl(animeImage);if(!supplied.isEmpty())return supplied;
  if(title==null||KuroPresence563.unsafePublicText(title))return "";
  String query=KuroPresence563.publicText(title,120,""),key=key(query);if(key.length()<3||query.isEmpty())return "";
  Entry cover;synchronized(this){cover=fresh(key,clock.getAsLong());}
  if(cover!=null)return cover.value;
  schedule(key,()->findCover(query),ready);return "";
 }
 private String findCover(String title){
  try{JSONObject result=search.find(title);if(aniMatches(result,title)>1)return "";String a=pick(result,title);if(!a.isEmpty())return a;}catch(Exception ignored){}
  try{return pickKitsuCover(kitsu.find(title),title);}catch(Exception ignored){return "";}
 }
 // A known ambiguous AniList title must not be guessed using another catalog.
 private static int aniMatches(JSONObject response,String requested){
  JSONObject data=response==null||response.has("errors")?null:response.optJSONObject("data"),page=data==null?null:data.optJSONObject("Page");JSONArray media=page==null?null:page.optJSONArray("media");String wanted=key(requested);int matches=0;
  if(media==null||media.length()>5||wanted.length()<3)return 0;
  for(int i=0;i<media.length();i++){
   JSONObject item=media.optJSONObject(i);if(item==null||item.optLong("id",0)<=0)continue;JSONObject titles=item.optJSONObject("title");boolean same=false;
   if(titles!=null)for(String k:new String[]{"romaji","english","native"})if(wanted.equals(key(titles.optString(k,""))))same=true;
   JSONArray synonyms=item.optJSONArray("synonyms");if(synonyms!=null)for(int j=0;j<Math.min(50,synonyms.length());j++)if(wanted.equals(key(synonyms.optString(j,""))))same=true;
   if(same)matches++;
  }
  return matches;
 }
 /** Kitsu sometimes adds a numeric cache timestamp; never forward query parameters. */
 static String posterUrl(String value){
  if(value==null)return "";String url=value;
  if((url.startsWith("https://media.kitsu.app/")||url.startsWith("https://media.kitsu.io/"))&&url.indexOf('?')>=0){
   int q=url.indexOf('?');if(url.substring(q+1).matches("[0-9]{1,16}"))url=url.substring(0,q);
  }
  return KuroPresence563.safeUrl(url,true);
 }
 static String key(String value){
  if(value==null)return "";String x=Normalizer.normalize(value,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);StringBuilder b=new StringBuilder();
  for(int i=0;i<x.length();){int c=x.codePointAt(i);i+=Character.charCount(c);if(Character.isLetterOrDigit(c))b.appendCodePoint(c);}
  return b.toString();
 }
 /** Only one exact AniList title/alias match is accepted. */
 static String pick(JSONObject response,String requested){
  if(response==null||response.has("errors"))return "";JSONObject data=response.optJSONObject("data"),page=data==null?null:data.optJSONObject("Page");JSONArray media=page==null?null:page.optJSONArray("media");
  String wanted=key(requested);if(wanted.length()<3||media==null||media.length()>5)return "";JSONObject match=null;int matches=0;
  for(int i=0;i<media.length();i++){
   JSONObject item=media.optJSONObject(i);if(item==null||item.optLong("id",0)<=0)continue;
   JSONObject titles=item.optJSONObject("title");boolean same=false;
   if(titles!=null)for(String k:new String[]{"romaji","english","native"})if(wanted.equals(key(titles.optString(k,""))))same=true;
   JSONArray synonyms=item.optJSONArray("synonyms");if(synonyms!=null)for(int j=0;j<Math.min(50,synonyms.length());j++)if(wanted.equals(key(synonyms.optString(j,""))))same=true;
   if(same){match=item;if(++matches>1)return "";}
  }
  if(matches!=1||match==null)return "";JSONObject cover=match.optJSONObject("coverImage");if(cover==null)return "";
  String image=posterUrl(cover.optString("extraLarge",""));return image.isEmpty()?posterUrl(cover.optString("large","")):image;
 }
 /** Kitsu fallback reads only the anime posterImage, after a unique exact match. */
 static String pickKitsuCover(JSONObject response,String requested){
  JSONArray data=response==null?null:response.optJSONArray("data");String wanted=key(requested);JSONObject match=null;int matches=0;
  if(data==null||data.length()>5||wanted.length()<3)return "";
  for(int i=0;i<data.length();i++){
   JSONObject item=data.optJSONObject(i),a=item==null?null:item.optJSONObject("attributes");if(a==null||!item.optString("id","").matches("[1-9][0-9]{0,9}"))continue;
   boolean same=wanted.equals(key(a.optString("canonicalTitle","")));JSONObject titles=a.optJSONObject("titles");
   if(titles!=null){Iterator<String> names=titles.keys();int count=0;while(names.hasNext()&&count++<20)if(wanted.equals(key(titles.optString(names.next(),""))))same=true;}
   JSONArray aliases=a.optJSONArray("abbreviatedTitles");if(aliases!=null)for(int j=0;j<Math.min(50,aliases.length());j++)if(wanted.equals(key(aliases.optString(j,""))))same=true;
   if(same){match=a;if(++matches>1)return "";}
  }
  if(matches!=1||match==null)return "";JSONObject poster=match.optJSONObject("posterImage");if(poster==null)return "";
  for(String size:new String[]{"original","large","medium","small","tiny"}){String image=posterUrl(poster.optString(size,""));if(!image.isEmpty())return image;}
  return "";
 }
 private static JSONObject requestKitsu(String title) throws Exception {
  String url="https://kitsu.io/api/edge/anime?filter%5Btext%5D="+URLEncoder.encode(title,"UTF-8")+"&page%5Blimit%5D=5&fields%5Banime%5D=canonicalTitle%2Ctitles%2CabbreviatedTitles%2CposterImage";
  HttpsURLConnection c=(HttpsURLConnection)new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/vnd.api+json");
  try{return read(c);}finally{c.disconnect();}
 }
 private static JSONObject request(String title) throws Exception {
  HttpsURLConnection c=(HttpsURLConnection)new URL("https://graphql.anilist.co").openConnection();
  c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setDoOutput(true);
  c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","application/json");
  byte[] body=new JSONObject().put("query",QUERY).put("variables",new JSONObject().put("search",title)).toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(body.length);
  try{try(OutputStream out=c.getOutputStream()){out.write(body);}return read(c);}finally{c.disconnect();}
 }
 private static JSONObject read(HttpsURLConnection c) throws Exception {
  if(c.getResponseCode()!=200)throw new IOException("Metadata service unavailable");
  try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[4096];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>MAX_RESPONSE)throw new IOException("Metadata response too large");out.write(b,0,n);}
   return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
  }
 }
}
