package app.lovelycrxshx.kuro;

import java.io.*;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Bounded, offline player-only host filtering. It does not modify device DNS. */
public final class PlayerAdBlocker {
 private static final Set<String> NETWORKS=new HashSet<>(Arrays.asList(
  "doubleclick.net","googlesyndication.com","googleadservices.com","adservice.google.com",
  "adnxs.com","adsrvr.org","exoclick.com","exosrv.com","exdynsrv.com","realsrv.com","orbsrv.com","optnx.com","exv6.com","magsrv.com",
  "trafficjunky.net","trafficjunky.com","juicyads.com","propellerads.com","propellerads.net","popads.net","popcash.net","popca.sh",
  "adsterra.com","adsterra.net","profitabledisplaynetwork.com","highrevenuegate.com","effectivegatecpm.com","highcpmgate.com",
  "onclicka.com","onclickperformance.com","trafficstars.com","clickadu.com","trafficfactory.biz"));
 private static final Set<String> CHALLENGES=new HashSet<>(Arrays.asList("challenges.cloudflare.com","hcaptcha.com","recaptcha.net"));
 private static final Pattern MEDIA_PATH=Pattern.compile(".*\\.(m3u8|mpd|m4s|ts|mp4|webm|vtt|srt|aac|m4a|key)$"),HOST_NAME=Pattern.compile("[a-z0-9.-]{1,253}");
 private final Set<String> domains;
 private PlayerAdBlocker(Set<String> values){domains=Collections.unmodifiableSet(values);}
 public static PlayerAdBlocker empty(){return new PlayerAdBlocker(new HashSet<String>());}
 public static PlayerAdBlocker from(InputStream input)throws IOException{
  HashSet<String> values=new HashSet<>();
  if(input==null)return empty();
  try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
   String line;int n=0;while((line=reader.readLine())!=null&&n++<100_000){
    String[] words=line.trim().split("\\s+");if(words.length<2||!("0.0.0.0".equals(words[0])||"127.0.0.1".equals(words[0])))continue;
    String h=normalize(words[1]);if(!h.isEmpty()&&h.contains(".")&&!h.equals("localhost"))values.add(h);
   }
  }return new PlayerAdBlocker(values);
 }
 private static String normalize(String input){
  if(input==null||input.length()>254)return "";
  try{String h=IDN.toASCII(input.trim().toLowerCase(Locale.ROOT),IDN.USE_STD3_ASCII_RULES);if(h.endsWith("."))h=h.substring(0,h.length()-1);return HOST_NAME.matcher(h).matches()?h:"";}catch(Exception e){return "";}
 }
 private static boolean belongs(Set<String> set,String h){
  for(String s=h;;){if(set.contains(s))return true;int dot=s.indexOf('.');if(dot<0)return false;s=s.substring(dot+1);}
 }
 private static boolean mediaPath(String path){
  if(path==null)return false;String p=path.toLowerCase(Locale.ROOT);
  return MEDIA_PATH.matcher(p).matches();
 }
 /** Main-frame navigations, player origins, human verification and normal media
  * segments are never blocked by the broad snapshot. Identified ad networks are
  * still blocked even if they label an ad resource as video. */
 public boolean blocked(String scheme,String host,String path,boolean mainFrame,boolean playerOrigin){
  if(mainFrame||playerOrigin||!("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme)))return false;
  String h=normalize(host);if(h.isEmpty()||belongs(CHALLENGES,h))return false;
  String p=path==null?"":path.toLowerCase(Locale.ROOT);
  if((h.equals("www.google.com")||h.equals("google.com")||h.equals("www.gstatic.com"))&&p.startsWith("/recaptcha/"))return false;
  if(belongs(NETWORKS,h))return true;
  if(mediaPath(p)||h.equals("googlevideo.com")||h.endsWith(".googlevideo.com"))return false;
  // Hosts files describe exact names, not wildcard DNS rules. Expanding the
  // whole list to every subdomain can break unrelated video/CDN services.
  return domains.contains(h);
 }
 public int size(){return domains.size();}
}
