package com.ggec.voice.assistservice.sub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.interfaces.speaker.SpeakerUtil;

/**
 * Created by ggec on 2017/4/12.
 */

public class VolumeChangeBroadcastReceiver extends BroadcastReceiver {
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static Runnable setVolumeChange = new Runnable() {
        @Override
        public void run() {
            long cv = SpeakerUtil.getAlexaVolume(MyApplication.getContext());
            Log.i(Log.TAG_APP, "VolumeChangeBroadcastReceiver receive. cv="+cv + " r:"+setVolumeChange+ " h:"+sMainHandler);
            MyApplication.getContext()
                    .startService(BackGroundProcessServiceControlCommand
                            .createVolumeChangeIntent(cv));

        }
    };
    @Override
    public void onReceive(Context context, Intent intent) {
        if(AudioManager.STREAM_MUSIC ==intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", 0)) {
            sMainHandler.removeCallbacks(setVolumeChange);
            sMainHandler.postDelayed(setVolumeChange, 3000); //这里由于系统发broadcast并不快，建议至少等2s
        }
    }
}
