import app.lovelycrxshx.kuro.KitsuneCatalog;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Tests bounded compacting without storing provider stream links. Run with JSON_JAR. */
public class KitsuneCatalogTest {
 private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 private static JSONObject provider(int count,int garbage){
  JSONArray sub=new JSONArray(),dub=new JSONArray();String filler="x".repeat(garbage);
  for(int n=1;n<=count;n++){JSONObject e=new JSONObject().put("number",n).put("id","watch/anikoto/21/sub/anikoto-"+n).put("title","Episode "+n).put("audio","sub").put("description",filler).put("embedUrl","https://unsafe.invalid/expiring");sub.put(e);if(n<=count-1)dub.put(new JSONObject().put("number",n).put("id","watch/anikoto/21/dub/anikoto-"+n).put("audio","dub").put("title","Episode "+n).put("description",filler));}
  JSONObject episodes=new JSONObject().put("sub",sub).put("dub",dub);return new JSONObject().put("providers",new JSONObject().put("anikoto",new JSONObject().put("meta",new JSONObject().put("malId",21).put("slug","one-piece").put("source","anikoto")).put("episodes",episodes)));
 }
 public static void main(String[] args)throws Exception{
  String input=provider(1180,1500).toString();check(input.length()>2500000&&input.length()<6500000,"synthetic full response must exceed old 2.5MB cap");
  JSONObject compact=new JSONObject(KitsuneCatalog.compact(input));check(compact.getJSONObject("providers").getJSONObject("anikoto").getJSONObject("meta").getInt("malId")==21,"metadata");JSONObject episodes=compact.getJSONObject("providers").getJSONObject("anikoto").getJSONObject("episodes");check(episodes.getJSONArray("sub").length()==1180,"all sub episodes");check(episodes.getJSONArray("dub").length()==1179,"all dub episodes");check(compact.toString().length()<300000,"large descriptions compacted");check(!compact.toString().contains("unsafe.invalid"),"stream URLs are not copied from episode metadata");
  JSONArray noisy=new JSONArray().put(new JSONObject().put("number",0).put("id","bad")).put(new JSONObject().put("number",5001)).put(new JSONObject().put("number",2).put("id","watch/anikoto/21/sub/anikoto-1").put("audio","sub")).put(new JSONObject().put("number",3).put("id","watch/anikoto/21/sub/anikoto-3").put("audio","sub"));JSONObject n=new JSONObject().put("providers",new JSONObject().put("anikoto",new JSONObject().put("meta",new JSONObject().put("malId",21)).put("episodes",new JSONObject().put("sub",noisy))));JSONArray kept=new JSONObject(KitsuneCatalog.compact(n.toString())).getJSONObject("providers").getJSONObject("anikoto").getJSONObject("episodes").getJSONArray("sub");check(kept.length()==1&&kept.getJSONObject(0).getInt("number")==3,"reject invalid/contradictory episode IDs");
  boolean rejected=false;try{KitsuneCatalog.compact("x".repeat(6500001));}catch(Exception e){rejected=true;}check(rejected,"oversize response refused");
  if(args.length>0){String sample=Files.readString(Paths.get(args[0]),StandardCharsets.UTF_8);JSONObject real=new JSONObject(KitsuneCatalog.compact(sample));JSONObject r=real.getJSONObject("providers").getJSONObject("anikoto").getJSONObject("episodes");check(r.getJSONArray("sub").length()>1000&&r.getJSONArray("dub").length()>1000,"real saved sample compacted");System.out.println("Saved real response: sub="+r.getJSONArray("sub").length()+", dub="+r.getJSONArray("dub").length()+", bytes="+sample.getBytes(StandardCharsets.UTF_8).length);}
  System.out.println("PASS bounded Kitsune response, duplicate payload compaction, episode validation and oversize rejection");
 }
}
