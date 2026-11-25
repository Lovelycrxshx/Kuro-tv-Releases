package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/** User-initiated OAuth code + PKCE flow. The SDK exchanges the returned code. */
final class KuroDiscordOAuth564 {
 static final String SCHEME="discord-1569077670074122250",REDIRECT=SCHEME+":/authorize/callback";
 private static final long MAX_AGE=10*60*1000L;
 static final class Result{final String code,verifier,error;final boolean consume;Result(String c,String v,String e,boolean used){code=c;verifier=v;error=e;consume=used;}}
 private KuroDiscordOAuth564(){}
 private static String random(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.encodeToString(b,Base64.NO_WRAP|Base64.NO_PADDING|Base64.URL_SAFE);}
 static boolean isCallback(Intent i){return i!=null&&i.getData()!=null&&SCHEME.equalsIgnoreCase(i.getData().getScheme());}
 static boolean begin(Activity a){
  try{
   String verifier=random(),state=random();String challenge=Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)),Base64.NO_WRAP|Base64.NO_PADDING|Base64.URL_SAFE);
   JSONObject p=new JSONObject().put("verifier",verifier).put("state",state).put("created",System.currentTimeMillis());
   if(!KuroDiscordVault564.store(a,"pending",p.toString().getBytes(StandardCharsets.UTF_8)))return false;
   Uri uri=Uri.parse("https://discord.com/oauth2/authorize").buildUpon().appendQueryParameter("client_id",String.valueOf(KuroDiscordRpc563.APPLICATION_ID))
    .appendQueryParameter("redirect_uri",REDIRECT).appendQueryParameter("response_type","code").appendQueryParameter("scope","openid sdk.social_layer_presence")
    .appendQueryParameter("state",state).appendQueryParameter("code_challenge",challenge).appendQueryParameter("code_challenge_method","S256").appendQueryParameter("prompt","consent").build();
   Intent browser=new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE);
   // Prefer the user's default web browser, so they can sign in there as a different account.
   Intent probe=new Intent(Intent.ACTION_VIEW,Uri.parse("https://example.org/")).addCategory(Intent.CATEGORY_BROWSABLE);
   ResolveInfo ri=a.getPackageManager().resolveActivity(probe,PackageManager.MATCH_DEFAULT_ONLY);
   if(ri!=null&&ri.activityInfo!=null&&!a.getPackageName().equals(ri.activityInfo.packageName)&&!"android".equals(ri.activityInfo.packageName))browser.setPackage(ri.activityInfo.packageName);
   a.startActivity(browser);return true;
  }catch(Exception ignored){KuroDiscordVault564.remove(a,"pending");return false;}
 }
 private static String one(Uri u,String key){List<String> values=u.getQueryParameters(key);return values.size()==1?values.get(0):"";}
 static Result validate(Uri u,String pending,long now){
  try{
   if(u==null||!SCHEME.equalsIgnoreCase(u.getScheme())||u.toString().length()>8192||u.getEncodedAuthority()!=null||!"/authorize/callback".equals(u.getPath())||u.getFragment()!=null)return new Result("","","Invalid Discord callback.",false);
   if(pending==null||pending.isEmpty())return new Result("","","No pending Discord authorization. Please try linking again.",false);
   JSONObject p=new JSONObject(pending);String expected=p.optString("state",""),received=one(u,"state"),verifier=p.optString("verifier","");long created=p.optLong("created",0);
   if(created<=0||now<created||now-created>MAX_AGE)return new Result("","","Discord authorization expired. Please try again.",true);
   if(!expected.matches("[A-Za-z0-9_-]{43}")||!verifier.matches("[A-Za-z0-9_-]{43}")||!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),received.getBytes(StandardCharsets.US_ASCII)))return new Result("","","Discord authorization state did not match.",false);
   String code=one(u,"code"),err=one(u,"error");
   if(!err.isEmpty())return new Result("","","Discord authorization was cancelled or denied.",true);
   if(!code.matches("[A-Za-z0-9._~-]{3,2048}"))return new Result("","","Discord did not return a valid authorization code.",true);
   return new Result(code,verifier,"",true);
  }catch(Exception ignored){return new Result("","","Could not complete Discord authorization. Please try again.",false);}
 }
 static Result consume(Context c,Intent intent){
  if(!isCallback(intent))return null;
  Uri u=intent.getData();intent.setData(null); // do not retain the code on an Activity intent
  try{
   byte[] raw=KuroDiscordVault564.load(c,"pending");String pending=null;
   if(raw!=null){pending=new String(raw,StandardCharsets.UTF_8);Arrays.fill(raw,(byte)0);}
   Result r=validate(u,pending,System.currentTimeMillis());if(r.consume)KuroDiscordVault564.remove(c,"pending");return r;
  }catch(Exception ignored){return new Result("","","Could not complete Discord authorization. Please try again.",false);}
 }
}
