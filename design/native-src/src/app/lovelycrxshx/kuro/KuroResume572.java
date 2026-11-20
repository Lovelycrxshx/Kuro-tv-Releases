package app.lovelycrxshx.kuro;

import java.util.Locale;

/**
 * Exact-position resume and Restart decisions.
 *
 * Restart is offered only where the player proves it has a real position and can seek:
 * Kuro's native ExoPlayer surface, or Kuro's own stream page when exactly one video
 * element reports a finite position and duration. Third-party embeds keep the previous
 * last-episode behaviour, because Kuro cannot trust their timeline.
 */
final class KuroResume572 {
    static final long MIN_MS = 5000L;
    static final String WEB_OK = "ok", WEB_NONE = "none", WEB_UNRELIABLE = "unreliable", WEB_BLOCKED = "blocked";
    private KuroResume572() {}

    /** A saved position is resumable when it is real, in range and not effectively the end. */
    static boolean resumable(long position, long duration) {
        if (position < MIN_MS || position > KuroPlayback561.MAX_POSITION_MS) return false;
        if (duration > 0 && duration <= KuroPlayback561.MAX_POSITION_MS && position >= duration - 1000L) return false;
        return true;
    }

    /** Restart is only meaningful when resume is: there is something to go back from. */
    static boolean restartable(long position, long duration) {
        return resumable(position, duration);
    }

    /** "12:34" or "1:02:03"; used by the resume notice and the Restart chip. */
    static String timeLabel(long milliseconds) {
        if (milliseconds < 0) milliseconds = 0;
        long total = milliseconds / 1000L;
        long hours = total / 3600L, minutes = (total % 3600L) / 60L, seconds = total % 60L;
        if (hours > 0) return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    /**
     * JavaScript for Kuro's own stream page. It reports a small status token so the
     * caller never acts on an unknown or third-party timeline.
     */
    static final String WEB_RESTART_JS =
            "(function(){try{var v=document.querySelectorAll('video');" +
            "if(v.length!==1)return 'none';var m=v[0];" +
            "if(!m||!Number.isFinite(m.currentTime)||!Number.isFinite(m.duration)||m.duration<=0)return 'unreliable';" +
            "if(m.readyState<1)return 'unreliable';" +
            "try{m.currentTime=0;}catch(e){return 'blocked';}" +
            "return 'ok';}catch(e){return 'blocked';}})()";

    /** Strips the JSON string quoting that evaluateJavascript wraps around a string result. */
    static String webStatus(String raw) {
        if (raw == null) return WEB_NONE;
        String value = raw.trim();
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            value = value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        if (WEB_OK.equals(value) || WEB_NONE.equals(value) || WEB_UNRELIABLE.equals(value) || WEB_BLOCKED.equals(value)) return value;
        return WEB_NONE;
    }

    /** True only for the token that proves the seek was applied. */
    static boolean webRestartApplied(String raw) {
        return WEB_OK.equals(webStatus(raw));
    }

    /** User-facing explanation for a status token; never claims a restart that did not happen. */
    static String webRestartMessage(String raw) {
        String status = webStatus(raw);
        if (WEB_OK.equals(status)) return "Restarted this episode from the beginning.";
        if (WEB_UNRELIABLE.equals(status)) return "This player has not reported a usable position yet, so Kuro kept the last-episode behaviour.";
        if (WEB_BLOCKED.equals(status)) return "This player would not allow seeking, so Kuro kept the last-episode behaviour.";
        return "This page is not Kuro’s own player, so Restart is unavailable here.";
    }
}
