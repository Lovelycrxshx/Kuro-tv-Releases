package app.lovelycrxshx.kuro;
import java.util.*;

/** Feed-specific baseline and bounded deduplication, independent of Android for host testing. */
public final class NewsLogic {
 public static final int MAX_SEEN=350;
 public static final class Result {
  public final LinkedHashSet<String> seen;
  public final List<NewsParser.Item> newlyPublished;
  public final long seededAt;
  Result(LinkedHashSet<String> s,List<NewsParser.Item> n,long t){seen=s;newlyPublished=n;seededAt=t;}
 }
 public static Result update(List<NewsParser.Item> items,Collection<String> alreadySeen,boolean seeded,long seededAt,long now){
  LinkedHashSet<String> seen=new LinkedHashSet<>(alreadySeen);List<NewsParser.Item> fresh=new ArrayList<>();
  long baseline=seeded&&seededAt>0?seededAt:now;
  // Ignore undated, old and suspiciously future entries for alerts; they remain visible in the feed.
  // Aniplex publishes only the Japanese calendar day, not a clock time: compare
  // its midnight with the baseline's JST day, without inventing a time.
  Calendar day=Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"),Locale.US);day.setTimeInMillis(baseline);
  day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);
  long timedCutoff=Math.max(baseline-5*60_000L,now-72*60*60_000L),dayCutoff=Math.max(day.getTimeInMillis(),now-72*60*60_000L);
  for(NewsParser.Item item:items){if(item==null||item.id==null||item.id.isEmpty())continue;
   long cutoff=item.dateOnly?dayCutoff:timedCutoff;
   if(seeded&&!seen.contains(item.id)&&item.published>0&&item.published>=cutoff&&item.published<=now+2*60*60_000L)fresh.add(item);
   seen.remove(item.id);seen.add(item.id);
  }
  while(seen.size()>MAX_SEEN)seen.remove(seen.iterator().next());
  return new Result(seen,fresh,baseline);
 }
}
