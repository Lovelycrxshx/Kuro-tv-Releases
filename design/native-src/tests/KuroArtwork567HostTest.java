package app.lovelycrxshx.kuro;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Offline, simulated metadata and cache checks; no live Discord or user data. */
public final class KuroArtwork567HostTest {
 static int checks;static void check(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
 static final String COVER="https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx1-example.jpg";
 static final String COVER2="https://s3.anilist.co/file/anilistcdn/media/anime/cover/medium/bx2-example.png";
 static final String MAL="https://cdn.myanimelist.net/images/anime/3/72078.jpg";
 static final String KITSU="https://media.kitsu.app/anime/poster_images/1/original.jpg";
 static final String EP="https://media.kitsu.app/episodes/thumbnails/100/original.jpeg";
 static JSONObject item(int id,String english,String image){return new JSONObject().put("id",id).put("title",new JSONObject().put("english",english).put("romaji",english).put("native",""))
  .put("synonyms",new JSONArray()).put("coverImage",new JSONObject().put("extraLarge",image).put("large",image));}
 static JSONObject response(JSONObject... entries){JSONArray a=new JSONArray();for(JSONObject x:entries)a.put(x);return new JSONObject().put("data",new JSONObject().put("Page",new JSONObject().put("media",a)));}
 static JSONObject kitsuItem(String id,String title,String image){return new JSONObject().put("id",id).put("attributes",new JSONObject().put("canonicalTitle",title).put("titles",new JSONObject().put("en",title)).put("posterImage",new JSONObject().put("original",image)));}
 static JSONObject kitsuResponse(JSONObject... entries){JSONArray a=new JSONArray();for(JSONObject x:entries)a.put(x);return new JSONObject().put("data",a);}
 public static void main(String[]args){
  check(KuroPresence563.safeUrl(COVER,true).equals(COVER),"AniList anime cover allowed");
  check(KuroPresence563.safeUrl(COVER2,true).equals(COVER2),"another cover size allowed");
  check(KuroPresence563.safeUrl(MAL,true).equals(MAL),"MAL anime poster allowed");
  check(KuroPresence563.safeUrl(KITSU,true).equals(KITSU),"Kitsu anime poster allowed");
  String modern="https://media.kitsu.io/anime/42/poster_image/abcdef123.jpg";
  check(KuroPresence563.safeUrl(modern,true).equals(modern),"Kitsu modern anime poster allowed");
  for(String bad:new String[]{EP,"https://media.kitsu.app/anime/42/episode/1.jpg","https://s4.anilist.co/file/anilistcdn/media/anime/banner/bx1.jpg","https://s4.anilist.co/file/episode.jpg","https://cdn.myanimelist.net/images/anime/3/episode.jpg","https://hianime.ms/poster.jpg","https://s4.anilist.co.evil.test/file/a.jpg",COVER+"?token=secret",COVER+"#fragment"})
   check(KuroPresence563.safeUrl(bad,true).isEmpty(),"reject episode, unknown or private URL: "+bad);
  check(KuroArtwork567.posterUrl(KITSU+"?1597699920").equals(KITSU),"Kitsu numeric cache timestamp is stripped");
  check(KuroArtwork567.posterUrl(KITSU+"?token=secret").isEmpty(),"arbitrary query never published");
  check(KuroArtwork567.posterUrl(EP+"?1597699920").isEmpty(),"stripping a timestamp does not allow episode art");

  List<Runnable> jobs=new ArrayList<>();long[] t={1_000_000};int[] aniCalls={0},kitsuCalls={0},done={0};List<String> searched=new ArrayList<>();
  KuroArtwork567 art=new KuroArtwork567(jobs::add,q->{aniCalls[0]++;searched.add(q);return response(item(1,"Cowboy Bebop",COVER));},q->{kitsuCalls[0]++;return null;},()->t[0]);
  check(art.resolve("Cowboy Bebop",MAL,null).equals(MAL)&&jobs.isEmpty(),"trusted supplied anime cover is immediate");
  check(art.resolve("Cowboy Bebop",EP,()->done[0]++).isEmpty()&&jobs.size()==1,"episode image rejected while anime cover lookup is pending");
  check(art.resolve("Cowboy Bebop","",null).isEmpty()&&jobs.size()==1,"same anime lookup is deduplicated");jobs.remove(0).run();
  check(aniCalls[0]==1&&kitsuCalls[0]==0&&done[0]==1&&searched.equals(java.util.Collections.singletonList("Cowboy Bebop")),"only the title is searched and completion refreshes");
  check(art.resolve("Cowboy Bebop",EP,null).equals(COVER),"matched anime cover, not episode art, is cached");
  check(art.resolve("hianime.ms/watch-secret","",null).isEmpty()&&jobs.isEmpty(),"source URLs are not searched");
  check(KuroArtwork567.pick(response(item(1,"Different",COVER)),"Cowboy Bebop").isEmpty(),"no guessed title");
  check(KuroArtwork567.pick(response(item(1,"Cowboy Bebop",COVER),item(2,"Cowboy Bebop",COVER2)),"Cowboy Bebop").isEmpty(),"ambiguous exact AniList titles rejected");
  check(KuroArtwork567.pick(response(item(1,"Cowboy Bebop",EP)),"Cowboy Bebop").isEmpty(),"episode URL in a cover field is rejected");
  check(KuroArtwork567.pick(response(item(1,"Dr. Stone",COVER)),"Dr.Stone").equals(COVER),"normalized punctuation exact match");
  t[0]+=KuroArtwork567.POSITIVE_TTL+1;art.resolve("Cowboy Bebop","",null);check(jobs.size()==1,"positive cover cache eventually refreshes");jobs.remove(0).run();

  JSONObject k=kitsuItem("1","Cowboy Bebop",KITSU+"?1597699920");
  check(KuroArtwork567.pickKitsuCover(kitsuResponse(k),"Cowboy Bebop").equals(KITSU),"unique Kitsu anime poster selected without query");
  check(KuroArtwork567.pickKitsuCover(kitsuResponse(k,kitsuItem("2","Cowboy Bebop",COVER)),"Cowboy Bebop").isEmpty(),"ambiguous Kitsu matches rejected");
  check(KuroArtwork567.pickKitsuCover(kitsuResponse(kitsuItem("1","Cowboy Bebop",EP)),"Cowboy Bebop").isEmpty(),"Kitsu episode field cannot masquerade as poster");
  List<Runnable> fallbackJobs=new ArrayList<>();List<String> titles=new ArrayList<>();long[] now={2_000_000};
  KuroArtwork567 fallback=new KuroArtwork567(fallbackJobs::add,q->{titles.add("AniList:"+q);throw new java.io.IOException("unavailable");},q->{titles.add("Kitsu:"+q);return kitsuResponse(k);},()->now[0]);
  check(fallback.resolve("Cowboy Bebop","",null).isEmpty()&&fallbackJobs.size()==1,"fallback starts without an invented image");fallbackJobs.remove(0).run();
  check(fallback.resolve("Cowboy Bebop","",null).equals(KITSU),"exact Kitsu cover used when AniList fails");
  check(titles.equals(java.util.Arrays.asList("AniList:Cowboy Bebop","Kitsu:Cowboy Bebop")),"metadata services receive title only");

  List<Runnable> ambiguousJobs=new ArrayList<>();int[] afterAmbiguous={0};long[] ta={2_500_000};
  KuroArtwork567 ambiguous=new KuroArtwork567(ambiguousJobs::add,q->response(item(1,"Cowboy Bebop",COVER),item(2,"Cowboy Bebop",COVER2)),q->{afterAmbiguous[0]++;return kitsuResponse(k);},()->ta[0]);
  ambiguous.resolve("Cowboy Bebop","",null);ambiguousJobs.remove(0).run();check(afterAmbiguous[0]==0&&ambiguous.resolve("Cowboy Bebop","",null).isEmpty(),"known ambiguous AniList title is never guessed via Kitsu");

  List<Runnable> badJobs=new ArrayList<>();long[] t2={3_000_000};int[] bad={0};KuroArtwork567 offline=new KuroArtwork567(badJobs::add,q->{bad[0]++;throw new java.io.IOException("offline");},()->t2[0]);
  offline.resolve("Some Anime","",null);badJobs.remove(0).run();t2[0]+=3000;offline.resolve("Some Anime","",null);check(bad[0]==1&&badJobs.isEmpty(),"failure is negatively cached");
  t2[0]+=KuroArtwork567.NEGATIVE_TTL;offline.resolve("Some Anime","",null);check(badJobs.size()==1,"failed cover lookup retries after cooldown");
  List<Runnable> boundedJobs=new ArrayList<>();long[] t3={4_000_000};KuroArtwork567 bounded=new KuroArtwork567(boundedJobs::add,q->response(),()->t3[0]);
  bounded.resolve("Anime Alpha","",null);t3[0]+=KuroArtwork567.LOOKUP_INTERVAL;bounded.resolve("Anime Beta","",null);t3[0]+=KuroArtwork567.LOOKUP_INTERVAL;bounded.resolve("Anime Gamma","",null);check(boundedJobs.size()==2,"pending lookup count is bounded");
  boundedJobs.remove(0).run();bounded.resolve("Anime Gamma","",null);check(boundedJobs.size()==2,"next title can be queued after a slot frees");
  System.out.println("PASS "+checks+" anime-cover-only, privacy, exact matching, fallback and bounded cache checks");
 }
}
