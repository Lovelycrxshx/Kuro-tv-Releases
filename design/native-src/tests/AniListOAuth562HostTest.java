package app.lovelycrxshx.kuro;
import java.net.URI;
/** Host-only, no network/credentials; verifies the exact AniList URL shape. */
public final class AniListOAuth562HostTest {
 public static void main(String[] args) throws Exception {
  String url=AccountOAuth.authorize("anilist","12345","ignored","ignored");
  URI u=new URI(url);
  if(!"https".equals(u.getScheme())||!"anilist.co".equals(u.getHost())||!"/api/v2/oauth/authorize".equals(u.getPath()))throw new AssertionError(url);
  if(!"client_id=12345&response_type=token".equals(u.getRawQuery()))throw new AssertionError("Unexpected query: "+u.getRawQuery());
  if(!"https://anilist.co/api/v2/oauth/pin".equals(AccountOAuth.ANILIST_PIN))throw new AssertionError("PIN redirect changed");
  for(String invalid:new String[]{"","a123","123456789012345678901"}) {
   try {AccountOAuth.authorize("anilist",invalid,"","");throw new AssertionError("Accepted invalid ID");}
   catch(AccountError expected) { /* expected */ }
  }
  System.out.println("PASS AniList implicit URL: numeric ID + token response only; PIN redirect unchanged");
 }
}
