package com.ggec.voice.assistservice.sub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.toollibrary.log.Log;

/**
 * Created by ggec on 2017/4/14.
 */

public class CommonBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            Log.d("CommonBroadcastReceiver", "CONNECTIVITY_CHANGE");
            context.startService(BackGroundProcessServiceControlCommand.createIntentByType(context, BackGroundProcessServiceControlCommand.NETWORK_CONNECT));
        }
    }
}
