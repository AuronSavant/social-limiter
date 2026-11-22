# Social Limiter

Limits time on X/Twitter, TikTok and Instagram. One shared budget across all of them.

- **Session:** starts at first use, runs on the wall clock (default 20 min)
- **Cooldown:** lockout after the session ends (default 60 min), then it resets
- **Daily max:** real usage per day (default 120 min), resets at midnight

All three values are editable in the options.

## Browser extension (Chrome / Edge / Brave)
`chrome://extensions` -> Developer mode -> Load unpacked -> select `extension/`.
Covers twitter.com, x.com, tiktok.com, instagram.com.

## Android
Install `SocialLimiter.apk` (or build from `android/`). Covers X/Twitter, TikTok, Instagram incl. Lite versions.
Allow restricted settings in App info (Android 13+), then enable the accessibility service.

Build without Gradle: `aapt2 compile/link`, `javac`, `d8`, `zipalign`, `apksigner` from Android build-tools 35.
