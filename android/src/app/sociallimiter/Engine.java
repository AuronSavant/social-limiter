package app.sociallimiter;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

/** Budget logic shared by all tracked apps: session, cooldown, daily cap. */
final class Engine {
    static final long MIN = 60000L;

    static final class Status {
        boolean blocked;
        boolean daily;
        long until;
        long sessionLeft;
        long dailyLeft;
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("limiter", Context.MODE_PRIVATE);
    }

    static int get(Context c, String key, int def) {
        return sp(c).getInt(key, def);
    }

    static void saveSettings(Context c, int session, int cooldown, int daily) {
        sp(c).edit().putInt("sessionMin", session).putInt("cooldownMin", cooldown).putInt("dailyMin", daily).apply();
    }

    private static String dayKey(long t) {
        Calendar k = Calendar.getInstance();
        k.setTimeInMillis(t);
        return k.get(Calendar.YEAR) + "-" + k.get(Calendar.DAY_OF_YEAR);
    }

    private static long nextMidnight(long t) {
        Calendar k = Calendar.getInstance();
        k.setTimeInMillis(t);
        k.set(Calendar.HOUR_OF_DAY, 0);
        k.set(Calendar.MINUTE, 0);
        k.set(Calendar.SECOND, 0);
        k.set(Calendar.MILLISECOND, 0);
        k.add(Calendar.DAY_OF_YEAR, 1);
        return k.getTimeInMillis();
    }

    /**
     * Claude-style window: starts at first use, runs on the wall clock for sessionMin,
     * then cooldownMin of lockout, then resets. Daily cap counts real usage, resets at midnight.
     */
    static synchronized Status tick(Context c, long now, long addMs) {
        SharedPreferences p = sp(c);
        long sessionLen = get(c, "sessionMin", 20) * MIN;
        long cooldown = get(c, "cooldownMin", 60) * MIN;
        long dailyLimit = get(c, "dailyMin", 120) * MIN;

        String day = p.getString("day", "");
        long dailyMs = p.getLong("dailyMs", 0);
        long start = p.getLong("sessionStart", 0);

        if (!day.equals(dayKey(now))) {
            day = dayKey(now);
            dailyMs = 0;
        }
        if (start != 0 && now >= start + sessionLen + cooldown) start = 0;

        boolean blockedNow = dailyMs >= dailyLimit || (start != 0 && now >= start + sessionLen);
        if (addMs > 0 && !blockedNow) {
            if (start == 0) start = now - addMs;
            dailyMs += addMs;
        }

        p.edit().putString("day", day).putLong("dailyMs", dailyMs).putLong("sessionStart", start).apply();

        Status s = new Status();
        s.sessionLeft = start == 0 ? sessionLen : Math.max(0, start + sessionLen - now);
        s.dailyLeft = Math.max(0, dailyLimit - dailyMs);
        if (dailyMs >= dailyLimit) {
            s.blocked = true;
            s.daily = true;
            s.until = nextMidnight(now);
        } else if (start != 0 && now >= start + sessionLen) {
            s.blocked = true;
            s.until = start + sessionLen + cooldown;
        }
        return s;
    }
}
