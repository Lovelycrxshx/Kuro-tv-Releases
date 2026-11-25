package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import androidx.annotation.OptIn;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadCursor;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadRequest;
import androidx.media3.exoplayer.offline.DownloadService;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/** User-confirmed, app-private offline storage; never extracts embed URLs or DRM keys. */
@OptIn(markerClass = UnstableApi.class)
final class KuroOffline561 {
    private static DownloadManager manager;
    private static final java.util.concurrent.Executor DOWNLOAD_EXECUTOR=Executors.newFixedThreadPool(2);
    private static Cache cache;
    private KuroOffline561(){}
    // Playback uses PlaceholderDataSource upstream: a missing cache span can never fetch the network.
    static androidx.media3.datasource.DataSource.Factory offlineFactory(Context c){return new androidx.media3.datasource.cache.CacheDataSource.Factory().setCache(cache(c)).setUpstreamDataSourceFactory(androidx.media3.datasource.PlaceholderDataSource.FACTORY).setCacheWriteDataSinkFactory(null);}
    static synchronized DownloadManager manager(Context c){
        if(manager!=null)return manager;
        Context app=c.getApplicationContext();
        DatabaseProvider db=new StandaloneDatabaseProvider(app);
        cache=new SimpleCache(new File(app.getFilesDir(),"kuro-offline561"),new NoOpCacheEvictor(),db);
        androidx.media3.datasource.okhttp.OkHttpDataSource.Factory http=KuroHttp561.factory();
        manager=new DownloadManager(app,db,cache,http,DOWNLOAD_EXECUTOR);
        manager.setMaxParallelDownloads(1);manager.setMinRetryCount(0);
        return manager;
    }
    static synchronized Cache cache(Context c){manager(c);return cache;}
    static void confirmDownload(Activity activity,String address,String key,String title,String episode){
        if(!KuroPlayback561.allowedHls(address,"zenkai","hls")){Toast.makeText(activity,"This source does not expose a supported direct download.",Toast.LENGTH_LONG).show();return;}
        new AlertDialog.Builder(activity).setTitle("Download for offline viewing?")
            .setMessage("Only continue if you own this video or the provider permits offline downloads. Kuro does not bypass DRM or access checks; protected media may not work offline. The private download index may contain the source URL; offline media is not included in Kuro's JSON backup. A download may include multiple qualities and use substantial storage or mobile data. The source may refuse downloading or expire.")
            .setNegativeButton("Cancel",null).setPositiveButton("I have permission",(d,w)->{
                try{
                    JSONObject info=new JSONObject();info.put("title",limit(title,160));info.put("episode",limit(episode,20));info.put("key",KuroPlayback561.validKey(key)?key:"");
                    String id=KuroPlayback561.sha((key==null?"":key)+"\n"+address);
                    DownloadRequest request=new DownloadRequest.Builder(id,android.net.Uri.parse(address)).setMimeType(MimeTypes.APPLICATION_M3U8).setData(info.toString().getBytes(StandardCharsets.UTF_8)).build();
                    manager(activity);DownloadService.sendAddDownload(activity,KuroDownloadService561.class,request,false);DownloadService.sendResumeDownloads(activity,KuroDownloadService561.class,false);
                    Toast.makeText(activity,"Download queued. Open Manage downloads to check its status.",Toast.LENGTH_LONG).show();
                }catch(Exception e){Toast.makeText(activity,"Could not start the download on this device.",Toast.LENGTH_LONG).show();}
            }).show();
    }
    private static String limit(String text,int max){if(text==null)return "";return text.length()>max?text.substring(0,max):text;}
    static JSONObject metadata(Download d){try{return new JSONObject(new String(d.request.data,StandardCharsets.UTF_8));}catch(Exception ignored){return new JSONObject();}}
    private static String state(Download d){
        switch(d.state){case Download.STATE_COMPLETED:return "Ready offline";case Download.STATE_DOWNLOADING:float p=d.getPercentDownloaded();return p>=0?"Downloading · "+Math.round(p)+"%":"Downloading";case Download.STATE_FAILED:return "Failed — the source may not permit downloading";case Download.STATE_STOPPED:return "Paused";case Download.STATE_REMOVING:return "Removing";default:return "Queued";}
    }
    static void showDownloads(Activity activity){
        final DownloadManager m;
        try{m=manager(activity);}catch(RuntimeException e){Toast.makeText(activity,"Offline storage is unavailable.",Toast.LENGTH_LONG).show();return;}
        final androidx.media3.exoplayer.offline.DownloadIndex index=m.getDownloadIndex();
        java.util.concurrent.ExecutorService io=Executors.newSingleThreadExecutor();
        io.execute(()->{List<Download> list=new ArrayList<>();boolean failed=false;try(DownloadCursor cursor=index.getDownloads()){while(cursor.moveToNext()&&list.size()<500)list.add(cursor.getDownload());}catch(Exception e){failed=true;}
            final boolean error=failed;activity.runOnUiThread(()->{if(activity.isFinishing()||activity.isDestroyed())return;if(error){Toast.makeText(activity,"Could not read offline downloads.",Toast.LENGTH_LONG).show();return;}showList(activity,list);});io.shutdown();
        });
    }
    private static void showList(Activity a,List<Download> downloads){
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle("Kuro downloads").setNegativeButton("Close",null).setNeutralButton("Android downloads",(d,w)->{try{a.startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));}catch(Exception e){Toast.makeText(a,"Android Downloads is unavailable.",Toast.LENGTH_SHORT).show();}});
        if(downloads.isEmpty())b.setMessage("No Kuro offline downloads yet. A Download button appears for a supported direct stream. Only download content you have permission to store.");
        else{String[] labels=new String[downloads.size()];for(int i=0;i<labels.length;i++){Download x=downloads.get(i);JSONObject info=metadata(x);String title=info.optString("title","Video"),ep=info.optString("episode","");labels[i]=title+(ep.isEmpty()?"":" · Episode "+ep)+"\n"+state(x);}b.setItems(labels,(d,which)->actions(a,downloads.get(which)));}b.show();
    }
    private static void actions(Activity a,Download download){
        boolean ready=download.state==Download.STATE_COMPLETED,failed=download.state==Download.STATE_FAILED;String[] choices=ready?new String[]{"Play offline","Remove this download"}:failed?new String[]{"Retry this download","Remove this download"}:new String[]{"Remove this download"};
        new AlertDialog.Builder(a).setTitle(metadata(download).optString("title","Download")).setItems(choices,(dialog,which)->{
            if(ready&&which==0){Intent intent=new Intent(a,KuroOfflineActivity561.class);intent.putExtra("id",download.request.id);a.startActivity(intent);return;}
            if(failed&&which==0){new AlertDialog.Builder(a).setTitle("Retry this download?").setMessage("Retry only when the provider permits it. If access or verification is required, stop and check it manually.").setNegativeButton("Cancel",null).setPositiveButton("Retry",(d,w)->{try{DownloadService.sendAddDownload(a,KuroDownloadService561.class,download.request,false);DownloadService.sendResumeDownloads(a,KuroDownloadService561.class,false);Toast.makeText(a,"Download queued again.",Toast.LENGTH_SHORT).show();}catch(RuntimeException e){Toast.makeText(a,"Could not retry this download.",Toast.LENGTH_SHORT).show();}}).show();return;}
            new AlertDialog.Builder(a).setTitle("Remove this download?").setMessage("Only this offline copy is removed. Your lists and history stay.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{try{DownloadService.sendRemoveDownload(a,KuroDownloadService561.class,download.request.id,false);Toast.makeText(a,"Removing download.",Toast.LENGTH_SHORT).show();}catch(RuntimeException e){Toast.makeText(a,"Could not remove download.",Toast.LENGTH_SHORT).show();}}).show();
        }).setNegativeButton("Back",(d,w)->showDownloads(a)).show();
    }
}
