package com.willblaschko.android.alexa.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.willblaschko.android.alexa.datepersisted.MultiprocessSharedPreferences;

import java.util.UUID;

/**
 * A collection of utility functions.
 *
 * @author wblaschko on 8/13/15.
 */
public class Util {
    private static SharedPreferences mPreferences;

    /**
     * Show an authorization toast on the main thread to make sure the user sees it
     * @param context local context
     * @param message the message to show the user
     */
    public static void showAuthToast(final Context context, final String message){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast authToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                authToast.show();
            }
        });
    }


    /**
     * Get our default shared preferences
     * @param context local/application context
     * @return default shared preferences
     */
    public static SharedPreferences getPreferences(Context context) {
        if (mPreferences == null) {
            mPreferences = MultiprocessSharedPreferences.getSharedPreferences(context, "PREF_DATA", Context.MODE_PRIVATE);
        }
        return mPreferences;
    }

    public static String getUuid(){
        return UUID.randomUUID().toString();
    }
}
