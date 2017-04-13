package com.willblaschko.android.alexa;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import okhttp3.HttpUrl;

/**
 * Created by ggec on 2017/4/13.
 */

public class SharedPreferenceUtil {
    private final static String PREF = "SharedPreferenceUtil";

    public static String getEndPointUrl(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String endPoint = sp.getString("endpoint", null);

        if (!TextUtils.isEmpty(endPoint)) {
            HttpUrl httpUrl = HttpUrl.parse(endPoint);
            if (httpUrl.isHttps() && (httpUrl.host().contains("avs") || httpUrl.host().contains("alexa") || httpUrl.host().contains("amazon"))) {
                return endPoint;
            }
        }
        return context.getString(R.string.alexa_api);
    }

    public static boolean putEndPoint(Context context, String endPoint) {
        if (!TextUtils.isEmpty(endPoint)) {
            HttpUrl httpUrl = HttpUrl.parse(endPoint);
            if (httpUrl.isHttps() && (httpUrl.host().contains("avs") || httpUrl.host().contains("alexa") || httpUrl.host().contains("amazon"))) {
                context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                        .edit()
                        .putString("endpoint", endPoint)
                        .apply();

                return true;
            }
        }
        return false;
    }
}
