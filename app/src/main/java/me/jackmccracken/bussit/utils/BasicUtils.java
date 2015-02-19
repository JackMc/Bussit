package me.jackmccracken.bussit.utils;

import android.util.Log;

/**
 * Created by jack on 12/02/15.
 */
public class BasicUtils {
    private static final int LOG_LIMIT = 4000;

    private BasicUtils() {}

    /**
     * Logs a long string to logcat by splitting it into many strings.
     */
    public static void logLong(int priority, String tag, String str) {
        int count = 0;

        while (count < str.length()) {
            int endIndex = count + LOG_LIMIT + 1;
            if (endIndex > str.length()) {
                endIndex = str.length();
            }

            Log.println(priority, tag, str.substring(count, endIndex));
            count += LOG_LIMIT;
        }
    }
}
