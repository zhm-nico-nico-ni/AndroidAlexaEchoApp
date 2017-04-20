package com.ggec.voice.assistservice.sub;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.ggec.voice.assistservice.AssistService;
import com.ggec.voice.toollibrary.log.Log;

/**
 * Created by ggec on 2017/3/29.
 */

public class MyBootIntentService extends IntentService {
    public MyBootIntentService(){
        super("boot");
    }
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MyBootIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.e("MyBootIntentService", " MyBootIntentService onHandleIntent : "+ intent);
        startService(new Intent(this, AssistService.class));

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }
}
