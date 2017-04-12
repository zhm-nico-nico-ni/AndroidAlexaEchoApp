package com.ggec.voice.assistservice.speaker;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

/**
 * Created by ggec on 2017/4/12.
 */

public class VolumeUtil {
    private static final String TAG = "VolumeUtil";

    public static void setVolume(Context context, AlexaManager alexaManager, final long volume, final boolean adjust, AsyncCallback<AvsResponse, Exception> callback) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (adjust) {
            vol += volume * max / 100;
        } else {
            vol = volume * max / 100;
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) vol, AudioManager.FLAG_SHOW_UI);

        alexaManager.sendVolumeChangedEvent(volume, vol == 0, callback);

        Log.d(TAG, "Volume set to : " + vol + "/" + max + " (" + volume + ") adjust:" + adjust);
    }

    public static void setMute(Context context, AlexaManager alexaManager, final boolean isMute, AsyncCallback<AvsResponse, Exception> callback){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamMute(AudioManager.STREAM_MUSIC, isMute);

        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);

        alexaManager.sendMutedEvent(isMute, (vol* 100) / max , callback);

        Log.i(TAG, "Mute set to : "+isMute);

    }

    public static long getAlexaVolume(Context context){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        return vol* 100 / max ;
    }
}
