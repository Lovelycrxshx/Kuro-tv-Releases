package app.lovelycrxshx.kuro;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Public catalogue metadata only; fixed query and endpoint, no account tokens. */
public final class KuroCatalog573 {
 static JSONObject request(String action,String input) throws Exception {
  if(input==null||input.length()>300)throw new IOException("Invalid request");
  JSONObject p=new JSONObject(input),v=new JSONObject();String query;
  if("season".equals(action)){
   int year=p.optInt("year",0),page=p.optInt("page",0);String season=p.optString("season","");
   if(year<1940||year>2100||page<1||page>50||!java.util.Arrays.asList("WINTER","SPRING","SUMMER","FALL").contains(season))throw new IOException("Invalid season");
   v.put("year",year).put("season",season).put("page",page);
   query="query($year:Int,$season:MediaSeason,$page:Int){Page(page:$page,perPage:20){pageInfo{hasNextPage} media(type:ANIME,seasonYear:$year,season:$season,isAdult:false,sort:POPULARITY_DESC){id isAdult title{romaji english} format status episodes startDate{year month day} nextAiringEpisode{airingAt episode}}}}";
  }else if("calendar".equals(action)){
   long start=p.optLong("start",0),end=p.optLong("end",0);int page=p.optInt("page",0);
   if(start<1||end<=start||end-start>8*86400L||end>2147483647L||page<1||page>50)throw new IOException("Invalid calendar range");
   v.put("start",start).put("end",end).put("page",page);
   query="query($start:Int,$end:Int,$page:Int){Page(page:$page,perPage:50){pageInfo{hasNextPage} airingSchedules(airingAt_greater:$start,airingAt_lesser:$end,sort:TIME){airingAt episode media{id type isAdult title{romaji english}}}}}";
  }else if("recommend".equals(action)){
   long id=p.optLong("id",0);String provider=p.optString("provider","");if(id<1||id>999999999||!(provider.equals("anilist")||provider.equals("mal")))throw new IOException("Invalid catalogue ID");v.put("id",id);
   String field=provider.equals("mal")?"idMal":"id";
   query="query($id:Int){Media("+field+":$id,type:ANIME){id isAdult recommendations(page:1,perPage:20,sort:RATING_DESC){nodes{rating mediaRecommendation{id type isAdult title{romaji english} format}}}}}";
  }else if("related".equals(action)){
   long id=p.optLong("id",0);String provider=p.optString("provider","");if(id<1||id>999999999||!(provider.equals("anilist")||provider.equals("mal")))throw new IOException("Invalid catalogue ID");v.put("id",id);
   String field=provider.equals("mal")?"idMal":"id";
   query="query($id:Int){Media("+field+":$id,type:ANIME){id isAdult title{romaji english} relations{edges{relationType node{id type isAdult title{romaji english} format startDate{year month day}}}}}}";
  }else throw new IOException("Invalid action");
  return new JSONObject().put("query",query).put("variables",v);
 }
 public static String lookup(String action,String input) throws Exception {
  JSONObject data=request(action,input);HttpURLConnection c=(HttpURLConnection)new URL("https://graphql.anilist.co").openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","Kuro.tv/5.73 (public catalogue)");
  try{try(OutputStream out=c.getOutputStream()){out.write(data.toString().getBytes(StandardCharsets.UTF_8));}if(c.getResponseCode()!=200)throw new IOException("Catalogue unavailable");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>700000)throw new IOException("Response too large");out.write(b,0,n);}String result=out.toString("UTF-8");JSONObject parsed=new JSONObject(result);if(parsed.has("errors")||parsed.optJSONObject("data")==null)throw new IOException("Catalogue unavailable");return result;}}finally{c.disconnect();}
 }
}
