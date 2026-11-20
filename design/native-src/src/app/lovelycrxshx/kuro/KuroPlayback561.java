package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Small, independent validation and app-private progress helpers. */
final class KuroPlayback561 {
    static final long MAX_POSITION_MS = 24L * 60 * 60 * 1000;
    private static final String PREFS = "kuro-native-progress561";
    private KuroPlayback561() {}

    /** This is the existing explicit Zenkai proxy route, not an embed extractor. */
    static boolean allowedHls(String address, String referer, String kind) {
        if (!"zenkai".equals(referer) || !"hls".equals(kind) || address == null || address.length() > 8192) return false;
        try {
            Uri outer = Uri.parse(address);
            if (!"https".equals(outer.getScheme()) || !"www.zenkaitv.com".equals(outer.getHost()) ||
                    outer.getUserInfo() != null || outer.getPort() != -1 || outer.getFragment() != null ||
                    !"/api/source".equals(outer.getPath()) || outer.getQueryParameters("url").size() != 1) return false;
            Uri inner = Uri.parse(outer.getQueryParameter("url"));
            return "https".equals(inner.getScheme()) && "player.zilla-networks.com".equals(inner.getHost()) &&
                    inner.getUserInfo() == null && inner.getPort() == -1 && inner.getFragment() == null &&
                    inner.getPath() != null && inner.getPath().startsWith("/m3u8/");
        } catch (RuntimeException e) { return false; }
    }

    static String key(String source, String showIdentity, String episode) {
        if (source == null || !source.matches("[a-z0-9-]{1,30}") || showIdentity == null ||
                showIdentity.isEmpty() || showIdentity.length() > 4096 || episode == null ||
                !episode.matches("[0-9]{1,5}(?:\\.[0-9]{1,3})?")) return "";
        return sha(source + "\n" + showIdentity + "\n" + episode);
    }
    static String sha(String value) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder(64);
            for(byte b:digest) out.append(String.format(Locale.ROOT,"%02x",b&255));
            return out.toString();
        } catch (Exception e) { return ""; }
    }
    static boolean validKey(String key) { return key != null && key.matches("[0-9a-f]{64}"); }
    static long read(Context context, String key) {
        if (!validKey(key)) return 0;
        long value=context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(key,0);
        return value >= 5000 && value <= MAX_POSITION_MS ? value : 0;
    }
    static void save(Context context, String key, long position, long duration) {
        if (!validKey(key) || position < 0 || position > MAX_POSITION_MS ||
                (duration > 0 && duration <= MAX_POSITION_MS && position > duration + 10000)) return;
        // A deliberate rewind to the beginning must replace an older checkpoint.
        if(position<5000){completed(context,key);return;}
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putLong(key,position).apply();
    }
    static void completed(Context context,String key) {
        if (validKey(key)) context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(key).apply();
    }

    static final class Range {
        final long startMs,endMs;
        Range(long startMs,long endMs){this.startMs=startMs;this.endMs=endMs;}
        boolean contains(long position){return position >= startMs && position < endMs;}
    }
    static Range range(double start,double end) {
        if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start || end > MAX_POSITION_MS/1000.0) return null;
        return new Range((long)(start*1000),(long)(end*1000));
    }
    static Range fromJson(JSONObject object) {
        return object==null ? null : range(object.optDouble("start",Double.NaN),object.optDouble("end",Double.NaN));
    }
    static Range manualRange(Context c,String key) {
        if(!validKey(key))return null;
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        long start=p.getLong("intro-start:"+key,-1),end=p.getLong("intro-end:"+key,-1);
        return start>=0 && end>start && end<=MAX_POSITION_MS ? new Range(start,end) : null;
    }
    static void setManualRange(Context c,String key,Range range) {
        if(!validKey(key))return;
        SharedPreferences.Editor e=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
        if(range==null)e.remove("intro-start:"+key).remove("intro-end:"+key);
        else e.putLong("intro-start:"+key,range.startMs).putLong("intro-end:"+key,range.endMs);
        e.apply();
    }
    // Separate per-episode outro keys preserve existing intro settings.
    static Range manualOutro(Context c,String key) {
        if(!validKey(key))return null;
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        long start=p.getLong("outro-start:"+key,-1),end=p.getLong("outro-end:"+key,-1);
        return start>=0 && end>start && end<=MAX_POSITION_MS ? new Range(start,end) : null;
    }
    static void setManualOutro(Context c,String key,Range range) {
        if(!validKey(key))return;
        SharedPreferences.Editor e=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
        if(range==null)e.remove("outro-start:"+key).remove("outro-end:"+key);
        else e.putLong("outro-start:"+key,range.startMs).putLong("outro-end:"+key,range.endMs);
        e.apply();
    }

}
