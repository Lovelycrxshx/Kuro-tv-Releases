package app.lovelycrxshx.kuro;
import java.nio.charset.StandardCharsets;
/** Offline tests of privacy, accurate state/timing, Unicode, copy and rate limiting. */
public final class KuroPresence563HostTest {
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static KuroPresence563.Snapshot w(KuroPresence564.Options o,String title,String episode,String lang,String poster,String playback,long position,long duration,long now){
  return KuroPresence564.watching(o,title,episode,"",lang,poster,"","","","",playback,position,duration,now,1.0);
 }
 public static void main(String[]args){
  long now=1_800_000_000_000L;
  KuroPresence564.Options o=new KuroPresence564.Options();
  KuroPresence563.Snapshot playing=w(o,"進撃の巨人 😀","3","dub","https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg","playing",30000,1200000,now);
  check(playing.details.contains("進撃")&&playing.details.contains("😀"),"Unicode preserved");
  check(playing.state.contains("Episode 3")&&playing.state.contains("DUB"),"actual episode and audio");
  check(playing.startMs==now-30000&&playing.endMs==now+1170000,"actual playback timing");
  check(!playing.image.isEmpty()&&playing.buttonUrl.isEmpty(),"metadata artwork without a show link");
  KuroPresence563.Snapshot paused=w(o,"Title","3","sub","","paused",30000,1200000,now);
  check(paused.state.contains("Paused 0:30")&&paused.startMs==0&&paused.endMs==0,"pause does not keep a running timer");
  KuroPresence563.Snapshot selected=w(o,"Title","3","","","unobservable",999,1200000,now);
  check(selected.state.contains("Selected")&&selected.startMs==0,"unknown embed is not claimed playing");
  check(w(o,"Title","3","","","playing",-1,-1,now).startMs==0,"unknown time is not invented");
  // 5.82: a long episode name is clipped with an ellipsis and never leaves a dangling quote.
  KuroPresence564.Options named=new KuroPresence564.Options();
  KuroPresence563.Snapshot longName=KuroPresence564.watching(named,"A very long anime title that keeps going","12","The extremely long episode name that absolutely cannot fit inside one presence line at all","sub","","","","","","playing",1000,1200000,now,1.0);
  String line=longName.state;
  check(line.getBytes(StandardCharsets.UTF_8).length<=120,"episode line stays inside the byte clamp");
  check(!line.contains("\"\u2026")||line.trim().endsWith("\"")||line.contains("\u2026\""),"no dangling quote");
  // 5.82: rewatching is a label the app supplies, never a guess.
  KuroPresence564.Options again=new KuroPresence564.Options();again.rewatch=true;
  check(w(again,"Title","3","sub","","playing",30000,1200000,now).state.contains("Rewatching"),"rewatch label");
  // 5.82: the browsing card names the app, and title privacy keeps it neutral.
  KuroPresence564.Options private1=new KuroPresence564.Options();private1.title=false;
  KuroPresence563.Snapshot card=KuroPresence564.browsing(now,o);
  check("Looking for an anime to watch".equals(card.details)&&"Kuro.tv".equals(card.state),"browsing card copy");
  check(!KuroPresence564.browsing(now,private1).details.contains("Kuro"),"privacy hides the service name");
  check(KuroPresence563.safeUrl("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg?token=secret",true).isEmpty(),"no query-bearing image");
  check(KuroPresence563.safeUrl("https://user:pass@s4.anilist.co/file/a.jpg",true).isEmpty(),"no credentials");
  check(KuroPresence563.safeUrl("https://s4.anilist.co.evil.test/file/a.jpg",true).isEmpty(),"no image host spoof");
  check(KuroPresence563.safeUrl("https://hianime.ms/anime/a#access_token",false).isEmpty(),"no token fragment");
  check(KuroPresence563.safeUrl("https://hianime.ms:8443/anime/a",false).isEmpty(),"no non-default port");
  check(KuroPresence563.safeUrl("https://megaplay.buzz/stream/secret",false).isEmpty(),"no stream URL");
  String t=KuroPresence563.text("😀".repeat(200),120,"Anime");check(t.getBytes(StandardCharsets.UTF_8).length<=120&&!t.contains("\uFFFD"),"bounded valid utf8");
  check(!KuroPresence563.text("Title\nInjected",120,"Anime").contains("\n"),"controls sanitized");
  KuroPresence563.Gate gate=new KuroPresence563.Gate();check(gate.due(playing,0),"initial send");gate.sent(playing,0);
  KuroPresence563.Snapshot next=w(o,"進撃の巨人 😀","3","dub",playing.image,"playing",33000,1200000,now+3000);
  check(!gate.due(next,2999)&&gate.due(next,3000),"three-second maximum request cadence");
  gate.sent(next,3000);check(!gate.due(paused,5999)&&gate.due(paused,6000),"change waits for cadence");
  gate.reset();check(gate.due(next,6001),"new session can send immediately");
  KuroPresence563.Retry retry=new KuroPresence563.Retry();check(!retry.ready(true,0,2999)&&retry.ready(true,0,3000),"failure waits first three seconds");
  retry.sent(true);check(!retry.ready(true,3000,8999)&&retry.ready(true,3000,9000),"failure doubles delay to six seconds");
  check(retry.waitFor(3000,6000)==3000,"the status view can count the retry down");
  check(retry.waitFor(3000,9000)==0,"a due retry has no countdown left");
  retry.sent(true);check(!retry.ready(true,9000,20999)&&retry.ready(true,9000,21000),"repeated failure backs off to twelve seconds");
  check(retry.ready(false,9000,9001),"success resets error backoff");
  System.out.println("PASS RPC metadata safety, accurate pause/unknown/timing, UTF-8, copy and bounded update scheduling");
 }
}
