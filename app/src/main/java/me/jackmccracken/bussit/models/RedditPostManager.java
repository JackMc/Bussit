package me.jackmccracken.bussit.models;

import android.content.Context;
import android.widget.Toast;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import me.jackmccracken.bussit.ReaderActivity;
import me.jackmccracken.bussit.utils.RedditAPIHelper;

/**
 * Created by jack on 05/02/15.
 */
public class RedditPostManager implements PostManager {
    private List<Post> posts = new ArrayList<>();
    private RedditAPIHelper helper;
    private ReaderActivity context;

    public RedditPostManager(ReaderActivity context, RedditAPIHelper helper) {
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
    public void update() {
        helper.getHot(context, new RedditAPIHelper.AfterCallTask<List<Post>>() {
            @Override
            public void run(List<Post> param) {
                // HACK: We do some acrobatics here to keep the reference in the adapter the same
                // This is because then we can just invalidate the adapter
                posts.clear();
                posts.addAll(param);
                context.invalidate();
            }

            @Override
            public void fail(String message) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
