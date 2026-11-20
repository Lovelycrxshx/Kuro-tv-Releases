package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local-only RPC choices. Kept separate from Kuro's accounts, lists and existing settings. */
final class KuroDiscordPreferences564 {
 static final String FILE="kuro-discord-rpc563";
 private static final Map<String,String[]> CHOICES=new LinkedHashMap<>();
 static {
  CHOICES.put("layout",new String[]{"anime","episode"});
  CHOICES.put("separator",new String[]{"dot","bar","dash"});
  CHOICES.put("title",new String[]{"show","hide"});
  CHOICES.put("episode",new String[]{"show","hide"});
  CHOICES.put("poster",new String[]{"show","icon"});
  CHOICES.put("watch",new String[]{"show","hide"});
  CHOICES.put("server",new String[]{"off","on"});
  CHOICES.put("total",new String[]{"off","on"});
  CHOICES.put("current",new String[]{"on","off"});
  CHOICES.put("linked",new String[]{"on","off"});
  CHOICES.put("pause",new String[]{"show","noProgress","clear1","clear5"});
  CHOICES.put("buffering",new String[]{"on","off"});
  CHOICES.put("download",new String[]{"off","on"});
  CHOICES.put("notification",new String[]{"off","on"});
  // How long presence survives after Kuro leaves the foreground.
  CHOICES.put("away",new String[]{"quick","min1","min5","keep"});
 }
 private final SharedPreferences prefs;
 KuroDiscordPreferences564(Context c){prefs=c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
 String get(String key){String[] values=CHOICES.get(key);if(values==null)return "";String v=prefs.getString("option-"+key,values[0]);return Arrays.asList(values).contains(v)?v:values[0];}
 boolean set(String key,String value){String[] values=CHOICES.get(key);if(values==null||value==null||!Arrays.asList(values).contains(value))return false;prefs.edit().putString("option-"+key,value).apply();return true;}
 boolean setDownloadUrl(String value){String safe=KuroPresence564.safeDownloadUrl(value);if(value!=null&&!value.isEmpty()&&safe.isEmpty())return false;prefs.edit().putString("download-url",safe).apply();return true;}
 String downloadUrl(){return KuroPresence564.safeDownloadUrl(prefs.getString("download-url",""));}
 JSONObject json(){JSONObject o=new JSONObject();try{for(String k:CHOICES.keySet())o.put(k,get(k));o.put("downloadUrl",downloadUrl());}catch(Exception ignored){}return o;}
 KuroPresence564.Options options(){
  KuroPresence564.Options o=new KuroPresence564.Options();o.layout=get("layout");o.separator=get("separator");o.pause=get("pause");
  o.title="show".equals(get("title"));o.episode="show".equals(get("episode"));o.poster="show".equals(get("poster"));o.watch="show".equals(get("watch"));
  o.server="on".equals(get("server"));o.total="on".equals(get("total"));o.buffering="on".equals(get("buffering"));o.download="on".equals(get("download"));o.downloadUrl=downloadUrl();o.away=get("away");return o;
 }
}
