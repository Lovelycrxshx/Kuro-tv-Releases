package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Kuro 5.81 — themed dialogs.
 *
 * The settings screens had two visual languages: the app itself is dark violet with
 * rounded cards (see PlayerOptionsDialog), while every settings dialog was a stock
 * Material alert: grey panel, teal system accent, flat list rows. This class renders
 * those dialogs with the app's own palette, rounded panel, focus states and motion so
 * the same screen stops looking like two different apps.
 *
 * Everything here is built from framework views only, so it also works on the
 * android:style/Theme.Material.NoActionBar base theme the app uses (no AppCompat,
 * no Material Components dependency).
 *
 * Remote-first rules baked in:
 *  - every action and every choice row is focusable and takes a visible focus ring
 *    through a StateListDrawable, so DPAD navigation never loses the cursor;
 *  - initial focus is placed on the first action or the selected choice, never on
 *    the panel itself;
 *  - a focused-but-not-selected choice still shows a ring, distinct from "selected".
 */
public final class KuroDialogs581 {

    // Palette taken from the app's own CSS and PlayerOptionsDialog so the native
    // layer cannot drift from the WebView layer again.
    public static final int INK = 0xfff3eef7;
    public static final int MUTED = 0xffb7a4c7;
    public static final int PURPLE = 0xffa65be4;
    public static final int PURPLE_SOFT = 0xffc088f4;
    public static final int PANEL = 0xff100c18;
    public static final int CARD = 0xff211229;
    public static final int BORDER = 0xff665078;
    public static final int SELECTED = 0xff2b1a3a;
    public static final int DIVIDER = 0xff3a2c4a;

    private KuroDialogs581() {
    }

    /** Context for the stock alert dialogs we have not rebuilt yet: no more grey panels. */
    public static Context themed(Context context) {
        return new ContextThemeWrapper(context, R.style.KuroDialogTheme);
    }

    public static Builder build(Activity activity, String title) {
        return new Builder(activity, title);
    }

    public interface ChoiceListener {
        void onChoice(int index);
    }

    private interface Block {
        void render(LinearLayout host);
    }

    private static final class Action {
        final String label;
        final boolean primary;
        final Runnable run;

        Action(String label, boolean primary, Runnable run) {
            this.label = label;
            this.primary = primary;
            this.run = run;
        }
    }

    public static final class Builder {
        private final Activity activity;
        private final String title;
        private String subtitle = "";
        private boolean cancelable = true;
        private final List<Block> blocks = new ArrayList<>();
        private final List<Action> actions = new ArrayList<>();
        private final Dialog[] owner = new Dialog[1];

        Builder(Activity activity, String title) {
            this.activity = activity;
            this.title = title;
        }

        private int dp(int n) {
            return Math.round(n * activity.getResources().getDisplayMetrics().density);
        }

        public Builder subtitle(String text) {
            this.subtitle = text == null ? "" : text;
            return this;
        }

        /** A label/value line: "Installed" / "Kuro 5.80". */
        public Builder row(String label, String value) {
            final String l = label == null ? "" : label;
            final String v = value == null ? "" : value;
            blocks.add(host -> {
                LinearLayout line = new LinearLayout(activity);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(Gravity.CENTER_VERTICAL);
                line.setPadding(0, dp(5), 0, dp(5));
                TextView name = body(l, 13, MUTED);
                name.setMaxLines(2);
                TextView val = body(v, 13, INK);
                val.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                val.setGravity(Gravity.END);
                val.setMaxLines(3);
                line.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                vp.setMarginStart(dp(12));
                line.addView(val, vp);
                host.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            });
            return this;
        }

        /** A muted paragraph. */
        public Builder note(String text) {
            final String t = text == null ? "" : text;
            if (t.isEmpty()) return this;
            blocks.add(host -> {
                TextView tv = body(t, 12, MUTED);
                tv.setLineSpacing(dp(3), 1f);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                p.topMargin = dp(6);
                host.addView(tv, p);
            });
            return this;
        }

        /** Small uppercase group label. */
        public Builder section(String text) {
            final String t = text == null ? "" : text;
            if (t.isEmpty()) return this;
            blocks.add(host -> {
                TextView tv = body(t.toUpperCase(java.util.Locale.US), 11, MUTED);
                tv.setLetterSpacing(0.12f);
                tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                p.topMargin = dp(12);
                p.bottomMargin = dp(6);
                host.addView(tv, p);
            });
            return this;
        }

        /**
         * Parses a "Label: value" report (the updater's describe() output) into rows,
         * keeping anything that is not a label/value pair as a paragraph. Formatting
         * stays owned by the caller; only presentation changes here.
         */
        public Builder body(String report) {
            if (report == null) return this;
            for (String raw : report.split("\n")) {
                String line = raw == null ? "" : raw.trim();
                if (line.isEmpty()) continue;
                int colon = line.indexOf(": ");
                if (colon > 1 && colon <= 40) {
                    row(line.substring(0, colon), line.substring(colon + 2));
                } else {
                    note(line);
                }
            }
            return this;
        }

        /** Radio list. Picking a row dismisses the dialog and reports the index. */
        public Builder choice(String[] labels, String[] hints, int selected, ChoiceListener listener) {
            final String[] texts = labels == null ? new String[0] : labels;
            final String[] subs = hints == null ? new String[texts.length] : hints;
            final int current = selected;
            final ChoiceListener onChoice = listener;
            blocks.add(host -> {
                for (int i = 0; i < texts.length; i++) {
                    final int index = i;
                    boolean isSelected = index == current;
                    LinearLayout rowView = new LinearLayout(activity);
                    rowView.setOrientation(LinearLayout.HORIZONTAL);
                    rowView.setGravity(Gravity.CENTER_VERTICAL);
                    rowView.setFocusable(true);
                    rowView.setClickable(true);
                    rowView.setContentDescription(texts[index] + (isSelected ? ", selected" : ""));
                    rowView.setBackground(rowBackground(isSelected, dp(12)));
                    rowView.setPadding(dp(12), dp(10), dp(12), dp(10));

                    // radio dot
                    LinearLayout dot = new LinearLayout(activity);
                    dot.setGravity(Gravity.CENTER);
                    GradientDrawable ring = new GradientDrawable();
                    ring.setShape(GradientDrawable.OVAL);
                    ring.setColor(Color.TRANSPARENT);
                    ring.setStroke(dp(2), isSelected ? PURPLE : BORDER);
                    dot.setBackground(ring);
                    if (isSelected) {
                        View core = new View(activity);
                        GradientDrawable fill = new GradientDrawable();
                        fill.setShape(GradientDrawable.OVAL);
                        fill.setColor(PURPLE);
                        core.setBackground(fill);
                        LinearLayout.LayoutParams coreParams = new LinearLayout.LayoutParams(dp(10), dp(10));
                        dot.addView(core, coreParams);
                    }
                    rowView.addView(dot, new LinearLayout.LayoutParams(dp(22), dp(22)));

                    LinearLayout column = new LinearLayout(activity);
                    column.setOrientation(LinearLayout.VERTICAL);
                    TextView label = body(texts[index], 14, INK);
                    label.setTypeface(Typeface.DEFAULT, isSelected ? Typeface.BOLD : Typeface.NORMAL);
                    column.addView(label);
                    String hint = index < subs.length ? subs[index] : null;
                    if (hint != null && !hint.isEmpty()) {
                        TextView h = body(hint, 11, MUTED);
                        h.setLineSpacing(dp(2), 1f);
                        column.addView(h);
                    }
                    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    cp.setMarginStart(dp(12));
                    rowView.addView(column, cp);

                    if (isSelected) {
                        TextView mark = body("✓", 15, PURPLE_SOFT);
                        rowView.addView(mark);
                    }

                    rowView.setOnClickListener(v -> {
                        if (owner[0] != null) owner[0].dismiss();
                        if (onChoice != null) onChoice.onChoice(index);
                    });
                    rowView.setOnKeyListener((v, keyCode, event) -> {
                        if (event.getAction() == android.view.KeyEvent.ACTION_DOWN
                                && (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER
                                || keyCode == android.view.KeyEvent.KEYCODE_ENTER
                                || keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                            v.performClick();
                            return true;
                        }
                        return false;
                    });

                    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    if (i > 0) rp.topMargin = dp(8);
                    host.addView(rowView, rp);
                }
            });
            return this;
        }

        public Builder action(String label, boolean primary, Runnable run) {
            actions.add(new Action(label, primary, run));
            return this;
        }

        public Builder cancelable(boolean value) {
            this.cancelable = value;
            return this;
        }

        public Dialog show() {
            Dialog dialog = new Dialog(activity);
            owner[0] = dialog;
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(cancelable);
            dialog.setCancelable(cancelable);

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable panel = new GradientDrawable();
            panel.setColor(PANEL);
            panel.setCornerRadius(dp(20));
            panel.setStroke(dp(1), BORDER);
            root.setBackground(panel);
            root.setPadding(dp(20), dp(16), dp(20), dp(14));

            // accent strip: the app's violet, faded out to the right
            View accent = new View(activity);
            GradientDrawable accentShape = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{PURPLE, 0x00a65be4});
            accentShape.setCornerRadius(dp(2));
            accent.setBackground(accentShape);
            LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(56), dp(3));
            accentParams.bottomMargin = dp(10);
            root.addView(accent, accentParams);

            TextView heading = body(title, 20, INK);
            heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            root.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (!subtitle.isEmpty()) {
                TextView sub = body(subtitle, 12, MUTED);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                sp.topMargin = dp(4);
                root.addView(sub, sp);
            }

            if (!blocks.isEmpty()) {
                View line = new View(activity);
                line.setBackgroundColor(DIVIDER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                lp.topMargin = dp(12);
                lp.bottomMargin = dp(4);
                root.addView(line, lp);

                LinearLayout bodyColumn = new LinearLayout(activity);
                bodyColumn.setOrientation(LinearLayout.VERTICAL);
                for (Block block : blocks) block.render(bodyColumn);

                ScrollView scroll = new ScrollView(activity);
                scroll.setFillViewport(false);
                scroll.addView(bodyColumn);
                int maxHeight = Math.min(dp(460), Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.52f));
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                root.addView(scroll, sp);
                scroll.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int content = bodyColumn.getMeasuredHeight();
                        ViewGroup.LayoutParams params = scroll.getLayoutParams();
                        params.height = Math.min(content, maxHeight);
                        scroll.setLayoutParams(params);
                    }
                });
            }

            final View[] firstFocus = new View[1];
            if (!actions.isEmpty()) {
                LinearLayout footer = new LinearLayout(activity);
                footer.setOrientation(LinearLayout.HORIZONTAL);
                footer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                fp.topMargin = dp(14);
                root.addView(footer, fp);
                for (Action action : actions) {
                    TextView button = actionView(action.label, action.primary, dp(12));
                    button.setOnClickListener(v -> {
                        dialog.dismiss();
                        if (action.run != null) action.run.run();
                    });
                    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    bp.setMarginStart(dp(8));
                    footer.addView(button, bp);
                    if (firstFocus[0] == null && action.primary) firstFocus[0] = button;
                    if (firstFocus[0] == null) firstFocus[0] = button;
                }
            }

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
                root.animate().alpha(1f).translationY(0f).setDuration(220)
                        .setInterpolator(new DecelerateInterpolator()).start();
                if (firstFocus[0] != null) firstFocus[0].requestFocus();
            });

            dialog.show();
            return dialog;
        }

        private TextView body(String text, int sp, int color) {
            TextView view = new TextView(activity);
            view.setText(text);
            view.setTextColor(color);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
            view.setLineSpacing(dp(2), 1f);
            return view;
        }

        private StateListDrawable rowBackground(boolean selected, int radius) {
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_focused}, shape(SELECTED, PURPLE_SOFT, radius));
            states.addState(new int[]{android.R.attr.state_pressed}, shape(SELECTED, PURPLE_SOFT, radius));
            states.addState(new int[]{}, shape(selected ? SELECTED : CARD, selected ? PURPLE : BORDER, radius));
            return states;
        }

        private GradientDrawable shape(int fill, int stroke, int radiusDp) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(fill);
            drawable.setCornerRadius(dp(radiusDp));
            drawable.setStroke(dp(1), stroke);
            return drawable;
        }

        private TextView actionView(String label, boolean primary, int radiusDp) {
            TextView button = new TextView(activity);
            button.setText(label);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setMinHeight(dp(46));
            button.setMinWidth(dp(96));
            button.setPadding(dp(18), dp(10), dp(18), dp(10));
            button.setFocusable(true);
            button.setClickable(true);
            button.setTextColor(primary ? 0xff140b1a : INK);
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_focused}, shape(primary ? PURPLE_SOFT : SELECTED, PURPLE_SOFT, radiusDp));
            states.addState(new int[]{android.R.attr.state_pressed}, shape(primary ? PURPLE_SOFT : SELECTED, PURPLE_SOFT, radiusDp));
            states.addState(new int[]{}, shape(primary ? PURPLE : CARD, primary ? PURPLE : BORDER, radiusDp));
            button.setBackground(states);
            return button;
        }
    }
}
