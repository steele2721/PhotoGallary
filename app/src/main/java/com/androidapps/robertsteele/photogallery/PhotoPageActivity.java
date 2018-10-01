package com.androidapps.robertsteele.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.webkit.WebView;

public class PhotoPageActivity extends SingleFragmentActivity {

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        WebView webView = findViewById(R.id.web_view);
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static Intent newIntent(Context context, Uri uri) {
        Intent i = new Intent(context, PhotoPageActivity.class);
        i.setData(uri);
        return i;
    }

    @Override
    protected Fragment createFragment() {
        return PhotoViewFragment.newInstance(getIntent().getData());
    }


}

