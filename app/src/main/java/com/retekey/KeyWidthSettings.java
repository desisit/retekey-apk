package com.retekey;

import android.content.SharedPreferences;

/** User-adjustable visual width of on-screen keys. Touch targets remain full grid cells. */
public final class KeyWidthSettings {
    public static final String KEY_PERCENT = "key_width_percent";
    public static final int MIN_PERCENT = 50;
    public static final int MAX_PERCENT = 100;
    public static final int DEFAULT_PERCENT = 90;

    private KeyWidthSettings() {}

    public static int clamp(int value) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, value));
    }

    public static int percent(SharedPreferences prefs, ScreenOrientation orientation) {
        return clamp(OrientedPrefs.getInt(prefs, KEY_PERCENT, orientation, DEFAULT_PERCENT));
    }

    public static void setPercent(SharedPreferences prefs, ScreenOrientation orientation, int value) {
        OrientedPrefs.putInt(prefs, KEY_PERCENT, orientation, clamp(value));
    }
}
