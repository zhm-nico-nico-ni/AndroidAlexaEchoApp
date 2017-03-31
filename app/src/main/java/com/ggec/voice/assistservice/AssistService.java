package com.ggec.voice.assistservice;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by ggec on 2017/3/29.
 */

public class AssistService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startForeground(21940, new Notification.Builder(this)
                .setContentText("GGEC Assist Service")
                .build());
    }
}
