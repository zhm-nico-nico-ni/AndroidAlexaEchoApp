package com.ggec.voice.assistservice;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.ggec.voice.assistservice.connectlink.DeviceLinkHandler;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.wakeword.CumSphinxWakeWordAgent;
import com.ggec.voice.assistservice.wakeword.IWakeWordAgentEvent;
import com.ggec.voice.assistservice.wakeword.WakeWordAgent;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.BroadCast;

/**
 * Created by ggec on 2017/3/29.
 */

public class AssistService extends Service implements IWakeWordAgentEvent ,DeviceLinkHandler.IDeviceLinkCallback {
    private static final String TAG = "AssistService";

    private WakeWordAgent mWakeWordAgent;
    private DeviceLinkHandler mDeviceLinkHandler;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive RECEIVE_START_WAKE_WORD_LISTENER");

            if(null != mWakeWordAgent) mWakeWordAgent.continueSearch();
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
        onConnectingWifi();
        if(null != mWakeWordAgent) mWakeWordAgent.continueSearch();

        mDeviceLinkHandler = new DeviceLinkHandler(this);
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


    @Override
    public void onConnectingWifi() {
        if(PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            mWakeWordAgent = new CumSphinxWakeWordAgent(this, this);
//        mWakeWordAgent = new SnowboyWakeWordAgent(this, this);
        }
    }
}
