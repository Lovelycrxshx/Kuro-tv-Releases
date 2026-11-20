package app.lovelycrxshx.kuro;
import android.content.Context;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class KuroPlayback561Test {
 private static final String ROOT="https://www.zenkaitv.com/api/source?url=";
 private static final String INNER="https%3A%2F%2Fplayer.zilla-networks.com%2Fm3u8%2Ffixture";
 @Test public void onlyTheExistingExplicitProxyRouteIsAccepted(){
  assertTrue(KuroPlayback561.allowedHls(ROOT+INNER+"&refererHost=player.zilla-networks.com","zenkai","hls"));
  assertFalse(KuroPlayback561.allowedHls(ROOT+INNER,"ms","hls"));
  assertFalse(KuroPlayback561.allowedHls(ROOT+INNER,"zenkai","embed"));
  assertFalse(KuroPlayback561.allowedHls(ROOT+INNER+"&url="+INNER,"zenkai","hls"));
  assertFalse(KuroPlayback561.allowedHls(ROOT+"https%3A%2F%2Fevil.example%2Fm3u8%2Fa","zenkai","hls"));
  assertFalse(KuroPlayback561.allowedHls(ROOT+"https%3A%2F%2Fuser%40player.zilla-networks.com%2Fm3u8%2Fa","zenkai","hls"));
  assertFalse(KuroPlayback561.allowedHls(ROOT+"https%3A%2F%2Fplayer.zilla-networks.com%3A444%2Fm3u8%2Fa","zenkai","hls"));
  assertFalse(KuroPlayback561.allowedHls("http://www.zenkaitv.com/api/source?url="+INNER,"zenkai","hls"));
  assertFalse(KuroPlayback561.allowedHls("https://www.zenkaitv.com.evil.example/api/source?url="+INNER,"zenkai","hls"));
 }
 @Test public void resumeIsPerExactShowAndEpisodeAndRejectsInvalidPositions(){
  Context c=RuntimeEnvironment.getApplication();String a=KuroPlayback561.key("zenkai","https://www.zenkaitv.com/anime/season-1","3"),b=KuroPlayback561.key("zenkai","https://www.zenkaitv.com/anime/season-2","3"),next=KuroPlayback561.key("zenkai","https://www.zenkaitv.com/anime/season-1","4");
  assertTrue(KuroPlayback561.validKey(a));assertNotEquals(a,b);assertNotEquals(a,next);assertEquals("",KuroPlayback561.key("zenkai","","3"));assertEquals("",KuroPlayback561.key("zenkai","show","../3"));
  KuroPlayback561.completed(c,a);KuroPlayback561.save(c,a,42340,1200000);assertEquals(42340,KuroPlayback561.read(c,a));assertEquals(0,KuroPlayback561.read(c,b));assertEquals(0,KuroPlayback561.read(c,next));
  KuroPlayback561.save(c,a,-10,100000);KuroPlayback561.save(c,a,90000000,100000);KuroPlayback561.save(c,a,50000,10000);assertEquals(42340,KuroPlayback561.read(c,a));KuroPlayback561.save(c,a,2000,100000);assertEquals(0,KuroPlayback561.read(c,a));KuroPlayback561.save(c,a,42340,1200000);KuroPlayback561.completed(c,a);assertEquals(0,KuroPlayback561.read(c,a));
 }
 @Test public void markersAreValidatedAndManualEditsAreEpisodeScoped()throws Exception{
  Context c=RuntimeEnvironment.getApplication();String a=KuroPlayback561.key("ms","show-a","1"),b=KuroPlayback561.key("ms","show-a","2");KuroPlayback561.Range r=KuroPlayback561.fromJson(new JSONObject().put("start",12.5).put("end",90));assertNotNull(r);assertEquals(12500,r.startMs);assertEquals(90000,r.endMs);assertTrue(r.contains(12500));assertFalse(r.contains(90000));
  assertNull(KuroPlayback561.range(-1,2));assertNull(KuroPlayback561.range(20,20));assertNull(KuroPlayback561.range(Double.NaN,20));assertNull(KuroPlayback561.range(0,90000));
  KuroPlayback561.setManualRange(c,a,r);assertEquals(90000,KuroPlayback561.manualRange(c,a).endMs);assertNull(KuroPlayback561.manualRange(c,b));KuroPlayback561.setManualRange(c,a,null);assertNull(KuroPlayback561.manualRange(c,a));
 }
 @Test public void everyNativeSegmentAndRedirectMustStayOnTheHttpsProxy(){
  assertTrue(KuroHttp561.allowed(okhttp3.HttpUrl.get("https://www.zenkaitv.com/api/source?part=one")));
  assertFalse(KuroHttp561.allowed(okhttp3.HttpUrl.get("https://www.zenkaitv.com.evil.example/a")));
  assertFalse(KuroHttp561.allowed(okhttp3.HttpUrl.get("https://127.0.0.1/a")));
  assertFalse(KuroHttp561.allowed(okhttp3.HttpUrl.get("https://www.zenkaitv.com:444/a")));
  assertFalse(KuroHttp561.allowed(okhttp3.HttpUrl.get("https://user@www.zenkaitv.com/a")));
  assertFalse(KuroHttp561.allowed(okhttp3.HttpUrl.get("http://www.zenkaitv.com/a")));
 }
 @Test public void verificationFailuresStopRetries(){
  androidx.media3.datasource.DataSpec spec=new androidx.media3.datasource.DataSpec.Builder().setUri("https://www.zenkaitv.com/").build();
  for(int code:new int[]{401,403,429}){
   androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException e=new androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException(code,"blocked",null,java.util.Collections.emptyMap(),spec,new byte[0]);
   assertTrue(KuroLoadPolicy561.blocked(new java.io.IOException(e)));
   androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo info=new androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo(null,null,e,1);
   assertEquals(androidx.media3.common.C.TIME_UNSET,new KuroLoadPolicy561().getRetryDelayMsFor(info));
  }
  androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException e=new androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException(500,"failed",null,java.util.Collections.emptyMap(),spec,new byte[0]);assertFalse(KuroLoadPolicy561.blocked(e));
 }
 @Test public void outroOverridesAreIndependentAndEpisodeScoped(){
  Context c=RuntimeEnvironment.getApplication();String a=KuroPlayback561.key("ms","outro-test","1"),b=KuroPlayback561.key("ms","outro-test","2");
  KuroPlayback561.Range intro=KuroPlayback561.range(10,80),outro=KuroPlayback561.range(1200,1285);KuroPlayback561.setManualRange(c,a,intro);KuroPlayback561.setManualOutro(c,a,outro);
  assertEquals(80000,KuroPlayback561.manualRange(c,a).endMs);assertEquals(1285000,KuroPlayback561.manualOutro(c,a).endMs);assertNull(KuroPlayback561.manualOutro(c,b));
  KuroPlayback561.setManualOutro(c,a,null);assertNull(KuroPlayback561.manualOutro(c,a));assertEquals(80000,KuroPlayback561.manualRange(c,a).endMs);KuroPlayback561.setManualRange(c,a,null);
 }

}
