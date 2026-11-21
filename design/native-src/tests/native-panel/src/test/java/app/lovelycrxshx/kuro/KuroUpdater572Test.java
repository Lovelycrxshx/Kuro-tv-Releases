package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Release parsing, reminder spacing and download gating.
 * No network call, no installer and no real notification is sent.
 */
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class KuroUpdater572Test {
    private Context context;
    private SharedPreferences prefs;

    @Before public void setUp() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        context = activity.getApplicationContext();
        prefs = KuroUpdater572.prefs(context);
        prefs.edit().clear().commit();
    }

    private static String release(String tag, String assetName, long size, String url) {
        return "{\"tag_name\":\"" + tag + "\",\"draft\":false,\"prerelease\":false,\"assets\":[{\"name\":\"" + assetName
                + "\",\"size\":" + size + ",\"browser_download_url\":\"" + url + "\"}]}";
    }

    private static final String GOOD_URL = "https://github.com/ImAuraxSiva/Kuro-tv-Releases/releases/download/v5.72/Kuro-tv-5.72.apk";

    private static KuroUpdater572.Release releaseObject(String version) {
        return new KuroUpdater572.Release("v" + version, version, "Kuro-tv-" + version + ".apk", GOOD_URL, 54000000L, "");
    }

    @Test public void tagsAreReadOnlyFromKuroStyleNames() {
        assertEquals("5.72", KuroUpdater572.versionFromTag("v5.72"));
        assertEquals("5.72", KuroUpdater572.versionFromTag("V5.72"));
        assertEquals("5.72", KuroUpdater572.versionFromTag(" 5.72 "));
        assertEquals("5.72.1", KuroUpdater572.versionFromTag("v5.72.1"));
        for (String bad : new String[]{"", "v", "v5", "release-5.72", "v5.72-beta", "v5.72.1.2", "vv5.72", null}) {
            assertEquals("unexpected tag " + bad, "", KuroUpdater572.versionFromTag(bad));
        }
    }

    @Test public void onlyStrictlyHigherVersionsAreNewer() {
        assertTrue(KuroUpdater572.isNewer("5.72", "5.71"));
        assertFalse(KuroUpdater572.isNewer("5.72", "5.72"));
        assertFalse(KuroUpdater572.isNewer("5.71", "5.72"));
        assertTrue(KuroUpdater572.isNewer("5.100", "5.72"));
        assertTrue(KuroUpdater572.isNewer("6.0", "5.99"));
        assertFalse(KuroUpdater572.isNewer("5.72-beta", "5.71"));
        assertFalse(KuroUpdater572.isNewer(null, "5.71"));
    }

    @Test public void updateTrafficStaysOnHttpsGitHubHosts() {
        assertTrue(KuroUpdater572.safeHttps(GOOD_URL));
        assertTrue(KuroUpdater572.safeHttps("https://objects.githubusercontent.com/x/Kuro.apk"));
        assertTrue(KuroUpdater572.safeHttps("https://api.github.com/repos/ImAuraxSiva/Kuro-tv-Releases/releases/latest"));
        for (String bad : new String[]{"http://github.com/x.apk", "https://user:pass@github.com/x.apk",
                "https://github.com:8443/x.apk", "https://evil.example/x.apk", "https://github.com.evil.example/x.apk",
                "ftp://github.com/x.apk", "", null}) {
            assertFalse("unsafe address accepted: " + bad, KuroUpdater572.safeHttps(bad));
        }
    }

    @Test public void releaseParsingPicksTheMatchingApkOnly() {
        KuroUpdater572.Release parsed = KuroUpdater572.parseRelease(release("v5.72", "Kuro-tv-5.72.apk", 54000000L, GOOD_URL));
        assertNotNull(parsed);
        assertEquals("5.72", parsed.version);
        assertEquals("Kuro-tv-5.72.apk", parsed.assetName);
        assertEquals(54000000L, parsed.size);

        assertNull("drafts are ignored", KuroUpdater572.parseRelease(
                "{\"tag_name\":\"v5.72\",\"draft\":true,\"assets\":[{\"name\":\"Kuro-tv-5.72.apk\",\"size\":10,\"browser_download_url\":\"" + GOOD_URL + "\"}]}"));
        assertNull("prereleases are ignored", KuroUpdater572.parseRelease(
                "{\"tag_name\":\"v5.72\",\"prerelease\":true,\"assets\":[{\"name\":\"Kuro-tv-5.72.apk\",\"size\":10,\"browser_download_url\":\"" + GOOD_URL + "\"}]}"));
        assertNull("no apk asset", KuroUpdater572.parseRelease(release("v5.72", "Kuro-tv-5.72.zip", 10L, GOOD_URL)));
        assertNull("oversized asset", KuroUpdater572.parseRelease(release("v5.72", "Kuro-tv-5.72.apk", KuroUpdater572.MAX_APK_BYTES + 1, GOOD_URL)));
        assertNull("plain http asset", KuroUpdater572.parseRelease(release("v5.72", "Kuro-tv-5.72.apk", 10L, "http://github.com/a.apk")));
        assertNull("unreadable json", KuroUpdater572.parseRelease("not json"));
        assertNull("null body", KuroUpdater572.parseRelease(null));
    }

    @Test public void publishedChecksumIsUsedWhenPresent() {
        String hex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String json = "{\"tag_name\":\"v5.72\",\"assets\":[{\"name\":\"Kuro-tv-5.72.apk\",\"size\":10,"
                + "\"digest\":\"sha256:" + hex + "\",\"browser_download_url\":\"" + GOOD_URL + "\"}]}";
        KuroUpdater572.Release parsed = KuroUpdater572.parseRelease(json);
        assertNotNull(parsed);
        assertEquals(hex, parsed.digest);
        // A malformed digest is ignored rather than treated as a match.
        KuroUpdater572.Release bad = KuroUpdater572.parseRelease(
                "{\"tag_name\":\"v5.72\",\"assets\":[{\"name\":\"Kuro-tv-5.72.apk\",\"size\":10,\"digest\":\"sha256:xyz\",\"browser_download_url\":\"" + GOOD_URL + "\"}]}");
        assertNotNull(bad);
        assertEquals("", bad.digest);
    }

    @Test public void threeRemindersAboutADayApartThenTheDownload() {
        long day = KuroUpdater572.DAY_MS;
        long now = 1_000_000_000_000L;
        KuroUpdater572.Release release = releaseObject("5.72");

        KuroUpdater572.Step first = KuroUpdater572.advance(prefs, now, release, "5.71", true);
        assertEquals(1, first.notify);
        assertFalse("first notice must not download", first.download);

        KuroUpdater572.Step early = KuroUpdater572.advance(prefs, now + 60_000L, release, "5.71", true);
        assertEquals("a second notice must not bunch up", 0, early.notify);
        assertFalse(early.download);

        KuroUpdater572.Step second = KuroUpdater572.advance(prefs, now + day, release, "5.71", true);
        assertEquals(2, second.notify);
        assertFalse(second.download);

        KuroUpdater572.Step third = KuroUpdater572.advance(prefs, now + 2 * day, release, "5.71", true);
        assertEquals(3, third.notify);
        assertTrue("the third notice is followed by the automatic download", third.download);

        KuroUpdater572.Step after = KuroUpdater572.advance(prefs, now + 3 * day, release, "5.71", true);
        assertEquals("there is never a fourth reminder", 0, after.notify);
        assertEquals(3, prefs.getInt("notifications", 0));
    }

    @Test public void blockedNotificationsDoNotConsumeReminders() {
        long now = 1_000_000_000_000L;
        KuroUpdater572.Step step = KuroUpdater572.advance(prefs, now, releaseObject("5.72"), "5.71", false);
        assertEquals(0, step.notify);
        assertFalse(step.download);
        assertEquals("notifications_blocked", step.reason);
        assertEquals("nothing may be prepared silently", 0, prefs.getInt("notifications", 0));
    }

    @Test public void aNewReleaseRestartsTheSequenceOnce() {
        long now = 1_000_000_000_000L;
        KuroUpdater572.advance(prefs, now, releaseObject("5.72"), "5.71", true);
        KuroUpdater572.advance(prefs, now + KuroUpdater572.DAY_MS, releaseObject("5.72"), "5.71", true);
        assertEquals(2, prefs.getInt("notifications", 0));
        KuroUpdater572.Step next = KuroUpdater572.advance(prefs, now + 2 * KuroUpdater572.DAY_MS, releaseObject("5.73"), "5.71", true);
        assertEquals("a different release starts again at the first notice", 1, next.notify);
        assertEquals("5.73", prefs.getString("version", ""));
    }

    @Test public void anInstalledVersionResetsEverything() {
        long now = 1_000_000_000_000L;
        KuroUpdater572.advance(prefs, now, releaseObject("5.72"), "5.71", true);
        KuroUpdater572.Step done = KuroUpdater572.advance(prefs, now + KuroUpdater572.DAY_MS, releaseObject("5.72"), "5.72", true);
        assertEquals(0, done.notify);
        assertFalse(done.download);
        assertEquals("up_to_date", done.reason);
        assertEquals(0, prefs.getInt("notifications", 0));
    }

    @Test public void failedDownloadsStopAfterThreeAttempts() {
        long now = 1_000_000_000_000L;
        KuroUpdater572.Release release = releaseObject("5.72");
        prefs.edit().putString("tag", "v5.72").putString("version", "5.72").putInt("notifications", 3).commit();
        prefs.edit().putInt("downloadFailures", KuroUpdater572.MAX_DOWNLOAD_FAILURES).commit();
        KuroUpdater572.Step step = KuroUpdater572.advance(prefs, now, release, "5.71", true);
        assertFalse(step.download);
        assertEquals("download_failed", step.reason);
    }

    @Test public void aReadyFileIsNotDownloadedTwice() {
        long now = 1_000_000_000_000L;
        prefs.edit().putString("tag", "v5.72").putString("version", "5.72").putInt("notifications", 3)
                .putString("stage", "ready").commit();
        KuroUpdater572.Step step = KuroUpdater572.advance(prefs, now, releaseObject("5.72"), "5.71", true);
        assertFalse(step.download);
        assertEquals("ready", step.reason);
    }

    @Test public void updateFilesStayInsideKurosPrivateFolder() {
        File valid = KuroUpdater572.apkFile(context, "5.72");
        assertNotNull(valid);
        assertTrue(valid.getAbsolutePath().contains("/updates/"));
        assertEquals("Kuro-tv-5.72.apk", valid.getName());
        for (String bad : new String[]{"../5.72", "5.72/../x", "", "v5.72", "5.72-beta", null}) {
            assertNull("unsafe version accepted: " + bad, KuroUpdater572.apkFile(context, bad));
        }
        assertNull("nothing is ready yet", KuroUpdater572.readyFile(context));
    }

    @Test public void noticeWordsPromiseNothingKuroCannotDo() {
        KuroUpdater572.Release release = releaseObject("5.72");
        StringBuilder copy = new StringBuilder();
        for (int n = 1; n <= 3; n++) copy.append(KuroUpdater572.reminderTitle(n, release)).append(' ').append(KuroUpdater572.reminderText(n, release)).append(' ');
        String text = copy.toString().toLowerCase();
        assertFalse("must not promise a silent install", text.contains("install automatically"));
        assertFalse("must not open GitHub", text.contains("github.com"));
        assertFalse("no snooze", text.contains("remind me"));
        assertTrue(text.contains("android may delay"));
        assertEquals("Kuro 5.72 · final notice", KuroUpdater572.reminderTitle(3, release));
    }

    @Test public void statusIsReadableAndHonest() {
        JSONObject status = KuroUpdater572.status(context);
        assertTrue(status.optBoolean("enabled", false));
        assertEquals(0, status.optInt("notifications", 0));
        assertFalse(status.optBoolean("fileReady", true));
        String described = KuroUpdater572.describe(context).toLowerCase();
        assertTrue(described.contains("cannot install silently"));
        assertTrue(described.contains("can be delayed"));
        assertFalse(described.contains("/data/"));
    }

    @Test public void schedulingIsSafeWithoutAJobScheduler() {
        // Robolectric supplies a JobScheduler; calling twice must not duplicate the job.
        KuroUpdater572.schedule(context);
        KuroUpdater572.schedule(context);
        KuroUpdater572.cancelSchedule(context);
        KuroUpdater572.setEnabled(context, false);
        assertFalse(KuroUpdater572.enabled(context));
        KuroUpdater572.setEnabled(context, true);
        assertTrue(KuroUpdater572.enabled(context));
    }

    @Test public void channelExistsForTheReadyHandoff() {
        KuroUpdater572.ensureChannel(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assertNotNull(manager.getNotificationChannel(KuroUpdater572.CHANNEL));
    }
    @Test public void configurableReminderSpacingStillRequiresThreeNotices() {
        long now=1000000L;KuroUpdater572.Release release=releaseObject("5.72");
        for(int days=1;days<=3;days++){
            prefs.edit().clear().commit();KuroUpdater572.setReminderDays(context,days);assertEquals(days,KuroUpdater572.reminderDays(prefs));long gap=KuroUpdater572.reminderGap(prefs);
            assertEquals(1,KuroUpdater572.advance(prefs,now,release,"5.71",true).notify);
            assertEquals(0,KuroUpdater572.advance(prefs,now+gap-1,release,"5.71",true).notify);
            assertEquals(2,KuroUpdater572.advance(prefs,now+gap,release,"5.71",true).notify);
            KuroUpdater572.Step third=KuroUpdater572.advance(prefs,now+2*gap,release,"5.71",true);assertEquals(3,third.notify);assertTrue(third.download);
        }
        try {KuroUpdater572.setReminderDays(context,4);fail("invalid spacing");} catch(IllegalArgumentException expected){}
    }

    @Test public void wifiPreferenceAndWaitingDoNotConsumeDownloadFailures(){
        assertFalse(KuroUpdater572.wifiOnly(context));KuroUpdater572.setWifiOnly(context,true);assertTrue(KuroUpdater572.wifiOnly(context));prefs.edit().putInt("downloadFailures",1).putLong("lastDownloadAt",100).commit();KuroUpdater572.waitForWifi(context);assertEquals("waiting_wifi",prefs.getString("stage",""));assertEquals(1,prefs.getInt("downloadFailures",0));assertEquals(0,prefs.getLong("lastDownloadAt",-1));assertTrue(KuroUpdater572.describe(context).contains("Waiting for Wi-Fi"));KuroUpdater572.setWifiOnly(context,false);assertFalse(KuroUpdater572.wifiOnly(context));
    }

    @Test public void patchRelease151IsNewerThan15AndUpToDateClearsStale100Percent() {
        assertEquals("1.5.1", KuroUpdater572.versionFromTag("v1.5.1"));
        assertTrue(KuroUpdater572.isNewer("1.5.1", "1.5"));
        assertFalse(KuroUpdater572.isNewer("1.5.1", "1.5.1"));

        prefs.edit()
                .putLong("dlBytes", 48706154L)
                .putLong("dlTotal", 48706154L)
                .putLong("assetSize", 48706154L)
                .putLong("dlSpeed", 500000L)
                .putString("latestSeen", "1.5.1")
                .commit();

        KuroUpdater572.Step step = KuroUpdater572.advance(prefs, 1_000_000L, releaseObject("1.5.1"), "1.5.1", true);
        assertEquals("up_to_date", step.reason);
        assertEquals(0L, prefs.getLong("dlBytes", -1));
        assertEquals(0L, prefs.getLong("dlTotal", -1));
        assertEquals(0L, prefs.getLong("assetSize", -1));

        KuroUpdateCenter582.Snapshot upToDate = new KuroUpdateCenter582.Snapshot(
                "", "", "", 0L, 0L, 0L, 0L, 0L, "1.5.1", "1.5.1");
        assertTrue(upToDate.upToDate());
        assertEquals(-1, upToDate.percent);
        assertEquals("UP TO DATE", KuroUpdateDialog582.chip(upToDate));
        assertEquals("up_to_date", KuroUpdateDialog582.key(upToDate));
        assertEquals("Kuro 1.5.1 is installed — no newer release was found.", KuroUpdateCenter582.detail(upToDate));

        KuroUpdateCenter582.Snapshot neverChecked = new KuroUpdateCenter582.Snapshot(
                "", "", "", 0L, 0L, 0L, 0L, 0L, "1.5.1", "");
        assertFalse(neverChecked.upToDate());
        assertEquals("IDLE", KuroUpdateDialog582.chip(neverChecked));
    }

}
