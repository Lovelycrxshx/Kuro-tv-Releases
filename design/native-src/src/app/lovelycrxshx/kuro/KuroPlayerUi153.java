package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.OptIn;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * Kuro 1.5.3 player UI layer:
 *  - Universal OLED Segmented Chapters & Pill-Chips theme bootstrap for WebView frames
 *  - Native ExoPlayer OLED pill styling (Skip Intro pill, speed/episodes/fullscreen pills, timebar tint)
 *  - Fullscreen bottom swipe-up gesture + "Up Next & Episodes" drawer inside fullscreen
 *  - App-UI floating Up-Next autoplay countdown overlay (preserving AlertDialog test hooks)
 */
@OptIn(markerClass = UnstableApi.class)
final class KuroPlayerUi153 {

    interface FullscreenActions {
        JSONObject watchInfo();
        boolean isEnhancedFullscreen();
        void chooseEpisode(int index);
        void nextEpisode();
        void toggleEnhancedFullscreen();
        long savedPositionMs(String episodeNumber);
    }

    static final class AutoNextUi {
        final AlertDialog dialog;
        private final TextView badge;
        private final View progressFill;
        private final View progressTrack;

        AutoNextUi(AlertDialog dialog, TextView badge, View progressFill, View progressTrack) {
            this.dialog = dialog;
            this.badge = badge;
            this.progressFill = progressFill;
            this.progressTrack = progressTrack;
        }

        void updateRemainingMs(long remainingMs, long totalMs) {
            long secs = Math.max(1L, (remainingMs + 999L) / 1000L);
            if (badge != null) badge.setText("in " + secs + "s");
            if (progressFill != null && progressTrack != null) {
                int w = progressTrack.getWidth();
                if (w > 0 && totalMs > 0) {
                    float frac = Math.max(0f, Math.min(1f, (float) remainingMs / (float) totalMs));
                    ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
                    int target = Math.max(1, Math.round(w * frac));
                    if (lp.width != target) {
                        lp.width = target;
                        progressFill.setLayoutParams(lp);
                    }
                }
            }
        }
    }

    private static View activeFullscreenSheet;
    private static final float[] SPEEDS = new float[]{1.0f, 1.25f, 1.5f, 2.0f, 0.75f};

    private KuroPlayerUi153() {}

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private static String hex6(int argb) {
        return String.format(Locale.ROOT, "#%06x", argb & 16777215);
    }

    static String skinBootstrapJs(JSONObject watchInfo, long attempt) {
        JSONObject theme = new JSONObject();
        try {
            theme.put("accent", hex6(KuroTheme12.accentColor()));
            theme.put("soft", hex6(KuroDialogs581.PURPLE_SOFT));
            theme.put("card", hex6(Color.rgb(10, 10, 14)));
            theme.put("border", hex6(KuroDialogs581.BORDER));
            theme.put("ink", hex6(KuroDialogs581.INK));
            theme.put("muted", hex6(KuroDialogs581.MUTED));
            theme.put("amoled", KuroTheme12.amoled());
            theme.put("attempt", attempt);
            boolean hasNext = false;
            String nextNum = "";
            if (watchInfo != null) {
                JSONArray eps = watchInfo.optJSONArray("episodes");
                int cur = watchInfo.optInt("current", -1);
                if (eps != null && cur >= 0 && cur + 1 < eps.length()) {
                    hasNext = true;
                    JSONObject n = eps.optJSONObject(cur + 1);
                    nextNum = n != null ? n.optString("number", "") : eps.optString(cur + 1, "");
                }
            }
            theme.put("hasNext", hasNext);
            theme.put("nextEp", nextNum);
        } catch (Exception ignored) {}
        return "window.__kuroSkinTheme153=" + theme.toString() + ";if(window.__kuroApplySkin153)window.__kuroApplySkin153(window.__kuroSkinTheme153);";
    }

    static GradientDrawable pillBackground(Context c, int fill, int strokeColor, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, 999));
        if (strokeDp > 0f) d.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return d;
    }

    static RippleDrawable pillDrawable(Context c, boolean primary) {
        int fill = primary ? Color.argb(238, 12, 10, 18) : Color.argb(226, 10, 10, 14);
        int border = primary ? KuroTheme12.accentColor() : KuroDialogs581.BORDER;
        float width = primary ? 1.5f : 1.0f;
        GradientDrawable normal = pillBackground(c, fill, border, width);
        GradientDrawable focused = pillBackground(c, Color.argb(245, 22, 16, 32), KuroDialogs581.PURPLE_SOFT, 2.0f);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{android.R.attr.state_pressed}, focused);
        states.addState(new int[]{}, normal);
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(64, 255, 255, 255)), states, null);
    }

    static TextView makePill(Context c, String label, boolean primary) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        t.setMinHeight(dp(c, 36));
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(pillDrawable(c, primary));
        return t;
    }

    /** Styles native ExoPlayer (`KuroNativePlayer561`) with OLED pill chips + accent timebar. */
    static void styleNativePlayer(
            Activity activity,
            FrameLayout surface,
            PlayerView view,
            TextView skip,
            ExoPlayer engine,
            Runnable onEpisodes,
            Runnable onFullscreen) {
        if (surface == null || view == null || skip == null || engine == null) return;
        skip.setBackground(pillDrawable(activity, true));
        skip.setTextColor(Color.WHITE);
        skip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        skip.setPadding(dp(activity, 16), dp(activity, 9), dp(activity, 16), dp(activity, 9));

        tintTimeBar(view);

        final LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setTag("native-oled-pills-153");

        final TextView speedPill = makePill(activity, "1.0x", false);
        speedPill.setContentDescription("Playback speed");
        speedPill.setOnClickListener(v -> {
            float cur = engine.getPlaybackParameters().speed;
            int idx = 0;
            for (int i = 0; i < SPEEDS.length; i++) {
                if (Math.abs(SPEEDS[i] - cur) < 0.04f) { idx = i; break; }
            }
            float next = SPEEDS[(idx + 1) % SPEEDS.length];
            engine.setPlaybackParameters(new PlaybackParameters(next));
            speedPill.setText((next == (int) next ? String.format(Locale.ROOT, "%.1f", next) : String.valueOf(next)) + "x");
        });
        bar.addView(speedPill);

        if (onEpisodes != null) {
            Space s1 = new Space(activity);
            bar.addView(s1, new LinearLayout.LayoutParams(dp(activity, 6), 1));
            TextView epPill = makePill(activity, "Episodes", false);
            epPill.setOnClickListener(v -> onEpisodes.run());
            bar.addView(epPill);
        }

        if (onFullscreen != null) {
            Space s2 = new Space(activity);
            bar.addView(s2, new LinearLayout.LayoutParams(dp(activity, 6), 1));
            TextView fsPill = makePill(activity, "Fullscreen", false);
            fsPill.setOnClickListener(v -> onFullscreen.run());
            bar.addView(fsPill);
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        lp.setMargins(dp(activity, 12), dp(activity, 12), dp(activity, 12), 0);
        surface.addView(bar, lp);

        view.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            bar.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
            tintTimeBar(view);
        });
    }

    /** Styles the offline downloads ExoPlayer (`KuroOfflineActivity561`) with OLED pills + accent timebar. */
    static void styleOfflinePlayer(
            Activity activity,
            LinearLayout top,
            TextView back,
            TextView pip,
            PlayerView view,
            ExoPlayer engine) {
        if (top == null || view == null || engine == null) return;
        top.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));
        if (back != null) back.setBackground(pillDrawable(activity, false));
        if (pip != null) {
            pip.setText("PiP");
            pip.setBackground(pillDrawable(activity, false));
        }
        final TextView speedPill = makePill(activity, "1.0x", false);
        speedPill.setContentDescription("Playback speed");
        speedPill.setOnClickListener(v -> {
            float cur = engine.getPlaybackParameters().speed;
            int idx = 0;
            for (int i = 0; i < SPEEDS.length; i++) {
                if (Math.abs(SPEEDS[i] - cur) < 0.04f) { idx = i; break; }
            }
            float next = SPEEDS[(idx + 1) % SPEEDS.length];
            engine.setPlaybackParameters(new PlaybackParameters(next));
            speedPill.setText((next == (int) next ? String.format(Locale.ROOT, "%.1f", next) : String.valueOf(next)) + "x");
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, 0, dp(activity, 6), 0);
        int insertAt = Math.max(0, top.getChildCount() - 1);
        top.addView(speedPill, insertAt, slp);
        tintTimeBar(view);
        view.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> tintTimeBar(view));
    }

    static void tintTimeBar(View root) {
        if (root == null) return;
        if (root instanceof DefaultTimeBar) {
            DefaultTimeBar tb = (DefaultTimeBar) root;
            tb.setPlayedColor(KuroTheme12.accentColor());
            tb.setScrubberColor(KuroDialogs581.PURPLE_SOFT);
            tb.setAdMarkerColor(KuroDialogs581.BORDER);
            tb.setBufferedColor(Color.argb(90, 255, 255, 255));
            tb.setUnplayedColor(Color.argb(45, 255, 255, 255));
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) tintTimeBar(vg.getChildAt(i));
        }
    }

    /** Creates the 16:9 watch player container that also intercepts bottom swipe-up in Enhanced Fullscreen. */
    static FrameLayout createWatchPlayerBox(Activity activity, FullscreenActions actions) {
        return new SwipeUpFullscreenLayout(activity, actions, true);
    }

    /** Wraps WebChromeClient custom fullscreen view with bottom swipe-up Up-Next gesture. */
    static FrameLayout wrapCustomFullscreen(Activity activity, View customView, FullscreenActions actions) {
        SwipeUpFullscreenLayout host = new SwipeUpFullscreenLayout(activity, actions, false);
        host.setBackgroundColor(Color.BLACK);
        host.addView(customView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        host.showSwipeHint();
        return host;
    }

    /** Enhances `enterEnhancedFullscreen()` toolbar + flashes bottom swipe-up hint. */
    static void attachEnhancedFullscreen(
            Activity activity,
            FrameLayout playerBox,
            LinearLayout toolbar,
            TextView exitBtn,
            FullscreenActions actions) {
        if (playerBox == null || toolbar == null || actions == null) return;
        JSONObject info = actions.watchInfo();
        JSONArray eps = info == null ? null : info.optJSONArray("episodes");
        int cur = info == null ? -1 : info.optInt("current", -1);
        boolean hasNext = eps != null && cur >= 0 && cur + 1 < eps.length();

        TextView epBtn = makePill(activity, "Episodes", false);
        epBtn.setMinHeight(dp(activity, 40));
        epBtn.setOnClickListener(v -> openFullscreenSheet(activity, playerBox, actions));
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp1.setMargins(0, 0, dp(activity, 6), 0);
        toolbar.addView(epBtn, 0, lp1);

        if (hasNext) {
            JSONObject nextObj = eps.optJSONObject(cur + 1);
            String nextNum = nextObj != null ? nextObj.optString("number", "") : eps.optString(cur + 1, "");
            TextView nextBtn = makePill(activity,
                    nextNum.isEmpty() ? "Next Ep \u203a" : ("Next Ep " + nextNum + " \u203a"), true);
            nextBtn.setMinHeight(dp(activity, 40));
            nextBtn.setOnClickListener(v -> actions.nextEpisode());
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp2.setMargins(0, 0, dp(activity, 6), 0);
            toolbar.addView(nextBtn, 1, lp2);
        }

        if (exitBtn != null) {
            exitBtn.setBackground(pillDrawable(activity, false));
            exitBtn.setTextColor(Color.WHITE);
            exitBtn.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));
        }

        if (playerBox instanceof SwipeUpFullscreenLayout) {
            ((SwipeUpFullscreenLayout) playerBox).showSwipeHint();
        }
    }

    static void detachEnhancedFullscreen(FrameLayout playerBox) {
        closeActiveSheet();
        if (playerBox != null) {
            View oldHint = playerBox.findViewWithTag("kuro-fs-swipe-hint-153");
            if (oldHint != null) playerBox.removeView(oldHint);
        }
    }

    static boolean closeActiveSheet() {
        View sheet = activeFullscreenSheet;
        activeFullscreenSheet = null;
        if (sheet != null && sheet.getParent() instanceof ViewGroup) {
            ((ViewGroup) sheet.getParent()).removeView(sheet);
            return true;
        }
        return false;
    }

    static void openFullscreenSheet(Activity activity, FrameLayout host, FullscreenActions actions) {
        if (host == null || actions == null) return;
        JSONObject info = actions.watchInfo();
        JSONArray eps = info == null ? null : info.optJSONArray("episodes");
        if (eps == null || eps.length() == 0) return;
        closeActiveSheet();

        final int count = eps.length();
        final int current = Math.max(0, Math.min(count - 1, info.optInt("current", 0)));
        final boolean hasNext = current + 1 < count;

        final FrameLayout scrim = new FrameLayout(activity);
        scrim.setTag("kuro-fullscreen-sheet-153");
        scrim.setBackgroundColor(Color.argb(178, 0, 0, 0));
        scrim.setClickable(true);
        scrim.setFocusableInTouchMode(true);
        scrim.setOnClickListener(v -> closeActiveSheet());
        scrim.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                closeActiveSheet();
                return true;
            }
            return false;
        });

        final LinearLayout sheet = new LinearLayout(activity);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setClickable(true);
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(KuroTheme12.amoled() ? Color.rgb(6, 6, 9) : KuroDialogs581.PANEL);
        float r = dp(activity, 22);
        sheetBg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheetBg.setStroke(Math.max(1, dp(activity, 1)), KuroDialogs581.BORDER);
        sheet.setBackground(sheetBg);
        sheet.setPadding(dp(activity, 18), dp(activity, 10), dp(activity, 18), dp(activity, 18));

        /* Drag handle at top (also supports swipe-down to dismiss) */
        LinearLayout handleWrap = new LinearLayout(activity);
        handleWrap.setOrientation(LinearLayout.VERTICAL);
        handleWrap.setGravity(Gravity.CENTER_HORIZONTAL);
        handleWrap.setPadding(0, dp(activity, 2), 0, dp(activity, 10));
        View handleBar = new View(activity);
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(Color.argb(110, 255, 255, 255));
        hBg.setCornerRadius(dp(activity, 99));
        handleBar.setBackground(hBg);
        handleWrap.addView(handleBar, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 4)));
        final float[] dragStartY = new float[]{0f};
        handleWrap.setOnTouchListener((v, ev) -> {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                dragStartY[0] = ev.getRawY();
                return true;
            }
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dy = Math.max(0f, ev.getRawY() - dragStartY[0]);
                sheet.setTranslationY(dy);
                return true;
            }
            if (ev.getActionMasked() == MotionEvent.ACTION_UP || ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                if (ev.getRawY() - dragStartY[0] > dp(activity, 56)) closeActiveSheet();
                else sheet.animate().translationY(0f).setDuration(160).start();
                return true;
            }
            return false;
        });
        sheet.addView(handleWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        /* Hero "UP NEXT" Banner inside Fullscreen Sheet */
        if (hasNext) {
            final int nextIdx = current + 1;
            JSONObject nextEp = eps.optJSONObject(nextIdx);
            String nextNum = nextEp != null ? nextEp.optString("number", String.valueOf(nextIdx + 1)) : eps.optString(nextIdx, String.valueOf(nextIdx + 1));
            String nextName = nextEp != null ? nextEp.optString("name", "").trim() : "";

            LinearLayout hero = new LinearLayout(activity);
            hero.setOrientation(LinearLayout.HORIZONTAL);
            hero.setGravity(Gravity.CENTER_VERTICAL);
            hero.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 12), dp(activity, 12));
            GradientDrawable heroBg = new GradientDrawable();
            heroBg.setColor(KuroTheme12.amoled() ? Color.rgb(12, 12, 18) : KuroDialogs581.CARD);
            heroBg.setCornerRadius(dp(activity, 16));
            heroBg.setStroke(Math.max(1, dp(activity, 1.5f)), KuroTheme12.accentColor());
            hero.setBackground(heroBg);

            LinearLayout meta = new LinearLayout(activity);
            meta.setOrientation(LinearLayout.VERTICAL);
            TextView badge = new TextView(activity);
            badge.setText("\u2726  UP NEXT");
            badge.setTextColor(KuroDialogs581.PURPLE_SOFT);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setLetterSpacing(0.08f);
            meta.addView(badge);

            TextView nextTitle = new TextView(activity);
            nextTitle.setText("Episode " + nextNum + (nextName.isEmpty() ? "" : " \u00b7 " + nextName));
            nextTitle.setTextColor(KuroDialogs581.INK);
            nextTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            nextTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            nextTitle.setSingleLine(true);
            nextTitle.setEllipsize(TextUtils.TruncateAt.END);
            meta.addView(nextTitle);
            hero.addView(meta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Space gap = new Space(activity);
            hero.addView(gap, new LinearLayout.LayoutParams(dp(activity, 10), 1));

            TextView playNext = makePill(activity, "\u25b6  Play Ep " + nextNum, true);
            playNext.setMinHeight(dp(activity, 42));
            playNext.setOnClickListener(v -> {
                closeActiveSheet();
                actions.nextEpisode();
            });
            hero.addView(playNext);
            sheet.addView(hero, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            Space afterHero = new Space(activity);
            sheet.addView(afterHero, new LinearLayout.LayoutParams(1, dp(activity, 12)));
        }

        /* Header row for Episodes list */
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView hTitle = new TextView(activity);
        hTitle.setText("Episodes  \u00b7  " + count);
        hTitle.setTextColor(KuroDialogs581.INK);
        hTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(hTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView closeBtn = makePill(activity, "Close \u2715", false);
        closeBtn.setOnClickListener(v -> closeActiveSheet());
        header.addView(closeBtn);
        sheet.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Space afterHead = new Space(activity);
        sheet.addView(afterHead, new LinearLayout.LayoutParams(1, dp(activity, 8)));

        /* Episode rows window around current episode */
        final ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        int from = Math.max(0, current - 8);
        int to = Math.min(count, from + 60);
        View currentRowView = null;
        for (int i = from; i < to; i++) {
            final int idx = i;
            final boolean isCur = idx == current;
            JSONObject ep = eps.optJSONObject(idx);
            String num = ep != null ? ep.optString("number", String.valueOf(idx + 1)) : eps.optString(idx, String.valueOf(idx + 1));
            String name = ep != null ? ep.optString("name", "").trim() : "";
            if (name.isEmpty()) name = "Episode " + num;

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
            row.setFocusable(true);
            row.setClickable(true);
            GradientDrawable rBg = new GradientDrawable();
            rBg.setColor(isCur
                    ? KuroDialogs581.SELECTED
                    : (KuroTheme12.amoled() ? Color.rgb(12, 12, 16) : KuroDialogs581.CARD));
            rBg.setCornerRadius(dp(activity, 12));
            rBg.setStroke(Math.max(1, dp(activity, 1)), isCur ? KuroTheme12.accentColor() : KuroDialogs581.DIVIDER);
            row.setBackground(rBg);

            TextView numView = new TextView(activity);
            numView.setText(num);
            numView.setTextColor(isCur ? KuroDialogs581.PURPLE_SOFT : KuroDialogs581.MUTED);
            numView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            numView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            numView.setMinWidth(dp(activity, 34));
            row.addView(numView);

            LinearLayout titleCol = new LinearLayout(activity);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            TextView tView = new TextView(activity);
            tView.setText(name);
            tView.setTextColor(KuroDialogs581.INK);
            tView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            tView.setTypeface(Typeface.DEFAULT, isCur ? Typeface.BOLD : Typeface.NORMAL);
            tView.setSingleLine(true);
            tView.setEllipsize(TextUtils.TruncateAt.END);
            titleCol.addView(tView);

            long resumeMs = actions.savedPositionMs(num);
            if (resumeMs >= 5000L && !isCur) {
                long sec = resumeMs / 1000L;
                TextView sub = new TextView(activity);
                sub.setText(String.format(Locale.ROOT, "Resume %d:%02d", sec / 60L, sec % 60L));
                sub.setTextColor(KuroDialogs581.MUTED);
                sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                titleCol.addView(sub);
            }
            row.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tag = new TextView(activity);
            tag.setText(isCur ? "PLAYING" : "\u25b6");
            tag.setTextColor(isCur ? KuroDialogs581.PURPLE_SOFT : KuroDialogs581.MUTED);
            tag.setTextSize(TypedValue.COMPLEX_UNIT_SP, isCur ? 10f : 13f);
            tag.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(tag);

            row.setOnClickListener(v -> {
                closeActiveSheet();
                if (!isCur) actions.chooseEpisode(idx);
            });

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0, 0, 0, dp(activity, 6));
            list.addView(row, rlp);
            if (isCur) currentRowView = row;
        }
        scroll.addView(list);
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int maxListH = Math.max(dp(activity, 140), Math.round(dm.heightPixels * 0.42f));
        sheet.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxListH));

        int maxSheetW = Math.min(dm.widthPixels, dp(activity, 640));
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(
                maxSheetW, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        scrim.addView(sheet, sheetLp);
        host.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        activeFullscreenSheet = scrim;

        sheet.setTranslationY(dp(activity, 220));
        sheet.setAlpha(0.4f);
        sheet.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        final View scrollTarget = currentRowView;
        if (scrollTarget != null) {
            scroll.post(() -> scroll.scrollTo(0, Math.max(0, scrollTarget.getTop() - dp(activity, 28))));
        }
    }

    /** Builds the Kuro App-UI Auto-Next floating overlay card while keeping AlertDialog compatibility. */
    static AutoNextUi buildAutoNextDialog(
            Activity activity,
            AlertDialog.Builder preconfiguredBuilder,
            JSONObject watchInfo,
            int nextIndex,
            Runnable onCancel,
            Runnable onPlayNow) {
        String animeTitle = watchInfo != null ? watchInfo.optString("title", "Anime") : "Anime";
        String nextNum = String.valueOf(nextIndex + 1);
        String nextName = "";
        if (watchInfo != null) {
            JSONArray eps = watchInfo.optJSONArray("episodes");
            JSONObject ep = eps != null ? eps.optJSONObject(nextIndex) : null;
            if (ep != null) {
                nextNum = ep.optString("number", nextNum);
                nextName = ep.optString("name", "").trim();
            } else if (eps != null) {
                nextNum = eps.optString(nextIndex, nextNum);
            }
        }

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setTag("kuro-autonext-card-153");
        card.setPadding(dp(activity, 18), dp(activity, 16), dp(activity, 18), dp(activity, 16));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(KuroTheme12.amoled() ? Color.rgb(8, 7, 12) : KuroDialogs581.PANEL);
        cardBg.setCornerRadius(dp(activity, 20));
        cardBg.setStroke(Math.max(1, dp(activity, 1.5f)), KuroTheme12.accentColor());
        card.setBackground(cardBg);

        /* Top Eyebrow + Countdown Pill */
        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView eyebrow = new TextView(activity);
        eyebrow.setText("\u2726  UP NEXT");
        eyebrow.setTextColor(KuroDialogs581.PURPLE_SOFT);
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eyebrow.setLetterSpacing(0.08f);
        topRow.addView(eyebrow, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView countBadge = new TextView(activity);
        countBadge.setText("in 10s");
        countBadge.setTextColor(Color.WHITE);
        countBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        countBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        countBadge.setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 10), dp(activity, 4));
        countBadge.setBackground(pillBackground(activity, KuroDialogs581.SELECTED, KuroDialogs581.BORDER, 1f));
        topRow.addView(countBadge);
        card.addView(topRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Space s1 = new Space(activity);
        card.addView(s1, new LinearLayout.LayoutParams(1, dp(activity, 8)));

        /* Series + Next Episode Headline */
        TextView showView = new TextView(activity);
        showView.setText(animeTitle);
        showView.setTextColor(KuroDialogs581.MUTED);
        showView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        showView.setSingleLine(true);
        showView.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(showView);

        TextView epHeadline = new TextView(activity);
        epHeadline.setText("Episode " + nextNum + (nextName.isEmpty() ? "" : "  \u00b7  " + nextName));
        epHeadline.setTextColor(KuroDialogs581.INK);
        epHeadline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        epHeadline.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        epHeadline.setMaxLines(2);
        epHeadline.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(epHeadline);

        Space s2 = new Space(activity);
        card.addView(s2, new LinearLayout.LayoutParams(1, dp(activity, 12)));

        /* Smooth Animated Glowing Progress Bar */
        FrameLayout track = new FrameLayout(activity);
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setColor(KuroDialogs581.CARD);
        trackBg.setCornerRadius(dp(activity, 99));
        track.setBackground(trackBg);
        View fill = new View(activity);
        GradientDrawable fillBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{KuroTheme12.accentColor(), KuroDialogs581.PURPLE_SOFT});
        fillBg.setCornerRadius(dp(activity, 99));
        fill.setBackground(fillBg);
        track.addView(fill, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.addView(track, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 5)));

        Space s3 = new Space(activity);
        card.addView(s3, new LinearLayout.LayoutParams(1, dp(activity, 14)));

        /* Pill Action Buttons */
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        TextView cancelPill = makePill(activity, "Cancel", false);
        cancelPill.setMinHeight(dp(activity, 40));
        cancelPill.setOnClickListener(v -> onCancel.run());
        actions.addView(cancelPill);

        Space btnGap = new Space(activity);
        actions.addView(btnGap, new LinearLayout.LayoutParams(dp(activity, 8), 1));

        final TextView playPill = makePill(activity, "\u25b6  Play Ep " + nextNum, true);
        playPill.setMinHeight(dp(activity, 40));
        playPill.setOnClickListener(v -> onPlayNow.run());
        actions.addView(playPill);
        card.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog d = preconfiguredBuilder
                .setTitle(null)
                .setMessage(null)
                .setView(card)
                .create();

        d.setOnShowListener(ignored -> {
            hideStockChrome(d);
            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawableResource(android.R.color.transparent);
                w.setDimAmount(0.20f);
                DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                int width = Math.min(dm.widthPixels - dp(activity, 28), dp(activity, 372));
                WindowManager.LayoutParams wlp = w.getAttributes();
                wlp.gravity = Gravity.BOTTOM | Gravity.END;
                wlp.x = dp(activity, 14);
                wlp.y = dp(activity, 18);
                wlp.width = Math.max(dp(activity, 250), width);
                wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                w.setAttributes(wlp);
            }
            card.setTranslationY(dp(activity, 24));
            card.setAlpha(0f);
            card.animate().translationY(0f).alpha(1f).setDuration(200)
                    .setInterpolator(new DecelerateInterpolator()).start();
            playPill.requestFocus();
        });

        return new AutoNextUi(d, countBadge, fill, track);
    }

    private static void hideStockChrome(AlertDialog d) {
        try {
            View neg = d.getButton(AlertDialog.BUTTON_NEGATIVE);
            View pos = d.getButton(AlertDialog.BUTTON_POSITIVE);
            if (neg != null) {
                neg.setVisibility(View.GONE);
                if (neg.getParent() instanceof View) ((View) neg.getParent()).setVisibility(View.GONE);
            }
            if (pos != null) pos.setVisibility(View.GONE);
        } catch (RuntimeException ignored) {}
    }

    private static final class SwipeUpFullscreenLayout extends FrameLayout {
        private final Activity activity;
        private final FullscreenActions actions;
        private final boolean watchBoxMode;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private float downX, downY;
        private boolean trackingBottom;

        SwipeUpFullscreenLayout(Activity activity, FullscreenActions actions, boolean watchBoxMode) {
            super(activity);
            this.activity = activity;
            this.actions = actions;
            this.watchBoxMode = watchBoxMode;
        }

        @Override
        protected void onMeasure(int w, int h) {
            if (!watchBoxMode || (actions != null && actions.isEnhancedFullscreen())) {
                super.onMeasure(w, h);
                return;
            }
            int width = MeasureSpec.getSize(w);
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY));
        }

        void showSwipeHint() {
            View old = findViewWithTag("kuro-fs-swipe-hint-153");
            if (old != null) removeView(old);
            JSONObject info = actions != null ? actions.watchInfo() : null;
            JSONArray eps = info != null ? info.optJSONArray("episodes") : null;
            if (eps == null || eps.length() <= 1) return;
            LinearLayout pill = new LinearLayout(activity);
            pill.setTag("kuro-fs-swipe-hint-153");
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.CENTER_HORIZONTAL);
            pill.setPadding(dp(activity, 14), dp(activity, 5), dp(activity, 14), dp(activity, 6));
            pill.setClickable(false);
            pill.setFocusable(false);
            pill.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            pill.setBackground(pillBackground(activity, Color.argb(165, 10, 10, 14), KuroDialogs581.BORDER, 1f));
            TextView label = new TextView(activity);
            label.setText("\u2303  Swipe up for Next Episode");
            label.setTextColor(Color.argb(225, 243, 238, 247));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            pill.addView(label);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.setMargins(0, 0, 0, dp(activity, 8));
            addView(pill, lp);
            handler.postDelayed(() -> pill.animate().alpha(0f).setDuration(350).withEndAction(() -> {
                if (pill.getParent() == this) removeView(pill);
            }).start(), 2500L);
        }

        private boolean fullscreenActive() {
            return !watchBoxMode || (actions != null && actions.isEnhancedFullscreen());
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (!fullscreenActive()) return false;
            if (activeFullscreenSheet != null && activeFullscreenSheet.getParent() == this) {
                return false;
            }
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = ev.getX();
                downY = ev.getY();
                int h = getHeight();
                trackingBottom = h > 0 && downY >= h - dp(activity, 92);
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE && trackingBottom) {
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (dy < -dp(activity, 28) && (-dy) > Math.abs(dx) * 1.55f) {
                    trackingBottom = false;
                    openFullscreenSheet(activity, this, actions);
                    return true;
                }
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                trackingBottom = false;
            }
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return fullscreenActive() || super.onTouchEvent(event);
        }
    }
}
