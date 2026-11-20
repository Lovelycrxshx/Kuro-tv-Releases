package app.lovelycrxshx.kuro;

import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

/** Privacy-first presentation of data Kuro actually knows. No player scraping is added here. */
final class KuroPresence564 {
 private KuroPresence564() {}
 static final class Options {
  String layout="anime",separator="dot",pause="show",downloadUrl="",away="quick";
  boolean title=true,episode=true,poster=true,watch=true,server=false,total=false,buffering=true,download=false,rewatch=false;
 }
 static String safeDownloadUrl(String value){
  if(value==null||value.length()>256||value.length()<10)return "";
  try{
   URI u=new URI(value);String host=u.getHost();
   if(!"https".equalsIgnoreCase(u.getScheme())||host==null||u.getPort()!=-1||u.getRawUserInfo()!=null||u.getRawQuery()!=null||u.getRawFragment()!=null)return "";
   host=host.toLowerCase(Locale.ROOT);
   if(!host.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")||!host.contains(".")||host.endsWith(".local")||host.equals("localhost")||host.matches("[0-9.]+"))return "";
   String path=u.getRawPath();if(path==null||!path.startsWith("/")||path.equals("/")||path.length()>220||!path.matches("/[A-Za-z0-9._~/-]+"))return "";
   return "https://"+host+path;
  }catch(Exception ignored){return "";}
 }
 // Source and episode pages are intentionally never used for Discord buttons.
 private static String nameText(String input,int max){
  if(input==null)return "";StringBuilder b=new StringBuilder();int bytes=0;
  for(int i=0;i<input.length();){int cp=input.codePointAt(i);i+=Character.charCount(cp);if(Character.isISOControl(cp)||Character.getType(cp)==Character.FORMAT)cp=' ';String c=new String(Character.toChars(cp));int len=c.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;if(bytes+len>max)break;b.append(c);bytes+=len;}
  return b.toString().replaceAll("\\s+"," ").trim();
 }
 private static boolean inList(ArrayList<String> list,String value){for(String s:list)if(s.equals(value))return true;return false;}
 static String episodeName(String input,String number){
  String n=nameText(input,96).replaceFirst("^[\\s:–—-]+","").trim();
  if(n.isEmpty()||KuroPresence563.unsafePublicText(n)||n.matches("(?i)^(?:episode|ep)[. ]+0*"+java.util.regex.Pattern.quote(number)+"$")||
     n.matches("(?i).*(?:https?://|www\\.|token=|access_token|authorization:).*"))return "";
  return n;
 }
 static KuroPresence563.Snapshot browsing(long sessionStart,Options o){
  // No buttons are published. Title privacy applies here too: when the user
  // hides titles the app never names the service it is idling in.
  boolean named=o!=null&&o.title;
  String details=named?"Looking for an anime to watch":"In the app";
  String state=named?"Kuro.tv":"Browsing";
  return new KuroPresence563.Snapshot(details,state,"","",true,Math.max(0,sessionStart),0,"");
 }
 // Compatibility overload used by the earlier offline tests.
 static KuroPresence563.Snapshot watching(Options o,String title,String episode,String language,String poster,String showUrl,
     String server,String total,String playback,long position,long duration,long now){
  return watching(o,title,episode,"",language,poster,showUrl,"",server,total,playback,position,duration,now,1.0);
 }
 static KuroPresence563.Snapshot watching(Options o,String title,String episode,String episodeTitle,String language,String poster,String showUrl,String episodeUrl,
     String server,String total,String playback,long position,long duration,long now,double rate){
  String titleLine=o.title?KuroPresence563.publicText(title,120,"Anime"):"Watching an anime";
  ArrayList<String> parts=new ArrayList<>();
  if(o.episode){
   String n=episode==null?"":episode.trim();
   if(!n.matches("[0-9]{1,5}(?:\\.[0-9]{1,2})?"))n="?";
   String ep="Episode "+n;
   if(o.total&&total!=null&&total.matches("[1-9][0-9]{0,4}"))ep+=" of "+total;
   parts.add(ep);
  }else parts.add("Watching");
  if("sub".equalsIgnoreCase(language))parts.add("SUB");else if("dub".equalsIgnoreCase(language))parts.add("DUB");
  // A server label is data from the visible server chooser, never a player URL.
  if(o.server&&server!=null){String s=server.trim();if(!KuroPresence563.unsafePublicText(s)&&s.matches("[A-Za-z0-9][A-Za-z0-9 ._-]{0,45}"))parts.add(s);}
  if("paused".equals(playback)){
   String p="Paused";
   if("show".equals(o.pause)&&position>=0&&position<=KuroPresence563.MAX_MEDIA_MS){long sec=position/1000;p+=" "+(sec/60)+":"+String.format(Locale.ROOT,"%02d",sec%60);}
   parts.add(p);
  }else if("buffering".equals(playback)){if(o.buffering)parts.add("Buffering");}
  else if("ended".equals(playback))parts.add("Complete");
  else if("loading".equals(playback))parts.add("Loading");
  else if(!"playing".equals(playback))parts.add("Selected");
  String sep="bar".equals(o.separator)?" | ":"dash".equals(o.separator)?" — ":" · ";
  // Keep the episode name under the same visibility control as its number.
  // Hiding the anime title also hides its identifying episode name.
  if(o.episode&&o.title){
   String name=episodeName(episodeTitle,episode==null?"":episode.trim());
   if(!name.isEmpty()){
    int used=String.join(sep,parts).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    // Reserve the separator, both quotes and the ellipsis so the line never
    // ends in a dangling quote after the 120-byte clamp below.
    int budget=120-used-" - \"\u2026\"".getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    int room=Math.min(96,budget);
    if(room>=4){
     String clipped=nameText(name,room);
     if(clipped.length()<name.length()){
      // Reserve the three bytes of the ellipsis inside the same budget.
      clipped=nameText(name,room-3).replaceAll("[\\s\u2013\u2014-]+$","");
      if(!clipped.isEmpty())clipped+="\u2026";
     }
     if(!clipped.isEmpty())parts.set(0,parts.get(0)+" - \""+clipped+"\"");
    }
   }
  }
  // Rewatching is decided by the app from the episode's own saved progress.
  if(o.rewatch&&!inList(parts,"Rewatching"))parts.add("Rewatching");
  String progress=KuroPresence563.text(String.join(sep,parts),120,"Episode selected");
  String details=titleLine,state=progress;
  if("episode".equals(o.layout)&&o.episode){details=progress;state=titleLine;}
  long start=0,end=0;
  if("playing".equals(playback)&&duration>=5000&&duration<=KuroPresence563.MAX_MEDIA_MS&&position>=0&&position<=duration&&Double.isFinite(rate)&&rate>=0.25&&rate<=4.0){
   long elapsed=Math.round(position/rate),wallDuration=Math.round(duration/rate);
   if(now>=elapsed&&wallDuration>0){start=now-elapsed;end=start+wallDuration;}
  }
  // Only neutral metadata-site covers are shared, and title privacy hides them.
  // Source page identities are never passed into a Discord activity.
  String image=o.title&&o.poster?KuroPresence563.safeUrl(poster,true):"";
  return new KuroPresence563.Snapshot(details,state,image,"",true,start,end,"");
 }
 static KuroPresence563.Snapshot test(){return new KuroPresence563.Snapshot("Kuro connection test","Test activity for 15 seconds","","",true,0,0,"");}
 /** Pause clearing uses monotonic time, never a fabricated player state. */
 static final class PauseClear {
  private long since=-1;
  boolean shouldClear(String playback,String mode,long elapsed){
   if(!"paused".equals(playback)){since=-1;return false;}
   if(since<0||elapsed<since)since=elapsed;
   long threshold="clear1".equals(mode)?60_000L:"clear5".equals(mode)?300_000L:Long.MAX_VALUE;
   return threshold!=Long.MAX_VALUE&&elapsed-since>=threshold;
  }
  void reset(){since=-1;}
 }
}
