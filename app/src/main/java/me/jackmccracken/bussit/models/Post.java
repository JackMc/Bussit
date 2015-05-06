package me.jackmccracken.bussit.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;

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
    private String thingId;
    // HTML content.
    private String content;
    private Calendar lastMod;

    public Post(String title, String subreddit, String url, String thingId, String content, long lastModMillis) {
        this.title = title;
        this.subreddit = subreddit;
        this.url = url;
        this.thingId = thingId;
        this.content = content;

        if (lastModMillis >= 0) {
            lastMod = Calendar.getInstance();
            lastMod.setTimeInMillis(lastModMillis);
        }
    }

    public Post(Parcel in) {
        this(in.readString(), in.readString(), in.readString(), in.readString(), in.readString(),
                in.readLong());
    }

    public View genView(ViewGroup parent, Context context) {
        LayoutInflater li = LayoutInflater.from(context);

        View ret = li.inflate(R.layout.item_redditpost, parent, false);

        fillView(ret);

        return ret;
    }

    public void fillView(View v) {
        // TODO: Is there a better way to get e.x. &amp to render as "&"?
        ((TextView)v.findViewById(R.id.post_title)).setText(Html.fromHtml(this.title));
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
        dest.writeString(thingId);
        dest.writeString(content);
        // We write a long (the time in millis when the last update occurred)
        // -1 is a sentinel value saying that there is no lastMod time.
        dest.writeLong(lastMod == null ? -1 : lastMod.getTimeInMillis());
    }

    public String getURL() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public String getThingId() {
        return thingId;
    }

    public String getContent() {
        return content;
    }

    public void updateLastModified() {
        this.lastMod = Calendar.getInstance();
    }

    public void setContent(String content) {
        updateLastModified();
        this.content = content;
    }

    public boolean needsRefresh() {
        // Refresh every day
        return (Calendar.getInstance().getTimeInMillis() - (lastMod == null ? 0 : lastMod.getTimeInMillis()))
                    > (1000L*60L*60L*24L);
    }

    public boolean isCached() {
        return content != null;
    }

    public long getLastMod() {
        return lastMod == null ? -1 : lastMod.getTimeInMillis();
    }
}
