package com.androidapps.robertsteele.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link PhotoGalleryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PhotoGalleryFragment extends VisibleFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private RecyclerView mRecyclerView;
    private Thumbnaildownloader mThumbnaildownloader;
    private Handler mResponseHandler = new Handler();
    private List<GalleryItem> mItems = new ArrayList<>();
    private static final String TAG = "PhotoGalleryFragment";

    public PhotoGalleryFragment() {
        // Required empty public constructor
    }

    public static PhotoGalleryFragment newInstance() {
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();
//        Intent pollServiceIntent = PollService.newIntent(getActivity());
//        getActivity().startService(pollServiceIntent);
//        PollService.setServiceAlarm(getActivity(), true);
        mThumbnaildownloader = new Thumbnaildownloader<>(mResponseHandler);
        mThumbnaildownloader.setmThumbnailDownloadListener(
                new Thumbnaildownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawableView(drawable);
                    }
                }
        );
        mThumbnaildownloader.start();
        mThumbnaildownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallary, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.photo_recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        setUpAdapter();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void setUpAdapter() {
        if (isAdded()) {
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnaildownloader.quit();
        mThumbnaildownloader.clearQueue();
        Log.i(TAG, "Background thread quit");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);
        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "Query submitted " + s);
                QueryPreferences.setStoredQuery(getActivity(), s);
                updateItems();
                searchItem.collapseActionView();
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "Search text changed" + s);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        private String mQuery;

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            FlickrFetcher flickrFetcher = new FlickrFetcher();
            if (mQuery == null) {
                return flickrFetcher.fetchRecentPhotos();
            } else {
                return flickrFetcher.searchPhotos(mQuery);
            }

        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setUpAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener {

        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);

            mImageView = (ImageView) itemView;
            itemView.findViewById(R.id.photo_recycler_view);
            itemView.setOnClickListener(this);
        }

        public void bindDrawableView(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View v) {
            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoUri());
            startActivity(i);
        }

    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.item_list_gallery, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
            Drawable placeHolder = getResources().
                    getDrawable(android.R.drawable.ic_lock_idle_charging);
            photoHolder.bindDrawableView(placeHolder);
            mThumbnaildownloader.queueThumbnail(photoHolder, galleryItem.getUrl_s());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
}
