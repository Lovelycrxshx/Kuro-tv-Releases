package app.lovelycrxshx.kuro;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import org.json.*;

/** Optional public catalogue metadata. Candidates are exact-matched in the UI before display. */
public final class NewsMetadata {
 private static final String FIELDS="{media(search:$q%d,type:ANIME,sort:SEARCH_MATCH){id title{romaji english native} synonyms description(asHtml:false) genres studios(isMain:true){nodes{name isAnimationStudio}} coverImage{large} siteUrl}}";
 public static String lookup(String input)throws Exception{
  if(input==null||input.length()>1800)throw new Exception("Invalid lookup");JSONArray names=new JSONArray(input);if(names.length()==0||names.length()>8)throw new Exception("Invalid lookup");
  StringBuilder params=new StringBuilder("query("),body=new StringBuilder("{");JSONObject vars=new JSONObject();
  for(int i=0;i<names.length();i++){
   String name=names.optString(i,"").trim();if(name.length()<2||name.length()>140)throw new Exception("Invalid title");
   if(i>0)params.append(',');params.append("$q").append(i).append(":String");body.append("m").append(i).append(":Page(page:1,perPage:8)").append(String.format(java.util.Locale.ROOT,FIELDS,i));vars.put("q"+i,name);
  }
  String query=params.append(')').append(body).append('}').toString();JSONObject data=new JSONObject();data.put("query",query);data.put("variables",vars);
  HttpURLConnection c=(HttpURLConnection)new URL("https://graphql.anilist.co").openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","Kuro.tv/5.36 (anime metadata)");
  try{try(OutputStream out=c.getOutputStream()){out.write(data.toString().getBytes(StandardCharsets.UTF_8));}if(c.getResponseCode()!=200)throw new IOException("Metadata unavailable");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1_500_000)throw new IOException("Metadata too large");out.write(b,0,n);}return out.toString("UTF-8");}}
  finally{c.disconnect();}
 }
}
