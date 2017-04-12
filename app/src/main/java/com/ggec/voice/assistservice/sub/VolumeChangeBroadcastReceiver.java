package com.ggec.voice.assistservice.sub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.ggec.voice.assistservice.MyApplication;
import com.ggec.voice.assistservice.data.BackGroundProcessServiceControlCommand;
import com.ggec.voice.assistservice.log.Log;
import com.ggec.voice.assistservice.speaker.VolumeUtil;

/**
 * Created by ggec on 2017/4/12.
 */

public class VolumeChangeBroadcastReceiver extends BroadcastReceiver {
    private static Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static Runnable setVolumeChange = new Runnable() {
        @Override
        public void run() {
            long cv = VolumeUtil.getAlexaVolume(MyApplication.getContext());
            Log.i(Log.TAG_APP, "VolumeChangeBroadcastReceiver receive. cv="+cv + " r:"+setVolumeChange+ " h:"+sMainHandler);
            MyApplication.getContext()
                    .startService(BackGroundProcessServiceControlCommand
                            .createVolumeChangeIntent(cv));

        }
    };
    @Override
    public void onReceive(Context context, Intent intent) {
        //// FIXME: 2017/4/12  这里有个问题，当AVS 推送服务设置音量后，这里也会再次触发，看看怎样处理
        sMainHandler.removeCallbacks(setVolumeChange);
        sMainHandler.postDelayed(setVolumeChange, 3000); //这里由于系统发broadcast并不快，建议至少等2s
    }
}
