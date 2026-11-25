import app.lovelycrxshx.kuro.RetrySelector;
import org.json.JSONArray;
import org.json.JSONObject;

public class RetrySelectorTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  JSONArray options=new JSONArray().put(new JSONObject().put("lang","dub")).put(new JSONObject().put("lang","sub")).put(new JSONObject().put("lang","DUB"));
  boolean[] tried={true,false,false};check(RetrySelector.next(options,tried,"dub")==2,"same-language server first");tried[2]=true;
  check(RetrySelector.next(options,tried,"dub")==1,"other language only after same-language candidates");tried[1]=true;
  check(RetrySelector.next(options,tried,"dub")==-1,"no unbounded retry after exhaustion");
  check(RetrySelector.next(options,new boolean[]{true},"dub")==-1,"tried array bounds honored");
  JSONArray incomplete=new JSONArray().put(JSONObject.NULL).put(new JSONObject().put("lang","sub"));check(RetrySelector.next(incomplete,new boolean[]{false,false},"sub")==1,"malformed option skipped");
  System.out.println("PASS native retry selector: language preference, exhaustion, bounded indexing, malformed options");
 }
}
