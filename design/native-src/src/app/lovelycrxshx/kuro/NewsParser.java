package app.lovelycrxshx.kuro;

import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/** Bounded, non-executing parser for publisher RSS/Atom/RDF and Aniplex's public news index. */
public final class NewsParser {
 public static final class Feed {
  public final String id, label, kind, url, format;
  public Feed(String id,String label,String kind,String url){this(id,label,kind,url,"auto");}
  public Feed(String id,String label,String kind,String url,String format){this.id=id;this.label=label;this.kind=kind;this.url=url;this.format=format;}
 }
 public static final class Item {
  public String id="",title="",summary="",url="",image="",source="",kind="";
  public long published=0;
  // A Japanese date-only index does not provide a publication time.
  public boolean dateOnly=false;
 }
 private static final String ATOM="http://www.w3.org/2005/Atom", MEDIA="http://search.yahoo.com/mrss/", DC="http://purl.org/dc/elements/1.1/";
 private static final Pattern ANIME_TERM=Pattern.compile("アニメ(?!イト|ーション)|声優|(?i:\\banime\\b)");
 private static String child(Element e,String namespace,String name){
  for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling())if(c.getNodeType()==Node.ELEMENT_NODE){
   Element x=(Element)c;String local=x.getLocalName()==null?x.getNodeName():x.getLocalName();
   if(name.equals(local)&&(namespace==null||namespace.equals(x.getNamespaceURI())))return x.getTextContent();
  }return "";
 }
 private static Element first(Element e,String ns,String name){NodeList list=e.getElementsByTagNameNS(ns,name);return list.getLength()>0?(Element)list.item(0):null;}
 private static String hash(String text)throws Exception{
  byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
 }
 private static boolean host(String h,String domain){return h.equals(domain)||h.equals("www."+domain);}
 public static String safeLink(String address){
  try{URI u=new URI(address.trim());String h=u.getHost();if(!"https".equalsIgnoreCase(u.getScheme())||h==null||u.getUserInfo()!=null||u.getPort()!=-1)return "";
   h=h.toLowerCase(Locale.ROOT);
   if(h.equals("crunchyroll.com")||h.endsWith(".crunchyroll.com")||host(h,"animecorner.me")||host(h,"anitrendz.net")||host(h,"myanimelist.net")||host(h,"tohoanimationstore.us")||host(h,"aniplex.co.jp")||host(h,"prtimes.jp")||host(h,"youtube.com")||h.equals("anilist.co"))return u.toASCIIString();
  }catch(Exception ignored){}return "";
 }
 public static String safeImage(String address){
  try{URI u=new URI(address.trim());String h=u.getHost();if(!"https".equalsIgnoreCase(u.getScheme())||h==null||u.getUserInfo()!=null||u.getPort()!=-1)return "";
   h=h.toLowerCase(Locale.ROOT);String path=u.getRawPath()==null?"":u.getRawPath();
   if(h.equals("a.storyblok.com")||host(h,"animecorner.me")||host(h,"anitrendz.net")||h.equals("cdn.myanimelist.net")||h.equals("ytimg.com")||h.endsWith(".ytimg.com")||h.equals("s4.anilist.co")||h.equals("s3.anilist.co")||(h.equals("i0.wp.com")&&path.startsWith("/anitrendz.net/news/wp-content/uploads/")))return u.toASCIIString();
  }catch(Exception ignored){}return "";
 }
 private static String entity(String s){
  String t=s.replace("&nbsp;"," ").replace("&hellip;","…").replace("&mdash;","—").replace("&ndash;","–").replace("&rsquo;","’").replace("&lsquo;","‘").replace("&ldquo;","“").replace("&rdquo;","”").replace("&quot;","\"").replace("&apos;","'").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&");
  Matcher m=Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(t);StringBuffer out=new StringBuffer();
  while(m.find()){try{String v=m.group(1);int cp=v.charAt(0)=='x'?Integer.parseInt(v.substring(1),16):Integer.parseInt(v);String repl=Character.isValidCodePoint(cp)?new String(Character.toChars(cp)):"";m.appendReplacement(out,Matcher.quoteReplacement(repl));}catch(Exception ex){m.appendReplacement(out,"");}}
  m.appendTail(out);return out.toString();
 }
 public static String plain(String text,int max){
  if(text==null)return "";String s=entity(text).replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>"," ").replaceAll("(?s)<[^>]*>"," ").replaceAll("\\s+"," ").trim();
  if(s.length()>max){int n=s.lastIndexOf(' ',max);if(n<max/2)n=max;s=s.substring(0,n).trim()+"…";}return s;
 }
 private static long date(String s){
  String[] patterns={"EEE, dd MMM yyyy HH:mm:ss Z","EEE, d MMM yyyy HH:mm:ss Z","yyyy-MM-dd'T'HH:mm:ssXXX","yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd'T'HH:mm:ss.SSSXXX"};
  for(String p:patterns)try{SimpleDateFormat f=new SimpleDateFormat(p,Locale.US);f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.parse(s.trim()).getTime();}catch(Exception ignored){}return 0;
 }
 private static long japanDay(String s){try{SimpleDateFormat f=new SimpleDateFormat("yyyy.MM.dd",Locale.US);f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));return f.parse(s.trim()).getTime();}catch(Exception ignored){return 0;}}
 private static String classText(String html,String className){
  Pattern p=Pattern.compile("<div\\b[^>]*\\bclass\\s*=\\s*['\"][^'\"]*\\b"+Pattern.quote(className)+"\\b[^'\"]*['\"][^>]*>(.*?)</div\\s*>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
  Matcher m=p.matcher(html);return m.find()?m.group(1):"";
 }
 private static List<Item> aniplexIndex(String html,Feed feed)throws Exception{
  // Match only the documented news cards and numeric detail IDs. No page scripts execute.
  Pattern cards=Pattern.compile("<li\\b[^>]*\\bclass\\s*=\\s*['\"][^'\"]*\\bp-news-archive__list-item\\b[^'\"]*['\"][^>]*>(.*?)</li\\s*>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
  Pattern href=Pattern.compile("href\\s*=\\s*['\"]/news/detail/\\?id=([0-9]{1,9})['\"]",Pattern.CASE_INSENSITIVE);
  Matcher m=cards.matcher(html);ArrayList<Item> out=new ArrayList<>();HashSet<String> used=new HashSet<>();
  while(m.find()&&out.size()<40){String card=m.group(1);Matcher link=href.matcher(card);if(!link.find())continue;
   Item x=new Item();x.source=feed.label;x.kind=feed.kind;x.title=plain(classText(card,"p-news-article__title"),300);x.summary=plain(classText(card,"p-news-article__category"),150);x.published=japanDay(plain(classText(card,"p-news-article__date"),20));x.dateOnly=true;
   x.url=safeLink("https://www.aniplex.co.jp/news/detail/?id="+link.group(1));if(x.title.isEmpty()||x.url.isEmpty()||x.published==0)continue;
   x.id=feed.id+":"+hash(x.url);if(used.add(x.id))out.add(x);
  }
  if(out.isEmpty())throw new Exception("Official index unavailable or changed");return out;
 }
 private static String encodedImage(Element e){
  Element content=first(e,"http://purl.org/rss/1.0/modules/content/","encoded");if(content==null)return "";
  String html=content.getTextContent();Matcher m=Pattern.compile("<img\\b[^>]*\\bsrc\\s*=\\s*['\"]([^'\"]+)['\"]",Pattern.CASE_INSENSITIVE).matcher(html);
  return m.find()?safeImage(entity(m.group(1))):"";
 }
 public static List<Item> parse(byte[] bytes,Feed feed)throws Exception{
  if(bytes==null||bytes.length>1_500_000)throw new Exception("Feed too large");String input=new String(bytes,StandardCharsets.UTF_8);
  if("aniplex-html".equals(feed.format))return aniplexIndex(input,feed);
  if(input.toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))throw new Exception("DTD not allowed");
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setExpandEntityReferences(false);
  for(String feature:new String[]{"http://apache.org/xml/features/disallow-doctype-decl","http://xml.org/sax/features/external-general-entities","http://xml.org/sax/features/external-parameter-entities"})try{f.setFeature(feature,feature.contains("disallow"));}catch(Exception ignored){}
  Document doc=f.newDocumentBuilder().parse(new InputSource(new StringReader(input)));Element document=doc.getDocumentElement();
  String root=document.getLocalName();if(root==null)root=document.getNodeName();boolean atom="feed".equals(root)&&ATOM.equals(document.getNamespaceURI());boolean rss="rss".equals(root);boolean rdf="RDF".equals(root)&&"http://www.w3.org/1999/02/22-rdf-syntax-ns#".equals(document.getNamespaceURI());
  if(!atom&&!rss&&!rdf)throw new Exception("Unexpected feed document");NodeList entries=atom?doc.getElementsByTagNameNS(ATOM,"entry"):doc.getElementsByTagNameNS("*","item");
  ArrayList<Item> out=new ArrayList<>();HashSet<String> used=new HashSet<>();
  for(int i=0;i<entries.getLength()&&i<200;i++){
   Element e=(Element)entries.item(i);Item x=new Item();x.source=feed.label;x.kind=feed.kind;x.title=plain(child(e,atom?ATOM:null,"title"),300);
   String link="";
   if(atom){for(Node n=e.getFirstChild();n!=null;n=n.getNextSibling())if(n.getNodeType()==Node.ELEMENT_NODE){Element a=(Element)n;if("link".equals(a.getLocalName())&&(a.getAttribute("rel").isEmpty()||"alternate".equals(a.getAttribute("rel")))){link=a.getAttribute("href");break;}}}
   else link=child(e,null,"link");
   x.url=safeLink(link);if(x.title.isEmpty()||x.url.isEmpty())continue;
   String description=atom?child(e,ATOM,"summary"):child(e,null,"description");Element md=first(e,MEDIA,"description");if(atom&&md!=null)description=md.getTextContent();
   // KADOKAWA's company feed also includes many non-anime products. Avoid
   // classifying the unrelated retailer name アニメイト as an anime keyword.
   if("anime-only".equals(feed.format)&&!ANIME_TERM.matcher(x.title+" "+description).find())continue;
   x.summary=plain(description,380);String timestamp=child(e,atom?ATOM:null,atom?"published":"pubDate");if(timestamp.isEmpty())timestamp=atom?child(e,ATOM,"updated"):child(e,DC,"date");x.published=date(timestamp);
   Element thumb=first(e,MEDIA,"thumbnail");if(thumb!=null)x.image=safeImage(thumb.hasAttribute("url")?thumb.getAttribute("url"):thumb.getTextContent());if(x.image.isEmpty())x.image=encodedImage(e);
   String guid=child(e,atom?ATOM:null,atom?"id":"guid").trim();if(guid.isEmpty())guid=x.url;
   x.id=feed.id+":"+hash(guid);if(used.add(x.id))out.add(x);
  }
  return out;
 }
}
