package com.ggec.voice.assistservice;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.ggec.voice.assistservice.connectlink.DeviceLinkHandler;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.wakeword.IWakeWordAgentEvent;
import com.ggec.voice.assistservice.wakeword.WakeWordAgent;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.BroadCast;

/**
 * Created by ggec on 2017/3/29.
 */

public class AssistService extends Service implements IWakeWordAgentEvent {
    private static final String TAG = "AssistService";

    private WakeWordAgent mWakeWordAgent;
    private DeviceLinkHandler mDeviceLinkHandler;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive RECEIVE_START_WAKE_WORD_LISTENER");
//            mWakeWordAgent.continueSearch();
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "AssistService # onCreate");
        startForeground(21940, new Notification.Builder(this)
                .setContentText("GGEC Assist Service")
                .build());

//        mWakeWordAgent = new CumSphinxWakeWordAgent(this, this);
//        mWakeWordAgent.continueSearch();

        mDeviceLinkHandler = new DeviceLinkHandler();
        registerReceiver(receiver, new IntentFilter(BroadCast.RECEIVE_START_WAKE_WORD_LISTENER));
    }

    @Override
    public void onDetectWakeWord() {
        Log.w(TAG, "onDetectWakeWord");

        startService(
                BackGroundProcessServiceControlCommand.createIntentByType(this,
                        BackGroundProcessServiceControlCommand.START_VOICE_RECORD)
        );
    }
}
