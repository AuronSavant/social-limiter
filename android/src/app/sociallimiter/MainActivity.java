package app.sociallimiter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView status, usage;
    private EditText session, cooldown, daily;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            status.setText(serviceEnabled()
                    ? "Protection is ON (accessibility service enabled)."
                    : "Protection is OFF. Enable \"Social Limiter\" in Accessibility settings.");
            Engine.Status s = Engine.tick(MainActivity.this, System.currentTimeMillis(), 0);
            if (s.blocked) {
                usage.setText((s.daily ? "Daily limit reached. " : "Cooling down. ")
                        + "Back in " + BlockActivity.fmt(s.until - System.currentTimeMillis()));
            } else {
                usage.setText("Session left: " + Math.min(s.sessionLeft, s.dailyLeft) / Engine.MIN
                        + " min\nToday left: " + s.dailyLeft / Engine.MIN + " min");
            }
            h.postDelayed(this, 2000);
        }
    };

    private boolean serviceEnabled() {
        String v = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return v != null && v.contains(getPackageName() + "/");
    }

    private EditText field(LinearLayout root, String label, int value) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setPadding(0, 32, 0, 0);
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(value));
        root.addView(t);
        root.addView(e);
        return e;
    }

    private int num(EditText e, int def) {
        try {
            int n = Integer.parseInt(e.getText().toString().trim());
            return n >= 1 ? n : def;
        } catch (Exception x) { return def; }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);
        sv.addView(root);

        TextView title = new TextView(this);
        title.setText("Social Limiter");
        title.setTextSize(26);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Shared budget for X/Twitter, TikTok (incl. Lite) and Instagram (incl. Lite).");
        info.setPadding(0, 16, 0, 16);
        root.addView(info);

        status = new TextView(this);
        usage = new TextView(this);
        usage.setPadding(0, 16, 0, 16);
        root.addView(status);
        root.addView(usage);

        Button acc = new Button(this);
        acc.setText("Open Accessibility settings");
        acc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(acc);

        Button appInfo = new Button(this);
        appInfo.setText("App info (allow restricted settings)");
        appInfo.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()))));
        root.addView(appInfo);

        session = field(root, "Session length (minutes)", Engine.get(this, "sessionMin", 20));
        cooldown = field(root, "Cooldown after a session (minutes)", Engine.get(this, "cooldownMin", 60));
        daily = field(root, "Daily maximum (minutes)", Engine.get(this, "dailyMin", 120));

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> {
            Engine.saveSettings(this, num(session, 20), num(cooldown, 60), num(daily, 120));
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        root.addView(save);
        setContentView(sv);
    }

    @Override protected void onResume() { super.onResume(); h.post(refresh); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(refresh); }
}
