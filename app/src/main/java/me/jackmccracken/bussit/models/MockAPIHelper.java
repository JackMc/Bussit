package me.jackmccracken.bussit.models;

import android.content.Context;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;

import me.jackmccracken.bussit.utils.AfterCallTask;

/**
 * Created by jack on 19/02/15.
 */
public class MockAPIHelper implements APIHelper {
    @Override
    public void getFullPermission(WebView web) {
        // The user never needs permission for this class
    }

    @Override
    public void getHot(Context c, AfterCallTask<List<Post>> after) {
        List<Post> posts = new ArrayList<>();

        posts.add(new Post("Hello &amp world", "/r/hello_world", "http://test.com", "", null, -1));
        // Do it in this thread...
        after.run(posts);
    }

    @Override
    public boolean needsFullPermission() {
        return false;
    }

    @Override
    public boolean needsTokenRefresh() {
        return false;
    }

    @Override
    public void refreshTokens(Context context, AfterCallTask<Void> after) {

    }
}
