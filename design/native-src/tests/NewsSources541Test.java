import app.lovelycrxshx.kuro.NewsParser;
import app.lovelycrxshx.kuro.NewsLogic;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class NewsSources541Test {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static List<NewsParser.Item> parse(String doc,NewsParser.Feed f)throws Exception{return NewsParser.parse(doc.getBytes(StandardCharsets.UTF_8),f);}
 static NewsParser.Feed feed(String id,String kind,String format){return new NewsParser.Feed(id,id,kind,"",format);}
 public static void main(String[]args)throws Exception{
  NewsParser.Feed atom=feed("toho541","official","auto");
  String a="<feed xmlns='http://www.w3.org/2005/Atom'><entry><id>id-1</id><title>Official announcement</title><link rel='alternate' href='https://tohoanimationstore.us/blogs/news/update'/><published>2026-11-07T12:15:00-08:00</published><summary type='html'>&lt;p&gt;New series&lt;/p&gt;</summary></entry></feed>";
  List<NewsParser.Item> x=parse(a,atom);ok(x.size()==1&&x.get(0).summary.equals("New series")&&x.get(0).published>0,"official Atom publisher summary/date");
  NewsParser.Feed mal=feed("mal541","news","auto");
  String m="<rss xmlns:media='http://search.yahoo.com/mrss/'><channel><item><title>News</title><link>https://myanimelist.net/news/42</link><pubDate>Sat, 07 Nov 2026 01:30:53 -0800</pubDate><media:thumbnail>https://cdn.myanimelist.net/photo.jpg</media:thumbnail></item></channel></rss>";
  x=parse(m,mal);ok(x.size()==1&&x.get(0).image.equals("https://cdn.myanimelist.net/photo.jpg")&&x.get(0).published>0,"MAL text thumbnail and date");
  NewsParser.Feed trend=feed("trending541","news","auto");
  String t="<rss xmlns:content='http://purl.org/rss/1.0/modules/content/'><channel><item><title>Trending</title><link>https://anitrendz.net/news/test</link><content:encoded><![CDATA[<p><img src='https://i0.wp.com/anitrendz.net/news/wp-content/uploads/img.jpg?fit=1&amp;ssl=1'/></p>]]></content:encoded></item></channel></rss>";
  x=parse(t,trend);ok(x.size()==1&&x.get(0).image.contains("i0.wp.com/anitrendz.net/news/wp-content/uploads/"),"restricted WordPress source image");
  ok(NewsParser.safeImage("https://i0.wp.com/evil.test/tracking.jpg").isEmpty(),"unrelated WordPress image rejected");
  NewsParser.Feed k=feed("kadopr541","official","anime-only");
  String rdf="<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns='http://purl.org/rss/1.0/' xmlns:dc='http://purl.org/dc/elements/1.1/'><channel><title>KADOKAWA</title></channel>"+
   "<item><title>TVアニメの新情報</title><link>https://prtimes.jp/main/html/rd/p/test.html</link><description>公式のお知らせ</description><dc:date>2026-11-07T15:00:00+09:00</dc:date></item>"+
   "<item><title>アニメイトでシャツ販売</title><link>https://prtimes.jp/main/html/rd/p/other.html</link><description>洋服</description><dc:date>2026-11-07T15:00:00+09:00</dc:date></item></rdf:RDF>";
  x=parse(rdf,k);ok(x.size()==1&&x.get(0).title.contains("TVアニメ")&&x.get(0).published>0,"official RDF anime-only and namespaced date");
  NewsParser.Feed ani=feed("aniplexjp541","official","aniplex-html");
  String html="<html><body><li class='p-news-archive__list-item'><a href='/news/detail/?id=71659'><div class='p-news-article__title'>TVアニメ &amp; 最新情報</div><div class='p-news-article__category'>作品名</div><div class='p-news-article__date'>2026.11.08</div></a></li>"+
   "<li class='p-news-archive__list-item'><a href='https://attacker.test/news/detail/?id=1'><div class='p-news-article__title'>bad</div><div class='p-news-article__date'>2026.11.08</div></a></li></body></html>";
  x=parse(html,ani);ok(x.size()==1&&x.get(0).title.equals("TVアニメ & 最新情報")&&x.get(0).summary.equals("作品名")&&x.get(0).dateOnly&&x.get(0).published>0&&x.get(0).url.startsWith("https://www.aniplex.co.jp/"),"bounded Japanese official index with date-only precision");
  long start=x.get(0).published,baseline=start+12*3600000L,now=baseline+3600000L;
  NewsLogic.Result fresh=NewsLogic.update(x,Collections.emptySet(),true,baseline,now);ok(fresh.newlyPublished.size()==1,"new same-day date-only post not silently missed");
  ok(NewsLogic.update(x,Collections.emptySet(),false,0,now).newlyPublished.isEmpty(),"new source seeds without notification flood");
  try{parse("<html>security check</html>",ani);throw new AssertionError("unexpected Aniplex page accepted");}catch(Exception expected){}
  ok(NewsParser.safeLink("https://www.aniplex.co.jp.evil.test/news").isEmpty(),"publisher lookalike refused");
  if(args.length>0){String base=args[0]+"/";String[][] files={
   {"Crunchyroll.xml","cr","news","auto"},{"Anime-Corner.xml","ac","news","auto"},{"Anime-Trending.xml","trend","news","auto"},{"MyAnimeList-News.xml","mal","news","auto"},
   {"TOHO-store-official-tagged.xml","toho","official","auto"},{"Aniplex-official-news.html","aniplex","official","aniplex-html"},{"KADOKAWA-PR.xml","kado","official","anime-only"}};
   for(String[] f:files){List<NewsParser.Item> live=NewsParser.parse(Files.readAllBytes(Paths.get(base+f[0])),feed(f[1],f[2],f[3]));ok(!live.isEmpty(),"observed source parsing: "+f[0]);ok(live.stream().anyMatch(z->z.published>0),"observed dates: "+f[0]);System.out.println("PASS observed "+f[0]+": "+live.size()+" items, "+live.stream().filter(z->z.published>0).count()+" dated");}
  }
  System.out.println("PASS publisher Atom/RDF/HTML, source-specific labels, dates, new-source baseline and safe URLs");
 }
}
