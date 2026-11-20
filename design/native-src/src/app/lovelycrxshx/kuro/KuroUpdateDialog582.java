package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Kuro 5.82 — the in-app update screen.
 *
 * The classic download surface, rebuilt on Kuro's own palette: a rounded violet panel,
 * the release it is fetching, a big percentage, a progress bar, transfer speed and time
 * left, and a footer whose actions follow the state.
 *
 * States it shows: checking, downloading (with a resumable byte count), waiting for
 * Wi-Fi, verifying the signature, ready to hand to Android's installer, and failed with
 * Retry. It polls the updater's own prefs, so it also reflects a download that started
 * while the app was closed, and it never invents progress it does not have.
 *
 * Remote-first: every action is focusable with a visible ring and focus lands on the
 * primary action, so a DPAD can drive it. Closing it never cancels anything — the
 * foreground service keeps downloading and the notification stays.
 */
public final class KuroUpdateDialog582 {

    /** What the screen asks the activity to do. */
    public interface Host {
        /** Re-check and download again after a failure. */
        void retryUpdate582();

        /** Hand the verified file to Android's installer. */
        void installUpdate582();
    }

    private static KuroUpdateDialog582 current;

    /** The stage the owner last closed by hand, so the screen never nags by reopening. */
    private static String hiddenStage = "";

    private final Activity activity;
    private final Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Dialog dialog;

    private final TextView title, chip, percent, detail, status;
    private final ProgressBar bar;
    private final LinearLayout footer;
    private boolean autoFired;
    private boolean closed;
    private String actionKey = "";

    private KuroUpdateDialog582(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
        this.dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable panel = new GradientDrawable();
        panel.setColor(KuroDialogs581.PANEL);
        panel.setCornerRadius(dp(20));
        panel.setStroke(dp(1), KuroDialogs581.BORDER);
        root.setBackground(panel);
        root.setPadding(dp(22), dp(16), dp(22), dp(14));

        View accent = new View(activity);
        GradientDrawable accentShape = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{KuroDialogs581.PURPLE, 0x00a65be4});
        accentShape.setCornerRadius(dp(2));
        accent.setBackground(accentShape);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(56), dp(3));
        accentParams.bottomMargin = dp(12);
        root.addView(accent, accentParams);

        // header: title on the left, state chip on the right
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        title = text("Kuro update", 19, KuroDialogs581.INK, true);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        chip = text("", 10, KuroDialogs581.PURPLE_SOFT, true);
        chip.setPadding(dp(9), dp(4), dp(9), dp(4));
        chip.setBackground(pill(KuroDialogs581.SELECTED, KuroDialogs581.PURPLE));
        chip.setLetterSpacing(0.08f);
        header.addView(chip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // big percentage
        percent = text("0%", 34, KuroDialogs581.INK, true);
        LinearLayout.LayoutParams percentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        percentParams.topMargin = dp(12);
        root.addView(percent, percentParams);

        bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setIndeterminate(true);
        try {
            bar.setProgressTintList(ColorStateList.valueOf(KuroDialogs581.PURPLE));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(KuroDialogs581.DIVIDER));
            bar.setIndeterminateTintList(ColorStateList.valueOf(KuroDialogs581.PURPLE));
        } catch (RuntimeException ignored) {
        }
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        barParams.topMargin = dp(10);
        root.addView(bar, barParams);

        detail = text("", 12, KuroDialogs581.MUTED, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(10);
        root.addView(detail, detailParams);

        status = text("", 12, KuroDialogs581.MUTED, false);
        status.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = dp(8);
        root.addView(status, statusParams);

        View divider = new View(activity);
        divider.setBackgroundColor(KuroDialogs581.DIVIDER);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.topMargin = dp(14);
        root.addView(divider, dividerParams);

        footer = new LinearLayout(activity);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = dp(12);
        root.addView(footer, footerParams);

        dialog.setContentView(root);
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window == null) return;
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.78f;
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            attributes.width = Math.min(dp(520), Math.round(screenWidth * 0.92f));
            attributes.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(attributes);
            root.setAlpha(0f);
            root.setTranslationY(dp(14));
            root.animate().alpha(1f).translationY(0f).setDuration(200)
                    .setInterpolator(new DecelerateInterpolator()).start();
        });
        dialog.setOnDismissListener(d -> {
            closed = true;
            handler.removeCallbacksAndMessages(null);
            if (current == this) current = null;
        });
    }

    /** Opens the update screen, or raises the one already open. */
    public static void open(Activity activity, Host host) {
        if (activity == null || activity.isFinishing()) return;
        if (current != null && current.dialog.isShowing()) {
            current.refresh();
            return;
        }
        KuroUpdateDialog582 screen = new KuroUpdateDialog582(activity, host);
        current = screen;
        try {
            screen.dialog.show();
        } catch (RuntimeException e) {
            current = null;
            return;
        }
        screen.refresh();
        screen.schedule();
    }

    /** True when the screen may open by itself; a stage the owner closed stays closed. */
    public static boolean autoOpenAllowed(android.content.Context c) {
        return !KuroUpdateCenter582.snapshot(c).stage.equals(hiddenStage);
    }

    /** A new check or download may show the screen again even if it was closed before. */
    public static void resetHidden() {
        hiddenStage = "";
    }

    public static boolean isOpen() {
        return current != null && current.dialog.isShowing();
    }

    public static void close() {
        KuroUpdateDialog582 screen = current;
        current = null;
        if (screen != null) {
            try {
                screen.dialog.dismiss();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void schedule() {
        if (closed) return;
        handler.postDelayed(this::refresh, 400);
    }

    // ---------------------------------------------------------------------- render

    private void refresh() {
        if (closed) return;
        KuroUpdateCenter582.Snapshot s = KuroUpdateCenter582.snapshot(activity);
        title.setText(KuroUpdateCenter582.headline(s));
        chip.setText(chip(s));
        chip.setTextColor(s.failed() ? KuroDialogs581.INK : KuroDialogs581.PURPLE_SOFT);
        percent.setText(s.percent >= 0 && !s.scheduled() ? s.percent + "%" : "—");
        if (s.percent >= 0 && (s.transferring() || s.verifying() || s.ready())) {
            bar.setIndeterminate(false);
            bar.setProgress(Math.max(0, Math.min(1000, s.percent * 10)));
        } else if (s.ready()) {
            bar.setIndeterminate(false);
            bar.setProgress(1000);
        } else {
            bar.setIndeterminate(true);
        }
        String line = KuroUpdateCenter582.detail(s);
        detail.setText(line);
        detail.setVisibility(line.isEmpty() ? View.GONE : View.VISIBLE);
        String note = status(s);
        status.setText(note);
        status.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
        String key = key(s);
        if (!key.equals(actionKey)) {
            actionKey = key;
            renderActions(s);
        }
        if (s.ready() && KuroUpdateCenter582.autoOfferAllowed(activity, s.version)) {
            KuroUpdateCenter582.markAutoOffered(activity, s.version);
            fireInstall();
            return;
        }
        schedule();
    }

    private void fireInstall() {
        if (autoFired) return;
        autoFired = true;
        final Host target = host;
        handler.postDelayed(() -> {
            close();
            if (target != null) target.installUpdate582();
        }, 900);
    }

    /** The action set only depends on which state the updater is in. */
    private static String key(KuroUpdateCenter582.Snapshot s) {
        if (s.ready()) return "ready";
        if (s.failed()) return "failed";
        if (s.checking()) return "checking";
        if (s.verifying()) return "verifying";
        if (s.transferring()) return "transferring";
        if (s.scheduled()) return "scheduled";
        return "idle";
    }

    private static String chip(KuroUpdateCenter582.Snapshot s) {
        if (s.ready()) return "READY";
        if (s.verifying()) return "VERIFYING";
        if (s.checking()) return "CHECKING";
        if (s.paused()) return "WAITING";
        if (s.scheduled()) return "SCHEDULED";
        if (s.failed()) return "FAILED";
        if (s.transferring()) return "DOWNLOADING";
        return "IDLE";
    }

    private String status(KuroUpdateCenter582.Snapshot s) {
        if (s.checking()) return "Kuro reads only the public release page on github.com.";
        if (s.paused()) return "Wi-Fi only is on for update downloads (Settings → Kuro updates).";
        if (s.verifying()) return "A file that fails this check is deleted, never installed.";
        if (s.ready()) return "Install over the top — no uninstall. Your library stays.";
        if (s.scheduled()) return "Automatic updates wait for the release to be " + KuroUpdateSchedule583.delayLabel(KuroUpdateSchedule583.delayHours(activity)).replace("after ", "") + " old. Update now skips the wait.";
        if (s.failed()) return "Nothing was installed and Kuro was not changed.";
        if (s.transferring()) return "Closing this screen does not stop the download.";
        return KuroUpdateCenter582.summary(activity);
    }

    private void renderActions(KuroUpdateCenter582.Snapshot s) {
        footer.removeAllViews();
        View first = null;
        if (s.ready()) {
            first = addAction("Install now", true, () -> {
                close();
                if (host != null) host.installUpdate582();
            });
            addAction("Later", false, this::hide);
        } else if (s.scheduled()) {
            first = addAction("Update now", true, () -> {
                close();
                if (host != null) host.retryUpdate582();
            });
            addAction("Close", false, this::hide);
        } else if (s.failed()) {
            first = addAction("Retry", true, () -> {
                close();
                if (host != null) host.retryUpdate582();
            });
            addAction("Close", false, this::hide);
        } else if (s.transferring() || s.checking() || s.verifying()) {
            first = addAction("Hide", true, this::hide);
            if (s.transferring()) addAction("Stop", false, () -> {
                KuroUpdateCenter582.stopDownload(activity);
                hide();
            });
        } else {
            first = addAction("Check now", true, () -> {
                close();
                if (host != null) host.retryUpdate582();
            });
            addAction("Close", false, this::hide);
        }
        if (first != null) first.requestFocus();
    }

    private void hide() {
        hiddenStage = KuroUpdateCenter582.snapshot(activity).stage;
        close();
    }

    // ---------------------------------------------------------------------- views

    private TextView addAction(String label, boolean primary, Runnable run) {
        TextView button = text(label, 13, primary ? 0xff140b1a : KuroDialogs581.INK, true);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(46));
        button.setMinWidth(dp(96));
        button.setPadding(dp(18), dp(10), dp(18), dp(10));
        button.setFocusable(true);
        button.setClickable(true);
        button.setBackground(buttonBackground(primary));
        button.setOnClickListener(v -> {
            if (run != null) run.run();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(dp(8));
        footer.addView(button, params);
        return button;
    }

    private StateListDrawable buttonBackground(boolean primary) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, shape(primary ? KuroDialogs581.PURPLE_SOFT : KuroDialogs581.SELECTED, KuroDialogs581.PURPLE_SOFT));
        states.addState(new int[]{android.R.attr.state_pressed}, shape(primary ? KuroDialogs581.PURPLE_SOFT : KuroDialogs581.SELECTED, KuroDialogs581.PURPLE_SOFT));
        states.addState(new int[]{}, shape(primary ? KuroDialogs581.PURPLE : KuroDialogs581.CARD, primary ? KuroDialogs581.PURPLE : KuroDialogs581.BORDER));
        return states;
    }

    private GradientDrawable shape(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable pill(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(20));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
