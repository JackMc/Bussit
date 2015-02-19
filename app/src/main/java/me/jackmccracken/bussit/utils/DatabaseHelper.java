package me.jackmccracken.bussit.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jack on 19/02/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // Database information.
    public static final String DB_NAME = "reddit.db";
    public static final int DB_VERSION = 1;

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

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        makeTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
