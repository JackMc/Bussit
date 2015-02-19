package me.jackmccracken.bussit.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import me.jackmccracken.bussit.R;
import me.jackmccracken.bussit.models.Post;

/**
 * Created by jack on 05/02/15.
 */
public class RedditAPIHelper {
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


    private String stateString;

    /**
     * Initializes a Reddit API helper.
     * @param refreshToken The token given to us by the server for when we need to refresh our token
     *                     (every hour). If this is null, we assume we will need an auth request.
     */
    public RedditAPIHelper(String refreshToken) {
        this.refreshToken = refreshToken;
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

        this.stateString = builder.toString();

        return stateString;
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
                        //TODO: Do this in another thread
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
        if (this.expires < System.currentTimeMillis()) {
            refreshPermissions(c);
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
        Uri.Builder builder = new Uri.Builder().scheme("https")
                                .authority("oauth.reddit.com")
                                .path(endpoint);
        for (NameValuePair p : params) {
            builder.appendQueryParameter(p.getName(), p.getValue());
        }

        return builder.toString();
    }

    private void enqueueTask(AsyncTask<Void, Void, ?> task) {
        ops.add(task);
    }

    /**
     * Defines a task which will be executed <b>on the main thread</b> after the execution of a
     * Reddit API call.
     * @param <T> The data to be returned from the server.
     */
    public interface AfterCallTask<T> {
        /**
         * Called after a success.
         * @param param A function-defined parameter.
         */
        public void run(T param);

        /**
         * Called if the operation fails.
         *
         * @param message A user-readable message to be displayed if it is appropriate.
         */
        public void fail(String message);
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
                System.out.println(postData.toString());
                posts.add(new Post(postData.getString("title"),
                                   "/r/" + postData.getString("subreddit"),
                                   postData.getString("url")));
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
                        JSONObject object = new JSONObject(convertToString(entity.getContent()));

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
                String result = convertToString(stream);
                JSONObject object = new JSONObject(result);
                token = object.getString("access_token");
                refreshToken = object.getString("refresh_token");
                expires = System.currentTimeMillis() + object.getLong("expires_in");
                return true;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    private String convertToString(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return builder.toString();
    }

    /**
     * Returns true if we need to get permission from the Reddit server, and require user interaction
     * to do so.
     *
     * @return True if we need to pull up a WebView to allow for the user to authenticate and tell
     *         the server that they want to use our app.
     */
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
    public boolean needsTokenRefresh() {
        // When the token is null and the refresh token isn't, we only need to "remind" the server
        return token == null && refreshToken != null;
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
                    InputStream stream = entity.getContent();
                    String result = convertToString(stream);
                    JSONObject object = new JSONObject(result);
                    token = object.getString("access_token");
                    expires = System.currentTimeMillis() + object.getLong("expires_in");
                    return true;
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            refreshing = false;
        }

        return false;
    }

    private class RefreshTokensTask extends AsyncTask<Context, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Context... context) {
            return refreshPermissions(context[0]);
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
