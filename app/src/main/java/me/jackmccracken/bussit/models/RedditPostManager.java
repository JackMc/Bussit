package me.jackmccracken.bussit.models;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.jackmccracken.bussit.ReaderActivity;
import me.jackmccracken.bussit.utils.AfterCallTask;
import me.jackmccracken.bussit.utils.DatabaseHelper;
import me.jackmccracken.bussit.utils.RedditAPIHelper;

/**
 * Created by jack on 05/02/15.
 */
public class RedditPostManager implements PostManager {
    private List<Post> posts = new ArrayList<>();
    private APIHelper helper;
    private ReaderActivity context;

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
    public void update(final AfterCallTask<Void> after) {
        helper.getHot(context, new AfterCallTask<List<Post>>() {
            @Override
            public void run(List<Post> param) {
                // HACK: We do some acrobatics here to keep the reference in the adapter the same
                // This is because then we can just invalidate the adapter to make the posts appear.
                posts.clear();
                posts.addAll(param);
                context.invalidate();


                if (after != null) {
                    after.run(null);
                }
            }

            @Override
            public void fail(String message) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();

                if (after != null) {
                    after.fail("Refresh failed: " + message);
                }
            }
        });
    }
}
