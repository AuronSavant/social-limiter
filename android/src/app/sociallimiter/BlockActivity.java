package app.sociallimiter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BlockActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView title, sub, clock;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            Engine.Status s = Engine.tick(BlockActivity.this, System.currentTimeMillis(), 0);
            if (s.blocked) {
                title.setText(s.daily ? "Daily limit reached" : "Session over — cooldown");
                sub.setText(s.daily ? "Back tomorrow at midnight." : "You can come back in:");
                clock.setText(fmt(s.until - System.currentTimeMillis()));
            } else {
                title.setText("You're free to go back");
                sub.setText("");
                clock.setText("");
            }
            h.postDelayed(this, 1000);
        }
    };

    static String fmt(long ms) {
        long s = Math.max(0, ms / 1000);
        long hh = s / 3600, mm = (s % 3600) / 60, ss = s % 60;
        return (hh > 0 ? hh + "h " : "") + String.format("%02dm %02ds", mm, ss);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0B0B10"));
        root.setPadding(48, 48, 48, 48);

        title = text(28, true);
        sub = text(16, false);
        clock = text(44, true);
        Button home = new Button(this);
        home.setText("Go home");
        home.setOnClickListener(v -> goHome());
        root.addView(title);
        root.addView(sub);
        root.addView(clock);
        root.addView(home);
        setContentView(root);
    }

    private TextView text(int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 16, 0, 16);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private void goHome() {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override public void onBackPressed() { goHome(); }

    @Override protected void onResume() { super.onResume(); h.post(refresh); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(refresh); }
}
