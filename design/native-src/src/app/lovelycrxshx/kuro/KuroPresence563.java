package app.lovelycrxshx.kuro;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure, bounded presence model. Never accepts streams, tokens, cookies or arbitrary image hosts. */
final class KuroPresence563 {
 static final long MAX_MEDIA_MS=24L*60*60*1000, MIN_SEND_MS=3000, REFRESH_MS=3000;
 // Only neutral metadata-service posters may be sent to Discord. Provider-hosted
 // artwork falls back to the registered Kuro app icon.
 private static final Set<String> POSTERS=new HashSet<>(Arrays.asList(
  "s4.anilist.co","s3.anilist.co","cdn.myanimelist.net","media.kitsu.app","media.kitsu.io"));
 // Reject obvious URLs, source names and credential-shaped strings in public
 // presence text. This is a conservative best effort, not network anonymity.
 private static final Pattern PRIVATE_TEXT=Pattern.compile(
  "(?i)(?:https?\\s*://|\\bwww\\s*\\.|\\b(?:[a-z0-9-]+\\.)+"+
  "(?:com|net|org|tv|ms|dk|ru|cx|top|ba|xyz|moe|io|me|app|cc|to|at|co|ws|sh|pw|site|info|link|online)\\b|"+
  "\\b(?:hianimeq?|9anime|zorotv|animesalt|aniwaves|zenkaitv|aniwixi|animeav1|aniwatch|miruro|kitsunee|animeparadise)\\b|"+
  "\\b(?:access_token|token|authorization)\\s*[:=])");
 static boolean unsafePublicText(String value){return value!=null&&PRIVATE_TEXT.matcher(value).find();}
 static String publicText(String input,int max,String fallback){String clean=text(input,max,fallback);return unsafePublicText(input)||unsafePublicText(clean)?fallback:clean;}
 private KuroPresence563() {}

 /** Limit by UTF-8 bytes without cutting a Unicode code point. */
 static String text(String input,int max,String fallback) {
  String raw=input==null?"":input.trim(), out="";StringBuilder b=new StringBuilder();int count=0;
  for(int i=0;i<raw.length();) {
   int cp=raw.codePointAt(i);i+=Character.charCount(cp);
   if(Character.isISOControl(cp)||Character.getType(cp)==Character.FORMAT)cp=' ';
   String c=new String(Character.toChars(cp));int size=c.getBytes(StandardCharsets.UTF_8).length;
   if(count+size>max)break;b.append(c);count+=size;
  }
  out=b.toString().replaceAll("\\s+"," ").trim();
  if(out.codePointCount(0,out.length())<2)out=fallback;
  return out;
 }
 static String safeUrl(String value,boolean image){
  if(!image||value==null||value.length()>300||value.length()<10)return "";
  try{
   URI u=new URI(value);String host=u.getHost();
   if(!"https".equalsIgnoreCase(u.getScheme())||host==null||u.getRawUserInfo()!=null||u.getPort()!=-1||u.getRawQuery()!=null||u.getRawFragment()!=null)return "";
   host=host.toLowerCase(Locale.ROOT);if(!POSTERS.contains(host))return "";
   String path=u.getRawPath();if(path==null||!path.startsWith("/")||path.equals("/")||path.length()>220)return "";
   // Accept only recognizable anime poster paths, not episode thumbnails or banners.
   if((host.equals("s4.anilist.co")||host.equals("s3.anilist.co"))&&!path.matches("/file/anilistcdn/media/anime/cover/(small|medium|large)/[A-Za-z0-9_~.-]+\\.(jpg|jpeg|png|webp)"))return "";
   if(host.equals("cdn.myanimelist.net")&&!path.matches("/images/anime/[0-9]{1,10}/[0-9][A-Za-z0-9_~.-]*\\.(jpg|jpeg|png|webp)"))return "";
   if(host.equals("media.kitsu.app")||host.equals("media.kitsu.io")){
    boolean legacy=path.matches("/anime/poster_images/[0-9]{1,10}/(original|large|medium|small|tiny)\\.(jpg|jpeg|png|webp)");
    boolean newer=path.matches("/anime/[0-9]{1,10}/(poster|poster_image)/[A-Za-z0-9_~.-]+\\.(jpg|jpeg|png|webp)");
    if(!legacy&&!newer)return "";
   }
   // Paths containing escapes, delimiters or controls can carry sensitive access data.
   if(!path.matches("/[A-Za-z0-9._~/-]+")||unsafePublicText(path))return "";
   return "https://"+host+path;
  }catch(Exception ignored){return "";}
 }
 static final class Snapshot {
  final String details,state,image,buttonUrl,downloadUrl;final boolean watching;final long startMs,endMs;
  Snapshot(String d,String s,String i,String b,boolean w,long start,long end){this(d,s,i,b,w,start,end,"");}
  Snapshot(String d,String s,String i,String b,boolean w,long start,long end,String download){
   details=d==null?"":d;state=s==null?"":s;image=i==null?"":i;buttonUrl="";watching=w;
   startMs=Math.max(0,start);endMs=end>startMs?end:0;
   downloadUrl=KuroPresence564.DOWNLOAD_PAGE_URL.equals(download)?download:"";
  }
  boolean equivalent(Snapshot x){
   if(x==null||watching!=x.watching||!details.equals(x.details)||!state.equals(x.state)||!image.equals(x.image)||!buttonUrl.equals(x.buttonUrl)||!downloadUrl.equals(x.downloadUrl))return false;
   if((startMs==0)!=(x.startMs==0)||(endMs==0)!=(x.endMs==0))return false;
   return startMs==0 || (Math.abs(startMs-x.startMs)<5000 && Math.abs(endMs-x.endMs)<5000);
  }
 }
 private static String clock(long ms){long s=ms/1000;return (s/60)+":"+String.format(Locale.ROOT,"%02d",s%60);}
 /** After SDK errors, progressively back off; success resets the cadence. */
 static final class Retry {
  private long delay=MIN_SEND_MS;
  boolean ready(boolean failed,long sentAt,long now){if(!failed){delay=MIN_SEND_MS;return true;}return sentAt<0||now<sentAt||now-sentAt>=delay;}
  void sent(boolean failed){delay=failed?Math.min(60000,delay*2):MIN_SEND_MS;}
  long waitFor(long sentAt,long now){if(sentAt<0||now<sentAt)return 0;long left=sentAt+delay-now;return left>0?left:0;}
  void reset(){delay=MIN_SEND_MS;}
 }
 /** Coalesces updates to a maximum of one request per three seconds. */
 static final class Gate {
  private Snapshot last;private long sentAt=-1;
  boolean due(Snapshot next,long elapsed){
   if(next==null)return false;if(last==null||sentAt<0||elapsed<sentAt)return true;
   long age=elapsed-sentAt;if(age<MIN_SEND_MS)return false;
   return !last.equivalent(next)||age>=REFRESH_MS;
  }
  void sent(Snapshot next,long elapsed){last=next;sentAt=elapsed;}
  void reset(){last=null;sentAt=-1;}
 }
}
