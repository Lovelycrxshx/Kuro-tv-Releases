package app.lovelycrxshx.kuro;
/** Offline model checks: these do not represent an actual Discord device test. */
public final class KuroPresence564HostTest {
 static int checks=0;static void check(boolean v,String label){checks++;if(!v)throw new AssertionError(label);}
 static KuroPresence563.Snapshot sample(KuroPresence564.Options o,String status,long pos,long len){return KuroPresence564.watching(o,"Jujutsu Kaisen","12","dub","https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg","https://hianime.ms/anime/test","Server 2 Volt","24",status,pos,len,1_800_000_000_000L);}
 public static void main(String[] args){
  KuroPresence564.Options o=new KuroPresence564.Options();o.server=true;o.total=true;
  KuroPresence563.Snapshot s=sample(o,"playing",30000,1200000);
  check(s.details.equals("Jujutsu Kaisen"),"anime first title");check(s.state.equals("Episode 12 of 24 · DUB · Server 2 Volt"),"reported episode, total, dub, selected server");
  check(s.startMs==1_800_000_000_000L-30000&&s.endMs==1_800_000_000_000L+1170000,"actual timing");check(!s.image.isEmpty()&&s.buttonUrl.isEmpty(),"public metadata artwork without a watch link");
  o.layout="episode";o.separator="bar";s=sample(o,"playing",30000,1200000);check(s.details.equals("Episode 12 of 24 | DUB | Server 2 Volt")&&s.state.equals("Jujutsu Kaisen"),"episode first and bar");
  o.separator="dash";check(sample(o,"playing",1,20000).details.contains(" — DUB — "),"dash separator");
  o.pause="show";s=sample(o,"paused",61000,1200000);check(s.details.contains("Paused 1:01")&&s.startMs==0&&s.endMs==0,"paused position without running clock");
  o.pause="noProgress";s=sample(o,"paused",61000,1200000);check(s.details.contains("Paused")&&!s.details.contains("1:01"),"hide paused progress");
  o.buffering=true;check(sample(o,"buffering",0,0).details.contains("Buffering"),"show observed buffering");o.buffering=false;check(!sample(o,"buffering",0,0).details.contains("Buffering"),"hide buffering");
  s=sample(o,"unknown",0,0);check(s.details.contains("Selected")&&s.startMs==0&&s.endMs==0,"do not invent state or timing for embeds");
  o.download=true;o.downloadUrl="https://example.com/get/kuro";check(sample(o,"playing",0,10000).downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"only pinned Kuro download page is published");
  o.download=false;check(sample(o,"playing",0,10000).downloadUrl.isEmpty(),"download button hidden when toggled off");o.download=true;
  o.title=false;s=sample(o,"playing",0,10000);check(!s.details.contains("Jujutsu")&&!s.state.contains("Jujutsu")&&s.image.isEmpty()&&s.buttonUrl.isEmpty()&&s.downloadUrl.isEmpty(),"title privacy hides identifying assets");
  o.title=true;o.episode=false;s=sample(o,"playing",0,10000);check(!s.details.contains("12")&&!s.state.contains("12")&&s.buttonUrl.isEmpty()&&s.downloadUrl.equals(KuroPresence564.DOWNLOAD_PAGE_URL),"episode privacy hides episode-specific links while keeping app download page");
  o.title=true;o.episode=true;o.poster=false;o.watch=false;s=sample(o,"playing",0,10000);check(s.image.isEmpty()&&s.buttonUrl.isEmpty(),"app icon fallback and no watch link");
  KuroPresence563.Snapshot bogus=KuroPresence564.watching(o,"A\nB","1?token=bad","sub","","","https://secret.invalid/path","20?key=x","playing",-1,-1,1000000);
  check(!bogus.details.contains("\n")&&!bogus.state.contains("token")&&!bogus.state.contains("key=")&&!bogus.state.contains("secret.invalid")&&bogus.startMs==0,"untrusted fields and absent timing sanitized");
  String[] bad={"http://example.com/x","https://user:pass@example.com/a","https://example.com/a?token=secret","https://example.com/a#access","https://127.0.0.1/a","https://kuro.local/a","https://example.com/","https://example.com/a%2fb","https://example.com:8443/a"};
  for(String x:bad)check(KuroPresence564.safeDownloadUrl(x).isEmpty(),"invalid custom URL "+x);
  check(KuroPresence564.safeDownloadUrl("https://EXAMPLE.com/public/get").equals("https://example.com/public/get"),"valid HTTPS public-shaped URL");
  KuroPresence564.PauseClear pause=new KuroPresence564.PauseClear();check(!pause.shouldClear("paused","clear1",1000),"start pause timer");check(!pause.shouldClear("paused","clear1",60999),"not cleared early");check(pause.shouldClear("paused","clear1",61000),"clear after one minute");check(!pause.shouldClear("playing","clear1",62000),"resume resets");check(!pause.shouldClear("paused","clear5",63000)&&!pause.shouldClear("paused","clear5",362999)&&pause.shouldClear("paused","clear5",363000),"five-minute pause");
  check(!pause.shouldClear("paused","show",9000000),"no clear in show mode");pause.reset();check(!pause.shouldClear("paused","clear1",2),"reset and monotonic rollback");
  KuroPresence563.Snapshot browsing=KuroPresence564.browsing(1000,new KuroPresence564.Options());check(browsing.watching&&browsing.startMs==1000&&browsing.details.equals("Browsing Kuro"),"browsing session still Watching");
  KuroPresence563.Snapshot test=KuroPresence564.test();check(test.details.contains("test")&&test.buttonUrl.isEmpty()&&test.image.isEmpty(),"generic non-identifying test");
  System.out.println("PASS "+checks+" presence 5.64 model, privacy, controls, metadata and timer checks");
 }
}
