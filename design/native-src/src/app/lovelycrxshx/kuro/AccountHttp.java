package app.lovelycrxshx.kuro;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

interface AccountTransport {
 JSONObject request(String method,String url,Map<String,String> headers,String body) throws Exception;
}

/** Authenticated requests never follow redirects and never leave known HTTPS hosts. */
final class AccountHttp implements AccountTransport {
 public JSONObject request(String method,String address,Map<String,String> headers,String body) throws Exception {
  URI u=new URI(address);String host=u.getHost();
  if(!"https".equals(u.getScheme())||host==null||u.getUserInfo()!=null||u.getPort()!=-1||!(host.equals("myanimelist.net")||host.equals("api.myanimelist.net")||host.equals("discord.com")||host.equals("graphql.anilist.co")))throw new AccountError("Blocked an unsafe account request.");
  if(!("GET".equals(method)||"POST".equals(method)||"PUT".equals(method)))throw new AccountError("Blocked an unsupported account request.");
  if("PUT".equals(method)&&!("api.myanimelist.net".equals(host)&&u.getPath().matches("/v2/anime/[0-9]{1,10}/my_list_status")))throw new AccountError("Blocked an unsafe tracker update.");
  HttpURLConnection c=null;
  try{
   c=(HttpURLConnection)u.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestMethod(method);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","Kuro.tv/5.59 (Android)");
   for(Map.Entry<String,String> h:headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
   if(body!=null){c.setDoOutput(true);byte[] bytes=body.getBytes(StandardCharsets.UTF_8);if(bytes.length>30000)throw new AccountError("Request too large.");try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
   int status=c.getResponseCode();
   if(status<200||status>=300){
    if(status==429){long seconds=60;try{seconds=Math.max(1,Math.min(86400,Long.parseLong(c.getHeaderField("Retry-After"))));}catch(Exception ignored){}throw new AccountError("Provider rate-limited the request (HTTP 429); retry after "+seconds+" seconds.");}
    if(status==403)throw new AccountError("Provider blocked the request (HTTP 403). Check access before retrying manually.");
    if(status==401)throw new AccountError("Sign-in was rejected or the session expired (HTTP 401). Check your setup or sign in again.");
    if(status>=300&&status<400)throw new AccountError("The provider sent an unexpected redirect. No credentials were forwarded.");
    throw new AccountError("The provider rejected the request (HTTP "+status+"). Check the account details and provider settings.");
   }
   try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(n>3000000-out.size())throw new AccountError("Provider response too large.");out.write(buf,0,n);}String text=out.toString("UTF-8").trim();return text.isEmpty()?new JSONObject():new JSONObject(text);
   }
  }catch(java.io.IOException e){throw new AccountError("Could not reach the provider. Check your connection and setup.");}finally{if(c!=null)c.disconnect();}
 }
}
