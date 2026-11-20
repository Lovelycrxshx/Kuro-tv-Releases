package app.lovelycrxshx.kuro;
import org.json.JSONArray;
import org.json.JSONObject;
/** Bounded, independent adapter. Keeps only AniKoto's identity and episode data. */
public final class KitsuneCatalog {
 private KitsuneCatalog(){}
 public static String compact(String json)throws Exception{
  if(json==null||json.length()>6500000)throw new Exception("Library 12 response is too large");
  JSONObject root=new JSONObject(json),providers=root.optJSONObject("providers"),original=providers==null?null:providers.optJSONObject("anikoto");
  if(original==null)throw new Exception("Library 12 episode list unavailable");
  JSONObject meta=original.optJSONObject("meta"),raw=original.optJSONObject("episodes");if(meta==null||raw==null)throw new Exception("Library 12 episode list is incomplete");
  String title=meta.optString("title","");if(title.length()>240)title=title.substring(0,240);
  JSONObject minimalMeta=new JSONObject().put("title",title).put("malId",meta.optInt("malId",0)),episodes=new JSONObject();
  for(String lang:new String[]{"sub","dub"}){
   JSONArray in=raw.optJSONArray(lang),out=new JSONArray();
   if(in!=null){if(in.length()>5000)throw new Exception("Library 12 episode list is too large");for(int i=0;i<in.length();i++){
    JSONObject e=in.optJSONObject(i);if(e==null)continue;double n=e.optDouble("number",0);String id=e.optString("id","");
    if(!Double.isFinite(n)||n!=Math.rint(n)||n<1||n>5000||id.length()>128)continue;
    if(!id.matches("watch/anikoto/[0-9]{1,8}/"+lang+"/anikoto-"+(int)n)||!lang.equals(e.optString("audio","")))continue;
    String name=e.optString("title","");if(name.length()>160)name=name.substring(0,160);
    out.put(new JSONObject().put("id",id).put("number",(int)n).put("audio",lang).put("title",name));
   }}episodes.put(lang,out);
  }
  return new JSONObject().put("providers",new JSONObject().put("anikoto",new JSONObject().put("meta",minimalMeta).put("episodes",episodes))).toString();
 }
}
