package app.lovelycrxshx.kuro;

import java.nio.charset.StandardCharsets;

/** Pure offline checks; not a live Discord or player-device test. */
public final class KuroPresence565HostTest {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static KuroPresence563.Snapshot sample(KuroPresence564.Options o,String number,String name,String page,String show,String state,long pos,long duration,double rate){
  return KuroPresence564.watching(o,"Jujutsu Kaisen",number,name,"dub","https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg",show,page,"Server 2","24",state,pos,duration,1_800_000_000_000L,rate);
 }
 static String js(long a,String state,String p,String d,String r){return "{\"kind\":\"sample\",\"attempt\":"+a+",\"state\":\""+state+"\",\"position\":"+p+",\"duration\":"+d+",\"rate\":"+r+"}";}
 public static void main(String[] args){
  KuroPresence564.Options o=new KuroPresence564.Options();
  String show="https://hianime.dk/anime/jujutsu",episode="https://hianime.dk/watch/jujutsu?ep=123456";
  KuroPresence563.Snapshot s=sample(o,"12","The Shibuya Incident",episode,show,"playing",600_000,1_200_000,1);
  check(s.details.equals("Jujutsu Kaisen"),"anime title");check(s.state.equals("Episode 12 - \"The Shibuya Incident\" · DUB"),"live episode number, actual name and dub");
  check(s.buttonUrl.isEmpty(),"HiAnime episode URL is not published");
  check(s.startMs==1_799_999_400_000L&&s.endMs==1_800_000_600_000L,"Discord gets real position and duration");
  check(s.downloadUrl.isEmpty(),"no Download button");
  o.download=true;o.downloadUrl="https://example.org/download";check(sample(o,"12","Title",episode,show,"playing",1,600_000,1).downloadUrl.isEmpty(),"old Download preference cannot publish");
  check(KuroPresence564.browsing(1,o).downloadUrl.isEmpty(),"browsing cannot publish Download");
  o.episode=false;s=sample(o,"12","Secret episode",episode,show,"playing",1,600_000,1);check(!s.state.contains("12")&&!s.state.contains("Secret")&&s.buttonUrl.isEmpty(),"hide episode also hides title and button");
  o.episode=true;o.title=false;s=sample(o,"12","Secret episode",episode,show,"playing",1,600_000,1);check(!s.details.contains("Jujutsu")&&!s.state.contains("Secret")&&s.image.isEmpty()&&s.buttonUrl.isEmpty(),"hide title does not reveal identifying name or link");
  o.title=true;o.layout="episode";o.separator="bar";o.total=true;o.server=true;s=sample(o,"12","The Shibuya Incident",episode,show,"playing",30_000,1_200_000,1);
  check(s.details.equals("Episode 12 of 24 - \"The Shibuya Incident\" | DUB | Server 2")&&s.state.equals("Jujutsu Kaisen"),"episode-first, total and separator");
  s=sample(o,"2","Episode 2",episode,show,"playing",0,600_000,1);check(!s.details.contains("\"Episode 2\""),"generic number is not duplicated");
  s=sample(o,"2","X",episode,show,"playing",0,600_000,1);check(s.details.contains("\"X\""),"single-character title is retained");
  s=sample(o,"2","未来の物語".repeat(50),episode,show,"playing",0,600_000,1);check(s.details.getBytes(StandardCharsets.UTF_8).length<=120&&s.details.contains("未来")&&s.details.contains("DUB"),"bounded UTF-8 and preserved audio");
  s=sample(o,"2","https://example.org/token=secret",episode,show,"playing",0,600_000,1);check(!s.details.contains("secret"),"URL-shaped name is not published");
  s=sample(o,"12","Name",episode,show,"paused",75_000,1_200_000,1);check(s.startMs==0&&s.endMs==0&&s.details.contains("Paused 1:15"),"pause removes moving timestamps");
  s=sample(o,"12","Name",episode,show,"buffering",75_000,1_200_000,1);check(s.startMs==0&&s.endMs==0,"buffering does not advance the clock");
  s=sample(o,"12","Name",episode,show,"selected",-1,-1,1);check(s.startMs==0&&s.endMs==0&&s.details.contains("Selected"),"unknown players never get invented progress");
  s=sample(o,"12","Name",episode,show,"playing",600_000,1_200_000,2);check(s.startMs==1_799_999_700_000L&&s.endMs==1_800_000_300_000L,"two-times speed has correct remaining wall time and fraction");
  s=sample(o,"12","Name",episode,show,"playing",600_000,1_200_000,Double.NaN);check(s.startMs==0&&s.endMs==0,"unknown rate means no fabricated timing");
  String[] pages={"https://hianime.ms/watch-naruto-1","https://hianime.dk/watch/test?ep=4","https://hianime.dk/watch/test?ep=4&token=x","https://evil.example/watch/ok"};
  for(String page:pages){s=sample(o,"12","Episode",page,page,"playing",0,600_000,1);check(s.buttonUrl.isEmpty()&&s.downloadUrl.isEmpty(),"no page URL may be published: "+page);}
  long attempt=42,now=200_000;String valid=js(attempt,"playing","30000","1200000","1.5");
  KuroProgress565.Sample x=KuroProgress565.parse(valid,attempt,now);check(x!=null&&x.fresh(now+9_000)&&!x.fresh(now+9_001),"freshness bound");
  check(x.positionAt(now+1_000)==31_500&&x.positionAt(now+8_000)==34_500,"speed-adjusted bounded sample extrapolation");
  x=KuroProgress565.parse(js(attempt,"paused","30000","1200000","1"),attempt,now);check(x!=null&&x.positionAt(now+8_000)==30_000,"paused frame does not advance");
  String[] rejects={js(41,"playing","1000","1200000","1"),js(42,"selected","1000","1200000","1"),js(42,"playing","-1","1200000","1"),js(42,"playing","10","30000","1"),js(42,"playing","10","86400001","1"),js(42,"playing","1300000","1200000","1"),js(42,"playing","10","1200000","0"),js(42,"playing","1.2","1200000","1"),js(42,"playing","\"10\"","1200000","1"),"{bad"};
  for(String raw:rejects)check(KuroProgress565.parse(raw,attempt,now)==null,"invalid/stale frame report");
  System.out.println("PASS "+checks+" episode identity/name, Watch privacy, real/speed-correct timing and bounded frame checks");
 }
}
