package app.lovelycrxshx.kuro;

import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * One local email/password convenience account per app installation.
 * It does not prove ownership of an address, call a server, back up data, or
 * lock/encrypt the user's separate WebView profile and watch history.
 * The credential record is stored in the existing Android Keystore-backed
 * vault; it contains a salted PBKDF2 verifier, never the plaintext password.
 */
final class LocalEmail551 {
 static final String RECORD="local.email.record.v1";
 static final int ITERATIONS=310000;
 interface Clock { long now(); }
 private final AccountVault vault;
 private final Clock clock;
 private final SecureRandom random;
 LocalEmail551(AccountVault v){this(v,System::currentTimeMillis,new SecureRandom());}
 LocalEmail551(AccountVault v,Clock c,SecureRandom r){vault=v;clock=c;random=r;}

 private static String email(String raw) throws AccountError {
  String value=raw==null?"":raw.trim().toLowerCase(Locale.ROOT);
  if(value.length()>254||!value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new AccountError("Enter a valid email address.");
  return value;
 }
 private static void password(String value,boolean creating) throws AccountError {
  if(value==null||value.isEmpty()||value.length()>1024)throw new AccountError("Enter your local password.");
  if(creating&&value.length()<8)throw new AccountError("Choose a password of at least 8 characters.");
 }
 private static byte[] derive(String value,byte[] salt,int iterations) throws Exception {
  char[] chars=value.toCharArray();PBEKeySpec spec=new PBEKeySpec(chars,salt,iterations,256);
  try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
  catch(java.security.GeneralSecurityException e){throw new AccountError("Local password protection is unavailable on this phone. No account was changed.");}
  finally{spec.clearPassword();Arrays.fill(chars,'\0');}
 }
 private JSONObject read() throws Exception {
  String raw=vault.get(RECORD);if(raw.isEmpty())return null;
  try{
   JSONObject r=new JSONObject(raw);
   if(r.getInt("v")!=1||r.getInt("iterations")!=ITERATIONS)throw new Exception();
   email(r.getString("email"));
   if(Base64.getDecoder().decode(r.getString("salt")).length!=16||Base64.getDecoder().decode(r.getString("hash")).length!=32)throw new Exception();
   int failures=r.optInt("failures",0);long retry=r.optLong("retryAfter",0);
   if(failures<0||failures>20||retry<0)throw new Exception();
   return r;
  }catch(Exception e){throw new AccountError("Local sign-in settings could not be read. Nothing was changed.");}
 }
 private void write(JSONObject record) throws Exception {vault.put(RECORD,record.toString());}
 synchronized JSONObject status() throws Exception {
  JSONObject record=read(),out=new JSONObject().put("connected",record!=null&&record.optBoolean("signedIn",false)).put("exists",record!=null);
  if(record!=null&&record.optBoolean("signedIn",false))out.put("name",record.getString("email")).put("method","local-email");
  return out;
 }
 synchronized JSONObject signup(String address,String pass) throws Exception {
  String mail=email(address);password(pass,true);
  if(read()!=null)throw new AccountError("A local email account already exists on this phone. Sign in with it; nothing was overwritten.");
  byte[] salt=new byte[16];random.nextBytes(salt);byte[] hash=derive(pass,salt,ITERATIONS);
  JSONObject record=new JSONObject().put("v",1).put("email",mail).put("salt",Base64.getEncoder().encodeToString(salt)).put("hash",Base64.getEncoder().encodeToString(hash)).put("iterations",ITERATIONS).put("signedIn",true).put("failures",0).put("retryAfter",0);
  Arrays.fill(hash,(byte)0);write(record);
  return new JSONObject().put("message","Local email account created on this phone. Your email was not verified.");
 }
 synchronized JSONObject login(String address,String pass) throws Exception {
  String mail=email(address);password(pass,false);JSONObject record=read();
  if(record==null)throw new AccountError("No local email account exists on this phone. Choose Create account first.");
  long now=Math.max(0,clock.now()),retry=record.optLong("retryAfter",0);
  if(retry>now)throw new AccountError("Too many attempts. Wait before trying your local password again.");
  byte[] salt=Base64.getDecoder().decode(record.getString("salt")),expected=Base64.getDecoder().decode(record.getString("hash")),candidate=derive(pass,salt,ITERATIONS);
  boolean passwordMatches=MessageDigest.isEqual(expected,candidate);
  boolean emailMatches=MessageDigest.isEqual(record.getString("email").getBytes(StandardCharsets.UTF_8),mail.getBytes(StandardCharsets.UTF_8));
  boolean equal=passwordMatches & emailMatches;
  Arrays.fill(candidate,(byte)0);Arrays.fill(expected,(byte)0);
  if(!equal){
   int failures=Math.min(20,record.optInt("failures",0)+1);record.put("failures",failures);
   if(failures>=5)record.put("retryAfter",now+Math.min(300000L,30000L*(failures-4)));
   write(record);throw new AccountError(failures>=5?"Email or password did not match. Wait before trying again.":"Email or password did not match this phone's local account.");
  }
  record.put("signedIn",true).put("failures",0).put("retryAfter",0);write(record);
  return new JSONObject().put("message","Signed in to the local account on this phone.");
 }
 synchronized JSONObject signOut() throws Exception {
  JSONObject record=read();if(record==null)throw new AccountError("No local email account exists on this phone.");
  record.put("signedIn",false);write(record);
  return new JSONObject().put("message","Signed out locally. Your account, profile and saved lists were kept on this phone.");
 }
}
