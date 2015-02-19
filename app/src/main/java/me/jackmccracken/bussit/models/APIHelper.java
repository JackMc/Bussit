package me.jackmccracken.bussit.models;

import android.content.Context;
import android.webkit.WebView;

import java.util.List;

import me.jackmccracken.bussit.models.Post;
import me.jackmccracken.bussit.utils.AfterCallTask;

/**
 * Created by jack on 19/02/15.
 */
public interface APIHelper {
    void getFullPermission(WebView web);

    void getHot(Context c, AfterCallTask<List<Post>> after);

    boolean needsFullPermission();

    boolean needsTokenRefresh();

    void refreshTokens(Context context, AfterCallTask<Void> after);
}
