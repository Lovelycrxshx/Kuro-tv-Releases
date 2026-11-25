package app.lovelycrxshx.kuro;

import org.json.JSONObject;

/** Validates a bounded frame-local playback report. It carries no URL or media bytes. */
final class KuroProgress565 {
 static final long MIN_WEB_DURATION_MS=60_000L, MAX_AGE_MS=9_000L;
 private KuroProgress565() {}
 static final class Sample {
  final String state; final long position,duration,received; final double rate;
  Sample(String s,long p,long d,double r,long at){state=s;position=p;duration=d;rate=r;received=at;}
  boolean fresh(long now){return now>=received&&now-received<=MAX_AGE_MS;}
  long positionAt(long now){
   if(!fresh(now)||!"playing".equals(state))return position;
   // Extrapolate only a fresh observation; later pauses or seeks cause a new sample.
   long advance=Math.round(Math.min(3_000L,now-received)*rate);
   return Math.min(duration,position+advance);
  }
 }
 static Sample parse(String raw,long expectedAttempt,long received){
  if(raw==null||raw.length()>400||expectedAttempt<1||received<0)return null;
  try{
   JSONObject j=new JSONObject(raw);if(!"sample".equals(j.optString("kind"))||j.optLong("attempt",-1)!=expectedAttempt)return null;
   String state=j.optString("state","");if(!("playing".equals(state)||"paused".equals(state)||"buffering".equals(state)||"ended".equals(state)))return null;
   Object p=j.opt("position"),d=j.opt("duration"),r=j.opt("rate");
   if(!(p instanceof Number)||!(d instanceof Number)||!(r instanceof Number))return null;
   double pos=((Number)p).doubleValue(),dur=((Number)d).doubleValue(),speed=((Number)r).doubleValue();
   if(!Double.isFinite(pos)||!Double.isFinite(dur)||!Double.isFinite(speed)||pos!=Math.rint(pos)||dur!=Math.rint(dur)||
      dur<MIN_WEB_DURATION_MS||dur>KuroPresence563.MAX_MEDIA_MS||pos<0||pos>dur||speed<0.25||speed>4.0)return null;
   return new Sample(state,(long)pos,(long)dur,speed,received);
  }catch(Exception ignored){return null;}
 }
}
