package com.ggec.voice.assistservice;

import android.app.Application;
import android.content.Context;

import com.facebook.stetho.Stetho;

/**
 * Created by ggec on 2017/3/29.
 */

public class MyApplication extends Application {

    private static Context sContext;
    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;

        Stetho.initializeWithDefaults(this);
    }

    public static Context getContext(){
        return sContext;
    }
}
