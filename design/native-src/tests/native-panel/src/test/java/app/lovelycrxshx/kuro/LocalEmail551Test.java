package app.lovelycrxshx.kuro;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import static org.junit.Assert.*;

/** Local account tests use memory vaults and simulated clocks, not a real Keystore. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class LocalEmail551Test {
 static final String ADDRESS="Some.User@Example.invalid",PASSWORD="strong fake password";
 static class Clock implements LocalEmail551.Clock {long time=1000000L;public long now(){return time;}}
 static LocalEmail551 account(AccountFlows542Test.Vault v,Clock c){return new LocalEmail551(v,c,new SecureRandom());}
 static JSONObject record(AccountFlows542Test.Vault v)throws Exception{return new JSONObject(v.get(LocalEmail551.RECORD));}
 interface Work{void run()throws Exception;}
 static AccountError fails(Work f)throws Exception{try{f.run();fail("expected AccountError");return null;}catch(AccountError e){return e;}}

 @Test public void createsSaltedVerifierWithoutPlaintextAndNormalizesEmail()throws Exception {
  AccountFlows542Test.Vault a=new AccountFlows542Test.Vault(),b=new AccountFlows542Test.Vault();Clock clock=new Clock();LocalEmail551 one=account(a,clock),two=account(b,clock);
  assertFalse(one.status().getBoolean("exists"));one.signup("  "+ADDRESS+"  ",PASSWORD);two.signup(ADDRESS,PASSWORD);
  JSONObject r=record(a),r2=record(b);assertEquals("some.user@example.invalid",r.getString("email"));assertTrue(r.getBoolean("signedIn"));
  assertEquals(310000,r.getInt("iterations"));assertEquals(16,Base64.getDecoder().decode(r.getString("salt")).length);assertNotEquals(r.getString("salt"),r2.getString("salt"));assertNotEquals(r.getString("hash"),r2.getString("hash"));
  PBEKeySpec spec=new PBEKeySpec(PASSWORD.toCharArray(),Base64.getDecoder().decode(r.getString("salt")),310000,256);byte[] expected=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();spec.clearPassword();
  assertArrayEquals(expected,Base64.getDecoder().decode(r.getString("hash")));assertFalse(a.values.toString().contains(PASSWORD));assertFalse(one.status().toString().contains("hash"));
 }
 @Test public void signOutAndRecreateObjectPreserveAccountAndCorrectLogin()throws Exception {
  AccountFlows542Test.Vault v=new AccountFlows542Test.Vault();Clock c=new Clock();LocalEmail551 x=account(v,c);x.signup(ADDRESS,PASSWORD);x.signOut();
  assertTrue(x.status().getBoolean("exists"));assertFalse(x.status().getBoolean("connected"));LocalEmail551 next=account(v,c);
  fails(()->next.login(ADDRESS,"wrong password"));assertFalse(next.status().getBoolean("connected"));
  next.login(ADDRESS.toLowerCase(),PASSWORD);assertTrue(next.status().getBoolean("connected"));assertEquals(0,record(v).getInt("failures"));
 }
 @Test public void oneAccountNeverOverwritesExistingRecord()throws Exception {
  AccountFlows542Test.Vault v=new AccountFlows542Test.Vault();LocalEmail551 x=account(v,new Clock());x.signup(ADDRESS,PASSWORD);x.signOut();String old=v.get(LocalEmail551.RECORD);
  AccountError e=fails(()->x.signup("another@example.invalid","another password"));assertTrue(e.getMessage().contains("nothing was overwritten"));assertEquals(old,v.get(LocalEmail551.RECORD));
  fails(()->x.login("another@example.invalid",PASSWORD));assertFalse(x.status().getBoolean("connected"));
  x.login(ADDRESS,PASSWORD);assertTrue(x.status().getBoolean("connected"));
 }
 @Test public void rejectedInputsAndMissingAccountDoNotWrite()throws Exception {
  AccountFlows542Test.Vault v=new AccountFlows542Test.Vault();LocalEmail551 x=account(v,new Clock());
  fails(()->x.signup("not an email","long password"));fails(()->x.signup(ADDRESS,"short"));fails(()->x.signup(ADDRESS,""));fails(()->x.login(ADDRESS,PASSWORD));fails(x::signOut);
  assertTrue(v.values.isEmpty());assertFalse(x.status().getBoolean("connected"));
 }
 @Test public void failedAttemptsDelayAndSuccessfulLoginResetsFailures()throws Exception {
  AccountFlows542Test.Vault v=new AccountFlows542Test.Vault();Clock c=new Clock();LocalEmail551 x=account(v,c);x.signup(ADDRESS,PASSWORD);x.signOut();
  for(int i=0;i<5;i++)fails(()->x.login(ADDRESS,"not the password"));JSONObject r=record(v);assertEquals(5,r.getInt("failures"));assertEquals(c.time+30000,r.getLong("retryAfter"));
  assertTrue(fails(()->x.login(ADDRESS,PASSWORD)).getMessage().contains("Wait"));assertFalse(x.status().getBoolean("connected"));c.time+=30001;
  fails(()->x.login(ADDRESS,"not the password"));assertEquals(c.time+60000,record(v).getLong("retryAfter"));c.time+=60001;
  x.login(ADDRESS,PASSWORD);assertTrue(x.status().getBoolean("connected"));assertEquals(0,record(v).getLong("retryAfter"));assertEquals(0,record(v).getInt("failures"));
 }
 @Test public void corruptRecordIsNotReplacedOrDeleted()throws Exception {
  AccountFlows542Test.Vault v=new AccountFlows542Test.Vault();v.put(LocalEmail551.RECORD,"not valid JSON");LocalEmail551 x=account(v,new Clock());
  fails(x::status);fails(()->x.signup(ADDRESS,PASSWORD));fails(()->x.login(ADDRESS,PASSWORD));fails(x::signOut);assertEquals("not valid JSON",v.get(LocalEmail551.RECORD));
 }
 @Test public void storageFailureDoesNotPretendSuccess()throws Exception {
  AccountVault bad=new AccountVault(){public String get(String k){return "";}public void put(String k,String v)throws Exception{throw new AccountError("storage failed");}public void remove(String k){}};
  LocalEmail551 x=new LocalEmail551(bad,new Clock(),new SecureRandom());assertTrue(fails(()->x.signup(ADDRESS,PASSWORD)).getMessage().contains("storage failed"));assertFalse(x.status().getBoolean("exists"));
 }
}
