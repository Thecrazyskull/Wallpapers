package com.anas.wallpapers.connection;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;

import com.anas.wallpapers.photo.PhotoInfo;
import com.anas.wallpapers.photo.PhotoItem;
import com.anas.wallpapers.photo.PhotosData;
import com.anas.wallpapers.photo.PhotosService;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Request;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import retrofit2.Callback;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Anass on 10-2-2018.
 */

public class ConnectionHelper {
    private static final String TAG = ConnectionHelper.class.getSimpleName();
    private static final String URL = "https://open.oneplus.cn/";

    private final static boolean DEBUG = true;

    private static final int THUMBNAIL_WIDTH = 320;

    private static final Retrofit.Builder RETROFIT_BUILDER = new Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create());

    private static final OkHttpClient.Builder HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(12000, TimeUnit.MILLISECONDS)
            .readTimeout(20000, TimeUnit.MILLISECONDS);

    private AccessToken mAccessToken;
    private String mUserInfo;

    public void getWallpapers(final Context context, String schedule, final WallpaperLoader callback) {
        createService(context, PhotosService.class, true).getPhotos(
                "service/router", schedule, "1", "75", TimeZone.getDefault().getID())
                .enqueue(new Callback<PhotosData>() {
                    @Override
                    public void onResponse(Call<PhotosData> call,
                                           retrofit2.Response<PhotosData> response) {
                        ArrayList<PhotoItem> wallpapers = new ArrayList<>();
                        PhotosData result = response.body();
                        if (result != null && result.getPhotos() != null) {
                            for (int i = 0; i < result.getPhotos().size(); i++) {
                                PhotoItem item = result.getPhotos().get(i);
                                PhotoInfo info = item.getPhotoInfo();
                                if (info != null) {
                                    String url = info.getPhotoUrl();
                                    String thumbnailUrl = url.replaceAll("\\.jpg", "_0_" +
                                            String.valueOf(THUMBNAIL_WIDTH) + "\\.jpg");
                                    info.setDate(item.getScheduleTime());
                                    info.setThumbnailUrl(thumbnailUrl);
                                    if (!wallpapers.contains(item)) {
                                        wallpapers.add(item);
                                    }
                                }
                            }
                            callback.onWallpapersLoaded(wallpapers);
                        }
                    }

                    @Override
                    public void onFailure(Call<PhotosData> call, Throwable throwable) {
                        callback.onWallpapersLoadingFailed();
                    }
                });
    }

    private <T> T createService(final Context context, Class<T> serviceClass,
                                final boolean photoMethod) {
        final String userAgent = getUserInfo("OPPhotos", context);
        HTTP_CLIENT.interceptors().clear();
        HTTP_CLIENT.addInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                if (DEBUG) {
                    Log.i(TAG, message);
                }
            }
        }).setLevel(Level.BODY));
        HTTP_CLIENT.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder().header("Accept",
                        "application/json").header("User-Agent", userAgent)
                        .method(original.method(), original.body());
                if (photoMethod) {
                    requestBuilder = requestBuilder.url(original.url().newBuilder()
                            .addQueryParameter("method", "open.daypic.photo.querySchedulePhoto")
                            .addQueryParameter("format", "json")
                            .addQueryParameter("access_token", getAccessToken(context)).build());
                }
                return chain.proceed(requestBuilder.build());
            }
        });
        HTTP_CLIENT.authenticator(new OAuthAuthenticator(context));
        return RETROFIT_BUILDER.client(HTTP_CLIENT.build()).build().create(serviceClass);
    }

    private int getResponseCount(Response response) {
        for (int result = 1; ; result++) {
            response = response.priorResponse();
            if (response == null) {
                return result;
            }
        }
    }
    private String getAccessToken(Context context) {
        if (mAccessToken == null) {
            try {
                String token = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString("token", null);
                mAccessToken = new Gson().fromJson(token, AccessToken.class);
            } catch (Exception e) {
                Log.e(TAG, "Can not get AccessToken from SharedPreferences!", e);
            }
        }
        return mAccessToken == null ? null : mAccessToken.getAccessToken();
    }

    private void setAccessToken(Context context, AccessToken accessToken) {
        mAccessToken = accessToken;
        if (mAccessToken != null) {
            SharedPreferences.Editor editor = PreferenceManager
                    .getDefaultSharedPreferences(context).edit();
            editor.putString("token", mAccessToken.toString());
            editor.apply();
        }
    }

    private String refreshAccessToken(Context context) {
        try {
            AccessToken token = createService(context, PhotosService.class, false)
                    .getAccessToken("oauth/token", "daypic", "client_credentials",
                            "read", "d645e0937c7943b9a9e964d50e486238").execute().body();
            setAccessToken(context, token);
            return token == null ? null : token.getAccessToken();
        } catch (Exception e) {
            Log.e(TAG, "Something wrong when refresh token!", e);
            return null;
        }
    }

    private String getUserInfo(String appName, Context context) {
        if (mUserInfo == null) {
            StringBuilder userInfoBuilder = new StringBuilder(appName);
            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int displayWidth = context.getResources().getDisplayMetrics().widthPixels;
            userInfoBuilder.append("/")
                    // packageInfo.versionName, update every time new apk is released
                    .append("1.0.0.170607154518.778ed81")
                    .append("_")
                    .append(200/*packageInfo.versionCode*/);
            userInfoBuilder
                    .append(" (Android ")
                    .append(Build.VERSION.RELEASE)
                    .append("; ");
            userInfoBuilder
                    .append("ONEPLUS A5000")
                    .append("; ");
            userInfoBuilder
                    .append(displayWidth)
                    .append("_")
                    .append(width)
                    .append(")");
            mUserInfo = userInfoBuilder.toString();
        }
        return mUserInfo;
    }

    private class OAuthAuthenticator implements Authenticator {
        private final Context mContext;

        private OAuthAuthenticator(Context context) {
            mContext = context;
        }

        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            if (getResponseCount(response) < 2) {
                String token = refreshAccessToken(mContext);
                Request original = response.request();
                return original.newBuilder().method(original.method(), original.body())
                        .url(original.url().newBuilder()
                                .setQueryParameter("access_token", token).build()).build();
            }
            return null;
        }
    }

    public interface WallpaperLoader {
        public void onWallpapersLoaded(ArrayList<PhotoItem> wallpapers);
        public void onWallpapersLoadingFailed();
    }
}
