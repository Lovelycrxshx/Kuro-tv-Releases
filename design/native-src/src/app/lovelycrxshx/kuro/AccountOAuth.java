package app.lovelycrxshx.kuro;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small, separately testable rules for Kuro's public mobile OAuth clients. */
final class AccountOAuth {
 static final String SCHEME="app.lovelycrxshx.kuro";
 static final String MAL_REDIRECT=SCHEME+"://oauth/mal";
 static final String DISCORD_REDIRECT=SCHEME+"://oauth/discord";
 static final String ANILIST_PIN="https://anilist.co/api/v2/oauth/pin";
 static final SecureRandom RANDOM=new SecureRandom();
 private AccountOAuth(){}
 static String random(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 static String challenge(String verifier) throws Exception {return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));}
 static String enc(String value){try{return URLEncoder.encode(value,"UTF-8");}catch(Exception impossible){throw new IllegalStateException(impossible);}}
 static String form(Map<String,String> values){StringBuilder b=new StringBuilder();for(Map.Entry<String,String> e:values.entrySet()){if(b.length()>0)b.append('&');b.append(enc(e.getKey())).append('=').append(enc(e.getValue()));}return b.toString();}
 static Map<String,String> map(String... v){if((v.length&1)!=0)throw new IllegalArgumentException();Map<String,String> m=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)m.put(v[i],v[i+1]);return m;}
 static boolean publicId(String provider,String value){
  if(value==null)return false;
  if("discord".equals(provider))return value.matches("[0-9]{5,25}");
  if("anilist".equals(provider))return value.matches("[0-9]{1,20}");
  return "mal".equals(provider)&&value.matches("[A-Za-z0-9_-]{8,128}");
 }
 static String authorize(String provider,String id,String state,String verifier) throws Exception {
  if("anilist".equals(provider)){
   if(!publicId(provider,id))throw new AccountError("Add your AniList client ID in Account setup first.");
   // AniList's implicit-grant endpoint uses the redirect registered on the
   // developer page. Sending redirect_uri here can trigger unsupported_grant_type.
   return "https://anilist.co/api/v2/oauth/authorize?"+form(map("client_id",id,"response_type","token"));
  }
  if("mal".equals(provider)){
   if(!publicId(provider,id))throw new AccountError("Add your MyAnimeList client ID in Account setup first.");
   // MAL's public native client currently specifies plain PKCE, not S256.
   return "https://myanimelist.net/v1/oauth2/authorize?"+form(map("response_type","code","client_id",id,"redirect_uri",MAL_REDIRECT,"state",state,"code_challenge",verifier,"code_challenge_method","plain"));
  }
  if("discord".equals(provider)){
   if(!publicId(provider,id))throw new AccountError("Add your Discord application ID in Account setup first.");
   return "https://discord.com/oauth2/authorize?"+form(map("response_type","code","client_id",id,"redirect_uri",DISCORD_REDIRECT,"scope","identify","state",state,"code_challenge",challenge(verifier),"code_challenge_method","S256"));
  }
  throw new AccountError("Unknown provider.");
 }
 static final class Callback {
  final String kind,state,code,error;
  Callback(String k,String s,String c,String e){kind=k;state=s;code=c;error=e;}
 }
 static Callback parseCallback(String raw) throws Exception {
  if(raw==null||raw.length()>8192)throw new AccountError("Invalid sign-in return link.");URI u=new URI(raw);
  if(!SCHEME.equals(u.getScheme())||!"oauth".equals(u.getHost())||u.getUserInfo()!=null||u.getPort()!=-1||u.getRawFragment()!=null)throw new AccountError("Invalid sign-in return link.");
  String p=u.getPath(),kind;
  if("/mal".equals(p))kind="mal";else if("/discord".equals(p))kind="discord";else throw new AccountError("Invalid sign-in return link.");
  Map<String,String> q=new LinkedHashMap<>();String query=u.getRawQuery();if(query!=null){for(String pair:query.split("&",-1)){String[] x=pair.split("=",2);String key=URLDecoder.decode(x[0],"UTF-8"),value=URLDecoder.decode(x.length==2?x[1]:"","UTF-8");if(q.containsKey(key))throw new AccountError("Invalid duplicate sign-in parameter.");q.put(key,value);}}
  String state=q.get("state"),code=q.get("code"),error=q.get("error");
  if(state==null||!state.matches("[A-Za-z0-9_-]{40,128}"))throw new AccountError("Sign-in state is missing or invalid.");
  if(code!=null&&(!code.matches("[\\x21-\\x7E]{1,4096}")))throw new AccountError("Invalid sign-in code.");
  if(error!=null&&error.length()>300)throw new AccountError("Invalid sign-in error.");
  if((code==null||code.isEmpty())&&(error==null||error.isEmpty()))throw new AccountError("No sign-in code was returned.");
  return new Callback(kind,state,code==null?"":code,error==null?"":error);
 }
 static boolean sameState(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
}
