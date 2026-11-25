package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.Arrays;

/** Validated app-private settings. No credentials, cookies, profile or list data. */
final class KuroPreferences560 {
 static final String FILE="kuro-settings560";
 private static final String[] BOOLEANS={"keepOn","errorAlerts","pull","adBlock","thirdCookies"};
 private static final String[] STRINGS={"audio","server1","server2","server3","siteView","orientation","startup","theme","accent","external"};
 private static final String[] INTS={"handleSize","handleOpacity","textZoom","brightness"};
 static SharedPreferences prefs(Context c){return c.getSharedPreferences(FILE,Context.MODE_PRIVATE);}
 private static boolean one(String value,String... choices){return Arrays.asList(choices).contains(value);}
 private static boolean valid(String key,String v){switch(key){
  case "audio": return one(v,"dub","default");
  case "server1":case "server2":case "server3": return one(v,"","Ryu","Volt","Warp","Ayame","HD-1");
  case "siteView":return one(v,"mobile","desktop");case "orientation":return one(v,"auto","portrait","landscape");
  case "startup":return one(v,"home","last");case "theme":return one(v,"dark","light");
  case "accent":return one(v,"purple","pink","blue","green","amber","teal","rose","ice");case "external":return one(v,"allow","ask","blocked");default:return false;}}
 private static boolean valid(String key,int v){switch(key){case "handleSize":return v==40||v==48||v==56;case "handleOpacity":return v==60||v==80||v==100;case "textZoom":return v==85||v==100||v==115||v==130;case "brightness":return v==-1||v==40||v==70||v==100;default:return false;}}
 static boolean store(Context c,String raw){
  if(raw==null||raw.length()>10000)return false;
  try{JSONObject j=new JSONObject(raw);if(j.length()!=BOOLEANS.length+STRINGS.length+INTS.length)return false;
   SharedPreferences.Editor e=prefs(c).edit();
   for(String k:BOOLEANS){Object v=j.get(k);if(!(v instanceof Boolean))return false;e.putBoolean(k,(Boolean)v);}
   for(String k:STRINGS){Object v=j.get(k);if(!(v instanceof String)||!valid(k,(String)v))return false;e.putString(k,(String)v);}
   for(String k:INTS){Object v=j.get(k);if(!(v instanceof Number)||((Number)v).doubleValue()!=((Number)v).intValue()||!valid(k,((Number)v).intValue()))return false;e.putInt(k,((Number)v).intValue());}
   e.apply();return true;
  }catch(Exception error){return false;}
 }
 static String[] preferredServers(Context c){SharedPreferences p=prefs(c);return new String[]{p.getString("server1",""),p.getString("server2",""),p.getString("server3","")};}
}
