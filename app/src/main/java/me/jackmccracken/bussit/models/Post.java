package me.jackmccracken.bussit.models;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import me.jackmccracken.bussit.R;

/**
 * Created by jack on 05/02/15.
 */
public class Post {
    private String title;
    private String subreddit;
    private String url;

    public Post(String title, String subreddit, String url) {
        this.title = title;
        this.subreddit = subreddit;
        this.url = url;
    }

    public View genView(ViewGroup parent, Context context) {
        LayoutInflater li = LayoutInflater.from(context);

        View ret = li.inflate(R.layout.item_redditpost, parent, false);

        fillView(ret);

        return ret;
    }

    public void fillView(View v) {
        ((TextView)v.findViewById(R.id.post_title)).setText(this.title);
        ((TextView)v.findViewById(R.id.post_subreddit)).setText(this.subreddit);
    }
}
