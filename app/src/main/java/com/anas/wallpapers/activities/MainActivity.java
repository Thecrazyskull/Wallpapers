package com.anas.wallpapers.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.anas.wallpapers.R;
import com.anas.wallpapers.connection.ConnectionHelper;
import com.anas.wallpapers.photo.PhotoInfo;
import com.anas.wallpapers.photo.PhotoItem;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    static final int VIEW_PAGER_RESULT_CODE = 2;

    private boolean mEnteredViewPager;
    private boolean mFirstTimeLoading = true;
    private boolean mLoading;
    private boolean mReEntering;
    private boolean mRefreshing;
    private boolean mSearching;
    private int mEnterPosition;
    private int mLeavePosition;
    private int mProgressBarPosition = -1;
    private int mScrollPosition;

    private ConnectionHelper mConnectionHelper = new ConnectionHelper();
    private GridAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private ArrayList<PhotoItem> mOriginalWallpapers = new ArrayList<>();
    private ArrayList<PhotoItem> mWallpapers = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private Snackbar mErrorSnackbar;
    private SwipeRefreshLayout mRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setExitSharedElementCallback(mSharedElementCallback);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        mLayoutManager = new GridLayoutManager(this, 2);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new GridAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new CustomScrollListener());

        mErrorSnackbar = Snackbar.make(mRecyclerView, R.string.wallpapers_failed, Snackbar.LENGTH_LONG);
        mErrorSnackbar.setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRefreshing = true;
                mRefreshLayout.setRefreshing(true);
                getWallpapers(null);
            }
        });
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshing = true;
                getWallpapers(null);
            }
        });
        getWallpapers(null);
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        if (resultCode == VIEW_PAGER_RESULT_CODE) {
            mEnteredViewPager = false;
            mReEntering = true;
            mEnterPosition = data.getIntExtra("position", 0);
            mRecyclerView.scrollToPosition(mEnterPosition);
        }
    }

    @Override
    public void onBackPressed() {
        if (!mSearchView.isIconified()) {
            mSearchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(searchItem,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        if (item.getItemId() == R.id.action_search) {
                            mSearching = false;
                            if (mWallpapers.size() < mOriginalWallpapers.size()) {
                                mWallpapers.clear();
                                mWallpapers.addAll(mOriginalWallpapers);
                                mAdapter.notifyDataSetChanged();
                            }
                            mRefreshLayout.setEnabled(true);
                            mRecyclerView.scrollToPosition(mScrollPosition);

                            ActionBar actionBar = getSupportActionBar();
                            if (actionBar != null) {
                                actionBar.setDisplayHomeAsUpEnabled(false);
                            }
                        }
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        if (item.getItemId() == R.id.action_search) {
                            mSearching = true;
                            mRefreshLayout.setEnabled(false);
                            mScrollPosition = mLayoutManager.findLastVisibleItemPosition();

                            ActionBar actionBar = getSupportActionBar();
                            if (actionBar != null) {
                                actionBar.setDisplayHomeAsUpEnabled(true);
                            }
                        }
                        return true;
                    }
                });

        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setOnQueryTextListener(mOnQuerySearchListener);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getWallpapers(String schedule) {
        mConnectionHelper.getWallpapers(this, schedule, new ConnectionHelper.WallpaperLoader() {
            @Override
            public void onWallpapersLoaded(ArrayList<PhotoItem> wallpapers) {
                if (mEnteredViewPager) {
                    // we entered view pager already. DO NOT update the list.
                    return;
                }

                // Insert all wallpapers
                mWallpapers.addAll(wallpapers);

                // insert progress bar
                mWallpapers.add(null);

                mOriginalWallpapers.clear();
                mOriginalWallpapers.addAll(mWallpapers);

                mAdapter.notifyDataSetChanged();

                if (mLoading) {
                    mLoading = false;
                    if (mAdapter.getItemViewType(mProgressBarPosition)
                            == GridAdapter.TYPE_PROGRESS_BAR) {
                        mAdapter.removeAt(mProgressBarPosition);
                    }
                }

                if (mRefreshing) {
                    mRefreshLayout.setRefreshing(false);
                    mRefreshing = false;
                }

                if (mFirstTimeLoading) {
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
                    progressBar.setVisibility(View.GONE);
                    mFirstTimeLoading = false;
                }
            }

            @Override
            public void onWallpapersLoadingFailed() {
                mErrorSnackbar.show();
            }
        });
    }

    private final SearchView.OnQueryTextListener mOnQuerySearchListener
            = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextChange(String query) {
            mAdapter.filter(query);
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return true;
        }
    };

    private final SharedElementCallback mSharedElementCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mReEntering && mLeavePosition != mEnterPosition) {
                android.util.Log.e("ANAS", "PPPP");
                // TODO: Remove this workaround by animating the item from viewpager back into it's original position
                names.clear();
                sharedElements.clear();
            }
        }
    };

    private class CustomScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                int totalItemCount = mLayoutManager.getItemCount();
                int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
                if (!mSearching && totalItemCount <= (lastVisibleItem + 3)) {
                    if (!mLoading && !mWallpapers.isEmpty()) {
                        int count = mWallpapers.size();
                        // Array starts at 0, so size -1.
                        // But in this case last item is progressbar.
                        // so get the one before progress bar.
                        PhotoItem item = mWallpapers.get(count - 2);
                        String schedule = String.valueOf(item.getScheduleTime());

                        mLoading = true;
                        mProgressBarPosition = count - 1;

                        getWallpapers(schedule);
                    }
                }
            }
        }
    }

    private class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {
        private static final int TYPE_IMAGE_NEWEST = 0;
        private static final int TYPE_NORMAL_IMAGE = 1;
        private static final int TYPE_PROGRESS_BAR = 3;

        private GridAdapter() {
            mRecyclerView.addItemDecoration(new WallpaperDecoration());
            mLayoutManager.setSpanSizeLookup(mSpanSizeLookup);
        }

        @Override
        public int getItemViewType(int position) {
            if (mWallpapers.get(position) == null) {
                return TYPE_PROGRESS_BAR;
            }
            if (position == TYPE_IMAGE_NEWEST) {
                return TYPE_IMAGE_NEWEST;
            }
            return TYPE_NORMAL_IMAGE;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_PROGRESS_BAR) {
                View v = MainActivity.this.getLayoutInflater().inflate(R.layout.progress_bar,
                        parent, false);
                return new ViewHolder(v);
            }
            View v;
            boolean thumbnail = viewType == TYPE_IMAGE_NEWEST;
            if (thumbnail) {
                v = MainActivity.this.getLayoutInflater().inflate(R.layout.image_first,
                        parent, false);
            } else {
                v = MainActivity.this.getLayoutInflater().inflate(R.layout.thumbnail,
                        parent, false);
            }

            final ViewHolder holder = new ViewHolder(v);
            holder.layout.setEnabled(false);
            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLeavePosition = holder.getAdapterPosition();
                    mReEntering = false;
                    Intent intent = new Intent(MainActivity.this, ViewPagerActivity.class);
                    intent.putExtra("position", mLeavePosition);
                    intent.putParcelableArrayListExtra("wallpapers", mWallpapers);

                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(MainActivity.this, holder.thumbnail,
                                    (String) holder.thumbnail.getTag());
                    startActivity(intent, options.toBundle());
                    mEnteredViewPager = true;
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_PROGRESS_BAR) {
                return;
            }
            PhotoItem item = mWallpapers.get(position);
            PhotoInfo info = item.getPhotoInfo();
            holder.name.setText(info.getPhotoTopic());
            holder.author.setText(info.getAuthor());
            if (holder.location != null) {
                holder.location.setText(info.getCountryName());
            }

            String name = Integer.toString(position);
            holder.thumbnail.setTransitionName(name);
            holder.thumbnail.setTag(name);

            Glide.with(MainActivity.this)
                    .load(info.getThumbnailUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(new SimpleTarget<GlideDrawable>() {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                            holder.thumbnail.setImageDrawable(resource);
                            holder.layout.setEnabled(true);
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return mWallpapers.size();
        }

        void removeAt(int position) {
            mWallpapers.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }

        void filter(String query) {
            if (!TextUtils.isEmpty(query)) {
                mWallpapers.clear();
                notifyDataSetChanged();

                for (PhotoItem item : mOriginalWallpapers) {
                    if (item == null) {
                        continue;
                    }

                    PhotoInfo info = item.getPhotoInfo();
                    if (info == null || info.getPhotoTopic() == null
                            || info.getAuthor() == null || info.getCountryName() == null) {
                        continue;
                    }
                    String name = info.getPhotoTopic().toLowerCase();
                    String author = info.getAuthor().toLowerCase();
                    String location = info.getCountryName().toLowerCase();
                    String input = query.toLowerCase();
                    if (!mWallpapers.contains(item) && (name.contains(input)
                            || author.contains(input) || location.contains(input))) {
                        mWallpapers.add(item);
                        notifyDataSetChanged();
                    }
                }
            }
        }

        private final SpanSizeLookup mSpanSizeLookup = new SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (getItemViewType(position) == TYPE_NORMAL_IMAGE) {
                    return 1;
                }
                return 2;
            }
        };

        class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView thumbnail;
            private View layout;
            private TextView name;
            private TextView author;
            private TextView location;

            private ViewHolder(View view) {
                super(view);

                layout = view;
                name = (TextView) view.findViewById(R.id.item_title);
                author = (TextView) view.findViewById(R.id.item_author);
                location = (TextView) view.findViewById(R.id.item_location);
                thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            }
        }

        private class WallpaperDecoration extends RecyclerView.ItemDecoration {
            private int spacing;

            private WallpaperDecoration() {
                spacing = MainActivity.this.getResources().getDimensionPixelSize(R.dimen.item_divider);
            }

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int factor = 1;

                if (getItemViewType(position) == TYPE_IMAGE_NEWEST) {
                    int newestSpacing = (int) (spacing * 2.5);
                    outRect.left = newestSpacing;
                    outRect.top = newestSpacing;
                    outRect.right = newestSpacing;
                } else {
                    outRect.left = spacing;
                    outRect.top = spacing;
                    outRect.right = spacing;
                }
                outRect.bottom = spacing;
            }
        }
    }
}
