package app.lovelycrxshx.kuro;
import android.app.job.JobParameters;
import android.app.job.JobService;

/** The OS controls execution time. Reopening after a force-stop schedules the job again. */
public final class NewsJobService extends JobService {
 private volatile boolean stopped=false; private Thread worker;
 @Override public boolean onStartJob(JobParameters params){
  stopped=false;worker=new Thread(()->{try{if(NewsSync.enabled(this))NewsSync.refresh(this,false);}finally{if(!stopped)jobFinished(params,false);}},"kuro-news-job");worker.start();return true;
 }
 @Override public boolean onStopJob(JobParameters params){stopped=true;if(worker!=null)worker.interrupt();return NewsSync.enabled(this);}
}
