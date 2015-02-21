package me.jackmccracken.bussit.models;

import java.util.List;

import me.jackmccracken.bussit.utils.AfterCallTask;

/**
 * Models a provider of posts
 */
public interface PostManager {
    /**
     * Gets a post at a particular place in the ordered listing.
     *
     * @param at The place in the ordered listing of Posts to retrieve.
     * @return The post at the given index.
     */
    public Post getPost(int at);

    /**
     * Returns a List in the order which getPost gives them. THis list
     * will be impacted by any calls to networkUpdate() and should not be modified.
     *
     * @return The list described above.
     */
    public List<Post> getPosts();

    /**
     * Update the list of posts from the network.
     * @param after This will be called after the networkUpdate has been completed,
     *              either with a success or a fail.
     */
    public void networkUpdate(AfterCallTask<Void> after);

    public void cachedUpdate(AfterCallTask<Void> after);
}
