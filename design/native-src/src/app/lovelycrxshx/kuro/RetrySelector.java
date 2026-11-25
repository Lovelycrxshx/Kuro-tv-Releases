package app.lovelycrxshx.kuro;
import org.json.JSONArray;
import org.json.JSONObject;

/** Chooses an untried source, preferring the active language; never resets tried. */
public final class RetrySelector {
 private RetrySelector(){}
 public static int next(JSONArray options, boolean[] tried, String language){
  return next(options,tried,language,null);
 }
 public static int next(JSONArray options, boolean[] tried, String language,String[] preferred){
  if(options==null||tried==null)return -1;
  String desired=language==null?"":language;
  if("dub".equalsIgnoreCase(desired)&&preferred!=null)for(String name:preferred){if(name==null||name.isEmpty())continue;for(int i=0;i<options.length()&&i<tried.length;i++){JSONObject x=options.optJSONObject(i);if(!tried[i]&&x!=null&&"dub".equalsIgnoreCase(x.optString("lang",""))&&name.equalsIgnoreCase(x.optString("name","").trim()))return i;}}
  for(int pass=0;pass<2;pass++)for(int i=0;i<options.length()&&i<tried.length;i++){
   JSONObject option=options.optJSONObject(i);if(option==null||tried[i])continue;
   if(pass==1||desired.equalsIgnoreCase(option.optString("lang","")))return i;
  }
  return -1;
 }
}
