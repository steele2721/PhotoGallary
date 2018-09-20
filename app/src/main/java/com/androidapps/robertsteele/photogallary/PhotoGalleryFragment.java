package com.androidapps.robertsteele.photogallary;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link PhotoGalleryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PhotoGalleryFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private RecyclerView mRecyclerView;
    private Thumbnaildownloader mThumbnaildownloader;
    private Handler mResponseHandler;
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
        new FetchItemsTask().execute();

        mThumbnaildownloader = new Thumbnaildownloader<>(new Handler());
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

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            String query = "robot";
            FlickrFetcher flickrFetcher = new FlickrFetcher();
            if (query == null) {
                return flickrFetcher.fetchRecentPhotos();
            }
            else {
                return flickrFetcher.searchPhotos(query);
            }

        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems = items;
            setUpAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mImageView = (ImageView) itemView;
            itemView.findViewById(R.id.photo_recycler_view);
        }

        public void bindDrawableView(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
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
