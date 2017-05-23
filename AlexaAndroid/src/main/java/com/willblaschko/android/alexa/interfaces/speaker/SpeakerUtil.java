package com.willblaschko.android.alexa.interfaces.speaker;

import android.content.Context;
import android.media.AudioManager;
import android.util.Pair;

import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

/**
 * Created by ggec on 2017/4/12.
 */

public class SpeakerUtil {
    private static final String TAG = "SpeakerUtil";

    public static void setVolume(Context context, AlexaManager alexaManager, final long volume, final boolean adjust, AsyncCallback<AvsResponse, Exception> callback) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (adjust) {
            vol += volume * max / 100;
        } else {
            vol = volume * max / 100;
        }
        if(vol<0) vol = 0;
        am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) vol, 0);
        Log.d(TAG, "Volume set to : " + vol + "/" + max + " (" + volume + ") adjust:" + adjust);
        alexaManager.sendVolumeChangedEvent(vol *100 / max, vol == 0, callback);
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

    public static Pair<Long, Boolean> getConvertVolumeState(Context context){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        long av = vol* 100 / max;
        boolean ismute = am.getStreamVolume(AudioManager.STREAM_MUSIC) >= 0;
        Log.d(TAG, "getConvertVolumeState v:"+av +" mute:"+ismute);
        return new Pair<>(av, ismute) ;
    }
}
