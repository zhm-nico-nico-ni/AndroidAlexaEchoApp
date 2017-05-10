package com.willblaschko.android.alexa;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.willblaschko.android.alexa.utility.Util;

import okhttp3.HttpUrl;


/**
 * Created by ggec on 2017/4/13.
 */

public class SharedPreferenceUtil {

    public static String getEndPointUrl(Context context) {
        SharedPreferences sp = Util.getPreferences(context);
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
                Util.getPreferences(context)
                        .edit()
                        .putString("endpoint", endPoint)
                        .apply();

                return true;
            }
        }
        return false;
    }

    public static String getStringByKey(Context context, String key, String defVal) {
        SharedPreferences sp = Util.getPreferences(context);
        return sp.getString(key, defVal);
    }

    public static void putString(Context context, String key, String value) {
        SharedPreferences.Editor editor = Util.getPreferences(context).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static boolean contains(Context context, String key){
        return Util.getPreferences(context).contains(key);
    }

    public static void putAuthToken(Context context, String accessToken, String refreshToken, long expires_in) {
        SharedPreferences.Editor preferences = Util.getPreferences(context.getApplicationContext()).edit();
        preferences.putString(TokenManager.PREF_ACCESS_TOKEN, accessToken);
        preferences.putString(TokenManager.PREF_REFRESH_TOKEN, refreshToken);
        //comes back in seconds, needs to be milis
        preferences.putLong(TokenManager.PREF_TOKEN_EXPIRES, (System.currentTimeMillis() + expires_in * 1000));
        preferences.commit();
    }

    public static boolean putAuthToken2(Context context, String clientId, String codeVerify, String accessToken, String refreshToken, long expires_in) {
        SharedPreferences.Editor preferences = Util.getPreferences(context.getApplicationContext()).edit();
        preferences.putString(TokenManager.PREF_ACCESS_TOKEN, accessToken);
        preferences.putString(TokenManager.PREF_REFRESH_TOKEN, refreshToken);
        preferences.putString(TokenManager.PREF_CLIENT_ID, clientId);
        preferences.putString(AuthorizationManager.CODE_VERIFIER, codeVerify);
        //comes back in seconds, needs to be milis
        preferences.putLong(TokenManager.PREF_TOKEN_EXPIRES, (System.currentTimeMillis() + expires_in * 1000));
        return preferences.commit();
    }
}
