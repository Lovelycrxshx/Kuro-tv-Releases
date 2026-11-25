package app.lovelycrxshx.kuro;

import static org.junit.Assert.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import org.robolectric.RuntimeEnvironment;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Synthetic guard tests; they do not download or decode video. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class KuroOffline561Test {
    @Test public void cacheMissFailsRatherThanOpeningNetwork() throws Exception {
        Context app=RuntimeEnvironment.getApplication();
        DataSource source=KuroOffline561.offlineFactory(app).createDataSource();
        try {
            source.open(new DataSpec(Uri.parse("https://www.zenkaitv.com/kuro-cache-miss-test.m3u8")));
            fail("An uncached item must not be fetched by offline playback");
        } catch(IOException expected) {
            // A missing cache span is an ordinary offline failure.
        } finally {source.close();}
    }
    @Test public void cacheOnlyFactoryReadsAnExistingSpan() throws Exception {
        Context app=RuntimeEnvironment.getApplication();
        androidx.media3.datasource.cache.Cache cache=KuroOffline561.cache(app);
        String key="kuro-fixture-cached-561", uri="https://www.zenkaitv.com/fixture-cached.ts";
        DataSpec spec=new DataSpec.Builder().setUri(uri).setKey(key).setLength(4).build();
        androidx.media3.datasource.cache.CacheSpan hole=cache.startReadWrite(key,0,4);
        assertTrue(hole.isHoleSpan());
        androidx.media3.datasource.DataSink sink=new androidx.media3.datasource.cache.CacheDataSink.Factory().setCache(cache).createDataSink();
        try{sink.open(spec);sink.write(new byte[]{1,2,3,4},0,4);}finally{sink.close();cache.releaseHoleSpan(hole);}
        DataSource source=KuroOffline561.offlineFactory(app).createDataSource();byte[] actual=new byte[4];int count=0;
        try{source.open(spec);while(count<actual.length){int n=source.read(actual,count,actual.length-count);if(n<0)break;count+=n;}}finally{source.close();}
        assertEquals(4,count);assertArrayEquals(new byte[]{1,2,3,4},actual);
    }
    @Test public void invalidOfflineIntentClosesCleanly() {
        Context app=RuntimeEnvironment.getApplication();android.content.Intent intent=new android.content.Intent(app,KuroOfflineActivity561.class).putExtra("id","invalid");
        org.robolectric.android.controller.ActivityController<KuroOfflineActivity561> controller=org.robolectric.Robolectric.buildActivity(KuroOfflineActivity561.class,intent).create();
        assertTrue(controller.get().isFinishing());controller.destroy();
    }
    @Test public void downloadServicesAreDeclaredWithExpectedRestrictions() throws Exception {
        Context app=RuntimeEnvironment.getApplication();
        ServiceInfo download=app.getPackageManager().getServiceInfo(new ComponentName(app,KuroDownloadService561.class),0);
        ServiceInfo scheduler=app.getPackageManager().getServiceInfo(new ComponentName(app,androidx.media3.exoplayer.scheduler.PlatformScheduler.PlatformSchedulerService.class),0);
        assertFalse(download.exported);
        assertEquals("android.permission.BIND_JOB_SERVICE",scheduler.permission);
    }
}
