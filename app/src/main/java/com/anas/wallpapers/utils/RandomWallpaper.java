package com.anas.wallpapers.utils;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.anas.wallpapers.connection.ConnectionHelper;
import com.anas.wallpapers.photo.PhotoInfo;
import com.anas.wallpapers.photo.PhotoItem;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RandomWallpaper extends BroadcastReceiver {
    private static final String TAG = RandomWallpaper.class.getSimpleName();
    private static final String ACTION_SET_WALLPAPER = "com.anas.wallpapers.SET_CUSTOM_WALLPAPER";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_SET_WALLPAPER.equals(action)) {
                ConnectionHelper helper = new ConnectionHelper();
                helper.getWallpapers(context, null, new ConnectionHelper.WallpaperLoader() {
                    @Override
                    public void onWallpapersLoaded(ArrayList<PhotoItem> wallpapers) {
                        boolean home = Utilities.getRandomSetting(context, false);
                        boolean keyguard = Utilities.getRandomSetting(context, true);
                        if ((!home && !keyguard) || wallpapers.size() < 1) {
                            return;
                        }

                        int homeFlag = WallpaperManager.FLAG_SYSTEM;
                        int keyguardFlag = WallpaperManager.FLAG_LOCK;
                        final int flags = (keyguard && !home ? keyguardFlag
                                : (!keyguard && home ? homeFlag
                                : (homeFlag | keyguardFlag)));
                        final PhotoItem item = wallpapers.get(0);
                        final PhotoInfo info = item.getPhotoInfo();
                        new ApplyWallpaperTask(flags, context, info).execute();
                    }

                    @Override
                    public void onWallpapersLoadingFailed() {
                        Log.e(TAG, "failed to load wallpapers");
                    }
                });
            }
        }
    }

    private static class ApplyWallpaperTask extends AsyncTask<Void, Void, Void> {
        private int flags;
        private WeakReference<Context> context;
        private PhotoInfo info;

        private ApplyWallpaperTask(int flags, Context context, PhotoInfo info) {
            this.flags = flags;
            this.context = new WeakReference<Context>(context);
            this.info = info;
        }

        @Override
        public Void doInBackground(Void... voids) {
            try {
                Context c = context.get();
                Bitmap wallpaper = Glide.with(c)
                        .load(info.getPhotoUrl())
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(-1, -1).get();
                Utilities.setWallpaper(c, wallpaper, flags);
            } catch (Exception e) {
                Log.e(TAG, "failed to set timed wallpaper", e);
            }
            return null;
        }

    }
}
