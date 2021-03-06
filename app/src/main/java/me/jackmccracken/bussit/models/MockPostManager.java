package me.jackmccracken.bussit.models;

import java.util.List;

import me.jackmccracken.bussit.utils.AfterCallTask;

/**
 * Lets you provide a list of Post objects to provide to the PostAdapter.
 */
public class MockPostManager implements PostManager {
    List<Post> posts;

    public MockPostManager(List<Post> posts) {
        this.posts = posts;
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
    public void networkUpdate(AfterCallTask<Void> after) {
        // Tell it we're done!
        after.run(null);
    }

    @Override
    public void cachedUpdate(AfterCallTask<Void> after) {
        // Mock it in the same way as a network update.
        networkUpdate(after);
    }
}
