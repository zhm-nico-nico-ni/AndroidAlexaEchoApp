package com.ggec.voice.assistservice;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.facebook.stetho.Stetho;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;

import ai.kitt.snowboy.AppResCopy;

/**
 * Created by ggec on 2017/3/29.
 */

public class MyApplication extends Application {

    private static Context sContext;
    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        AppResCopy.copyResFromAssetsToSD(this);
        Stetho.initializeWithDefaults(this);
        MultiDex.install(this);
        startService(BackGroundProcessServiceControlCommand.createIntentByType(this, BackGroundProcessServiceControlCommand.NETWORK_CONNECT));
        startService(BackGroundProcessServiceControlCommand.createIntentByType(this, BackGroundProcessServiceControlCommand.USER_INACTIVITY_REPORT));
    }

    public static Context getContext(){
        return sContext;
    }
}
