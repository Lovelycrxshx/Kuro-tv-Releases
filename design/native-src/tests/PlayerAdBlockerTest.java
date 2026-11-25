import app.lovelycrxshx.kuro.PlayerAdBlocker;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class PlayerAdBlockerTest {
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  PlayerAdBlocker empty=PlayerAdBlocker.empty();
  ok(empty.blocked("https","syndication.exdynsrv.com","/ads.js",false,false),"known ad network blocked even without asset");
  ok(empty.blocked("https","A.DOUBLECLICK.NET","/pagead/ad.mp4",false,false),"known ad video blocked case-insensitively");
  ok(!empty.blocked("https","not-doubleclick.net","/movie.js",false,false),"host boundary respected");
  ok(!empty.blocked("https","doubleclick.net.evil.test","/movie.js",false,false),"lookalike not matched");
  ok(!empty.blocked("https","exosrv.com","/ads.js",true,false),"main frame never intercepted");
  ok(!empty.blocked("https","exosrv.com","/ads.js",false,true),"player origin explicitly protected");
  ok(!empty.blocked("data","exosrv.com","/ads.js",false,false),"only network schemes considered");
  byte[] source=("# test\n0.0.0.0 ads.bad.test\n0.0.0.0 challenges.cloudflare.com\n0.0.0.0 recaptcha.net\n0.0.0.0 stream.bad.test\n0.0.0.0 googlevideo.com\n").getBytes(StandardCharsets.UTF_8);
  PlayerAdBlocker test=PlayerAdBlocker.from(new ByteArrayInputStream(source));
  ok(test.blocked("https","ads.bad.test","/a.js",false,false),"snapshot uses exact host entries");
  ok(!test.blocked("https","sub.ads.bad.test","/a.js",false,false),"snapshot entries are not expanded to unrelated subdomains");
  ok(!test.blocked("https","challenges.cloudflare.com","/turnstile",false,false),"verification service not blocked");
  ok(!test.blocked("https","www.recaptcha.net","/captcha",false,false),"captcha not blocked");
  ok(!test.blocked("https","stream.bad.test","/signed/video.m3u8",false,false),"media manifest preserved");
  ok(!test.blocked("https","stream.bad.test","/seg-001.m4s",false,false),"media segment preserved");
  ok(!test.blocked("https","a.googlevideo.com","/videoplayback",false,false),"video CDN preserved");
  try(InputStream in=Files.newInputStream(Paths.get("assets/player-ad-hosts541.txt"))){PlayerAdBlocker real=PlayerAdBlocker.from(in);
   ok(real.size()>40000,"real offline list loaded: "+real.size());
   ok(real.blocked("https","adserver.fyber.com","/tag.js",false,false),"MIT snapshot domain blocked");
   ok(!real.blocked("https","megaplay.buzz","/stream/test",true,false),"player navigation allowed");
  }
  System.out.println("PASS player-only host list, popup networks, host boundaries, verification and playback exceptions");
 }
}
