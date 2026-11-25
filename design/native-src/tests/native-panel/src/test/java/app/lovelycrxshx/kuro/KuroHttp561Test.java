package app.lovelycrxshx.kuro;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import static org.junit.Assert.*;
/** Checks the interceptor itself without making HTTP requests. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class KuroHttp561Test {
 private Interceptor.Chain chain(Request initial,String location,AtomicInteger calls){
  return (Interceptor.Chain)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Interceptor.Chain.class},(proxy,method,args)->{
   if(method.getName().equals("request"))return initial;
   if(method.getName().equals("proceed")){Request r=(Request)args[0];int n=calls.getAndIncrement();Response.Builder b=new Response.Builder().request(r).protocol(Protocol.HTTP_1_1).message("fixture").body(ResponseBody.create(null,new byte[0]));if(n==0&&location!=null)b.code(302).header("Location",location);else b.code(200);return b.build();}
   throw new UnsupportedOperationException(method.getName());
  });
 }
 @Test public void sameHostRedirectWorksButForeignRedirectIsNeverRequested()throws Exception{
  Request request=new Request.Builder().url("https://www.zenkaitv.com/api/source?fixture=1").build();AtomicInteger n=new AtomicInteger();try(Response response=new KuroHttp561.SameProxy().intercept(chain(request,"/api/segment",n))){assertEquals(200,response.code());assertEquals(2,n.get());}
  n.set(0);try{new KuroHttp561.SameProxy().intercept(chain(request,"https://127.0.0.1/private",n));fail("redirect must be rejected");}catch(IOException expected){assertEquals(1,n.get());}
 }
 @Test public void unapprovedInitialMediaHostIsNeverRequested()throws Exception{
  AtomicInteger n=new AtomicInteger();Request request=new Request.Builder().url("https://example.org/stream").build();try{new KuroHttp561.SameProxy().intercept(chain(request,null,n));fail("host must be rejected");}catch(IOException expected){assertEquals(0,n.get());}
 }
 @Test public void playlistCannotReadArbitraryLocalOrForeignResources(){
  android.net.Uri picked=android.net.Uri.parse("content://com.example.picker/subtitle.vtt");
  assertTrue(KuroHttp561.allowedMedia(android.net.Uri.parse("https://www.zenkaitv.com/video/seg.ts"),null));
  assertTrue(KuroHttp561.allowedMedia(picked,picked));
  assertFalse(KuroHttp561.allowedMedia(picked,null));
  assertFalse(KuroHttp561.allowedMedia(android.net.Uri.parse("content://other/private"),picked));
  assertFalse(KuroHttp561.allowedMedia(android.net.Uri.parse("file:///data/user/0/app/private"),picked));
  assertFalse(KuroHttp561.allowedMedia(android.net.Uri.parse("https://foreign.example/seg.ts"),picked));
 }
}
