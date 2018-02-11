package com.anas.wallpapers.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.anas.wallpapers.R;
import com.anas.wallpapers.photo.PhotoItem;
import com.anas.wallpapers.photo.PhotoInfo;
import com.anas.wallpapers.utils.PermissionHelper;
import com.anas.wallpapers.utils.Utilities;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import com.github.chrisbanes.photoview.OnPhotoTapListener;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import ooo.oxo.library.widget.PullBackLayout;

public class ViewPagerActivity extends AppCompatActivity {
    private static final String TAG = ViewPagerActivity.class.getSimpleName();

    private boolean mImmersive;
    private boolean mHasPermissions;
    private boolean mSavingWallpaper;
    private int mPosition;

    private List<PhotoItem> mWallpapers;
    private List<Integer> mWallpaperOptions = new ArrayList<>();
    private Handler mHandler = new Handler();
    private View mBottomDrawer;
    private ProgressDialog mProgressDialog;
    private Toolbar mToolbar;

    private boolean mFromActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postponeEnterTransition();
        setContentView(R.layout.view_pager);

        mFromActivity = true;

        final PullBackLayout pullBackLayout = (PullBackLayout) findViewById(R.id.pull_back);
        pullBackLayout.setCallback(mPullBackCallback);

        mPosition = getIntent().getIntExtra("position", 0);
        mWallpapers = getIntent().getParcelableArrayListExtra("wallpapers");

        mWallpaperOptions.add(R.drawable.ic_home);
        mWallpaperOptions.add(R.drawable.ic_lockscreen);
        mWallpaperOptions.add(R.drawable.ic_both);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        Drawable arrow = getDrawable(R.drawable.abc_ic_ab_back_material);
        arrow.setTint(getColor(android.R.color.white));
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeAsUpIndicator(arrow);
        }

        final CustomPagerAdapter pagerAdapter = new CustomPagerAdapter();
        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(mPosition);

        final View bottomSheet = findViewById(R.id.bottom_sheet_content);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        mBottomDrawer = findViewById(R.id.bottom_drawer);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCanceledOnTouchOutside(false);

        Button applyButton = (Button) mBottomDrawer.findViewById(R.id.apply_button);
        Button saveButton = (Button) mBottomDrawer.findViewById(R.id.save_button);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetImmersiveTimer();
                applyWallpaper(mPosition);
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetImmersiveTimer();
                saveWallpaper();
            }
        });

        checkDrawerIconsAndText(mPosition);

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mFromActivity = false;
                mPosition = position;
                checkDrawerIconsAndText(position);

                resetImmersiveTimer();
            }
        });
        resetImmersiveTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mImmersive) {
            setImmersiveMode(true, true, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (PermissionHelper.checkPermissionResult(permissions, grantResults)) {
            mHasPermissions = true;
            if (mSavingWallpaper) {
                mSavingWallpaper = false;
                saveWallpaper();
            } else {
                shareWallpaper();
            }
        } else {
            Snackbar.make(mBottomDrawer, mSavingWallpaper ? R.string.no_perms_wallpaper
                    : R.string.no_perms_share, Snackbar.LENGTH_LONG).show();
            mHasPermissions = false;
            resetImmersiveTimer();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.share_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            case R.id.action_share:
                shareWallpaper();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }

    @Override
    public void supportFinishAfterTransition() {
        animateBars(true);
        Intent intent = new Intent();
        intent.putExtra("position", mPosition);
        setResult(MainActivity.VIEW_PAGER_RESULT_CODE, intent);
        super.supportFinishAfterTransition();
    }

    private void checkDrawerIconsAndText(int position) {
        PhotoItem item = mWallpapers.get(position);
        PhotoInfo info = item.getPhotoInfo();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Update actionbar title
            actionBar.setTitle(info.getDate());
        }

        final String authorText = info.getAuthor();
        final String locationText = info.getCountryName();
        final String titleText = info.getPhotoTopic();
        final String modelText = info.getModel();
        final String emailText = info.getEmail();
        final String apertureText = info.getApertureValue();
        final String focalLengthText = info.getFocalLength();

        final View authorIcon = mBottomDrawer.findViewById(R.id.author_icon);
        final View locationIcon = mBottomDrawer.findViewById(R.id.location_icon);
        final View deviceIcon = mBottomDrawer.findViewById(R.id.device_icon);
        final View emailIcon = mBottomDrawer.findViewById(R.id.email_icon);
        final View apertureIcon = mBottomDrawer.findViewById(R.id.aperture_icon);
        final View focalLengthIcon = mBottomDrawer.findViewById(R.id.focal_length_icon);

        final TextView author = (TextView) mBottomDrawer.findViewById(R.id.author);
        final TextView location = (TextView) mBottomDrawer.findViewById(R.id.location);
        final TextView title = (TextView) mBottomDrawer.findViewById(R.id.title);
        final TextView device = (TextView) mBottomDrawer.findViewById(R.id.device);
        final TextView email = (TextView) mBottomDrawer.findViewById(R.id.email);
        final TextView aperture = (TextView) mBottomDrawer.findViewById(R.id.aperture);
        final TextView focalLength = (TextView) mBottomDrawer.findViewById(R.id.focal_length);

        if (authorText == null) {
            authorIcon.setVisibility(View.GONE);
            author.setVisibility(View.GONE);
        } else {
            authorIcon.setVisibility(View.VISIBLE);
            author.setVisibility(View.VISIBLE);
            author.setText(authorText);
        }

        if (locationText == null) {
            locationIcon.setVisibility(View.GONE);
            location.setVisibility(View.GONE);
        } else {
            locationIcon.setVisibility(View.VISIBLE);
            location.setVisibility(View.VISIBLE);
            location.setText(locationText);
        }

        if (modelText == null) {
            deviceIcon.setVisibility(View.GONE);
            device.setVisibility(View.GONE);
        } else {
            deviceIcon.setVisibility(View.VISIBLE);
            device.setVisibility(View.VISIBLE);
            device.setText(modelText);
        }

        if (emailText == null) {
            emailIcon.setVisibility(View.GONE);
            email.setVisibility(View.GONE);
        } else {
            emailIcon.setVisibility(View.VISIBLE);
            email.setVisibility(View.VISIBLE);
            email.setText(emailText);
        }

        if (apertureText == null) {
            apertureIcon.setVisibility(View.GONE);
            aperture.setVisibility(View.GONE);
        } else {
            apertureIcon.setVisibility(View.VISIBLE);
            aperture.setVisibility(View.VISIBLE);
            aperture.setText(apertureText);
        }

        if (focalLengthText == null) {
            focalLengthIcon.setVisibility(View.GONE);
            focalLength.setVisibility(View.GONE);
        } else {
            focalLengthIcon.setVisibility(View.VISIBLE);
            focalLength.setVisibility(View.VISIBLE);
            focalLength.setText(focalLengthText);
        }

        if (titleText == null) {
            title.setVisibility(View.GONE);
        } else {
            title.setText(titleText);
        }
    }

    private void setImmersiveMode(boolean immersive, boolean fromResume, boolean fromRunnable) {
        if ((mImmersive == immersive && !fromResume) || fromRunnable) return;
        mImmersive = immersive;
        if (!immersive) {
            resetImmersiveTimer();
        }
        animateBars(mImmersive);
    }

    private void animateBars(boolean immersive) {
        int drawerHeight = mBottomDrawer.getHeight();
        int toolbarHeight = mToolbar.getHeight();
        mBottomDrawer.animate().translationY(immersive ? drawerHeight : 0.0f).start();
        mToolbar.animate().translationY(immersive ? -toolbarHeight : 0.0f).start();
    }

    private void resetImmersiveTimer() {
        mHandler.removeCallbacks(mImmersiveRunnable);
        mHandler.postDelayed(mImmersiveRunnable, 10000);
    }

    private void applyWallpaper(final int position) {
        resetImmersiveTimer();
        final ListAdapter adapter = new ArrayAdapter<Integer>(this,
                android.R.layout.select_dialog_item,
                android.R.id.text1, mWallpaperOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Resources res = ViewPagerActivity.this.getResources();

                String[] items = res.getStringArray(R.array.wallpaper_options);
                float padding = res.getDimensionPixelSize(R.dimen.wallpaper_text_padding);
                float leftPadding = res.getDimensionPixelSize(R.dimen.wallpaper_text_padding_left);
                float textSize = res.getDimensionPixelSize(R.dimen.wallpaper_text_size);

                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setText(items[position]);
                text.setTextSize(textSize);
                text.setCompoundDrawablesWithIntrinsicBounds(mWallpaperOptions.get(position),
                        0, 0, 0);
                text.setPadding((int) leftPadding, 0, 0, 0);
                text.setCompoundDrawablePadding((int) padding);
                return view;
            }
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_instructions)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedItemIndex) {
                            int whichWallpaper;
                            if (selectedItemIndex == 0) {
                                whichWallpaper = WallpaperManager.FLAG_SYSTEM;
                            } else if (selectedItemIndex == 1) {
                                whichWallpaper = WallpaperManager.FLAG_LOCK;
                            } else {
                                whichWallpaper = WallpaperManager.FLAG_SYSTEM
                                        | WallpaperManager.FLAG_LOCK;
                            }
                            new ApplyWallpaperTask(position, whichWallpaper,
                                    ViewPagerActivity.this, mWallpapers,
                                    mProgressDialog, mBottomDrawer).execute();
                        }
                    }).show();
    }

    private void shareWallpaper() {
        mHasPermissions = PermissionHelper.checkPermissions(this);
        if (!mHasPermissions) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PhotoItem item = mWallpapers.get(mPosition);
                    PhotoInfo info = item.getPhotoInfo();

                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    Bitmap bm = Glide.with(ViewPagerActivity.this)
                            .load(info.getPhotoUrl())
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(-1, -1).get();
                    bm.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    String path = MediaStore.Images.Media.insertImage(
                            ViewPagerActivity.this.getContentResolver(),
                            bm,  UUID.randomUUID().toString() + ".png", "drawing");
                    Uri uri = Uri.parse(path);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent , "Share"));
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(TAG, "failed to share image", e);
                }
            }
        }).start();
    }

    private void saveWallpaper() {
        mSavingWallpaper = true;
        mHasPermissions = PermissionHelper.checkPermissions(this);
        if (mHasPermissions) {
            PhotoItem item = mWallpapers.get(mPosition);
            PhotoInfo info = item.getPhotoInfo();
            new SaveWallpaperTask(this, info, mBottomDrawer).execute();
        }
    }

    private final Runnable mImmersiveRunnable = new Runnable() {
        @Override
        public void run() {
            setImmersiveMode(true, false, true);
        }
    };

    private final PullBackLayout.Callback mPullBackCallback = new PullBackLayout.Callback() {
        @Override
        public void onPullStart() {
        }

        @Override
        public void onPull(float progress) {
        }

        @Override
        public void onPullCancel() {
        }

        @Override
        public void onPullComplete() {
            supportFinishAfterTransition();
        }
    };

    private static class SaveWallpaperTask extends AsyncTask<Void, Void, Void> {
        private boolean mFailed;

        private WeakReference<Context> context;
        private WeakReference<View> bottomDrawer;

        private PhotoInfo info;

        private SaveWallpaperTask(Context context, PhotoInfo info, View bottomDrawer) {
            this.info = info;
            this.context = new WeakReference<>(context);
            this.bottomDrawer = new WeakReference<>(bottomDrawer);
        }

        @Override
        public Void doInBackground(Void... voids) {
            String wallpaperName = "/" + info.getPhotoTopic().replace(" ", "-") + ".jpg";
            File path = new File(Environment.getExternalStorageDirectory().toString()
                    + "/Wallpapers");
            if (!path.exists() && !path.mkdir()) {
                Log.e(TAG, "failed to create path");
                return null;
            }

            File file = new File(path.toString() + wallpaperName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                Bitmap bm = Glide.with(context.get())
                        .load(info.getPhotoUrl())
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(-1, -1).get();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save wallpaper", e);
                mFailed = true;
            }
            return null;
        }

        @Override
        public void onPostExecute(Void aVoid) {
            Snackbar.make(bottomDrawer.get(), !mFailed ? R.string.saved : R.string.save_failed,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private static class ApplyWallpaperTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> context;
        private WeakReference<View> bottomDrawer;
        private PhotoInfo info;
        private ProgressDialog progressDialog;
        private int flags;

        private ApplyWallpaperTask(int position, int flags, Context context,
                                  List<PhotoItem> wallpapers,
                                  ProgressDialog progressDialog,
                                  View bottomDrawer) {
            this.flags = flags;
            this.progressDialog = progressDialog;
            this.context = new WeakReference<>(context);
            this.bottomDrawer = new WeakReference<>(bottomDrawer);

            PhotoItem item = wallpapers.get(position);
            info = item.getPhotoInfo();
        }

        @Override
        public void onPreExecute() {
            progressDialog.setMessage(context.get().getString(
                    R.string.applying_wallpaper));
            progressDialog.show();
        }

        @Override
        public Void doInBackground(Void... voids) {
            try {
                Bitmap bm = Glide.with(context.get())
                        .load(info.getPhotoUrl())
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(-1, -1).get();
                Utilities.setWallpaper(context.get(), bm, flags);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set wallpaper", e);
            }
            return null;
        }

        @Override
        public void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            Snackbar.make(bottomDrawer.get(), R.string.wallpaper_applied, Snackbar.LENGTH_SHORT).show();
        }
    }

    private class CustomPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(final ViewGroup collection, int position) {
            final PhotoItem item = mWallpapers.get(position);
            final PhotoInfo info = item.getPhotoInfo();
            final View layout = getLayoutInflater().inflate(
                    R.layout.image_view, null);
            final PhotoView view = (PhotoView) layout.findViewById(R.id.thumbnail);
            final ProgressBar progressBar = (ProgressBar) layout.findViewById(R.id.progress_bar);
            layout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent evt) {
                    if (evt.getAction() == MotionEvent.ACTION_DOWN) {
                        setImmersiveMode(!mImmersive, false, false);
                    }
                    return false;
                }
            });
            view.setTransitionName(Integer.toString(position));
            view.setEnabled(false);
            view.setOnPhotoTapListener(new OnPhotoTapListener() {
                @Override
                public void onPhotoTap(ImageView view, float x, float y) {
                    setImmersiveMode(!mImmersive, false, false);
                }
            });

            Glide.with(ViewPagerActivity.this).load(info.getThumbnailUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            view.setImageDrawable(resource);

                            if (mFromActivity) {

                                Transition sharedElementEnterTransition = getWindow().getSharedElementEnterTransition();
                                sharedElementEnterTransition.addListener(new Transition.TransitionListener() {
                                    @Override
                                    public void onTransitionStart(Transition transition) {
                                    }

                                    @Override
                                    public void onTransitionEnd(Transition transition) {
                                        loadImage(view, progressBar, info.getPhotoUrl());
                                    }

                                    @Override
                                    public void onTransitionCancel(Transition transition) {
                                    }

                                    @Override
                                    public void onTransitionPause(Transition transition) {
                                    }

                                    @Override
                                    public void onTransitionResume(Transition transition) {
                                    }
                                });
                            } else {
                                loadImage(view, progressBar, info.getPhotoUrl());
                            }
                            startPostponedEnterTransition();
                        }
                    });
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            // Last item is always the progress bar
            return mWallpapers.size() - 1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        private void loadImage(final ImageView view, final ProgressBar progressBar,
                               final String url) {
            Glide.with(ViewPagerActivity.this)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource,
                                                    GlideAnimation<? super GlideDrawable> glideAnimation) {
                            progressBar.setVisibility(View.GONE);
                            view.setImageDrawable(resource);
                            view.setEnabled(true);
                        }
                    });
        }
    }
}
