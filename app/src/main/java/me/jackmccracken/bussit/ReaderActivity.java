package me.jackmccracken.bussit;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import me.jackmccracken.bussit.adapters.PostAdapter;
import me.jackmccracken.bussit.models.MockPostManager;
import me.jackmccracken.bussit.models.Post;
import me.jackmccracken.bussit.models.PostManager;
import me.jackmccracken.bussit.models.RedditPostManager;
import me.jackmccracken.bussit.utils.RedditAPIHelper;


public class ReaderActivity extends ActionBarActivity {
    ListView postsView;
    PostManager postManager;
    RedditAPIHelper helper;
    PostAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);
        postsView = (ListView)findViewById(R.id.main_posts);

        //TODO: Keep the refresh token in a database.
        helper = new RedditAPIHelper(null);
        postManager = new RedditPostManager(this, helper);

        adapter = new PostAdapter(this, postManager);

        postsView.setAdapter(adapter);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            postManager.update();
        }
        else {
            //TODO: What do we do here?? The user cancelled something pretty important...
        }
    }

    public void invalidate() {
        adapter.notifyDataSetInvalidated();
    }
}
