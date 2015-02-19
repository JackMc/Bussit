package me.jackmccracken.bussit.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import me.jackmccracken.bussit.R;

/**
 * Created by jack on 05/02/15.
 */
public class Post implements Parcelable {
    // Required to make a Parcelable.
    public static final Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            return new Post(source);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    private String title;
    private String subreddit;
    private String url;

    public Post(String title, String subreddit, String url) {
        this.title = title;
        this.subreddit = subreddit;
        this.url = url;
    }

    public Post(Parcel in) {
        this(in.readString(), in.readString(), in.readString());
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(subreddit);
        dest.writeString(url);
    }

    public String getURL() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSubreddit() {
        return url;
    }
}
