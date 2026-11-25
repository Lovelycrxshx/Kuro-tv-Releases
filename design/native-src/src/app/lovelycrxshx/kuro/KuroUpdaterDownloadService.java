package app.lovelycrxshx.kuro;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Downloads the announced Kuro APK in the app over Wi-Fi or mobile data.
 *
 * There is no prompt, no browser hand-off and no "remind me later" button. The file is
 * checksummed when the release publishes one, then package, version and signing
 * identity are re-checked before the single "ready to install" notice. Android's
 * installer still decides whether the update is installed.
 */
public final class KuroUpdaterDownloadService extends Service {
    private volatile boolean running;
    private long lastProgressPost;

    /** Set by the in-app update screen's Stop action. Process-local, like the service. */
    private static volatile boolean cancelRequested;

    static void cancel() {
        cancelRequested = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (running) return START_NOT_STICKY;
        running = true;
        cancelRequested = false;
        KuroUpdater572.ensureChannel(this);
        try {
            startForeground(KuroUpdater572.PROGRESS_ID, progress(-1, "Preparing the update"));
        } catch (RuntimeException e) {
            running = false;
            stopSelf();
            return START_NOT_STICKY;
        }
        Thread worker = new Thread(this::work, "kuro-update-download");
        worker.setDaemon(true);
        worker.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    private static final class WaitForWifi extends IOException {}

    private static final class Cancelled extends IOException { Cancelled() { super("Update download stopped."); } }

    /** The bytes are wrong, not the connection: drop the partial file instead of resuming it. */
    private static final class BadUpdate extends IOException { BadUpdate(String message) { super(message); } }

    private void work() {
        SharedPreferences p = KuroUpdater572.prefs(this);
        File part = null;
        try {
            String version = p.getString("version", "");
            String address = p.getString("assetUrl", "");
            long expectedSize = p.getLong("assetSize", -1);
            String digest = p.getString("assetDigest", "");
            File target = KuroUpdater572.apkFile(this, version);
            if (target == null || address.isEmpty()) throw new IOException("No update is queued.");
            android.net.Network wifi=KuroUpdater572.wifiOnly(this)?KuroUpdater572.activeWifi(this):null;
            if(KuroUpdater572.wifiOnly(this)&&wifi==null)throw new WaitForWifi();
            p.edit().putString("stage", "downloading").putLong("lastDownloadAt", System.currentTimeMillis())
                    .putLong("dlSpeed", 0).putLong("dlUpdatedAt", 0).apply();
            part = new File(target.getPath() + ".part");
            String actual = download(address, part, expectedSize, wifi);
            if (!digest.isEmpty() && !digest.equalsIgnoreCase(actual))
                throw new BadUpdate("The downloaded file did not match the published checksum.");
            if (target.exists() && !target.delete()) throw new IOException("Could not replace the previous update file.");
            if (!part.renameTo(target)) throw new IOException("Could not finish saving the update file.");
            part = null;
            p.edit().putString("stage", "verifying").putLong("dlBytes", target.length())
                    .putLong("dlTotal", Math.max(1, expectedSize > 0 ? expectedSize : target.length()))
                    .putLong("dlSpeed", 0).apply();
            notifyProgress(100, target.length(), target.length());
            KuroApkPreflight572.Result check = KuroApkPreflight572.inspect(this, target);
            if (!check.ok()) {
                target.delete();
                throw new BadUpdate(check.message());
            }
            KuroUpdater572.markReady(this, version);
            KuroUpdater572.notifyReady(this, false);
        } catch (Exception e) {
            if (part != null && part.isFile() && e instanceof BadUpdate) part.delete();
            if (e instanceof Cancelled) {
                // The owner stopped it. Keep the .part file so the next attempt resumes.
                p.edit().putString("stage", "").putString("lastError", "").putLong("dlSpeed", 0).apply();
            } else if(e instanceof WaitForWifi||(KuroUpdater572.wifiOnly(this)&&KuroUpdater572.activeWifi(this)==null))KuroUpdater572.waitForWifi(this);
            else KuroUpdater572.markDownloadFailed(this, e.getMessage());
            p.edit().putLong("dlSpeed", 0).apply();
        } finally {
            running = false;
            try {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } catch (RuntimeException ignored) {}
            stopSelf();
        }
    }

    private String download(String address, File part, long expectedSize, android.net.Network wifi) throws Exception {
        String current = address;
        // Resume from whatever the previous attempt left behind. The part file is per
        // version, so it can never mix two releases together.
        long have = part.isFile() ? part.length() : 0L;
        if (have > 0 && expectedSize > 0 && have >= expectedSize) have = 0L;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        if (have > 0) hashExisting(md, part);
        for (int hop = 0; hop <= 3; hop++) {
            if (!KuroUpdater572.safeHttps(current)) throw new IOException("Unsupported update address");
            checkWifi(wifi);
            HttpURLConnection c = (HttpURLConnection) (wifi==null?new URL(current).openConnection():wifi.openConnection(new URL(current)));
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Accept", "application/octet-stream");
            c.setRequestProperty("Accept-Encoding", "identity");
            c.setRequestProperty("User-Agent", "Kuro.tv-Android-5.73");
            if (have > 0) c.setRequestProperty("Range", "bytes=" + have + "-");
            try {
                int code = c.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = c.getHeaderField("Location");
                    if (location == null) throw new IOException("Update redirect unavailable");
                    current = new URL(new URL(current), location).toString();
                    continue;
                }
                if (code != 200 && code != 206) throw new IOException("Update download returned HTTP " + code);
                // 206 means the range was honoured; 200 means the server sent the whole
                // file and the partial copy must be thrown away.
                boolean resumed = code == 206 && have > 0;
                if (!resumed) {
                    have = 0L;
                    md.reset();
                }
                long total = expectedSize > 0 ? expectedSize : (resumed ? have + c.getContentLengthLong() : c.getContentLengthLong());
                if (total > KuroUpdater572.MAX_APK_BYTES) throw new IOException("The update file is larger than expected.");
                long written = 0;
                lastProgressPost = 0;
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(part, resumed)) {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (!running || cancelRequested) throw new Cancelled();
                        checkWifi(wifi);
                        written += read;
                        if (have + written > KuroUpdater572.MAX_APK_BYTES) throw new IOException("The update file is larger than expected.");
                        out.write(buffer, 0, read);
                        md.update(buffer, 0, read);
                        long now = System.currentTimeMillis();
                        if (now - lastProgressPost > 1000) {
                            lastProgressPost = now;
                            int percent = total > 0 ? (int) ((have + written) * 100L / total) : -1;
                            notifyProgress(percent, have + written, total);
                        }
                    }
                    out.flush();
                }
                if (written <= 0 && !resumed) throw new IOException("The update download was empty.");
                return hex(md.digest());
            } finally {
                c.disconnect();
            }
        }
        throw new IOException("Too many update redirects");
    }

    /** Feeds the bytes already on disk into the digest so a resumed file is still verified. */
    private static void hashExisting(MessageDigest md, File file) throws IOException {
        byte[] buffer = new byte[65536];
        try (InputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) md.update(buffer, 0, read);
        }
    }

    private void checkWifi(android.net.Network pinned) throws WaitForWifi {
        if(!KuroUpdater572.wifiOnly(this))return;
        android.net.Network current=KuroUpdater572.activeWifi(this);
        if(pinned==null||current==null||!pinned.equals(current))throw new WaitForWifi();
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }

    private long lastSampleBytes = -1, lastSampleAt = 0;

    private void notifyProgress(int percent, long written, long total) {
        SharedPreferences p = KuroUpdater572.prefs(this);
        long now = System.currentTimeMillis();
        long speed = 0;
        if (lastSampleBytes >= 0 && now > lastSampleAt && written >= lastSampleBytes) {
            speed = (written - lastSampleBytes) * 1000L / Math.max(1, now - lastSampleAt);
        }
        if (lastSampleBytes < 0 || now - lastSampleAt >= 900) {
            lastSampleBytes = written;
            lastSampleAt = now;
        }
        // Written for the in-app update screen: byte counts only, never a path or a token.
        p.edit().putLong("dlBytes", written)
                .putLong("dlTotal", Math.max(0, total))
                .putLong("dlSpeed", speed)
                .putLong("dlUpdatedAt", now).apply();
        try {
            String text;
            if (total > 0) text = (written / 1048576L) + " of " + Math.max(1, total / 1048576L) + " MiB";
            else text = (written / 1048576L) + " MiB";
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(KuroUpdater572.PROGRESS_ID, progress(percent, text));
        } catch (RuntimeException ignored) {}
    }

    private Notification progress(int percent, String text) {
        Notification.Builder builder = new Notification.Builder(this, KuroUpdater572.CHANNEL)
                .setSmallIcon(getApplicationInfo().icon)
                .setColor(Color.rgb(174, 103, 230))
                .setContentTitle("Kuro update")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);
        if (percent < 0) builder.setProgress(0, 0, true);
        else builder.setProgress(100, Math.max(0, Math.min(100, percent)), false);
        return builder.build();
    }
}
