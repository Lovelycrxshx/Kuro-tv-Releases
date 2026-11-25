package app.lovelycrxshx.kuro;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
/** Do not retry or select a different HLS variant after verification/access failures. */
@OptIn(markerClass = UnstableApi.class)
final class KuroLoadPolicy561 extends DefaultLoadErrorHandlingPolicy {
    static boolean blocked(Throwable t){while(t!=null){if(t instanceof HttpDataSource.InvalidResponseCodeException){int n=((HttpDataSource.InvalidResponseCodeException)t).responseCode;return n==401||n==403||n==429;}t=t.getCause();}return false;}
    @Override public long getRetryDelayMsFor(LoadErrorHandlingPolicy.LoadErrorInfo info){return blocked(info.exception)?C.TIME_UNSET:super.getRetryDelayMsFor(info);}
    @Override public LoadErrorHandlingPolicy.FallbackSelection getFallbackSelectionFor(LoadErrorHandlingPolicy.FallbackOptions options,LoadErrorHandlingPolicy.LoadErrorInfo info){return blocked(info.exception)?null:super.getFallbackSelectionFor(options,info);}
}
