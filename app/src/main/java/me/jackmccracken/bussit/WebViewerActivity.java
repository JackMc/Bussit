package me.jackmccracken.bussit;

import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import me.jackmccracken.bussit.models.Post;
import me.jackmccracken.bussit.utils.BasicUtils;


public class WebViewerActivity extends ActionBarActivity {
    private WebView web;
    private Post post;
    public static final int READ_POST = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_viewer);

        web = (WebView)findViewById(R.id.reader_web);

        post = getIntent().getParcelableExtra("post");

        // Either use cached content if we're not connected or load it if we can
        if (BasicUtils.isWifiConnected(this)) {
            // Go to the link stored in the post
            web.loadUrl(post.getURL());
        }
        else if (post.getContent() != null) {
            web.loadData(post.getContent(), "text/html", "UTF-8");
        }
        else {
            Log.e("RenderWeb", "WiFi is not connected and post is not cached. :(");
        }

        // Display the back button.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(post.getTitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_web_viewer, menu);
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
        else if (id == android.R.id.home) {
            setResult(Activity.RESULT_OK);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
