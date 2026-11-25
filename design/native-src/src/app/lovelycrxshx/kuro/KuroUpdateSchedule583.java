package app.lovelycrxshx.kuro;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Kuro 5.83 — when an automatic update lands.
 *
 * 5.82 installed a release the moment it was found. That is too eager for a build that
 * arrives on a TV you are in the middle of watching with. This class adds one rule:
 *
 *   a release waits {@link #delayHours} hours after GitHub publishes it, then installs
 *   itself. If that window has *already* passed when the app next looks — because the
 *   device was off, or the app was not opened for two days — the update happens at once.
 *
 * That second half is the important one: the delay can never turn into an indefinite
 * postponement. The app is never more than one check away from current.
 *
 * The delay is a per-device choice of 24 hours (default), 48 hours, or "as soon as it
 * finds one". It is measured from the release's own publish time, not from when this
 * device happened to notice, so a fresh install and a phone that has been asleep for a
 * week behave the same way.
 *
 * What this class does not change: the file is still verified against the published
 * SHA-256 and the pinned certificate, the download network setting still applies, and
 * Android still shows its own install confirmation.
 */
public final class KuroUpdateSchedule583 {
    static final int JOB_ID = 58301;
    static final int DEFAULT_HOURS = 24;
    static final int MAX_HOURS = 48;
    static final long HOUR_MS = 60L * 60L * 1000L;
    /** Automatic catch-ups on resume never run more often than this. */
    static final long KICK_GAP_MS = 90L * 1000L;

    private static long lastKick;

    private KuroUpdateSchedule583() {}

    static SharedPreferences prefs(Context c) {
        return KuroUpdater572.prefs(c);
    }

    /** 0 = as soon as it finds one, 24 (default) or 48. Anything else falls back to 24. */
    static int delayHours(Context c) {
        int hours = prefs(c).getInt("delay583", DEFAULT_HOURS);
        if (hours == 0 || hours == DEFAULT_HOURS || hours == MAX_HOURS) return hours;
        return DEFAULT_HOURS;
    }

    static void setDelayHours(Context c, int hours) {
        if (hours != 0 && hours != DEFAULT_HOURS && hours != MAX_HOURS)
            throw new IllegalArgumentException("Invalid update delay");
        prefs(c).edit().putInt("delay583", hours).apply();
        if (hours == 0) {
            // "As soon as it finds one" also releases anything already waiting.
            long due = prefs(c).getLong("dueAt583", 0);
            if (due > 0) prefs(c).edit().putLong("dueAt583", 0).apply();
        }
    }

    static String delayLabel(int hours) {
        if (hours <= 0) return "as soon as it finds one";
        return "after " + hours + " hours";
    }

    /** Epoch millis at which this release may install itself; 0 means "no wait". */
    static long dueAt(long publishedAt, int hours) {
        if (publishedAt <= 0 || hours <= 0) return 0L;
        return publishedAt + hours * HOUR_MS;
    }

    /** True when the wait is over, or when there is nothing to wait for. */
    static boolean due(long now, long publishedAt, int hours) {
        long due = dueAt(publishedAt, hours);
        return due <= 0 || now >= due;
    }

    static boolean dueNow(Context c, long publishedAt) {
        return due(System.currentTimeMillis(), publishedAt, delayHours(c));
    }

    /** Milliseconds left before the release may install itself; 0 when it already may. */
    static long remaining(long now, long publishedAt, int hours) {
        long due = dueAt(publishedAt, hours);
        return due <= 0 ? 0L : Math.max(0L, due - now);
    }

    /** "in 14 h", "in 45 min", "in under a minute". */
    static String humanLeft(long millis) {
        if (millis <= 0) return "now";
        long minutes = millis / 60000L;
        if (minutes < 1) return "in under a minute";
        if (minutes < 60) return "in " + minutes + " min";
        long hours = minutes / 60L;
        if (hours < 48) return "in " + hours + " h";
        return "in " + (hours / 24L) + " days";
    }

    /** Local wall-clock time, e.g. "21 Nov, 14:03". Never a raw timestamp. */
    static String describeDue(long dueAt) {
        if (dueAt <= 0) return "as soon as it is found";
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dueAt);
        Locale locale = Locale.getDefault();
        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, locale);
        if (month == null) month = String.format(locale, "%02d", calendar.get(Calendar.MONTH) + 1);
        // "21 Nov, 14:03" — day, short month, 24-hour clock.
        return calendar.get(Calendar.DAY_OF_MONTH) + " " + month + ", "
                + String.format(Locale.ROOT, "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    // ------------------------------------------------------------------ state

    /** Records that this release is waiting out its window. */
    static void defer(Context c, KuroUpdater572.Release release) {
        if (release == null) return;
        long due = dueAt(release.publishedAt, delayHours(c));
        prefs(c).edit()
                .putString("stage", KuroUpdateCenter582.SCHEDULED)
                .putString("tag", release.tag)
                .putString("version", release.version)
                .putString("assetName", release.assetName)
                .putString("assetUrl", release.assetUrl)
                .putLong("assetSize", release.size)
                .putString("assetDigest", release.digest)
                .putLong("publishedAt583", release.publishedAt)
                .putLong("dueAt583", due)
                .putLong("dlBytes", 0)
                .putLong("dlTotal", release.size)
                .putString("lastError", "")
                .apply();
        scheduleJob(c, due);
    }

    static boolean scheduled(Context c) {
        return KuroUpdateCenter582.SCHEDULED.equals(prefs(c).getString("stage", ""));
    }

    static long scheduledDueAt(Context c) {
        return prefs(c).getLong("dueAt583", 0);
    }

    /** Clears a scheduled update once it is no longer wanted or no longer newer. */
    static void clear(Context c) {
        cancelJob(c);
        SharedPreferences p = prefs(c);
        if (!scheduled(c)) return;
        p.edit().putString("stage", "").putLong("dueAt583", 0).putLong("publishedAt583", 0).apply();
    }

    // -------------------------------------------------------------- scheduling

    /** One-shot job at the end of the window, so the update does not wait for tomorrow. */
    static void scheduleJob(Context c, long dueAt) {
        try {
            JobScheduler scheduler = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler == null) return;
            scheduler.cancel(JOB_ID);
            long now = System.currentTimeMillis();
            long latency = dueAt <= 0 ? 1000L : Math.max(1000L, dueAt - now);
            JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(c, KuroUpdateDueJob583.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setMinimumLatency(latency)
                    .build();
            scheduler.schedule(info);
        } catch (RuntimeException ignored) {
            // Android may refuse; the daily check and the resume catch-up still work.
        }
    }

    static void cancelJob(Context c) {
        try {
            JobScheduler scheduler = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler != null) scheduler.cancel(JOB_ID);
        } catch (RuntimeException ignored) {}
    }

    /**
     * Throttled catch-up used when the app comes to the front: a due window is acted on
     * immediately instead of waiting for the next scheduled pass.
     */
    static boolean claimCatchUp(Context c) {
        if (!scheduled(c)) return false;
        long due = scheduledDueAt(c);
        if (due > 0 && System.currentTimeMillis() < due) return false;
        long now = System.currentTimeMillis();
        if (now - lastKick < KICK_GAP_MS) return false;
        lastKick = now;
        return true;
    }
}
