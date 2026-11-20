package app.lovelycrxshx.kuro;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadService;
import androidx.media3.exoplayer.scheduler.Scheduler;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public final class KuroDownloadService561 extends DownloadService {
    public KuroDownloadService561(){super(5610,DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,"kuro-downloads561",R.string.kuro_download_channel,0);}
    @Override protected DownloadManager getDownloadManager(){return KuroOffline561.manager(this);}
    @Nullable @Override protected Scheduler getScheduler(){return new androidx.media3.exoplayer.scheduler.PlatformScheduler(this,5613);}
    @Override protected Notification getForegroundNotification(List<Download> downloads,int notMetRequirements){
        Intent launch=new Intent(this,MainActivity.class);launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(this,5610,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        int active=0;for(Download d:downloads)if(d.state==Download.STATE_DOWNLOADING||d.state==Download.STATE_QUEUED)active++;
        return new Notification.Builder(this,"kuro-downloads561").setSmallIcon(getApplicationInfo().icon).setContentTitle("Kuro.tv downloads").setContentText(active>0?"Downloading authorized media":"Preparing downloads").setContentIntent(tap).setOngoing(active>0).build();
    }
}
