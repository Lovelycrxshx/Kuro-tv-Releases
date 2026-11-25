package app.lovelycrxshx.kuro;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Fixed AniList classification query; unknown classifications are never treated as safe. */
public final class AdultMetadata {
 public static String lookup(String input) throws Exception {
  if(input==null||input.length()>4000)throw new IOException("Invalid classification request");
  JSONObject request=new JSONObject(input),variables=new JSONObject();
  JSONArray anilist=validIds(request.optJSONArray("anilist")),mal=validIds(request.optJSONArray("mal"));
  if(anilist.length()+mal.length()==0||anilist.length()+mal.length()>50)throw new IOException("Invalid classification request");
  String fields="{media(%s,type:ANIME){id idMal isAdult startDate{year} title{romaji english native} synonyms}}";
  StringBuilder vars=new StringBuilder(),body=new StringBuilder();
  if(anilist.length()>0){vars.append("$a:[Int]");body.append("a:Page(page:1,perPage:50)").append(String.format(java.util.Locale.ROOT,fields,"id_in:$a"));variables.put("a",anilist);}
  if(mal.length()>0){if(vars.length()>0)vars.append(',');vars.append("$m:[Int]");body.append(" m:Page(page:1,perPage:50)").append(String.format(java.util.Locale.ROOT,fields,"idMal_in:$m"));variables.put("m",mal);}
  JSONObject data=new JSONObject().put("query","query("+vars+"){"+body+"}").put("variables",variables);
  HttpURLConnection c=(HttpURLConnection)new URL("https://graphql.anilist.co").openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestMethod("POST");c.setDoOutput(true);
  c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","Kuro.tv/5.36 (content classification)");
  try {
   try(OutputStream out=c.getOutputStream()){out.write(data.toString().getBytes(StandardCharsets.UTF_8));}
   if(c.getResponseCode()!=200)throw new IOException("Classification temporarily unavailable");
   try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>700000)throw new IOException("Classification response too large");out.write(b,0,n);}return out.toString("UTF-8");
   }
  } finally { c.disconnect(); }
 }
 private static JSONArray validIds(JSONArray in) throws Exception {
  JSONArray out=new JSONArray();if(in==null)return out;if(in.length()>50)throw new IOException("Too many IDs");
  java.util.HashSet<Integer> seen=new java.util.HashSet<>();
  for(int i=0;i<in.length();i++){Object v=in.opt(i);if(!(v instanceof Number))throw new IOException("Invalid ID");long n=((Number)v).longValue();if(n<1||n>999999999||((Number)v).doubleValue()!=n)throw new IOException("Invalid ID");if(seen.add((int)n))out.put((int)n);}
  return out;
 }
}
