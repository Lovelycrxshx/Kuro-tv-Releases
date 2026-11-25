package app.lovelycrxshx.kuro;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Keep every manifest, segment and redirect on the same HTTPS proxy as Web CSP. */
@OptIn(markerClass = UnstableApi.class)
final class KuroHttp561 {
    private static final String UA="Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36";
    private KuroHttp561(){}
    static boolean allowed(HttpUrl u){return u!=null&&u.isHttps()&&"www.zenkaitv.com".equals(u.host())&&u.port()==443&&u.username().isEmpty()&&u.password().isEmpty();}
    // A selected content URI is the only local child resource allowed. An HLS
    // manifest may otherwise reference only the approved proxy, not file/content.
    static boolean allowedMedia(android.net.Uri uri,android.net.Uri selectedSubtitle){
        if(uri==null)return false;
        if("content".equals(uri.getScheme()))return uri.equals(selectedSubtitle);
        return allowed(HttpUrl.parse(uri.toString()));
    }
    static final class SameProxy implements Interceptor {
        @Override public Response intercept(Chain chain)throws IOException{
            Request request=chain.request();
            for(int redirects=0;redirects<=4;redirects++){
                if(!allowed(request.url())||!("GET".equals(request.method())||"HEAD".equals(request.method())))throw new IOException("Unsupported media destination");
                Response response=chain.proceed(request);int code=response.code();
                if(code!=301&&code!=302&&code!=303&&code!=307&&code!=308)return response;
                String location=response.header("Location");HttpUrl next=location==null?null:request.url().resolve(location);response.close();
                if(redirects==4||!allowed(next))throw new IOException("Unsupported media redirect");
                request=request.newBuilder().url(next).build();
            }
            throw new IOException("Too many media redirects");
        }
    }
    private static final OkHttpClient CLIENT=new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).addInterceptor(new SameProxy()).build();
    static OkHttpDataSource.Factory factory(){return new OkHttpDataSource.Factory(CLIENT).setUserAgent(UA).setDefaultRequestProperties(Collections.singletonMap("Referer","https://www.zenkaitv.com/"));}
}
