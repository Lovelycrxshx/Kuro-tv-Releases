package app.lovelycrxshx.kuro;

/** Offline policy regressions, not a live Discord or Android-device test. */
public final class KuroPresence566HostTest {
 static int checks;
 static void check(boolean ok,String msg){checks++;if(!ok)throw new AssertionError(msg);}
 static KuroPresence563.Snapshot build(KuroPresence564.Options o,String title,String epTitle,String poster,String show,String ep,String server){
  return KuroPresence564.watching(o,title,"12",epTitle,"dub",poster,show,ep,server,"24","playing",30000,1200000,1800000000000L,1.0);
 }
 public static void main(String[] args){
  KuroPresence564.Options o=new KuroPresence564.Options();o.watch=true;o.download=true;o.downloadUrl="https://example.org/get";o.server=true;o.total=true;
  String source="https://hianime.ms/watch-my-happy-marriage-special-episodes-episode-1-15aff";
  KuroPresence563.Snapshot a=build(o,"My Happy Marriage","The Start","https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg",source,source,"Server 2");
  check(a.details.equals("My Happy Marriage"),"title remains accurate");
  check(a.state.contains("Episode 12 of 24 - \"The Start\"")&&a.state.contains("DUB")&&a.state.contains("Server 2"),"episode and valid metadata remain");
  check(a.image.equals("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg"),"neutral artwork retained");
  check(a.buttonUrl.isEmpty()&&a.downloadUrl.isEmpty(),"old on settings cannot create links");
  check(a.startMs==1799999970000L&&a.endMs==1800001170000L,"real timestamps retained");
  check(!a.details.contains("hianime")&&!a.state.contains("hianime")&&!a.image.contains("hianime"),"no source text");
  String[] providerPosters={"https://hianime.ms/poster.jpg","https://hianime.dk/a.jpg","https://cdn.hianime.dk/a.jpg","https://cdn.noitatnemucod.net/a.jpg","https://s4.anilist.co/file/hianime.jpg","https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg?source=hianime.ms","https://evil.invalid/a.jpg"};
  for(String poster:providerPosters){a=build(o,"My Happy Marriage","The Start",poster,source,source,"Server 2");check(a.image.isEmpty(),"provider/unsafe poster falls back to icon: "+poster);}
  check(build(o,"My Happy Marriage","The Start","https://cdn.myanimelist.net/images/anime/1/72078.jpg",source,source,"Server 2").image.contains("myanimelist"),"MAL artwork retained");
  String[] badTitles={"https://hianime.ms/watch-a","Watch at hianime.ms","from HiAnime","www.somewhere.xyz","media.example.com","authorization: secret","token=secret"};
  for(String title:badTitles){a=build(o,title,title,"",source,source,"HiAnime");check(a.details.equals("Anime"),"unsafe title replaced: "+title);check(!a.state.toLowerCase().contains("hianime")&&!a.state.contains("secret")&&!a.state.contains("https"),"unsafe episode and server omitted");}
  check(build(o,"Dr.Stone","New World","",source,source,"Server 1").details.equals("Dr.Stone"),"ordinary title containing a dot retained");
  check(build(o,"進撃の巨人","未来","",source,source,"Server 1").details.equals("進撃の巨人"),"Unicode title retained");
  o.watch=false;o.download=false;o.title=false;a=build(o,"Private","Secret","https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg",source,source,"Server 2");
  check(a.details.equals("Watching an anime")&&a.image.isEmpty()&&a.buttonUrl.isEmpty()&&a.downloadUrl.isEmpty(),"title privacy retained");
  KuroPresence563.Snapshot raw=new KuroPresence563.Snapshot("test","test","",source,true,0,0,source);
  check(raw.buttonUrl.isEmpty()&&raw.downloadUrl.isEmpty(),"all snapshots discard button URLs");
  a=KuroPresence564.browsing(1000,o);check(a.buttonUrl.isEmpty()&&a.downloadUrl.isEmpty(),"browsing has no buttons");
  a=KuroPresence564.test();check(a.buttonUrl.isEmpty()&&a.downloadUrl.isEmpty(),"test presence has no buttons");
  System.out.println("PASS "+checks+" no-button, provider-text/artwork privacy, title, episode and real-progress checks");
 }
}
