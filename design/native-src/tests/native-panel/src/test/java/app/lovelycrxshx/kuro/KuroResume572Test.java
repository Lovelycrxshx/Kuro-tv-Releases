package app.lovelycrxshx.kuro;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.app.Activity;

import static org.junit.Assert.*;

/** Resume and Restart decisions. No real player, phone or stream is used. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class KuroResume572Test {
    @Test public void resumeNeedsARealInRangePosition() {
        assertFalse(KuroResume572.resumable(0, 0));
        assertFalse(KuroResume572.resumable(4999, 1440000));
        assertTrue(KuroResume572.resumable(5000, 1440000));
        assertTrue(KuroResume572.resumable(600000, 1440000));
        assertTrue("an unknown or zero duration does not block resume", KuroResume572.resumable(600000, -1));
        assertTrue("a zero duration counts as unknown, not as the end", KuroResume572.resumable(600000, 0));
        assertFalse("the last second is not a resume point", KuroResume572.resumable(1439500, 1440000));
        assertFalse(KuroResume572.resumable(-1, 1440000));
        assertFalse(KuroResume572.resumable(KuroPlayback561.MAX_POSITION_MS + 1, 0));
    }

    @Test public void restartIsOnlyOfferedWhereResumeIs() {
        assertFalse(KuroResume572.restartable(0, 1440000));
        assertTrue(KuroResume572.restartable(90000, 1440000));
        assertFalse(KuroResume572.restartable(1439900, 1440000));
    }

    @Test public void timeLabelsAreExact() {
        assertEquals("0:05", KuroResume572.timeLabel(5000));
        assertEquals("12:34", KuroResume572.timeLabel(754000));
        assertEquals("1:02:03", KuroResume572.timeLabel(3723000));
        assertEquals("0:00", KuroResume572.timeLabel(-5));
    }

    @Test public void onlyTheKuroStreamPageCanProveSeeking() {
        assertEquals(KuroResume572.WEB_OK, KuroResume572.webStatus("\"ok\""));
        assertTrue(KuroResume572.webRestartApplied("\"ok\""));
        for (String raw : new String[]{"\"none\"", "\"unreliable\"", "\"blocked\"", "null", "", null, "\"something\""}) {
            assertFalse("unexpected status " + raw, KuroResume572.webRestartApplied(raw));
        }
        assertEquals(KuroResume572.WEB_NONE, KuroResume572.webStatus("null"));
        assertTrue(KuroResume572.webRestartMessage(null).contains("not Kuro’s own player"));
        assertTrue(KuroResume572.webRestartMessage("\"unreliable\"").contains("last-episode"));
        assertTrue(KuroResume572.webRestartMessage("\"blocked\"").contains("last-episode"));
        assertTrue(KuroResume572.webRestartMessage("\"ok\"").contains("Restarted"));
        // The script must refuse anything that is not a single, seekable video.
        assertTrue(KuroResume572.WEB_RESTART_JS.contains("v.length!==1"));
        assertTrue(KuroResume572.WEB_RESTART_JS.contains("Number.isFinite(m.duration)"));
        assertTrue(KuroResume572.WEB_RESTART_JS.contains("m.currentTime=0"));
    }

    @Test public void restartClearsTheSavedCheckpointOnlyAfterSeeking() {
        Activity a = Robolectric.buildActivity(Activity.class).create().start().resume().get();
        String key = KuroPlayback561.key("zenkai", "show-1", "3");
        KuroPlayback561.save(a, key, 900000, 1440000);
        assertEquals(900000, KuroPlayback561.read(a, key));
        KuroPlayback561.completed(a, key);
        assertEquals(0, KuroPlayback561.read(a, key));
    }
}
