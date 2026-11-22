package app.sociallimiter;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LimiterService extends AccessibilityService {
    // X/Twitter (+Lite), TikTok (all regional builds + Lite), Instagram (+Lite)
    static final Set<String> TRACKED = new HashSet<>(Arrays.asList(
            "com.twitter.android", "com.twitter.android.lite",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
            "com.zhiliaoapp.musically.go", "com.ss.android.ugc.tiktok.lite",
            "com.tiktok.lite.go",
            "com.instagram.android", "com.instagram.lite"));

    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean fg = false;
    private long last;
    private long lastBlock = 0;

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            if (!fg) return;
            long el = SystemClock.elapsedRealtime();
            long d = Math.min(el - last, 5000);
            last = el;
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            boolean active = pm.isInteractive() && !km.isKeyguardLocked();
            Engine.Status s = Engine.tick(LimiterService.this, System.currentTimeMillis(), active ? d : 0);
            if (s.blocked) block();
            else h.postDelayed(this, 1000);
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cs = e.getPackageName();
        if (cs == null) return;
        String pkg = cs.toString();
        if (TRACKED.contains(pkg)) {
            if (fg) return;
            Engine.Status s = Engine.tick(this, System.currentTimeMillis(), 0);
            if (s.blocked) { block(); return; }
            fg = true;
            last = SystemClock.elapsedRealtime();
            h.removeCallbacks(loop);
            h.postDelayed(loop, 1000);
        } else if (!ignorable(pkg)) {
            fg = false;
            h.removeCallbacks(loop);
        }
    }

    private boolean ignorable(String pkg) {
        if (pkg.equals("com.android.systemui")) return true;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
                if (i.getPackageName().equals(pkg)) return true;
            }
        }
        return false;
    }

    private void block() {
        fg = false;
        h.removeCallbacks(loop);
        long now = SystemClock.elapsedRealtime();
        if (now - lastBlock < 1500) return;
        lastBlock = now;
        performGlobalAction(GLOBAL_ACTION_HOME);
        h.postDelayed(() -> {
            Intent i = new Intent(this, BlockActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }, 300);
    }

    @Override public void onInterrupt() {}

    @Override public boolean onUnbind(Intent intent) {
        h.removeCallbacksAndMessages(null);
        return super.onUnbind(intent);
    }
}
