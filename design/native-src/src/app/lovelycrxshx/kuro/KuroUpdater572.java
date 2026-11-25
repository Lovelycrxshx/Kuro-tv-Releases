package app.lovelycrxshx.kuro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/**
 * Reads the public GitHub release metadata for Kuro and prepares updates on this phone.
 *
 * Behaviour the owner asked for, and its limits:
 *  - Three notifications, about one day apart, once a newer release appears.
 *  - After the third notification Kuro downloads the APK itself over Wi-Fi or mobile
 *    data. There is no download prompt, no GitHub page, no "remind me later" button and
 *    no fourth reminder.
 *  - The downloaded file is checked for package, newer version code and identical
 *    signing certificate before Android's installer is offered it.
 *  - Android decides when background work actually runs, may delay notifications or
 *    downloads, and always shows its own install confirmation. Kuro cannot install
 *    silently and does not claim to.
 */
public final class KuroUpdater572 {
    static final String PREFS = "kuro-updater572";
    static final String CHANNEL = "kuro_updates572";
    static final String ACTION_INSTALL = "app.lovelycrxshx.kuro.INSTALL_UPDATE572";
    static final int JOB_ID = 57201, REMINDER_ID = 57202, PROGRESS_ID = 57203, READY_ID = 57204;

    /** Canonical repository. It was renamed from ImAuraxSiva/, which now 301-redirects. */
    static final String REPO = "Lovelycrxshx/Kuro-tv-Releases";
    static final String LEGACY = "https://api.github.com/repos/ImAuraxSiva/Kuro-tv-Releases/releases/latest";
    static final String LATEST = "https://api.github.com/repos/" + REPO + "/releases/latest";
    static final String RELEASES = "https://api.github.com/repos/" + REPO + "/releases?per_page=5";

    static final long DAY_MS = 24L * 60L * 60L * 1000L;
    /** "About one day apart": tolerate an early job run without bunching reminders. */
    static final long MIN_GAP_MS = 20L * 60L * 60L * 1000L;
    static final long PERIOD_MS = DAY_MS, FLEX_MS = 60L * 60L * 1000L;
    static final int MAX_NOTIFICATIONS = 3;
    static final int MAX_DOWNLOAD_FAILURES = 3;
    static final long MAX_APK_BYTES = 250L * 1024L * 1024L;

    private KuroUpdater572() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c) {
        return prefs(c).getBoolean("enabled", true);
    }

    static void setEnabled(Context c, boolean on) {
        prefs(c).edit().putBoolean("enabled", on).apply();
        if (on) schedule(c);
        else cancelSchedule(c);
        if (!on) cancelNotifications(c);
    }

    // Reminder spacing is local to this device. The default keeps the 5.72 cadence.
    static int reminderDays(SharedPreferences p) {int n=p.getInt("reminderDays573",1);return n>=1&&n<=3?n:1;}
    static void setReminderDays(Context c,int days) {if(days<1||days>3)throw new IllegalArgumentException("Invalid reminder interval");prefs(c).edit().putInt("reminderDays573",days).apply();}
    static long reminderGap(SharedPreferences p) {return MIN_GAP_MS+(reminderDays(p)-1)*DAY_MS;}

    static boolean wifiOnly(Context c){return prefs(c).getBoolean("wifiOnly573",false);}
    static void setWifiOnly(Context c,boolean value){prefs(c).edit().putBoolean("wifiOnly573",value).apply();}
    static android.net.Network activeWifi(Context c){
        try{android.net.ConnectivityManager m=(android.net.ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);if(m==null)return null;android.net.Network n=m.getActiveNetwork();android.net.NetworkCapabilities caps=n==null?null:m.getNetworkCapabilities(n);return caps!=null&&caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)&&caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)?n:null;}catch(RuntimeException e){return null;}
    }
    static void waitForWifi(Context c){prefs(c).edit().putString("stage","waiting_wifi").putLong("lastDownloadAt",0).putString("lastError","").apply();}

    // ---------------------------------------------------------------- release model

    static final class Release {
        final String tag, version, assetName, assetUrl, digest;
        final long size;
        /** GitHub's publish time in epoch millis, or 0 when the reply did not carry one. */
        final long publishedAt;

        Release(String tag, String version, String assetName, String assetUrl, long size, String digest) {
            this(tag, version, assetName, assetUrl, size, digest, 0L);
        }

        Release(String tag, String version, String assetName, String assetUrl, long size, String digest, long publishedAt) {
            this.tag = tag;
            this.version = version;
            this.assetName = assetName;
            this.assetUrl = assetUrl;
            this.size = size;
            this.digest = digest == null ? "" : digest;
            this.publishedAt = publishedAt;
        }
    }

    static final class Step {
        final int notify;
        final boolean download;
        final String reason;

        Step(int notify, boolean download, String reason) {
            this.notify = notify;
            this.download = download;
            this.reason = reason;
        }

        static Step none(String reason) {
            return new Step(0, false, reason);
        }
    }

    /** Accepts v5.72, V5.72 or 5.72 and returns 5.72; empty when it is not a Kuro tag. */
    static String versionFromTag(String tag) {
        if (tag == null) return "";
        String value = tag.trim();
        if (value.isEmpty() || value.length() > 40) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,3}\\.\\d{1,3}(?:\\.\\d{1,3})?)").matcher(value);
        return m.find() ? m.group(1) : "";
    }

    static int[] numbers(String version) {
        if (version == null) return new int[]{0};
        String[] parts = version.split("\\.");
        int[] out = new int[Math.max(1, parts.length)];
        for (int i = 0; i < parts.length && i < out.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    /** True when candidate is a strictly higher dotted version than installed. */
    static boolean isNewer(String candidate, String installed) {
        if (candidate == null || installed == null || !versionFromTag(candidate).equals(candidate)) return false;
        int[] a = numbers(candidate), b = numbers(installed);
        // The 5.x line ended at 5.83 and the 1.x line carries on from it. Version numbers
        // cannot say that on their own, so it is stated once, here: 1.x follows 5.x. Without
        // this rule every install on 5.x would read 1.0 as a downgrade and refuse it forever.
        // It only ever applies to a device still on 5.x, so 1.0 -> 1.1 compares normally.
        if (a[0] == 1 && b[0] >= 5) return true;
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int left = i < a.length ? a[i] : 0, right = i < b.length ? b[i] : 0;
            if (left != right) return left > right;
        }
        return false;
    }

    /**
     * Parses the release JSON and picks an APK asset.
     *
     * The name alone is not trusted: only an HTTPS GitHub-hosted .apk of a sane size is
     * accepted here, and the package, version code and signer are checked after download.
     */
    static Release parseRelease(String raw) {
        if (raw == null || raw.length() > 400_000) return null;
        try {
            JSONObject release = new JSONObject(raw);
            if (release.optBoolean("draft", false) || release.optBoolean("prerelease", false)) return null;
            String tag = release.optString("tag_name", "");
            String version = versionFromTag(tag);
            if (version.isEmpty()) return null;
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) return null;
            JSONObject chosen = null;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null) continue;
                String name = asset.optString("name", "");
                if (!name.toLowerCase(Locale.ROOT).endsWith(".apk")) continue;
                long size = asset.optLong("size", -1);
                if (size <= 0 || size > MAX_APK_BYTES) continue;
                String url = asset.optString("browser_download_url", "");
                if (!safeHttps(url)) continue;
                if (name.toLowerCase(Locale.ROOT).contains(version)) {
                    chosen = asset;
                    break;
                }
                if (chosen == null) chosen = asset;
            }
            if (chosen == null) return null;
            String published = release.optString("published_at", "");
            if (published.isEmpty()) published = release.optString("created_at", "");
            return new Release(tag, version, chosen.optString("name", ""), chosen.optString("browser_download_url", ""),
                    chosen.optLong("size", -1), digestOf(chosen), parseTime(published));
        } catch (Exception e) {
            return null;
        }
    }

    /** Fallback when /releases/latest is refused: pick the highest usable version from the list. */
    static Release parseReleasesList(String raw) {
        if (raw == null || raw.length() > 800_000) return null;
        try {
            JSONArray list = new JSONArray(raw);
            Release best = null;
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                if (item == null) continue;
                Release candidate = parseRelease(item.toString());
                if (candidate == null) continue;
                if (best == null || isNewer(candidate.version, best.version)) best = candidate;
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses GitHub's "2026-11-20T13:03:53Z" into epoch millis. Returns 0 on anything
     * unexpected, which 5.83 treats as "no publish time known" rather than a date in 1970.
     */
    static long parseTime(String iso) {
        if (iso == null) return 0L;
        String value = iso.trim();
        if (value.length() < 19) return 0L;
        try {
            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            format.setLenient(false);
            java.util.Date date = format.parse(value.substring(0, 19));
            return date == null ? 0L : date.getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String digestOf(JSONObject asset) {
        Object raw = asset.opt("digest");
        String value = raw instanceof String ? (String) raw : "";
        if (value == null) value = "";
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("sha256:")) {
            String hex = value.substring(7);
            if (hex.matches("[0-9a-f]{64}")) return hex;
        }
        return "";
    }

    // ------------------------------------------------------------------ web safety

    static boolean safeHttps(String address) {
        if (address == null || address.isEmpty() || address.length() > 2048) return false;
        try {
            URI uri = new URI(address);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || uri.getFragment() != null) return false;
            if (uri.getPort() != -1 && uri.getPort() != 443) return false;
            return allowedHost(uri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    static boolean allowedHost(String host) {
        if (host == null) return false;
        String value = host.toLowerCase(Locale.ROOT);
        return "github.com".equals(value)
                || "api.github.com".equals(value)
                || "objects.githubusercontent.com".equals(value)
                || "github-releases.githubusercontent.com".equals(value)
                || "release-assets.githubusercontent.com".equals(value);
    }

    /** Bounded GET with manual, host-checked redirects. Used for metadata and the APK. */
    static String getText(String address, int maxBytes) throws IOException {
        String current = address;
        for (int hop = 0; hop <= 3; hop++) {
            if (!safeHttps(current)) throw new IOException("Unsupported update address");
            HttpURLConnection connection = (HttpURLConnection) new URL(current).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "Kuro.tv-Android-5.74");
            try {
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Update redirect unavailable");
                    current = new URL(new URL(current), location).toString();
                    continue;
                }
                if (code != 200) throw new IOException("Update server returned HTTP " + code);
                try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (out.size() + read > maxBytes) throw new IOException("Update response too large");
                        out.write(buffer, 0, read);
                    }
                    return new String(out.toByteArray(), "UTF-8");
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many update redirects");
    }

    // ---------------------------------------------------------------- state machine

    /**
     * Advances reminders and decides whether a download should start.
     * Pure preference logic so it can be tested without a network or a device.
     */
    static Step advance(SharedPreferences p, long now, Release release, String installedVersion, boolean canNotify) {
        if (release == null) return Step.none("no_release");
        if (!isNewer(release.version, installedVersion)) {
            if (p.getInt("notifications", 0) != 0 || !p.getString("stage", "").isEmpty()
                    || !p.getString("tag", "").isEmpty()
                    || p.getLong("dlBytes", 0) != 0 || p.getLong("dlTotal", 0) != 0
                    || p.getLong("assetSize", 0) != 0) {
                p.edit().putInt("notifications", 0).putLong("lastNotifyAt", 0).putInt("downloadFailures", 0)
                        .putString("stage", "").putString("tag", "").putString("version", "")
                        .putLong("dlBytes", 0).putLong("dlTotal", 0).putLong("dlSpeed", 0)
                        .putLong("assetSize", 0)
                        .putString("lastError", "").putLong("lastDownloadAt", 0).apply();
            }
            return Step.none("up_to_date");
        }
        if (!release.tag.equals(p.getString("tag", ""))) {
            // A different release restarts the sequence exactly once.
            p.edit().putString("tag", release.tag).putString("version", release.version)
                    .putString("assetName", release.assetName).putString("assetUrl", release.assetUrl)
                    .putLong("assetSize", release.size).putString("assetDigest", release.digest)
                    .putInt("notifications", 0).putLong("lastNotifyAt", 0).putInt("downloadFailures", 0)
                    .putLong("lastDownloadAt", 0).putString("stage", "").putString("lastError", "").apply();
        } else {
            p.edit().putString("assetName", release.assetName).putString("assetUrl", release.assetUrl)
                    .putLong("assetSize", release.size).putString("assetDigest", release.digest)
                    .putString("version", release.version).apply();
        }
        int sent = p.getInt("notifications", 0);
        if (sent < MAX_NOTIFICATIONS) {
            if (!canNotify) return Step.none("notifications_blocked");
            long last = p.getLong("lastNotifyAt", 0);
            if (last != 0 && now - last < reminderGap(p)) return Step.none("waiting");
            int next = sent + 1;
            p.edit().putInt("notifications", next).putLong("lastNotifyAt", now).apply();
            // The third reminder is the last one; the download follows it automatically.
            return new Step(next, next >= MAX_NOTIFICATIONS, "reminder");
        }
        if ("ready".equals(p.getString("stage", ""))) return Step.none("ready");
        if (p.getInt("downloadFailures", 0) >= MAX_DOWNLOAD_FAILURES) return Step.none("download_failed");
        long lastDownload = p.getLong("lastDownloadAt", 0);
        if (lastDownload != 0 && now - lastDownload < MIN_GAP_MS) return Step.none("waiting");
        return new Step(0, true, "download");
    }

    /**
     * Fetches the current release from the three known endpoints, newest-first.
     * Records lastError and the source it used, and returns null when nothing usable
     * came back. Shared by the scheduled pass and by 5.82's in-app update screen.
     */
    static Release fetch(Context context) {
        Context c = context.getApplicationContext();
        SharedPreferences p = prefs(c);
        Release release = null;
        String source = "";
        String problem = "";
        String[] urls = {LATEST, LEGACY, RELEASES};
        for (int i = 0; i < urls.length && release == null; i++) {
            try {
                Release candidate = urls[i].equals(RELEASES)
                        ? parseReleasesList(getText(urls[i], 800_000))
                        : parseRelease(getText(urls[i], 400_000));
                if (candidate != null) { release = candidate; source = urls[i]; }
                else problem = "The release reply held no usable Kuro APK.";
            } catch (Exception e) {
                problem = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
        }
        if (release == null) {
            p.edit().putString("lastError", bounded(problem)).apply();
            return null;
        }
        p.edit().putString("lastSource", source)
                .putString("latestSeen", release.version)
                .putString("latestTag", release.tag)
                .putString("lastError", "").apply();
        return release;
    }

    /** One scheduled pass: fetch metadata, then notify or prepare the download. */
    static synchronized void run(Context context) {
        Context c = context.getApplicationContext();
        if (!enabled(c)) return;
        SharedPreferences p = prefs(c);
        p.edit().putLong("lastCheckAt", System.currentTimeMillis()).apply();
        Release release = fetch(c);
        if (release == null) return;
        // 5.82: automatic mode goes straight from "found" to a download, with no reminder
        // cadence to sit through. 5.83 adds the release window: a release inside its 24/48
        // hours is scheduled instead, and one whose window has passed downloads at once.
        if (KuroUpdateCenter582.autoStep(c, release)) return;
        Step step = advance(p, System.currentTimeMillis(), release, installedVersionName(c), allowed(c));
        if (step.notify > 0) notifyReminder(c, step.notify, release);
        if ("ready".equals(step.reason)) notifyReady(c, false);
        if (step.download) requestDownload(c);
    }

    static void requestDownload(Context c) {
        if(wifiOnly(c)&&activeWifi(c)==null){waitForWifi(c);return;}
        try {
            Intent intent = new Intent(c, KuroUpdaterDownloadService.class);
            c.startForegroundService(intent);
        } catch (RuntimeException ignored) {
            // Android can refuse a background foreground-service start; the next
            // scheduled pass tries again. Nothing is downloaded silently elsewhere.
        }
    }

    private static String bounded(String value) {
        if (value == null) return "Update check failed.";
        String text = value.replace('\n', ' ').trim();
        return text.length() <= 200 ? text : text.substring(0, 200);
    }

    static String installedVersionName(Context c) {
        try {
            PackageInfo info = c.getPackageManager().getPackageInfo(KuroApkPreflight572.PACKAGE, 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return "";
        }
    }

    // -------------------------------------------------------------------- scheduling

    static void schedule(Context c) {
        try {
            JobScheduler scheduler = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler == null) return;
            if (!enabled(c)) {
                scheduler.cancel(JOB_ID);
                return;
            }
            if (scheduler.getPendingJob(JOB_ID) != null) return;
            JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(c, KuroUpdaterJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setPeriodic(PERIOD_MS, FLEX_MS)
                    .build();
            scheduler.schedule(info);
        } catch (RuntimeException ignored) {}
    }

    static void cancelSchedule(Context c) {
        try {
            JobScheduler scheduler = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler != null) scheduler.cancel(JOB_ID);
        } catch (RuntimeException ignored) {}
    }

    // ------------------------------------------------------------------- files/state

    static File updatesDir(Context c) {
        File dir = new File(c.getApplicationContext().getFilesDir(), "updates");
        if (dir.isDirectory()) return dir;
        return dir.mkdirs() ? dir : null;
    }

    static File apkFile(Context c, String version) {
        if (version == null || !version.matches("\\d{1,3}\\.\\d{1,3}(?:\\.\\d{1,3})?")) return null;
        File dir = updatesDir(c);
        return dir == null ? null : new File(dir, "Kuro-tv-" + version + ".apk");
    }

    /** The verified download waiting for Android's installer, or null. */
    static File readyFile(Context c) {
        SharedPreferences p = prefs(c);
        if (!"ready".equals(p.getString("stage", ""))) return null;
        File file = apkFile(c, p.getString("version", ""));
        return file != null && file.isFile() && file.length() > 0 ? file : null;
    }

    static void markDownloadFailed(Context c, String message) {
        SharedPreferences p = prefs(c);
        SharedPreferences.Editor editor = p.edit()
                .putInt("downloadFailures", p.getInt("downloadFailures", 0) + 1)
                .putLong("lastDownloadAt", System.currentTimeMillis())
                .putString("stage", "")
                .putString("lastError", bounded(message));
        editor.apply();
        File file = apkFile(c, p.getString("version", ""));
        if (file != null && file.isFile()) file.delete();
    }

    static void markReady(Context c, String version) {
        prefs(c).edit().putString("stage", "ready").putString("version", version)
                .putLong("downloadedAt", System.currentTimeMillis())
                .putInt("downloadFailures", 0).putString("lastError", "").apply();
    }

    /** Removes the pending update and records why, without scheduling further reminders. */
    static void discard(Context c, String message) {
        File file = readyFile(c);
        if (file != null) file.delete();
        prefs(c).edit().putString("stage", "").putString("lastError", bounded(message)).apply();
    }

    // ----------------------------------------------------------------- notifications

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Kuro updates", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("New Kuro releases, and when an update is ready to install");
        manager.createNotificationChannel(channel);
    }

    static boolean allowed(Context c) {
        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager manager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.areNotificationsEnabled()) return false;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        }
        return true;
    }

    private static PendingIntent openApp(Context c, String action, int request) {
        Intent intent = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (action != null) intent.setAction(action);
        return PendingIntent.getActivity(c, request, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static String reminderTitle(int number, Release release) {
        String version = release == null ? "" : release.version;
        if (number <= 1) return "Kuro " + version + " is available";
        if (number == 2) return "Kuro " + version + " · second notice";
        return "Kuro " + version + " · final notice";
    }

    static String reminderText(int number, Release release) {
        String version = release == null ? "" : release.version;
        if (number <= 1) return "A newer Kuro release is published. Kuro prepares it on this phone; Android may delay background work.";
        if (number == 2) return "Kuro " + version + " is still available. After the final notice Kuro downloads it automatically.";
        return "Kuro will prepare " + version + " in the app according to your network setting. Android may ask you to allow installs.";
    }

    static void notifyReminder(Context c, int number, Release release) {
        try {
            ensureChannel(c);
            if (!allowed(c)) return;
            NotificationManager manager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            int icon = c.getApplicationInfo().icon;
            Notification.Builder builder = new Notification.Builder(c, CHANNEL)
                    .setSmallIcon(icon)
                    .setColor(Color.rgb(174, 103, 230))
                    .setContentTitle(reminderTitle(number, release))
                    .setContentText(reminderText(number, release))
                    .setStyle(new Notification.BigTextStyle().bigText(reminderText(number, release)))
                    .setContentIntent(openApp(c, null, REMINDER_ID))
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(true);
            manager.notify(REMINDER_ID, builder.build());
        } catch (RuntimeException ignored) {}
    }

    /** The single "ready to install" notice. It is the handoff, not a reminder. */
    static void notifyReady(Context c, boolean alreadyPosted) {
        if (alreadyPosted) return;
        try {
            ensureChannel(c);
            if (!allowed(c)) return;
            NotificationManager manager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            String version = prefs(c).getString("version", "");
            Notification.Builder builder = new Notification.Builder(c, CHANNEL)
                    .setSmallIcon(c.getApplicationInfo().icon)
                    .setColor(Color.rgb(174, 103, 230))
                    .setContentTitle("Kuro " + version + " is ready to install")
                    .setContentText("Tap to install. Android shows its own confirmation.")
                    .setStyle(new Notification.BigTextStyle().bigText("The update was downloaded and checked on this phone. Tap to open Android's installer; Android always asks before installing."))
                    .setContentIntent(openApp(c, ACTION_INSTALL, READY_ID))
                    .setAutoCancel(true)
                    .setOngoing(false);
            manager.notify(READY_ID, builder.build());
        } catch (RuntimeException ignored) {}
    }

    static void cancelNotifications(Context c) {
        try {
            NotificationManager manager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            manager.cancel(REMINDER_ID);
            manager.cancel(READY_ID);
            manager.cancel(PROGRESS_ID);
        } catch (RuntimeException ignored) {}
    }

    // ----------------------------------------------------------------------- status

    static JSONObject status(Context c) {
        JSONObject out = new JSONObject();
        try {
            SharedPreferences p = prefs(c);
            File file = readyFile(c);
            out.put("enabled", enabled(c));
            out.put("reminderDays", reminderDays(p));
            out.put("wifiOnly",wifiOnly(c));
            out.put("allowed", allowed(c));
            out.put("installedVersion", installedVersionName(c));
            out.put("version", p.getString("version", ""));
            out.put("stage", p.getString("stage", ""));
            out.put("notifications", p.getInt("notifications", 0));
            out.put("lastCheckAt", p.getLong("lastCheckAt", 0));
            out.put("lastNotifyAt", p.getLong("lastNotifyAt", 0));
            out.put("lastError", p.getString("lastError", ""));
            out.put("lastSource", p.getString("lastSource", ""));
            out.put("latestSeen", p.getString("latestSeen", ""));
            out.put("latestTag", p.getString("latestTag", ""));
            out.put("downloadFailures", p.getInt("downloadFailures", 0));
            out.put("fileReady", file != null);
            out.put("fileBytes", file == null ? 0 : file.length());
        } catch (Exception ignored) {}
        return out;
    }

    /** Human-readable status; never includes a path, token or key. */
    static String describe(Context c) {
        JSONObject s = status(c);
        StringBuilder text = new StringBuilder();
        text.append("Installed: Kuro ").append(s.optString("installedVersion", "unknown")).append('\n');
        text.append("Automatic update checks: ").append(s.optBoolean("enabled", true) ? "on" : "off").append('\n');
        text.append("Update downloads: ").append(s.optBoolean("wifiOnly",false)?"Wi-Fi only":"Wi-Fi or mobile data").append(". Release checks may use any network.\n");
        text.append("Reminder spacing: about ").append(s.optInt("reminderDays",1)).append(" day(s), subject to Android scheduling.\n");
        if (!s.optBoolean("allowed", true)) text.append("Notifications are blocked for Kuro, so reminders cannot appear until they are allowed.\n");
        String stage = s.optString("stage", "");
        int sent = s.optInt("notifications", 0);
        if (stage.isEmpty() && sent == 0) text.append("No update has been prepared yet.\n");
        if (sent > 0) text.append("Reminders shown for ").append(s.optString("version", "")).append(": ").append(sent).append(" of 3.\n");
        if ("ready".equals(stage)) text.append("Kuro ").append(s.optString("version", "")).append(" is downloaded and checked (").append(Math.max(1, s.optLong("fileBytes", 0) / 1048576L)).append(" MiB).\n");
        if ("waiting_wifi".equals(stage)) text.append("Waiting for Wi-Fi before downloading the update.\n");
        if ("downloading".equals(stage)) text.append("Downloading Kuro ").append(s.optString("version", "")).append(" now.\n");
        String seen = s.optString("latestSeen", "");
        if (!seen.isEmpty()) text.append("GitHub offers: ").append(seen)
                .append("  (tag ").append(s.optString("latestTag", "")).append(")\n");
        String source = s.optString("lastSource", "");
        if (!source.isEmpty()) text.append("Release source: ").append(source).append('\n');
        String error = s.optString("lastError", "");
        if (!error.isEmpty()) text.append("Last check: ").append(error).append('\n');
        long checked = s.optLong("lastCheckAt", 0);
        if (checked > 0) text.append("Last check: ").append(android.text.format.DateFormat.format("d MMM yyyy, HH:mm", checked)).append('\n');
        text.append("\nAndroid decides when background work runs, so notices and downloads can be delayed. Android always asks before installing; Kuro cannot install silently.");
        return text.toString();
    }
}
