package me.jackmccracken.bussit;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import me.jackmccracken.bussit.adapters.PostAdapter;
import me.jackmccracken.bussit.models.MockPostManager;
import me.jackmccracken.bussit.models.Post;
import me.jackmccracken.bussit.models.PostManager;
import me.jackmccracken.bussit.utils.RedditAPIHelper;


public class ReaderActivity extends ActionBarActivity {
    ListView postsView;
    PostManager postManager;
    RedditAPIHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        postsView = (ListView)findViewById(R.id.main_posts);

        List<Post> posts = new ArrayList<>();

        posts.add(new Post("Cat video!", "/r/aww"));
        posts.add(new Post("An Excellent Test!", "/r/magic"));
        posts.add(new Post("An Excellent Test!", "/r/magic1"));
        posts.add(new Post("An Excellent Test!", "/r/magic2"));
        posts.add(new Post("An Excellent Test!", "/r/magic3"));
        posts.add(new Post("An Excellent Test!", "/r/magic4"));
        posts.add(new Post("An Excellent Test!", "/r/magic5"));
        posts.add(new Post("An Excellent Test!", "/r/magic6"));
        posts.add(new Post("An Excellent Test!", "/r/magic7"));

        postManager = new MockPostManager(posts);

        //TODO: Keep the refresh token in a database.
        helper = new RedditAPIHelper(null);

        postsView.setAdapter(new PostAdapter(this, postManager));

        // If we need to get full authentication (ask the API helper),
        // then bring up a RedditLoginActivity
        if (helper.needsFullPermission()) {
            Intent openLogin = new Intent();
            openLogin.setClass(this, RedditLoginActivity.class);

            startActivityForResult(openLogin, RedditLoginActivity.FULL_LOGIN);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_reader, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
