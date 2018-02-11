package com.anas.wallpapers.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.anas.wallpapers.R;
import com.anas.wallpapers.utils.RandomWallpaper;

import java.util.Calendar;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Fragment settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private AlarmManager mAlarmManager;
        private Calendar mCalendar;
        private Intent mIntent;
        private ListPreference mAutoUpdate;
        private PendingIntent mPendingIntent;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            Context context = getContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.registerOnSharedPreferenceChangeListener(this);
            boolean enableHome = prefs.getBoolean("random-home", false);
            boolean enableKeyguard = prefs.getBoolean("random-kg", false);
            String autoUpdte = prefs.getString("auto-update", "0");

            mAutoUpdate = (ListPreference) findPreference("auto-update");

            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            mIntent = new Intent(context, RandomWallpaper.class);
            mIntent.setAction("com.anas.wallpapers.SET_CUSTOM_WALLPAPER");
            mPendingIntent = PendingIntent.getBroadcast(context,
                    0, mIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mCalendar = Calendar.getInstance();
            updateAlarm(enableHome || enableKeyguard);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            boolean enableHome = sharedPreferences.getBoolean("random-home", false);
            boolean enableKeyguard = sharedPreferences.getBoolean("random-kg", false);
            updateAlarm(enableHome || enableKeyguard);
        }

        private void updateAlarm(boolean enable) {
            if (enable) {
                mCalendar.setTimeInMillis(System.currentTimeMillis());
                mAlarmManager.cancel(mPendingIntent);
                mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mCalendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, mPendingIntent);
            } else {
                mAlarmManager.cancel(mPendingIntent);
            }
            mAutoUpdate.setEnabled(enable);
        }
    }
}
