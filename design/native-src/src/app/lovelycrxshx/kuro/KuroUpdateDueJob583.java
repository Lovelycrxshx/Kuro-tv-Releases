package app.lovelycrxshx.kuro;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;

/**
 * Kuro 5.83 — the end of the update window.
 *
 * A one-shot job parked at the release's publish time plus the chosen delay. When it
 * fires, the normal updater pass runs: the release is re-fetched, the window is now over,
 * and the download starts (or waits for Wi-Fi). If the job is lost — the app was force
 * stopped, the device was off, Android dropped it — nothing is stranded: the daily check
 * and the catch-up on resume both see the window has passed and act on it.
 */
public final class KuroUpdateDueJob583 extends JobService {
    @Override
    public boolean onStartJob(final JobParameters params) {
        final Context app = getApplicationContext();
        Thread worker = new Thread(() -> {
            try {
                KuroUpdater572.run(app);
            } catch (Throwable ignored) {
                // A failed pass leaves the state alone; the daily job retries.
            } finally {
                jobFinished(params, false);
            }
        }, "kuro-update-due");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true; // Ask Android to run this pass again later.
    }
}
