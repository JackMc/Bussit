package me.jackmccracken.bussit.models;

import android.os.AsyncTask;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.jackmccracken.bussit.ReaderActivity;
import me.jackmccracken.bussit.utils.AfterCallTask;
import me.jackmccracken.bussit.utils.DatabaseHelper;
import android.content.Context;

/**
 * Created by jack on 05/02/15.
 */
public class RedditPostManager implements PostManager {
    private List<Post> posts = new ArrayList<>();
    private APIHelper helper;
    private Context context;

    public RedditPostManager(ReaderActivity context, APIHelper helper) {
        this.helper = helper;
        this.context = context;
    }

    @Override
    public Post getPost(int at) {
        return posts.get(at);
    }

    @Override
    public List<Post> getPosts() {
        return posts;
    }

    @Override
    public void networkUpdate(final AfterCallTask<Void> after) {
        helper.getHot(context, new AfterCallTask<List<Post>>() {
            @Override
            public void run(List<Post> param) {
                sourcePosts(param);


                if (after != null) {
                    after.run(null);
                }
            }

            @Override
            public void fail(String message) {
                if (after != null) {
                    after.fail("Refresh failed: " + message);
                }
            }
        });
    }

    /**
     * An internal method which makes sure that the elements of {@param newPosts} are inserted into
     * and the only remaining elements of the internal list of Post objects. This will also indicate
     * to the calling context that this it is necessary to reload all of the list items in the main
     * post list.
     *
     * @param newPosts The new posts to display.
     */
    private void sourcePosts(List<Post> newPosts) {
        // HACK: We do some acrobatics here to keep the reference in the adapter the same
        // This is because then we can just invalidate the adapter to make the newPosts appear.
        this.posts.clear();
        this.posts.addAll(newPosts);
    }

    @Override
    public void cachedUpdate(final AfterCallTask<Void> after) {
        new AsyncTask<Void, Void, List<Post>>() {
            @Override
            protected List<Post> doInBackground(Void... params) {
                return DatabaseHelper.getInstance().getPosts();
            }

            @Override
            protected void onPostExecute(List<Post> result) {
                super.onPostExecute(result);

                if (result != null) {
                    sourcePosts(result);
                    if (after != null) {
                        after.run(null);
                    }
                }
                else {
                    if (after != null) {
                        after.fail("Unable to fetch posts from database.");
                    }
                }
            }
        }.execute();
    }
}
