package me.jackmccracken.bussit.models;

import java.util.ArrayList;
import java.util.List;

import me.jackmccracken.bussit.utils.RedditAPIHelper;

/**
 * Created by jack on 05/02/15.
 */
public class RedditPostManager implements PostManager {
    private List<Post> posts = new ArrayList<>();
    private RedditAPIHelper helper;

    public RedditPostManager(RedditAPIHelper helper) {
        this.helper = helper;
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
        // Read from Reddit.
    }
}
