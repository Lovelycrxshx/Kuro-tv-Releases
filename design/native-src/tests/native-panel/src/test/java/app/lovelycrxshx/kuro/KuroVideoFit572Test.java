package app.lovelycrxshx.kuro;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import org.robolectric.Robolectric;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;

/** Fit/crop labels and the APK preflight decisions; no installer is invoked. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class KuroVideoFit572Test {
    @Test public void fitValuesAreNormalised() {
        assertEquals(KuroVideoFit572.FIT, KuroVideoFit572.normalize(null));
        assertEquals(KuroVideoFit572.FIT, KuroVideoFit572.normalize(""));
        assertEquals(KuroVideoFit572.FIT, KuroVideoFit572.normalize("stretch"));
        assertEquals(KuroVideoFit572.CROP, KuroVideoFit572.normalize("crop"));
        assertEquals(KuroVideoFit572.CROP, KuroVideoFit572.next("fit"));
        assertEquals(KuroVideoFit572.FIT, KuroVideoFit572.next("crop"));
        assertEquals("Crop", KuroVideoFit572.actionLabel("fit"));
        assertEquals("Fit", KuroVideoFit572.actionLabel("crop"));
    }

    @Test public void descriptionNeverClaimsFullscreenChanges() {
        for (String mode : new String[]{"fit", "crop", null}) {
            String text = KuroVideoFit572.description(mode).toLowerCase();
            assertFalse(text.contains("fullscreen") && text.contains("change"));
        }
        assertTrue(KuroVideoFit572.description("crop").contains("cropped"));
    }

    private static Set<String> set(String... values) {
        Set<String> out = new TreeSet<>();
        Collections.addAll(out, values);
        return out;
    }

    private static final Set<String> KEY = set("1d37c3ca28ed80cc484601a7a7907d5c43970a5d57d51aa4e91475a60703e93e");

    @Test public void validUpdatePasses() {
        KuroApkPreflight572.Result result = KuroApkPreflight572.evaluate("app.lovelycrxshx.kuro", 72, "5.72", KEY, 71, KEY);
        assertEquals(KuroApkPreflight572.Status.OK, result.status);
        assertTrue(result.ok());
        assertTrue(result.message().contains("5.72"));
    }

    @Test public void otherAppsAreRejected() {
        KuroApkPreflight572.Result result = KuroApkPreflight572.evaluate("com.example.other", 99, "9.9", KEY, 71, KEY);
        assertEquals(KuroApkPreflight572.Status.WRONG_PACKAGE, result.status);
        assertFalse(result.ok());
    }

    @Test public void sameOrOlderVersionIsRejected() {
        assertEquals(KuroApkPreflight572.Status.NOT_NEWER, KuroApkPreflight572.evaluate("app.lovelycrxshx.kuro", 71, "5.71", KEY, 71, KEY).status);
        assertEquals(KuroApkPreflight572.Status.NOT_NEWER, KuroApkPreflight572.evaluate("app.lovelycrxshx.kuro", 70, "5.70", KEY, 71, KEY).status);
    }

    @Test public void aDifferentKeyIsRejected() {
        Set<String> other = set("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        KuroApkPreflight572.Result result = KuroApkPreflight572.evaluate("app.lovelycrxshx.kuro", 72, "5.72", other, 71, KEY);
        assertEquals(KuroApkPreflight572.Status.SIGNER_MISMATCH, result.status);
        assertTrue(result.message().toLowerCase().contains("different key"));
    }

    @Test public void missingOrUnknownSignersAreNotTrusted() {
        assertEquals(KuroApkPreflight572.Status.UNREADABLE,
                KuroApkPreflight572.evaluate("app.lovelycrxshx.kuro", 72, "5.72", set(), 71, KEY).status);
        assertEquals(KuroApkPreflight572.Status.UNREADABLE,
                KuroApkPreflight572.evaluate("app.lovelycrxshx.kuro", 72, "5.72", KEY, 71, set()).status);
        assertEquals(KuroApkPreflight572.Status.UNREADABLE,
                KuroApkPreflight572.evaluate("", 72, "5.72", KEY, 71, KEY).status);
    }

    @Test public void aMissingFileIsReportedNotInstalled() {
        Context context = Robolectric.buildActivity(android.app.Activity.class).create().get();
        assertEquals(KuroApkPreflight572.Status.MISSING, KuroApkPreflight572.inspect(context, null).status);
        java.io.File missing = new java.io.File(context.getCacheDir(), "Kuro-tv-5.72.apk");
        assertFalse(missing.exists());
        KuroApkPreflight572.Result result = KuroApkPreflight572.inspect(context, missing);
        assertEquals(KuroApkPreflight572.Status.MISSING, result.status);
        assertFalse(result.ok());
        // A file that exists but is not an APK must never reach the installer path.
        java.io.File junk = new java.io.File(context.getCacheDir(), "junk.apk");
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(junk);
            out.write(new byte[]{1, 2, 3, 4});
            out.close();
        } catch (java.io.IOException e) {
            fail("could not write the test file");
        }
        assertNotEquals(KuroApkPreflight572.Status.OK, KuroApkPreflight572.inspect(context, junk).status);
        junk.delete();
    }

    @Test public void digestHelperIsStable() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                KuroApkPreflight572.sha256(new byte[0]));
    }
}
