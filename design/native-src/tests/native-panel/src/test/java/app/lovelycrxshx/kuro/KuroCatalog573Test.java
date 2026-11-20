package app.lovelycrxshx.kuro;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.json.JSONObject;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=33,manifest=org.robolectric.annotation.Config.NONE)
public class KuroCatalog573Test {
 @Test public void seasonQueryIsBoundedAndExcludesAdult() throws Exception {
  JSONObject q=KuroCatalog573.request("season","{\"year\":2026,\"season\":\"FALL\",\"page\":2}");
  assertTrue(q.getString("query").contains("isAdult:false"));assertEquals(2,q.getJSONObject("variables").getInt("page"));
  try{KuroCatalog573.request("season","{\"year\":2026,\"season\":\"SPRING) { evil\",\"page\":1}");fail();}catch(java.io.IOException expected){}
  try{KuroCatalog573.request("season","{\"year\":2026,\"season\":\"SPRING\",\"page\":51}");fail();}catch(java.io.IOException expected){}
 }
 @Test public void calendarRequiresBoundedWeekAndPage() throws Exception {
  JSONObject c=KuroCatalog573.request("calendar","{\"start\":1700000000,\"end\":1700604800,\"page\":1}");assertTrue(c.getString("query").contains("airingSchedules"));
  try{KuroCatalog573.request("calendar","{\"start\":1700000000,\"end\":1800000000,\"page\":1}");fail();}catch(java.io.IOException expected){}
 }
 @Test public void recommendationsUseOnlyValidatedId() throws Exception {
  JSONObject r=KuroCatalog573.request("recommend","{\"provider\":\"anilist\",\"id\":20}");assertTrue(r.getString("query").contains("recommendations"));assertEquals(20,r.getJSONObject("variables").getInt("id"));
  try{KuroCatalog573.request("recommend","{\"provider\":\"invalid\",\"id\":20}");fail();}catch(java.io.IOException expected){}
 }
 @Test public void relatedRequiresTrustedIdentifier() throws Exception {
  JSONObject a=KuroCatalog573.request("related","{\"provider\":\"mal\",\"id\":42}");assertTrue(a.getString("query").contains("idMal:$id"));
  try{KuroCatalog573.request("related","{\"provider\":\"url\",\"id\":42}");fail();}catch(java.io.IOException expected){}
  try{KuroCatalog573.request("related","{\"provider\":\"anilist\",\"id\":0}");fail();}catch(java.io.IOException expected){}
 }
}
