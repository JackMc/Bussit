package me.jackmccracken.bussit.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import me.jackmccracken.bussit.models.Post;

/**
 * Created by jack on 19/02/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // Database information.
    public static final String DB_NAME = "reddit.db";
    public static final int DB_VERSION = 1;

    // Posts table
    public static final String TABLE_POSTS = "posts";
    public static final String POST_THINGID = "thingid";
    public static final String POST_TITLE = "title";
    public static final String POST_SUBREDDIT = "subreddit";
    private static final String POST_URL = "url";
    private static final String POST_CONTENT = "content";
    private static final String POST_LASTMOD = "lastmod";
    public static final String POSTS_CREATE = "CREATE TABLE " + TABLE_POSTS + "(" + POST_THINGID + " " +
            "TEXT, " + POST_TITLE + " TEXT, " + POST_SUBREDDIT + " TEXT, " + POST_URL + " TEXT," +
            POST_CONTENT + " TEXT, " + POST_LASTMOD + " INT" + ");";

    // Instance
    // Start the instance off as null.
    public static DatabaseHelper instance = null;

    // Database
    private final SQLiteDatabase db;

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);

        this.db = this.getWritableDatabase();
    }

    /**
     * Gets the currently saved instance of DatabaseHelper. Unexpected behaviour may occur if this
     * instance is not associated with the currently running activity.
     *
     * @return The currently saved instance of DatabaseHelper.
     */
    public static DatabaseHelper getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Database not initialized. Call makeInstance(Context)" +
                    " first.");
        }

        return instance;
    }

    /**
     * Creates a database helper with the given context. This helper will be saved for all future
     * calls to the getInstance() method. This method should be called in each new context.
     *
     * @param context The context in which to create the database instance.
     * @return The instance which was created.
     */
    public static DatabaseHelper makeInstance(Context context) {
        instance = new DatabaseHelper(context);

        return instance;
    }

    private void makeTables(SQLiteDatabase db) {
        db.execSQL(POSTS_CREATE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        makeTables(db);
    }

    public List<Post> getPosts() {
        Cursor cursor = db.query(TABLE_POSTS, null, null, null, null, null, null);

        List<Post> posts = new ArrayList<>();

        cursor.moveToNext();

        while (!cursor.isAfterLast()) {
            posts.add(new Post(
                    cursor.getString(cursor.getColumnIndex(POST_TITLE)),
                    cursor.getString(cursor.getColumnIndex(POST_SUBREDDIT)),
                    cursor.getString(cursor.getColumnIndex(POST_URL)),
                    cursor.getString(cursor.getColumnIndex(POST_THINGID)),
                    cursor.getString(cursor.getColumnIndex(POST_CONTENT)),
                    cursor.getLong(cursor.getColumnIndex(POST_LASTMOD))
                    ));
            cursor.moveToNext();
        }

        cursor.close();

        return posts;
    }

    private ContentValues postToCv(Post p) {
        ContentValues cv = new ContentValues();

        cv.put(POST_TITLE, p.getTitle());
        cv.put(POST_SUBREDDIT, p.getSubreddit());
        cv.put(POST_URL, p.getURL());
        cv.put(POST_THINGID, p.getThingId());
        cv.put(POST_CONTENT, p.getContent());
        cv.put(POST_LASTMOD, p.getLastMod());

        return cv;
    }

    public void putPost(Post p) {
        Cursor c = db.query(TABLE_POSTS, new String[] {}, "? = ?",
                            new String[] {POST_THINGID, p.getThingId()}, null, null, null);
        if (c.getCount() == 0) {
            // New post
            db.insert(TABLE_POSTS, null, postToCv(p));
        }
        else {
            updatePost(p);
        }

        c.close();
    }

    public void putPosts(List<Post> posts) {
        for (Post p : posts) {
            putPost(p);
        }
    }

    public void updatePost(Post p) {
        db.update(TABLE_POSTS, postToCv(p), "? = ?", new String[] {POST_THINGID, p.getThingId()});
    }

    public void updatePosts(List<Post> posts) {
        for (Post p : posts) {
            try {
                db.beginTransaction();
                updatePost(p);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * Makes a best effort to make sure that cached content is reused from old sites
     *
     * @param title See {@link Post}
     * @param subreddit See {@link Post}
     * @param url See {@link Post}
     * @param thingId See {@link Post}
     * @return A Post, potentially with old content.
     */
    public Post makePost(String title, String subreddit, String url, String thingId) {
        // So first we should check for an existing Post
        // We consider the thingId to be the unique element of a Post soooo...
        Cursor c = db.query(TABLE_POSTS, new String[] {POST_CONTENT, POST_LASTMOD},
                                "? = ?", new String[] {POST_THINGID, thingId}, null, null, null);

        String content = null;
        long lastMod = -1;

        // Grab the number of rows that were returned.
        int count = c.getCount();

        // There may be some content we can grab
        if (count == 1) {
            // See above for why these are indexed like this (see string array)
            content = c.getString(0);
            lastMod = c.getLong(1);
        }
        else if (count > 1) {
            // Having more than one of the same post is a major error and we need to throw an exception
            throw new IllegalStateException("Database is not in correct state. We need to only have one" +
                    " post per thingId");
        }

        // Now all the complicated stuff is out of the way and all we need to do is set up the
        // actual Post object.
        return new Post(title, subreddit, url, thingId, content, lastMod);
    }

    public void clearPosts() {
        db.execSQL("DELETE FROM " + TABLE_POSTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
