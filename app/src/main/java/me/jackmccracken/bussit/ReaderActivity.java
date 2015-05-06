package me.jackmccracken.bussit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import me.jackmccracken.bussit.adapters.PostAdapter;
import me.jackmccracken.bussit.models.APIHelper;
import me.jackmccracken.bussit.models.MockAPIHelper;
import me.jackmccracken.bussit.models.MockPostManager;
import me.jackmccracken.bussit.models.PostManager;
import me.jackmccracken.bussit.models.RedditPostManager;
import me.jackmccracken.bussit.utils.AfterCallTask;
import me.jackmccracken.bussit.utils.BasicUtils;
import me.jackmccracken.bussit.utils.DatabaseHelper;
import me.jackmccracken.bussit.utils.RedditAPIHelper;

public class ReaderActivity extends ActionBarActivity
        implements SwipeRefreshLayout.OnRefreshListener {
    private PostManager postManager;
    private PostAdapter adapter;
    private SwipeRefreshLayout refreshView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reader);

        // Database setup
        DatabaseHelper.makeInstance(this);

        ListView postsView = (ListView) findViewById(R.id.main_posts);

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);

        APIHelper helper = new RedditAPIHelper(preferences);
        postManager = new RedditPostManager(this, helper);

        adapter = new PostAdapter(this, postManager);

        postsView.setAdapter(adapter);

        refreshView = ((SwipeRefreshLayout)findViewById(R.id.refresh_view));

        refreshView.setOnRefreshListener(this);

        // If we need to get full authentication (ask the API helper),
        // then bring up a RedditLoginActivity
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (helper.needsFullPermission() && BasicUtils.isWifiConnected(connectivityManager)) {
            Log.d("StartState", "Obtaining token.");
            Intent openLogin = new Intent();
            openLogin.setClass(this, RedditLoginActivity.class);

            startActivityForResult(openLogin, RedditLoginActivity.FULL_LOGIN);
        } else if (helper.needsFullPermission() && !BasicUtils.isWifiConnected(connectivityManager)) {
            Toast.makeText(this, "Warning: You must start this app with WiFi connected the first time you launch it.",
                    Toast.LENGTH_LONG).show();
        } else if (helper.needsTokenRefresh() && BasicUtils.isWifiConnected(connectivityManager)) {
            Log.d("StartState", "Obtaining refreshed token.");
            helper.refreshTokens(this, new AfterCallTask<Void>() {
                @Override
                public void run(Void param) {
                    doUpdate();
                }

                @Override
                public void fail(String message) {
                    Toast.makeText(ReaderActivity.this,
                            "Cannot refresh posts", Toast.LENGTH_LONG).show();
                }
            });
        }
        else {
            Log.d("StartState", "Cached read.");
            postManager.cachedUpdate(null);
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
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RedditLoginActivity.FULL_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                // This makes sense to do because the user just came back from a successful login
                // i.e. they were connected to the Internet.
                doUpdate();
            } else {
                //TODO: What do we do here?? The user cancelled something pretty important...
            }
        }
    }

    public void invalidate() {
        adapter.notifyDataSetInvalidated();
    }

    @Override
    public void onRefresh() {
        doUpdate();
    }

    private void doUpdate() {
        refreshView.setRefreshing(true);
        postManager.networkUpdate(new FinishRefreshTask());
    }

    private class FinishRefreshTask implements AfterCallTask<Void> {
        @Override
        public void run(Void param) {
            refreshView.setRefreshing(false);
            // Next, we need to write the posts to the database.
            DatabaseHelper dbHelper = DatabaseHelper.getInstance();
            // Clear the cache
            dbHelper.clearPosts();
            // Put the posts in the database.
            dbHelper.putPosts(postManager.getPosts());
            // Tell the adapter that things have changed.
            invalidate();
        }

        @Override
        public void fail(String message) {
            refreshView.setRefreshing(false);
            Toast.makeText(ReaderActivity.this, message, Toast.LENGTH_LONG).show();
        }
    }
}
