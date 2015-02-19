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
    public static final String POSTS_CREATE = "CREATE TABLE " + TABLE_POSTS + "(" + POST_THINGID + " " +
            "TEXT, " + POST_TITLE + " TEXT, " + POST_TITLE + " TEXT, " + POST_SUBREDDIT + "TEXT);";

    // Instance
    // Start the instance off as null.
    public static DatabaseHelper instance = null;

    // Database
    public final SQLiteDatabase db;

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

        while (!cursor.isAfterLast()) {
            posts.add(new Post(
                    cursor.getString(cursor.getColumnIndex(POST_TITLE)),
                    cursor.getString(cursor.getColumnIndex(POST_SUBREDDIT)),
                    cursor.getString(cursor.getColumnIndex(POST_URL)),
                    cursor.getString(cursor.getColumnIndex(POST_THINGID))
                    ));
        }

        return posts;
    }

    public void putPost(Post p) {
        ContentValues cv = new ContentValues();
        cv.put(POST_TITLE, p.getTitle());
        cv.put(POST_SUBREDDIT, p.getSubreddit());
        cv.put(POST_URL, p.getURL());
        cv.put(POST_THINGID, p.getThingId());

        db.insert(TABLE_POSTS, null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
    }
}
