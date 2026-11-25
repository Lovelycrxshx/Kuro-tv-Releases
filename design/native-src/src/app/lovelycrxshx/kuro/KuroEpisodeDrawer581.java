package app.lovelycrxshx.kuro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.function.IntConsumer;

/**
 * 5.81: the episode drawer that sits over any player (native or WebView).
 *
 * The list comes from the watch payload the page already handed to the activity, so no
 * extra request is made. The current episode is pinned at the top of the list and marked;
 * tapping another row closes the player and asks the page to open that episode.
 */
final class KuroEpisodeDrawer581 {
    private static final int PANEL=0xff100c18, CARD=0xff211229, PURPLE=0xffa65be4, SELECTED=0xff2b1a3a, INK=0xfff3eef7, MUTED=0xffb7a4c7, BORDER=0xff665078;
    private final Activity activity;
    private final FrameLayout host;
    private final IntConsumer pick;
    /** Optional resume label for one episode row; null/empty means no progress. */
    interface Progress { String label(String number); }
    private Progress progress;
    private View root;
    private boolean opened;

    KuroEpisodeDrawer581(Activity activity, FrameLayout host, IntConsumer pick) {
        this.activity = activity;
        this.host = host;
        this.pick = pick;
    }

    void setProgress(Progress progress) { this.progress = progress; }

    boolean isOpen() { return opened && root != null && root.getParent() != null; }

    void close() {
        opened = false;
        View view = root;
        root = null;
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    void show(JSONArray episodes, int current) {
        if (host == null || episodes == null || episodes.length() == 0) return;
        close();
        int count = episodes.length();
        if (current < 0) current = 0;
        if (current >= count) current = count - 1;

        FrameLayout scrim = new FrameLayout(activity);
        scrim.setTag("episode-drawer-581");
        scrim.setBackgroundColor(0x99000000);
        scrim.setOnClickListener(v -> close());
        scrim.setOnKeyListener((v, code, event) -> {
            if (code == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                close();
                return true;
            }
            return false;
        });
        scrim.setFocusableInTouchMode(true);

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(rounded(PANEL, 0, 0));
        panel.setOnClickListener(v -> { });

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(10), dp(10));

        LinearLayout titles = new LinearLayout(activity);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(activity);
        title.setText("Episodes");
        title.setTextColor(INK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titles.addView(title);
        TextView sub = new TextView(activity);
        sub.setText(count + (count == 1 ? " episode" : " episodes") + " · tap to play");
        sub.setTextColor(MUTED);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        titles.addView(sub);
        header.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView x = new TextView(activity);
        x.setText("✕");
        x.setTextColor(INK);
        x.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        x.setGravity(Gravity.CENTER);
        x.setContentDescription("Close episode list");
        x.setBackground(rounded(0xff2b1a3a, 999, BORDER));
        x.setOnClickListener(v -> close());
        x.setMinWidth(dp(44));
        x.setMinHeight(dp(44));
        header.addView(x, new LinearLayout.LayoutParams(dp(44), dp(44)));
        panel.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(activity);
        scroll.setClipToPadding(false);
        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(10), dp(4), dp(10), dp(18));
        int currentRow = -1;
        for (int i = 0; i < count; i++) {
            JSONObject episode = episodes.optJSONObject(i);
            String number = episode != null ? episode.optString("number", "") : episodes.optString(i, "");
            String name = episode != null ? episode.optString("name", "") : "";
            boolean isCurrent = i == current;
            TextView row = new TextView(activity);
            String label = "Episode " + (number.isEmpty() ? String.valueOf(i + 1) : number);
            if (!isCurrent && progress != null && !number.isEmpty()) {
                String resume = progress.label(number);
                if (resume != null && !resume.isEmpty()) label += "  ·  " + resume;
            }
            row.setText(isCurrent ? "▶  " + label : label);
            row.setTextColor(isCurrent ? 0xffe6ccff : INK);
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            row.setTypeface(Typeface.DEFAULT, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(11), dp(12), dp(11));
            row.setMinHeight(dp(46));
            row.setBackground(rounded(isCurrent ? SELECTED : CARD, 12, isCurrent ? PURPLE : BORDER));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(6));
            final int index = i;
            row.setOnClickListener(v -> {
                close();
                if (pick != null) pick.accept(index);
            });
            rows.addView(row, lp);
            if (isCurrent) currentRow = i;
            if (!name.isEmpty() && isCurrent) {
                TextView detail = new TextView(activity);
                detail.setText(name);
                detail.setTextColor(MUTED);
                detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                detail.setPadding(dp(14), 0, dp(14), dp(8));
                rows.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }
        scroll.addView(rows, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        int width = Math.min(dp(330), Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.82f));
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        scrim.addView(panel, panelParams);
        host.addView(scrim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root = scrim;
        opened = true;
        scrim.requestFocus();
        if (currentRow >= 0) {
            final TextView first = (TextView) rows.getChildAt(Math.min(currentRow, rows.getChildCount() - 1));
            scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, first.getTop() - dp(8))));
        }
    }

    private int dp(int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable rounded(int color, int radius, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius >= 999 ? dp(24) : dp(radius));
        if (border != 0) drawable.setStroke(dp(1), border);
        return drawable;
    }
}
