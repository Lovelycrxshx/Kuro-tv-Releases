package app.lovelycrxshx.kuro;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.HttpsURLConnection;

/** GitHub device verification for the owner. No token is persisted or sent to JS. */
final class GitHubAdmin568 {
 static final String CLIENT_ID="Ov23li2LjGiWV0VW2K7p"; // public OAuth application ID, not a secret
 static final long OWNER_ID=226603888L; // GitHub's durable ID for the confirmed ImAuraxSiva account
 static final String VERIFY_URL="https://github.com/login/device";
 static final String DEVICE_URL="https://github.com/login/device/code";
 static final String TOKEN_URL="https://github.com/login/oauth/access_token";
 static final String USER_URL="https://api.github.com/user";
 static final long SESSION_MS=10*60*1000L;
 interface Transport { JSONObject post(String endpoint,String form) throws Exception; JSONObject user(String token) throws Exception; }
 private final Transport http;private final LongSupplier clock;
 private volatile boolean closed;private final AtomicLong generation=new AtomicLong();
 private String device="",userCode="";
 private long issuedAt=-1,expiresAt=-1,nextPoll=-1,interval=5000,backoff=5000,verifiedAt=-1,verifiedUntil=-1,pendingGeneration=-1,verifiedGeneration=-1;
 GitHubAdmin568(){this(new Http(),androidClock());}
 GitHubAdmin568(Transport http,LongSupplier clock){this.http=http;this.clock=clock;}
 private static LongSupplier androidClock(){return ()->android.os.SystemClock.elapsedRealtime();}
 private void checkOpen() throws AccountError {if(closed)throw new AccountError("Owner verification was closed. Start again.");}
 private void clearPending(){device="";userCode="";issuedAt=expiresAt=nextPoll=pendingGeneration=-1;interval=backoff=5000;}
 private void clearVerified(){verifiedAt=verifiedUntil=verifiedGeneration=-1;}
 /** Invalidate immediately, even when a network call currently holds the monitor. */
 long invalidate(){return generation.incrementAndGet();}
 synchronized void clearInvalidated(long invalidation){if(generation.get()==invalidation){clearPending();clearVerified();}}
 void cancel(){long invalidation=invalidate();clearInvalidated(invalidation);}
 private void checkAttempt(long attempt) throws AccountError {checkOpen();if(generation.get()!=attempt)throw new AccountError("Owner verification was cancelled. Start again.");}
 /** Non-blocking lifecycle invalidation: a request finishing after destruction cannot grant access. */
 void close(){closed=true;}
 synchronized boolean verified(){
  long now=clock.getAsLong();if(closed||verifiedGeneration!=generation.get()||verifiedAt<0||now<verifiedAt||now>=verifiedUntil){clearVerified();return false;}return true;
 }
 synchronized JSONObject status() throws Exception {return new JSONObject().put("verified",verified()).put("remainingMs",verified()?Math.max(0,verifiedUntil-clock.getAsLong()):0);}
 synchronized JSONObject begin() throws Exception {
  checkOpen();long attempt=generation.incrementAndGet();clearPending();clearVerified();
  JSONObject r;
  try{r=http.post(DEVICE_URL,AccountOAuth.form(AccountOAuth.map("client_id",CLIENT_ID,"scope","")));}
  catch(IOException e){throw new AccountError("Could not reach GitHub. Check your connection and try again.");}
  checkAttempt(attempt);
  if(r.has("error"))throw new AccountError("GitHub could not start device verification. Check that Device Flow is enabled.");
  String d=r.optString("device_code",""),code=r.optString("user_code",""),url=r.optString("verification_uri","");
  int seconds=r.optInt("expires_in",-1),wait=r.optInt("interval",-1);
  if(!d.matches("[A-Za-z0-9_-]{16,256}")||!code.matches("[A-Z0-9]{4}-[A-Z0-9]{4}")||!VERIFY_URL.equals(url)||seconds<1||seconds>3600||wait<1||wait>300)
   throw new AccountError("GitHub returned an unexpected verification response. Try again later.");
  long now=clock.getAsLong();device=d;userCode=code;issuedAt=now;expiresAt=now+seconds*1000L;pendingGeneration=attempt;interval=Math.max(5000L,wait*1000L);backoff=interval;nextPoll=now+interval;
  return new JSONObject().put("userCode",code).put("verificationUrl",VERIFY_URL).put("expiresSeconds",seconds).put("retryMs",interval);
 }
 private void requirePending() throws AccountError {
  checkOpen();long now=clock.getAsLong();
  if(device.isEmpty()||pendingGeneration!=generation.get()||issuedAt<0||now<issuedAt||now>=expiresAt){clearPending();throw new AccountError("Verification expired. Start again.");}
 }
 synchronized String currentCode() throws AccountError {requirePending();return userCode;}
 private JSONObject waiting(String notice) throws Exception {
  long wait=Math.max(250,nextPoll-clock.getAsLong());return new JSONObject().put("state","waiting").put("retryMs",wait).put("notice",notice);
 }
 synchronized JSONObject poll() throws Exception {
  requirePending();long attempt=pendingGeneration,now=clock.getAsLong();if(now<nextPoll)return waiting("");
  nextPoll=now+interval;JSONObject r;
  try{r=http.post(TOKEN_URL,AccountOAuth.form(AccountOAuth.map("client_id",CLIENT_ID,"device_code",device,"grant_type","urn:ietf:params:oauth:grant-type:device_code")));}
  catch(IOException e){
   checkAttempt(attempt);backoff=Math.min(60000L,Math.max(interval,backoff*2));nextPoll=clock.getAsLong()+backoff;
   return waiting("Connection interrupted; waiting before retrying GitHub.");
  }catch(AccountError e){clearPending();throw e;}
  checkOpen();requirePending();backoff=interval;String error=r.optString("error","");
  if("authorization_pending".equals(error)){nextPoll=clock.getAsLong()+interval;return waiting("");}
  if("slow_down".equals(error)){
   long requested=r.optLong("interval",0)*1000L;long newInterval=Math.max(interval+5000L,requested);
   if(newInterval>300000L){clearPending();throw new AccountError("GitHub requested a longer wait. Start verification again later.");}
   interval=newInterval;nextPoll=clock.getAsLong()+interval;return waiting("GitHub asked Kuro to slow down.");
  }
  if(!error.isEmpty()){
   clearPending();
   if("access_denied".equals(error))throw new AccountError("GitHub verification was declined. Start again if you want to verify.");
   if("expired_token".equals(error))throw new AccountError("Verification expired. Start again.");
   if("device_flow_disabled".equals(error))throw new AccountError("Enable Device Flow in the GitHub OAuth app settings, then try again.");
   throw new AccountError("GitHub could not complete verification. Start again later.");
  }
  String token=r.optString("access_token",""),type=r.optString("token_type","");
  boolean narrow=r.has("scope")&&!r.isNull("scope")&&r.optString("scope","").trim().isEmpty();
  r.remove("access_token");r.remove("refresh_token");clearPending();
  if(!narrow||!"bearer".equalsIgnoreCase(type)||!token.matches("[A-Za-z0-9_.-]{8,512}"))
   throw new AccountError("GitHub returned an unsafe or unexpected authorization. Revoke extra app permissions and try again.");
  JSONObject identity;
  try{identity=http.user(token);}catch(IOException e){throw new AccountError("Could not confirm the GitHub account. Start verification again.");}
  finally{token="";} // never stored, logged, persisted, or passed to the WebView
  checkAttempt(attempt);
  if(identity.optLong("id",0)!=OWNER_ID){clearVerified();throw new AccountError("That GitHub account is not the owner. Sign in as ImAuraxSiva and start again.");}
  now=clock.getAsLong();verifiedAt=now;verifiedUntil=now+SESSION_MS;verifiedGeneration=attempt;
  return new JSONObject().put("state","verified").put("remainingMs",SESSION_MS);
 }
 /** Minimal, fixed-endpoint HTTPS transport; no redirects, cookies, tokens in URLs, or logs. */
 static final class Http implements Transport {
  public JSONObject post(String endpoint,String form) throws Exception {
   if(!(DEVICE_URL.equals(endpoint)||TOKEN_URL.equals(endpoint))||form==null||form.length()>4096)throw new AccountError("Blocked an unsafe GitHub request.");
   return request(endpoint,"POST",form,Collections.emptyMap());
  }
  public JSONObject user(String token) throws Exception {
   if(token==null||!token.matches("[A-Za-z0-9_.-]{8,512}"))throw new AccountError("Invalid GitHub authorization.");
   return request(USER_URL,"GET",null,Collections.singletonMap("Authorization","Bearer "+token));
  }
  private static JSONObject request(String address,String method,String body,Map<String,String> headers) throws Exception {
   HttpsURLConnection c=(HttpsURLConnection)new URL(address).openConnection();
   c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestMethod(method);
   c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","Kuro.tv/5.68 (Android)");
   for(Map.Entry<String,String> h:headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
   try{
    if(body!=null){byte[] bytes=body.getBytes(StandardCharsets.UTF_8);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
    int status=c.getResponseCode();
    if(status==429)throw new AccountError("GitHub rate-limited verification. Wait before trying again.");
    if(status==401||status==403)throw new AccountError("GitHub refused verification (HTTP "+status+"). Check access before retrying.");
    if(status<200||status>=300)throw new AccountError("GitHub verification could not continue (HTTP "+status+").");
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
     byte[] buf=new byte[4096];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>65536)throw new AccountError("GitHub returned an oversized response.");out.write(buf,0,n);}
     return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
    }
   }finally{c.disconnect();}
  }
 }
}
