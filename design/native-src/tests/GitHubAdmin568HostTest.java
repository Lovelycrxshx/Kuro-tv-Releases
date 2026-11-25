package app.lovelycrxshx.kuro;

import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Simulated GitHub device flow: no real GitHub account or credentials. */
public final class GitHubAdmin568HostTest {
 static int checks;static void ok(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 interface Task {void run() throws Exception;}
 static void reject(Task task,String fragment)throws Exception{try{task.run();throw new AssertionError("expected refusal: "+fragment);}catch(AccountError e){ok(e.getMessage().toLowerCase().contains(fragment.toLowerCase()),"safe error: "+fragment);}}
 static JSONObject startData(){return new JSONObject().put("device_code","fake-device-1234567890").put("user_code","A1B2-C3D4").put("verification_uri",GitHubAdmin568.VERIFY_URL).put("expires_in",900).put("interval",5);}
 static JSONObject token(String scope){return new JSONObject().put("access_token","fake-token-123456789").put("token_type","bearer").put("scope",scope);}
 static final class Fake implements GitHubAdmin568.Transport {
  JSONObject start=startData(),identity=new JSONObject().put("id",GitHubAdmin568.OWNER_ID).put("login","ImAuraxSiva");
  final ArrayDeque<Object> polls=new ArrayDeque<>();final List<String> urls=new ArrayList<>(),bodies=new ArrayList<>();int users;
  GitHubAdmin568 closeDuringUser;
  public JSONObject post(String url,String body)throws Exception{
   urls.add(url);bodies.add(body);if(url.equals(GitHubAdmin568.DEVICE_URL))return new JSONObject(start.toString());
   if(!url.equals(GitHubAdmin568.TOKEN_URL))throw new AssertionError("unsafe endpoint");
   Object x=polls.remove();if(x instanceof IOException)throw (IOException)x;if(x instanceof AccountError)throw (AccountError)x;return (JSONObject)x;
  }
  public JSONObject user(String token)throws Exception{users++;ok(token.equals("fake-token-123456789"),"token passed only to identity endpoint");if(closeDuringUser!=null)closeDuringUser.close();return identity;}
 }
 public static void main(String[]args)throws Exception{
  long[] t={100000};Fake f=new Fake();GitHubAdmin568 a=new GitHubAdmin568(f,()->t[0]);
  ok(!a.verified(),"locked before GitHub authentication");JSONObject begun=a.begin();
  ok(begun.getString("userCode").equals("A1B2-C3D4")&&begun.getString("verificationUrl").equals(GitHubAdmin568.VERIFY_URL),"only safe verification information returned");
  ok(!begun.toString().contains("fake-device")&&!begun.toString().contains("token"),"device code and credentials never enter UI response");
  ok(f.bodies.get(0).contains("client_id="+GitHubAdmin568.CLIENT_ID)&&f.bodies.get(0).contains("scope=")&&!f.bodies.get(0).contains("repo"),"public ID and no repository scope");
  ok(a.currentCode().equals("A1B2-C3D4"),"current code available for safe copy");
  ok(a.poll().getString("state").equals("waiting")&&f.urls.size()==1,"early polling makes no network request");
  t[0]+=5000;f.polls.add(new JSONObject().put("error","authorization_pending"));ok(a.poll().getString("state").equals("waiting")&&f.urls.size()==2,"pending respects server interval");
  t[0]+=5000;f.polls.add(new JSONObject().put("error","slow_down").put("interval",10));JSONObject slow=a.poll();ok(slow.getLong("retryMs")==10000,"slow_down increases interval");
  t[0]+=9999;ok(a.poll().getString("state").equals("waiting")&&f.urls.size()==3,"slowed polling cannot run early");
  t[0]++;JSONObject rawToken=token("").put("refresh_token","fake-refresh-123456789");f.polls.add(rawToken);JSONObject done=a.poll();ok(done.getString("state").equals("verified")&&a.verified(),"owner numeric ID unlocks setup");
  ok(!done.toString().contains("fake-token")&&!rawToken.has("access_token")&&!rawToken.has("refresh_token")&&f.users==1,"token removed, identity checked and not sent to UI");
  t[0]+=GitHubAdmin568.SESSION_MS-1;ok(a.verified(),"owner session valid before deadline");t[0]++;ok(!a.verified(),"owner session automatically expires");
  Fake wrong=new Fake();wrong.identity.put("id",42).put("login","ImAuraxSiva");GitHubAdmin568 b=new GitHubAdmin568(wrong,()->t[0]);b.begin();t[0]+=5000;wrong.polls.add(token(""));reject(b::poll,"not the owner");ok(!b.verified(),"matching display name cannot spoof immutable numeric ID");
  Fake wide=new Fake();GitHubAdmin568 c=new GitHubAdmin568(wide,()->t[0]);c.begin();t[0]+=5000;wide.polls.add(token("repo,gist"));reject(c::poll,"extra app permissions");ok(wide.users==0&&!c.verified(),"broad scope fails closed before identity request");
  Fake denied=new Fake();GitHubAdmin568 d=new GitHubAdmin568(denied,()->t[0]);d.begin();t[0]+=5000;denied.polls.add(new JSONObject().put("error","access_denied"));reject(d::poll,"declined");ok(!d.verified(),"denial never unlocks");
  Fake noFlow=new Fake();noFlow.start=new JSONObject().put("error","device_flow_disabled");GitHubAdmin568 e=new GitHubAdmin568(noFlow,()->t[0]);reject(e::begin,"device flow");
  Fake hostile=new Fake();hostile.start=startData().put("verification_uri","https://evil.invalid/device");GitHubAdmin568 h=new GitHubAdmin568(hostile,()->t[0]);reject(h::begin,"unexpected");ok(!h.verified(),"untrusted verification URL fails closed");
  Fake offline=new Fake();GitHubAdmin568 n=new GitHubAdmin568(offline,()->t[0]);n.begin();t[0]+=5000;offline.polls.add(new IOException("offline"));ok(n.poll().getLong("retryMs")==10000,"network error backs off");int hits=offline.urls.size();t[0]+=9999;n.poll();ok(offline.urls.size()==hits,"offline failure not spammed");
  t[0]+=900000;reject(n::poll,"expired");ok(!n.verified(),"expired flow fails closed");
  Fake cancelled=new Fake();GitHubAdmin568 z=new GitHubAdmin568(cancelled,()->t[0]);z.begin();z.cancel();reject(z::currentCode,"expired");ok(!z.verified(),"cancel clears pending and verified state");
  Fake destroyed=new Fake();GitHubAdmin568 q=new GitHubAdmin568(destroyed,()->t[0]);destroyed.closeDuringUser=q;q.begin();t[0]+=5000;destroyed.polls.add(token(""));reject(q::poll,"closed");ok(!q.verified(),"activity destruction cannot grant a late success");
  Fake rate=new Fake();GitHubAdmin568 rr=new GitHubAdmin568(rate,()->t[0]);rr.begin();t[0]+=5000;rate.polls.add(new AccountError("GitHub rate-limited verification."));reject(rr::poll,"rate-limited");reject(rr::poll,"expired");
  // Cancellation must invalidate immediately even while the network holds the
  // monitor, and a stale cleanup must never erase a newer verification attempt.
  java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
  GitHubAdmin568.Transport blocking=new GitHubAdmin568.Transport(){
   public JSONObject post(String url,String body){return url.equals(GitHubAdmin568.DEVICE_URL)?startData():token("");}
   public JSONObject user(String ignored)throws Exception{entered.countDown();if(!release.await(4,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("test timed out");return new JSONObject().put("id",GitHubAdmin568.OWNER_ID);}
  };
  long[] raceTime={50000};GitHubAdmin568 race=new GitHubAdmin568(blocking,()->raceTime[0]);race.begin();raceTime[0]+=5000;
  java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
  Thread worker=new Thread(()->{try{race.poll();}catch(Throwable ex){failure.set(ex);}});worker.start();ok(entered.await(4,java.util.concurrent.TimeUnit.SECONDS),"identity request reached");
  long start=System.nanoTime(),generation=race.invalidate(),elapsed=System.nanoTime()-start;ok(elapsed<1000000000L,"cancel invalidates without waiting on network");
  release.countDown();worker.join(4000);ok(!worker.isAlive(),"in-flight identity call returned");race.clearInvalidated(generation);
  ok(failure.get() instanceof AccountError&&failure.get().getMessage().contains("cancelled")&&!race.verified(),"cancelled in-flight verification cannot grant setup");
  Fake stale=new Fake();GitHubAdmin568 fresh=new GitHubAdmin568(stale,()->t[0]);fresh.begin();long oldGeneration=fresh.invalidate();fresh.begin();fresh.clearInvalidated(oldGeneration);
  ok(fresh.currentCode().equals("A1B2-C3D4"),"stale cancellation cannot erase a newer attempt");
  System.out.println("PASS "+checks+" GitHub admin security, identity, scope, polling, expiry, cancellation and failure checks");
 }
}
