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
import android.text.TextUtils;

import com.ggec.voice.assistservice.audio.MyShortAudioPlayer;
import com.ggec.voice.assistservice.connectlink.DeviceLinkHandler;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.wakeword.CumSphinxWakeWordAgent;
import com.ggec.voice.assistservice.wakeword.IWakeWordAgentEvent;
import com.ggec.voice.assistservice.wakeword.WakeWordAgent;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.BroadCast;
import com.willblaschko.android.alexa.data.message.request.speechrecognizer.Initiator;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;

/**
 * Created by ggec on 2017/3/29.
 */

public class AssistService extends Service implements IWakeWordAgentEvent, DeviceLinkHandler.IDeviceLinkCallback {
    private static final String TAG = "AssistService";

    private WakeWordAgent mWakeWordAgent;
    private DeviceLinkHandler mDeviceLinkHandler;
    private MyShortAudioPlayer mMyShortAudioPlayer;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive RECEIVE_START_WAKE_WORD_LISTENER");

            if (null != mWakeWordAgent) mWakeWordAgent.continueSearch();
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
        SpeakerUtil.setConvertVolumeState(this);
        startForeground(21940, new Notification.Builder(this)
                .setContentText("GGEC Assist Service")
                .build());
        onConnectingWifi();
        if (null != mWakeWordAgent) mWakeWordAgent.continueSearch();

        mDeviceLinkHandler = new DeviceLinkHandler(this);
        registerReceiver(receiver, new IntentFilter(BroadCast.RECEIVE_START_WAKE_WORD_LISTENER));
        mMyShortAudioPlayer = new MyShortAudioPlayer("asset:///start.mp3");

        startService(
                BackGroundProcessServiceControlCommand.createIntentByType(this,
                        BackGroundProcessServiceControlCommand.LOAD_ALARM)
        );
    }

    @Override
    public void onDetectWakeWord(final String rawPath, final long startIndexInSamples, final long endIndexInSamples) {
        MyApplication.mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "onDetectWakeWord");
                playStart(new MyShortAudioPlayer.IOnCompletionListener() {
                    @Override
                    public void onCompletion() {
                        Intent it = BackGroundProcessServiceControlCommand.createIntentByType(AssistService.this,
                                BackGroundProcessServiceControlCommand.START_VOICE_RECORD);
                        if (!TextUtils.isEmpty(rawPath)) {
                            it.putExtra("initiator", new Initiator("WAKEWORD", startIndexInSamples, endIndexInSamples).toJson());
                            it.putExtra("rawPath", rawPath);
                        }
                        startService(it);
                    }
                });
            }
        });
    }


    @Override
    public void onConnectingWifi() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
            mWakeWordAgent = new CumSphinxWakeWordAgent(this, this);
//        mWakeWordAgent = new SnowboyWakeWordAgent(this, this);
        }
    }

    private void playStart(final MyShortAudioPlayer.IOnCompletionListener listener) {
        mMyShortAudioPlayer.play(listener);
    }
}
