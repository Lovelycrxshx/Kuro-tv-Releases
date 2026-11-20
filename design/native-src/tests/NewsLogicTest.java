import app.lovelycrxshx.kuro.NewsParser;
import app.lovelycrxshx.kuro.NewsLogic;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class NewsLogicTest {
 static void ok(boolean x,String message){if(!x)throw new AssertionError(message);}
 static NewsParser.Item item(String id,long time){NewsParser.Item x=new NewsParser.Item();x.id=id;x.published=time;return x;}
 public static void main(String[] args)throws Exception {
  NewsParser.Feed rss=new NewsParser.Feed("test","Sample outlet","news","https://example.test");
  String xml="<?xml version='1.0'?><rss xmlns:media='http://search.yahoo.com/mrss/'><channel>"+
   "<item><title>Test Anime &amp; New Season</title><guid>one</guid><link>https://animecorner.me/first/</link><pubDate>Sat, 07 Nov 2026 15:57:58 GMT</pubDate><description>&lt;b&gt;Hello&lt;/b&gt; &amp;hellip;</description><media:thumbnail url='https://a.storyblok.com/test.jpg'/></item>"+
   "<item><title>Evil link</title><guid>two</guid><link>https://attacker.invalid/story</link></item>"+
   "</channel></rss>";
  List<NewsParser.Item> parsed=NewsParser.parse(xml.getBytes(StandardCharsets.UTF_8),rss);ok(parsed.size()==1,"only allowed links retained");
  NewsParser.Item a=parsed.get(0);ok(a.title.equals("Test Anime & New Season"),"RSS title");ok(a.summary.equals("Hello …"),"plain HTML/entity summary: "+a.summary);ok(a.published==1794067078000L,"RFC date: "+a.published);ok(a.image.equals("https://a.storyblok.com/test.jpg"),"media thumbnail");
  ok(a.id.equals(NewsParser.parse(xml.getBytes(StandardCharsets.UTF_8),rss).get(0).id),"stable ID");
  NewsParser.Feed atom=new NewsParser.Feed("official","Official channel","official","https://example.test");
  String av="<feed xmlns='http://www.w3.org/2005/Atom' xmlns:media='http://search.yahoo.com/mrss/'><entry><id>yt:video:abc</id><title>Official preview</title><link rel='alternate' href='https://www.youtube.com/watch?v=abc'/><published>2026-11-07T00:00:32+00:00</published><media:group><media:thumbnail url='https://i2.ytimg.com/vi/abc/hqdefault.jpg'/><media:description>New video</media:description></media:group></entry></feed>";
  List<NewsParser.Item> ap=NewsParser.parse(av.getBytes(StandardCharsets.UTF_8),atom);ok(ap.size()==1&&ap.get(0).kind.equals("official"),"Atom entry");ok(ap.get(0).summary.equals("New video")&&ap.get(0).published>0&&ap.get(0).image.contains("ytimg"),"Atom media/date");
  String jp=av.replace("Official preview","日本語の作品");ok(NewsParser.parse(jp.getBytes(StandardCharsets.UTF_8),atom).get(0).title.equals("日本語の作品"),"Japanese titles retain Unicode");
  try{NewsParser.parse("<!DOCTYPE rss [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><rss>&xxe;</rss>".getBytes(StandardCharsets.UTF_8),rss);throw new AssertionError("DTD accepted");}catch(Exception expected){}
  try{NewsParser.parse("<html>security check</html>".getBytes(StandardCharsets.UTF_8),rss);throw new AssertionError("HTML accepted as RSS");}catch(Exception expected){}
  ok(NewsParser.safeLink("https://youtube.com.evil.test/watch?v=x").isEmpty(),"lookalike host refused");ok(NewsParser.safeLink("https://www.youtube.com@evil.test/").isEmpty(),"userinfo refused");ok(NewsParser.safeImage("https://tracker.invalid/a.png").isEmpty(),"unknown image refused");
  long now=1794100000000L;List<NewsParser.Item> first=Arrays.asList(item("one",now-10000),item("two",now-20000));
  NewsLogic.Result r=NewsLogic.update(first,Collections.emptySet(),false,0,now);ok(r.newlyPublished.isEmpty()&&r.seen.size()==2&&r.seededAt==now,"first feed baseline");
  NewsLogic.Result repeat=NewsLogic.update(first,r.seen,true,r.seededAt,now+60000);ok(repeat.newlyPublished.isEmpty(),"duplicate suppression");
  List<NewsParser.Item> next=Arrays.asList(item("three",now+30000),item("one",now-10000),item("old",now-5L*24*3600000L),item("undated",0),item("future",now+8L*3600000L));
  NewsLogic.Result updated=NewsLogic.update(next,repeat.seen,true,repeat.seededAt,now+60000);ok(updated.newlyPublished.size()==1&&updated.newlyPublished.get(0).id.equals("three"),"only recent new item alerts");
  ok(NewsLogic.update(next,updated.seen,true,updated.seededAt,now+120000).newlyPublished.isEmpty(),"new entry alerted only once");
  ok(NewsLogic.update(next,Collections.emptySet(),false,0,now+60000).newlyPublished.isEmpty(),"independent source baseline");
  LinkedHashSet<String> many=new LinkedHashSet<>();for(int i=0;i<500;i++)many.add("id"+i);ok(NewsLogic.update(first,many,true,now,now).seen.size()==NewsLogic.MAX_SEEN,"bounded seen set");
  System.out.println("PASS synthetic RSS/Atom, security, dates, baseline, deduplication, stale filtering, bounds");
  if(args.length>0){for(String name:new String[]{"crunchyroll","animecorner","toho","aniplexusa","kadokawa"}){boolean official=!name.equals("crunchyroll")&&!name.equals("animecorner");NewsParser.Feed f=new NewsParser.Feed(name,name,official?"official":"news","");List<NewsParser.Item> live=NewsParser.parse(Files.readAllBytes(Paths.get(args[0],name+".xml")),f);ok(!live.isEmpty(),name+" live parsing");long dates=live.stream().filter(v->v.published>0).count(), images=live.stream().filter(v->!v.image.isEmpty()).count();ok(dates>0,name+" dates");System.out.println("PASS live "+name+": "+live.size()+" entries, "+dates+" dated, "+images+" images; first: "+live.get(0).title);}}
 }
}
