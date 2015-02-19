package me.jackmccracken.bussit.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import me.jackmccracken.bussit.R;
import me.jackmccracken.bussit.WebViewerActivity;
import me.jackmccracken.bussit.models.Post;
import me.jackmccracken.bussit.models.PostManager;

public class PostAdapter extends ArrayAdapter<Post> {
    PostManager postManager;

    public PostAdapter(Context context, PostManager postManager) {
        super(context, R.layout.item_redditpost, postManager.getPosts());
        this.postManager = postManager;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        Post post = postManager.getPost(position);

        if (row == null) {
            // Get the post to go here, and generate a View to represent it.
            row = post.genView(parent, getContext());
        }
        else {
            // Fill up the view with a new item, reusing the old View.
            post.fillView(row);
        }

        // Set up the tag so the click handler knows what to do.
        row.setTag(post);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();

                Context context = getContext();

                intent.putExtra("post", postManager.getPost(position));
                intent.setClass(context, WebViewerActivity.class);

                context.startActivity(intent);
            }
        });

        return row;
    }
}
