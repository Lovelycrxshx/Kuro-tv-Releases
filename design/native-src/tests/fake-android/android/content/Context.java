package android.content;
import java.util.*;import android.content.res.Resources;import android.content.pm.ApplicationInfo;
public class Context {
 public static final int MODE_PRIVATE=0;public static final String NOTIFICATION_SERVICE="notification", JOB_SCHEDULER_SERVICE="jobs";
 public static int permission=0;public static final Map<String,Object> SERVICES=new HashMap<>();public static final Map<String,SharedPreferences> PREFS=new HashMap<>();
 public Context getApplicationContext(){return this;}public Object getSystemService(String name){return SERVICES.get(name);}public int checkSelfPermission(String name){return permission;}
 public String getPackageName(){return "app.lovelycrxshx.kuro";}public Resources getResources(){return new Resources();}public ApplicationInfo getApplicationInfo(){return new ApplicationInfo();}
 public SharedPreferences getSharedPreferences(String name,int mode){return PREFS.computeIfAbsent(name,x->new MemoryPrefs());}
 static class MemoryPrefs implements SharedPreferences {final Map<String,Object> m=new HashMap<>();public boolean getBoolean(String k,boolean d){return m.containsKey(k)?(Boolean)m.get(k):d;}public long getLong(String k,long d){return m.containsKey(k)?(Long)m.get(k):d;}public String getString(String k,String d){return m.containsKey(k)?(String)m.get(k):d;}public Editor edit(){return new E();}class E implements Editor {Map<String,Object> pending=new HashMap<>();public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}public Editor putLong(String k,long v){pending.put(k,v);return this;}public Editor putString(String k,String v){pending.put(k,v);return this;}public void apply(){m.putAll(pending);}}}
}
