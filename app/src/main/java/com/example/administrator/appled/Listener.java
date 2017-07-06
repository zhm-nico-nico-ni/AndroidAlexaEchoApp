package com.example.administrator.appled;

import android.content.Intent;

import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.toollibrary.log.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class Listener {
    private static String TAG = "Listener";
    public static AtomicBoolean canDetect = new AtomicBoolean(true);

    public Listener() {

    }

    public void notice() {

        if(canDetect.getAndSet(false)) {
            Log.w(TAG, "onDetectWakeWord");
            Intent it = BackGroundProcessServiceControlCommand.createIntentByType(MyApplication.getContext(),
                    BackGroundProcessServiceControlCommand.START_VOICE_RECORD);
//        if (!TextUtils.isEmpty(rawPath)) {
//            it.putExtra("initiator", new Initiator("WAKEWORD", startIndexInSamples, endIndexInSamples).toJson());
//            it.putExtra("rawPath", rawPath);
//        }
            MyApplication.getContext().startService(it);
            LedControl.myLedCtl(1);

        }
    }
}