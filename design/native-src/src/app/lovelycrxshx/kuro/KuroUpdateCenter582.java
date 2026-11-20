package app.lovelycrxshx.kuro;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Kuro 5.82 — automatic updating.
 *
 * 5.72 taught Kuro to check and, after three reminders, to download. It never installed
 * anything from inside the app: the download finished in the background, and unless the
 * owner tapped the notification nothing else happened. This class closes that gap:
 *
 *   check  ->  download  ->  verify  ->  hand straight to Android's installer
 *
 * A newer release found by a scheduled pass or by "Update now" is queued and downloaded
 * immediately (no reminder cadence to wait through) and the in-app update screen opens so
 * the download is visible while it happens. When the file passes the signature check the
 * installer is offered straight away, once per version, so a cancelled install never
 * turns into a loop.
 *
 * What does not change, and must not: the bytes are verified against the published
 * SHA-256 and the release certificate, the download network setting still applies, and
 * Android still owns the final confirmation. Kuro cannot install silently and this class
 * never claims otherwise.
 */
public final class KuroUpdateCenter582 {
    static final String CHECKING = "checking";
    static final String QUEUED = "queued";
    static final String DOWNLOADING = "downloading";
    static final String VERIFYING = "verifying";
    static final String READY = "ready";
    static final String WAITING_WIFI = "waiting_wifi";
    /** 5.83: the release is waiting out its 24/48-hour window before it installs itself. */
    static final String SCHEDULED = "scheduled";
    static final String ERROR = "error";

    private KuroUpdateCenter582() {}

    static SharedPreferences prefs(Context c) {
        return KuroUpdater572.prefs(c);
    }

    /** Automatic updating is on by default: the owner asked for check *and* install. */
    static boolean autoEnabled(Context c) {
        return prefs(c).getBoolean("auto582", true);
    }

    static void setAutoEnabled(Context c, boolean on) {
        prefs(c).edit().putBoolean("auto582", on).apply();
    }

    /** Hand the verified file to Android's installer without an extra tap. */
    static boolean autoInstall(Context c) {
        return prefs(c).getBoolean("autoInstall582", true);
    }

    static void setAutoInstall(Context c, boolean on) {
        prefs(c).edit().putBoolean("autoInstall582", on).apply();
    }

    // --------------------------------------------------------------- state changes

    /** True when automatic mode should take a found release straight to a download. */
    static boolean shouldDownloadNow(Context c, KuroUpdater572.Release release) {
        if (release == null || !autoEnabled(c)) return false;
        SharedPreferences p = prefs(c);
        if (READY.equals(p.getString("stage", ""))) return false;
        if (p.getInt("downloadFailures", 0) >= KuroUpdater572.MAX_DOWNLOAD_FAILURES) return false;
        String queued = p.getString("version", "");
        // Never restart a download that is already running for this same version.
        if (release.version.equals(queued)
                && (DOWNLOADING.equals(p.getString("stage", ""))
                || QUEUED.equals(p.getString("stage", ""))
                || WAITING_WIFI.equals(p.getString("stage", ""))
                || VERIFYING.equals(p.getString("stage", "")))) return false;
        return KuroUpdater572.isNewer(release.version, KuroUpdater572.installedVersionName(c));
    }

    /**
     * 5.83: the automatic path, with the release window on top of it.
     *
     * A release that is still inside its window is recorded as scheduled and a one-shot
     * job is parked at the end of it; a release whose window has already passed — the
     * device was off, the app was not opened for two days — downloads immediately.
     *
     * @return true when automatic mode handled this release (either way)
     */
    static boolean autoStep(Context c, KuroUpdater572.Release release) {
        if (!shouldDownloadNow(c, release)) return false;
        if (KuroUpdateSchedule583.dueNow(c, release.publishedAt)) {
            KuroUpdateSchedule583.clear(c);
            startDownload(c, release);
        } else {
            KuroUpdateSchedule583.defer(c, release);
        }
        return true;
    }

    /** Records the release, opens the progress screen and starts the foreground download. */
    static void startDownload(Context c, KuroUpdater572.Release release) {
        if (release == null) return;
        SharedPreferences p = prefs(c);
        p.edit()
                .putString("tag", release.tag)
                .putString("version", release.version)
                .putString("assetName", release.assetName)
                .putString("assetUrl", release.assetUrl)
                .putLong("assetSize", release.size)
                .putString("assetDigest", release.digest)
                // Automatic mode has no reminder cadence: the download happens now, so the
                // third-reminder gate in advance() must never hold it back.
                .putInt("notifications", KuroUpdater572.MAX_NOTIFICATIONS)
                .putLong("lastNotifyAt", 0)
                .putInt("downloadFailures", 0)
                .putLong("lastDownloadAt", 0)
                .putLong("dlBytes", 0)
                .putLong("dlTotal", release.size)
                .putLong("dlSpeed", 0)
                .putLong("dlUpdatedAt", 0)
                .putString("stage", QUEUED)
                .putString("lastError", "")
                .apply();
        KuroUpdater572.requestDownload(c);
    }

    /**
     * One pass: fetch the release, then either start the download or record that this
     * install is already current. Safe to call from a worker thread.
     *
     * @return true when a newer release was found
     */
    static boolean checkNow(Context c) {
        return checkNow(c, false);
    }

    /**
     * @param immediate true when a person asked for it, so the 24/48-hour window is skipped
     */
    static boolean checkNow(Context c, boolean immediate) {
        SharedPreferences p = prefs(c);
        p.edit().putString("stage", CHECKING).putString("lastError", "")
                .putLong("lastCheckAt", System.currentTimeMillis()).apply();
        KuroUpdater572.Release release = KuroUpdater572.fetch(c);
        if (release == null) {
            String problem = p.getString("lastError", "");
            p.edit().putString("stage", ERROR)
                    .putString("lastError", problem.isEmpty() ? "The update check failed." : problem).apply();
            return false;
        }
        if (!KuroUpdater572.isNewer(release.version, KuroUpdater572.installedVersionName(c))) {
            p.edit().putString("stage", "").putString("lastError", "")
                    .putString("latestSeen", release.version).putString("latestTag", release.tag).apply();
            return false;
        }
        if (immediate || KuroUpdateSchedule583.dueNow(c, release.publishedAt)) {
            KuroUpdateSchedule583.clear(c);
            startDownload(c, release);
        } else if (!autoEnabled(c)) {
            // Reminders-only mode keeps its 5.72 behaviour; nothing is scheduled here.
            return true;
        } else {
            KuroUpdateSchedule583.defer(c, release);
        }
        return true;
    }

    /** Stops an in-flight download. The partial file stays for the next attempt to resume. */
    static void stopDownload(Context c) {
        KuroUpdaterDownloadService.cancel();
        prefs(c).edit().putString("stage", QUEUED).putLong("dlSpeed", 0).apply();
    }

    /** True once Android's installer has been offered this version automatically. */
    static boolean autoOfferAllowed(Context c, String version) {
        if (version == null || version.isEmpty() || !autoInstall(c)) return false;
        return !version.equals(prefs(c).getString("autoOffered582", ""));
    }

    static void markAutoOffered(Context c, String version) {
        if (version == null) return;
        prefs(c).edit().putString("autoOffered582", version).apply();
    }

    // -------------------------------------------------------------------- snapshot

    static final class Snapshot {
        final String stage, version, error;
        final long bytes, total, speed, updatedAt, dueAt;
        final int percent;

        Snapshot(String stage, String version, String error, long bytes, long total, long speed, long updatedAt) {
            this(stage, version, error, bytes, total, speed, updatedAt, 0L);
        }

        Snapshot(String stage, String version, String error, long bytes, long total, long speed, long updatedAt, long dueAt) {
            this.dueAt = dueAt;
            this.stage = stage == null ? "" : stage;
            this.version = version == null ? "" : version;
            this.error = error == null ? "" : error;
            this.bytes = bytes;
            this.total = total;
            this.speed = speed;
            this.updatedAt = updatedAt;
            this.percent = total > 0 ? (int) Math.max(0, Math.min(100, bytes * 100L / total)) : -1;
        }

        boolean checking() { return CHECKING.equals(stage); }
        boolean scheduled() { return SCHEDULED.equals(stage); }
        boolean verifying() { return VERIFYING.equals(stage); }
        boolean ready() { return READY.equals(stage); }
        boolean failed() { return ERROR.equals(stage) || (!error.isEmpty() && stage.isEmpty()); }
        boolean paused() { return WAITING_WIFI.equals(stage); }

        boolean transferring() {
            return DOWNLOADING.equals(stage) || QUEUED.equals(stage) || WAITING_WIFI.equals(stage);
        }

        /** Anything the update screen should stay open for. */
        boolean busy() { return checking() || transferring() || verifying(); }
    }

    static Snapshot snapshot(Context c) {
        SharedPreferences p = prefs(c);
        long total = p.getLong("dlTotal", p.getLong("assetSize", 0));
        long bytes = p.getLong("dlBytes", 0);
        if (total <= 0) total = p.getLong("assetSize", 0);
        return new Snapshot(p.getString("stage", ""), p.getString("version", ""),
                p.getString("lastError", ""), bytes, total,
                p.getLong("dlSpeed", 0), p.getLong("dlUpdatedAt", 0),
                p.getLong("dueAt583", 0));
    }

    // ---------------------------------------------------------------------- strings

    static String size(long bytes) {
        if (bytes <= 0) return "0 MiB";
        double mib = bytes / 1048576.0;
        if (mib >= 1000) return Math.round(mib / 100.0) * 100 + " MiB";
        if (mib >= 100) return String.format(Locale.ROOT, "%.0f MiB", mib);
        return String.format(Locale.ROOT, "%.2f MiB", mib);
    }

    static String speed(long bytesPerSecond) {
        if (bytesPerSecond <= 0) return "";
        return size(bytesPerSecond) + "/s";
    }

    static String eta(long bytes, long total, long bytesPerSecond) {
        if (total <= 0 || bytesPerSecond <= 0 || bytes >= total) return "";
        long seconds = Math.max(1, (total - bytes) / Math.max(1, bytesPerSecond));
        if (seconds >= 3600) return (seconds / 3600) + " h left";
        if (seconds >= 60) return (seconds / 60) + " min left";
        return seconds + " s left";
    }

    static String headline(Snapshot s) {
        if (s == null) return "Kuro updates";
        if (s.ready()) return "Ready to install";
        if (s.verifying()) return "Checking the update";
        if (s.checking()) return "Checking for updates";
        if (s.paused()) return "Waiting for Wi-Fi";
        if (s.scheduled()) return "Kuro " + s.version + " is scheduled";
        if (DOWNLOADING.equals(s.stage)) return "Downloading Kuro " + s.version;
        if (QUEUED.equals(s.stage)) return "Starting the download";
        if (s.failed()) return "Update could not finish";
        return "Kuro updates";
    }

    static String detail(Snapshot s) {
        if (s == null) return "";
        if (s.checking()) return "Reading the public GitHub release page…";
        if (s.paused()) return "The download starts by itself when Wi-Fi is the active network.";
        if (s.scheduled()) return scheduledLine(s);
        if (s.verifying()) return "Confirming the package, the version code and the release certificate.";
        if (s.ready()) return "The file is downloaded and verified on this phone. Android asks before it installs.";
        if (s.failed()) return s.error.isEmpty() ? "Tap Retry to try again." : s.error;
        if (DOWNLOADING.equals(s.stage) || QUEUED.equals(s.stage)) {
            StringBuilder text = new StringBuilder();
            if (s.total > 0) text.append(size(s.bytes)).append(" of ").append(size(s.total));
            else text.append(size(s.bytes)).append(" downloaded");
            String speed = speed(s.speed);
            if (!speed.isEmpty()) text.append("  ·  ").append(speed);
            String eta = eta(s.bytes, s.total, s.speed);
            if (!eta.isEmpty()) text.append("  ·  ").append(eta);
            return text.toString();
        }
        return "Kuro checks the public release page about once a day.";
    }

    /** "Installs itself on 21 Nov, 18:05 (in 14 h)." */
    static String scheduledLine(Snapshot s) {
        if (s == null || !s.scheduled()) return "";
        if (s.dueAt <= 0) return "Installs itself as soon as the download finishes.";
        long left = s.dueAt - System.currentTimeMillis();
        String when = KuroUpdateSchedule583.describeDue(s.dueAt);
        return left > 0
                ? "Installs itself on " + when + " (" + KuroUpdateSchedule583.humanLeft(left) + ")."
                : "The window is over — the download starts on the next check.";
    }

    /** One line for the settings panel. */
    static String summary(Context c) {
        Snapshot s = snapshot(c);
        if (s.ready()) return "Kuro " + s.version + " is downloaded and waiting for Android's installer.";
        if (DOWNLOADING.equals(s.stage)) return "Downloading Kuro " + s.version + " — " + (s.percent < 0 ? size(s.bytes) : s.percent + "%");
        if (s.paused()) return "Kuro " + s.version + " is queued until Wi-Fi is available.";
        if (s.scheduled()) return "Kuro " + s.version + " waits until " + KuroUpdateSchedule583.describeDue(s.dueAt)
                + " (" + KuroUpdateSchedule583.delayLabel(KuroUpdateSchedule583.delayHours(c)) + ").";
        if (s.verifying()) return "Checking Kuro " + s.version + " before installing.";
        if (s.checking()) return "Checking the public release page now…";
        if (s.failed() && !s.error.isEmpty()) return s.error;
        return autoEnabled(c)
                ? "New releases install by themselves " + KuroUpdateSchedule583.delayLabel(KuroUpdateSchedule583.delayHours(c))
                        + " after publication, or on the next check once that window has passed."
                : "Kuro checks for new releases and reminds you; you start the download.";
    }
}
