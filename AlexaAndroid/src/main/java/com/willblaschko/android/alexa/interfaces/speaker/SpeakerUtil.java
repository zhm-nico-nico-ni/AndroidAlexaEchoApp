package com.willblaschko.android.alexa.interfaces.speaker;

import android.content.Context;
import android.media.AudioManager;
import android.util.Pair;

import com.ggec.voice.toollibrary.log.Log;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.data.Event;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.utility.Util;

public class SpeakerUtil {
    private static final String TAG = "SpeakerUtil";
    public static int VOLUME = -1;

    public static void setVolume(Context context, AlexaManager alexaManager, final long volume, final boolean adjust, AsyncCallback<AvsResponse, Exception> callback) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long oldVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        long vol = 0;
        if (adjust) {
            vol = oldVolume + volume * max / 100;
        } else {
            vol = volume * max / 100;
        }
        if(vol<0) vol = 0;

        if(oldVolume != vol) {
            Log.d(TAG, "Volume set to : " + vol + "/" + max + " (" + volume + ") adjust:" + adjust);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) vol, AudioManager.FLAG_SHOW_UI);
        } else {
            Log.d(TAG, "just send sendVolumeChangedEvent"+ vol + "/" + max + " (" + volume + ") adjust:" + adjust);
            alexaManager.sendEvent(Event.getVolumeChangedEvent(volume, vol == 0), callback);
        }
    }

    public static void sendVolumeEvent(Context context, AlexaManager alexaManager, AsyncCallback<AvsResponse, Exception> callback){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        alexaManager.sendEvent(Event.getVolumeChangedEvent((vol * 100 ) / max, vol == 0), callback);
    }


    public static void setMute(Context context, AlexaManager alexaManager, final boolean isMute, AsyncCallback<AvsResponse, Exception> callback){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        long localVolume = (vol * 100) / max;

        boolean localIsMute = localVolume == 0;
        if(localIsMute != isMute) {
            if(localIsMute){
                am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (max * 0.4f), 0);
            } else {
                am.setStreamMute(AudioManager.STREAM_MUSIC, isMute);
            }
            Log.i(TAG, "Mute set to : "+isMute);
            Util.getPreferences(context).edit().putLong("lastSetMuteTime", System.currentTimeMillis()).apply();
        } else {
            Log.i(TAG, "just sendMutedEvent : "+isMute);
            alexaManager.sendEvent(Event.getMuteChangeEvent(isMute, localVolume), callback);
        }
    }

    public static void sendMuteEvent(Context context, AlexaManager alexaManager, AsyncCallback<AvsResponse, Exception> callback){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        long localVolume = (vol * 100) / max;
        alexaManager.sendEvent(Event.getMuteChangeEvent(localVolume == 0, localVolume), callback);
    }

    public static long getAlexaVolume(Context context){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        return vol* 100 / max ;
    }

    public static void setConvertVolumeState(Context context){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        VOLUME = am.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public static Pair<Long, Boolean> getConvertVolumeState(Context context){
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        long vol;
        if(VOLUME > -1){
            vol = VOLUME;
        } else {
            vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            Log.w(TAG, "getConvertVolumeState getStreamVolume");
        }

        long av = vol* 100 / max;
        boolean ismute = vol <= 0;
        Log.d(TAG, "getConvertVolumeState v:"+av +" mute:"+ismute);
        return new Pair<>(av, ismute) ;
    }
}
