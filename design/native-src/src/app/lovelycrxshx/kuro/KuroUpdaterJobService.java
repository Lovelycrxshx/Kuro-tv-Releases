package app.lovelycrxshx.kuro;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;

/**
 * Persisted daily job: fetch release metadata, then notify or start the download.
 * Android schedules this at its own discretion, so the interval is only approximate.
 */
public final class KuroUpdaterJobService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters params) {
        final Context app = getApplicationContext();
        Thread worker = new Thread(() -> {
            try {
                KuroUpdater572.run(app);
            } catch (Throwable ignored) {
                // A failed check stays recorded in Settings; it never blocks playback.
            } finally {
                jobFinished(params, false);
            }
        }, "kuro-update-check");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // Ask Android to run this pass again later.
    }
}
