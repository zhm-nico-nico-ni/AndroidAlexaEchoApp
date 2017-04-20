package com.ggec.voice.toollibrary.log;


import com.ggec.voice.toollibrary.BuildConfig;

/**
 * Created by ggec on 2017/3/29.
 */

public final class Log {
    public static final String TAG_APP = BuildConfig.APPLICATION_ID;

    private final static boolean Debug = true;
    public static int LOG_LEVEL = Debug ? android.util.Log.DEBUG : android.util.Log.INFO;
    
    public static void d(String tag, String msg){
        if(android.util.Log.DEBUG >= LOG_LEVEL){
            android.util.Log.d(tag, msg);
        }
    }
    public static void i(String tag, String msg) {
        if(android.util.Log.INFO >= LOG_LEVEL){
            android.util.Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if(android.util.Log.WARN >= LOG_LEVEL){
            android.util.Log.w(tag, msg);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        if(android.util.Log.WARN >= LOG_LEVEL){
            android.util.Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if(android.util.Log.ERROR >= LOG_LEVEL){
            android.util.Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if(android.util.Log.ERROR >= LOG_LEVEL){
            android.util.Log.e(tag, msg);
        }
    }
}
