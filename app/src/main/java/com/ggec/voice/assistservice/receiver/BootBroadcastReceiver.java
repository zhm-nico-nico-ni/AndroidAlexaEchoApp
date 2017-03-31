package com.ggec.voice.assistservice.receiver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.ggec.voice.assistservice.sub.MyBootIntentService;


/**
 * Created by ggec on 2017/3/29.
 */

public class BootBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, MyBootIntentService.class);
        startWakefulService(context, startServiceIntent);
    }
}
