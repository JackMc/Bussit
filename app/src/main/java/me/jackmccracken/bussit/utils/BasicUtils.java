package me.jackmccracken.bussit.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

    public static boolean isWifiConnected(Context c) {
        ConnectivityManager manager = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        return isWifiConnected(manager);
    }

    public static boolean isWifiConnected(ConnectivityManager manager) {
        return manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState()
                == NetworkInfo.State.CONNECTED ||
                manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED;
    }

    public static String convertToString(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
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
}
