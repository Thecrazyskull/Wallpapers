package com.anas.wallpapers.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

public class Utilities {
    private static final String TAG = Utilities.class.getSimpleName();

    public static String getCountryName(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        Locale locale = new Locale("", countryCode.toUpperCase());
        return locale.getDisplayCountry();
    }

    public static boolean getRandomSetting(Context context, boolean keyguard) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean homeStatus = prefs.getBoolean("random-home", false);
        boolean keyguardStatus = prefs.getBoolean("random-kg", false);
        if (keyguard) {
            return keyguardStatus && autoUpdateAllowed(context);
        }
        return homeStatus && autoUpdateAllowed(context);
    }

    public static boolean autoUpdateAllowed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileData = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        String autoUpdateState = prefs.getString("auto-update", "0");
        if ("0".equals(autoUpdateState)) {
            return wifi.isConnectedOrConnecting();
        }
        return wifi.isConnectedOrConnecting() || mobileData.isConnectedOrConnecting();
    }

    public static void setWallpaper(Context context, Bitmap wallpaper, int flags) {
        WallpaperManager manager = WallpaperManager.getInstance(context);
        try {
            manager.setBitmap(wallpaper, null, true, flags);
        } catch (Exception e) {
            Log.e(TAG, "failed to set wallpaper", e);
        }
    }
}
