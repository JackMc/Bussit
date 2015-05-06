package me.jackmccracken.bussit.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import me.jackmccracken.bussit.R;
import me.jackmccracken.bussit.models.APIHelper;
import me.jackmccracken.bussit.models.Post;

/**
 * Created by jack on 05/02/15.
 */
public class RedditAPIHelper implements APIHelper {
    /*
     * References for most of the following:
     * - https://github.com/reddit/reddit/wiki/OAuth2
     * - https://github.com/reddit/reddit/wiki/API
     * - Many other pages on the Reddit wiki.
    */
    // Must be 'code'
    private static final String RESPONSE_TYPE = "code";
    // Redirect location
    private static final String REDIRECT_URI = "http://example.com";
    // The style of request we should use
    private static final String REQUEST_DURATION = "permanent";
    // The OAuth capabilities to request (currently only read because all we do is read the user's
    // homepage)
    private static final String REQUEST_SCOPE = "read";
    // Our HTTP user agent
    private static final String USER_AGENT = "Bussit by /u/Auv5";
    private static final int STATE_STRING_LENGTH = 16;

    private static final String API_DOMAIN = "oauth.reddit.com";
    private static final String API_PROTOCOL = "https";
    private final SharedPreferences preferences;

    /**
     * A token given to us by the Reddit servers. Used for when we need to refresh our authorization
     * and get a new normal token.
     */
    private String refreshToken;
    /**
     * The token we usually pass to the server to tell it we are us.
     */
    private String token;
    /**
     * The time in epoch seconds when the current token will expire
     */
    private long expires;

    /**
     * A queue of async operations which will be ran once the refresh
     * has occurred
     */
    private Queue<AsyncTask<Void, Void, ?>> ops;

    /**
     * True if we are currently refreshing in any state.
     */
    private boolean refreshing = false;

    /**
     * Initializes a Reddit API helper.
     * @param preferences The shared preferences from which any tokens that were written will
     *                    be extracted.
     */
    public RedditAPIHelper(SharedPreferences preferences) {
        this.preferences = preferences;
        this.refreshToken = preferences.getString("refresh_token", null);
        this.token = preferences.getString("token", null);
        this.expires = preferences.getLong("expires", 0);

        ops = new LinkedList<>();
        setInstance(this);
    }

    private static RedditAPIHelper instance;

    private static void setInstance(RedditAPIHelper instance) {
        RedditAPIHelper.instance = instance;
    }

    public static RedditAPIHelper getInstance() {
        return instance;
    }

    /**
     * Generates a state String to be used in communications with the Reddit servers during
     * first authentication. This String is pseudorandom.
     *
     * @return A pseudorandom string that is the state String for this request.
     */
    private String genStateString() {
        // Currently we use 16-letter states. All letters happen to be numbers.
        StringBuilder builder = new StringBuilder();
        // Use a normal Random as this doesn't have to be crypto-secure
        Random random = new Random();

        for (int i = 0; i < STATE_STRING_LENGTH; i ++) {
            // A one-digit number
            builder.append(random.nextInt(10));
        }

        return builder.toString();
    }

    /**
     * Utility method to generate an auth URL for the WebView to go to.
     * @return The auth URL to visit.
     */
    private String makeAuthURL(Context c) {
        // From the reddit docs:
        // https://www.reddit.com/api/v1/authorize?client_id=CLIENT_ID&response_type=TYPE&
        // state=RANDOM_STRING&redirect_uri=URI&duration=DURATION&scope=SCOPE_STRING

        return String.format("https://www.reddit.com/api/v1/authorize.compact?client_id=%s&" +
                                "response_type=%s&state=%s&redirect_uri=%s&duration=%s&scope=%s",
                                c.getString(R.string.reddit_app_id), RESPONSE_TYPE,
                                genStateString(), REDIRECT_URI, REQUEST_DURATION, REQUEST_SCOPE);
    }

    /**
     * Makes a full on request to the Reddit servers for access to read from a user's account.
     * This method should generally only be called on first run.
     * @param web The WebView to interact with.
     */
    @Override
    public void getFullPermission(final WebView web) {
        WebViewClient client = new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url.startsWith(REDIRECT_URI)) {
                    final String REDDIT_CODE_INDICATOR = "code=";
                    // Now we need to make another request to the Reddit servers (async) to
                    // get the actual access tokens
                    if (url.contains("error")) {
                        Toast.makeText(web.getContext(), "Reddit login request failed. Trying again.",
                                       Toast.LENGTH_LONG).show();
                        pointWeb(web);
                    }
                    else if (url.contains("code")) {
                        new GetTokensTask(web).execute(url.substring(url.indexOf(REDDIT_CODE_INDICATOR)
                                + REDDIT_CODE_INDICATOR.length()));
                    }
                    else {
                        Toast.makeText(web.getContext(), "Reddit login encountered a weird error... Trying again.",
                                        Toast.LENGTH_LONG).show();
                        pointWeb(web);
                    }
                }
            }
        };

        web.setWebViewClient(client);

        pointWeb(web);
    }

    /**
     * Points the WebView at the auth URL.
     * @param web The WebView to point.
     */
    private void pointWeb(WebView web) {
        web.loadUrl(makeAuthURL(web.getContext()));
    }

    /**
     * Sets up the request for OAuth
     *
     * @param request The request that will be sent to the server.
     */
    private void setupTokenAuthHeaders(HttpRequest request) {
        setupUserAgent(request);

        request.setHeader("Authorization", "bearer " + token);
    }

    /**
     * Checks if we need to refresh the token
     *
     * @param c The context in which the request was made.
     */
    private void checkNeedRefresh(Context c) {
        // expires is the time when our token will become invalid
        if (needsTokenRefresh()) {
            new RefreshTokensTask(null).execute(c);
        }

        checkAndExecuteQueue();
    }

    /**
     * Goes through and executes any pending tasks.
     */
    private void checkAndExecuteQueue() {
        // We loop and execute all the tasks that are waiting in threads.
        while (!ops.isEmpty()) {
            AsyncTask<Void, Void, ?> task = ops.poll();

            task.execute();
        }
    }

    /**
     * Sets up the basic auth parameters
     * @param request The request that will be sent to the server.
     * @param c The context in which the request was made.
     */
    private void setupBasicAuthHeaders(HttpRequest request, Context c) {
        setupUserAgent(request);

        // Start HTTP auth (app id + : + empty pw)
        // Source: https://github.com/reddit/reddit/wiki/OAuth2 (search for "HTTP Basic Auth")
        // Code Source: http://blog.leocad.io/basic-http-authentication-on-android/
        String credentials = c.getString(R.string.reddit_app_id) + ":";

        // Put the credentials into a base64 string.
        String base64Credentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        request.addHeader("Authorization", "Basic " + base64Credentials);
    }

    /**
     * Sets up the User Agent because Reddit doesn't like the standard one.
     *
     * @param request The request that will be sent to the server.
     */
    private void setupUserAgent(HttpRequest request) {
        // Set the user agent because Reddit doesn't like the standard one.
        request.setHeader("User-Agent", USER_AGENT);
    }

    private String getAPIEndpoint(String endpoint, NameValuePair...params) {
        Uri.Builder builder = new Uri.Builder().scheme(API_PROTOCOL)
                                .authority(API_DOMAIN)
                                .path(endpoint);
        for (NameValuePair p : params) {
            builder.appendQueryParameter(p.getName(), p.getValue());
        }

        return builder.toString();
    }

    private void enqueueTask(AsyncTask<Void, Void, ?> task) {
        ops.add(task);
    }

    private List<Post> jsonToPosts(JSONObject object) {
        List<Post> posts;

        try {
            JSONObject data = object.getJSONObject("data");
            JSONArray pageChildren = data.getJSONArray("children");

            // We give the arraylist the length we're gonna have.
            posts = new ArrayList<>(pageChildren.length());

            for (int i = 0; i < pageChildren.length(); i++) {
                JSONObject postData = pageChildren.getJSONObject(i).getJSONObject("data");

                posts.add(DatabaseHelper.getInstance().makePost(postData.getString("title"),
                                               "/r/" + postData.getString("subreddit"),
                                               postData.getString("url"),
                                               postData.getString("id")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return posts;
    }

    /**
     *
     * @param c The context in which the request was made.
     * @param after Will be executed after
     */
    @Override
    public void getHot(final Context c, final AfterCallTask<List<Post>> after) {
        AsyncTask<Void, Void, List<Post>> getHotTask = new AsyncTask<Void, Void, List<Post>>() {
            /**
             * A string containing a message explaining what went wrong. Should be user readable.
             */
            String message = null;

            @Override
            protected List<Post> doInBackground(Void... voids) {
                try {
                    HttpClient client = new DefaultHttpClient();

                    HttpGet get = new HttpGet(getAPIEndpoint("hot"));

                    setupUserAgent(get);
                    setupTokenAuthHeaders(get);
                    HttpResponse response = client.execute(get);

                    int code = response.getStatusLine().getStatusCode();

                    if (code != HttpStatus.SC_OK) {
                        message = String.format(c.getString(R.string.api_bad_response_code), code);
                        return null;
                    }

                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        JSONObject object = new JSONObject(BasicUtils.convertToString(entity.getContent()));

                        List<Post> posts = jsonToPosts(object);

                        if (posts != null) {
                            return posts;
                        }
                        else {
                            message = "Could not get posts from Reddit server.";
                            return null;
                        }
                    }
                } catch (IOException | JSONException e) {
                    //TODO: Nicer error messages.
                    message = e.getMessage();
                    return null;
                }


                return null;
            }

            @Override
            protected void onPostExecute(List<Post> returnValue) {
                // We got data back from the server!
                if (returnValue != null) {
                    after.run(returnValue);
                }
                else {
                    // The network failed or something else happened :(
                    // Notify the caller.
                    after.fail(message);
                }
            }
        };

        enqueueTask(getHotTask);

        // Check if we need to refresh the OAuth. This will also execute us after.
        checkNeedRefresh(c);
    }

    /**
     * Retrieves the tokens from the Reddit server using the code given.
     *
     * @param code The code from the auth page.
     */
    private boolean getFirstToken(String code, Context c) {
        try {
            HttpClient client = new DefaultHttpClient();

            List<NameValuePair> params = new ArrayList<>(3);

            // We put in the post params
            params.add(new BasicNameValuePair("grant_type", "authorization_code"));
            params.add(new BasicNameValuePair("code", code));
            params.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));

            HttpPost post = new HttpPost("https://www.reddit.com/api/v1/access_token");
            post.setEntity(new UrlEncodedFormEntity(params));

            setupBasicAuthHeaders(post, c);

            HttpResponse response = client.execute(post);

            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream stream = entity.getContent();
                String result = BasicUtils.convertToString(stream);
                JSONObject object = new JSONObject(result);

                token = object.getString("access_token");
                refreshToken = object.getString("refresh_token");
                expires = System.currentTimeMillis() + object.getLong("expires_in");

                putTokens();

                return true;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void putTokens() {
        SharedPreferences.Editor editor = preferences.edit();

        // Apply the tokens we got from Reddit.
        editor.putLong("expires", expires);
        editor.putString("refresh_token", refreshToken);
        editor.putString("token", token);

        // Put the changes into the file.
        editor.apply();
    }

    /**
     * Returns true if we need to get permission from the Reddit server, and require user interaction
     * to do so.
     *
     * @return True if we need to pull up a WebView to allow for the user to authenticate and tell
     *         the server that they want to use our app.
     */
    @Override
    public boolean needsFullPermission() {
        // When the refresh token is null, we need to ask for full permission.
        return refreshToken == null;
    }

    /**
     * Returns true if we need to remind the server that we are actually need access to the API
     * again.
     *
     * @return True if Reddit needs a reminder that we exist.
     */
    @Override
    public boolean needsTokenRefresh() {
        // When the token is null and the refresh token isn't, we only need to "remind" the server

        // Expires = 0 when we have not set an expiry time (this shouldn't happen, but it's safer
        // if we ask for a refresh)
        return expires == 0 ||
                // token == null means that we do not have a token from the server.
                // refreshToken != null means that there exists a token which we can use to refresh.
                (token == null && refreshToken != null) ||
                // token != null means we have a token
                // refreshToken != null means that we have a refresh token.
                // The third conditional means that our current token has expired.
                (token != null && refreshToken != null && System.currentTimeMillis() > expires);
    }

    /**
     * Refreshes permission from the Reddit server.
     *
     * @param c The context in which the request was made.
     * @return If the refresh was successful.
     */
    private boolean refreshPermissions(Context c) {
        if (!refreshing) {
            refreshing = true;
            try {
                HttpClient client = new DefaultHttpClient();

                List<NameValuePair> params = new ArrayList<>(3);

                // We put in the post params
                params.add(new BasicNameValuePair("grant_type", "refresh_token"));
                params.add(new BasicNameValuePair("refresh_token", refreshToken));

                HttpPost post = new HttpPost("https://www.reddit.com/api/v1/access_token");
                post.setEntity(new UrlEncodedFormEntity(params));

                setupBasicAuthHeaders(post, c);
                HttpResponse response = client.execute(post);

                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    // Err on the side of caution.
                    long beforeRequestTime = System.currentTimeMillis();
                    InputStream stream = entity.getContent();
                    String result = BasicUtils.convertToString(stream);
                    JSONObject object = new JSONObject(result);
                    token = object.getString("access_token");
                    // (*1000) because we need to convert to milliseconds.
                    expires = beforeRequestTime + (object.getLong("expires_in")*1000);
                    putTokens();
                    return true;
                }
            } catch (IOException | JSONException e) {

                e.printStackTrace();
            } finally {
                refreshing = false;
            }
        }

        return false;
    }

    /**
     * Refresh the tokens.
     *
     * @param context The context in which the request was made.
     */
    @Override
    public void refreshTokens(Context context, AfterCallTask<Void> after) {
        new RefreshTokensTask(after).execute(context);
    }

    private class RefreshTokensTask extends AsyncTask<Context, Void, Boolean> {
        private AfterCallTask<Void> after;

        public RefreshTokensTask(AfterCallTask<Void> after) {
            this.after = after;
        }

        @Override
        protected Boolean doInBackground(Context... context) {
            return refreshPermissions(context[0]);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                if (after != null) {
                    after.run(null);
                }
            }
            else {
                if (after != null) {
                    after.fail("Cannot refresh token.");
                }
            }
        }
    }

    private class GetTokensTask extends AsyncTask<String, Void, Boolean> {
        private final WebView web;

        public GetTokensTask(WebView web) {
            this.web = web;
        }

        @Override
        protected Boolean doInBackground(String... code) {
            return RedditAPIHelper.this.getFirstToken(code[0], web.getContext());
        }

        @Override
        protected void onPostExecute(Boolean returnValue) {
            if (returnValue) {
                // Go back to the main activity :D
                Toast.makeText(web.getContext(), "Login successful!", Toast.LENGTH_LONG).show();
                ((Activity)(web.getContext())).setResult(Activity.RESULT_OK);
                ((Activity)(web.getContext())).finish();
            }
            else {
                // Try again...
                Toast.makeText(web.getContext(), "Authentication failed... :( Trying again.", Toast.LENGTH_LONG).show();
                pointWeb(web);
            }
        }
    }
}
