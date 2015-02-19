package me.jackmccracken.bussit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import me.jackmccracken.bussit.adapters.PostAdapter;
import me.jackmccracken.bussit.models.PostManager;
import me.jackmccracken.bussit.models.RedditPostManager;
import me.jackmccracken.bussit.utils.AfterCallTask;
import me.jackmccracken.bussit.utils.RedditAPIHelper;


public class ReaderActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {
    private ListView postsView;
    private PostManager postManager;
    private RedditAPIHelper helper;
    private PostAdapter adapter;
    private SharedPreferences preferences;
    private SwipeRefreshLayout refreshView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        postsView = (ListView)findViewById(R.id.main_posts);

        preferences = getPreferences(Context.MODE_PRIVATE);

        helper = new RedditAPIHelper(preferences);
        postManager = new RedditPostManager(this, helper);

        adapter = new PostAdapter(this, postManager);

        postsView.setAdapter(adapter);

        refreshView = ((SwipeRefreshLayout)findViewById(R.id.refresh_view));

        refreshView.setOnRefreshListener(this);

        // If we need to get full authentication (ask the API helper),
        // then bring up a RedditLoginActivity
        if (helper.needsFullPermission()) {
            Intent openLogin = new Intent();
            openLogin.setClass(this, RedditLoginActivity.class);

            startActivityForResult(openLogin, RedditLoginActivity.FULL_LOGIN);
        }
        else if (helper.needsTokenRefresh()) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RedditLoginActivity.FULL_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
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
        postManager.update(new FinishRefreshTask());
    }

    private class FinishRefreshTask implements AfterCallTask<Void> {
        @Override
        public void run(Void param) {
            refreshView.setRefreshing(false);
        }

        @Override
        public void fail(String message) {
            refreshView.setRefreshing(false);
            Toast.makeText(ReaderActivity.this, message, Toast.LENGTH_LONG).show();
        }
    }
}
